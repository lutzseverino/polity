import { createHttpClient, hasHttpResponseStatus } from "@/api/http-client";
import type { Motion } from "@/domains/motion";
import {
  type GovernmentResponse,
  type MotionResponse,
  type OfficialRecordResponse,
  type PolityResponse,
  parseGovernment,
  parseMotionPage,
  parseMotionResponse,
  parseOfficialRecordPage,
  parsePolityActions,
  parsePolityPage,
  parsePolityResponse,
} from "@/domains/polity/api/polity-contract";
import type { Polity, PolitySummary } from "@/domains/polity/lib/polity";
import type { PageResult } from "@/lib/pagination";
import { ResourceNotFoundError } from "@/lib/resource-not-found";

const defaultPolityPageSize = 50;
const httpClient = createHttpClient();
export const maximumPolityPageSize = 100;

type RequestOptions = Readonly<{
  acceptedLanguage: string;
  signal?: AbortSignal;
}>;

type ListPolitiesOptions = RequestOptions &
  Readonly<{ page?: number; query?: string; size?: number }>;

export function normalizePolityQuery(query?: string) {
  const normalized = query?.trim();
  return normalized ? normalized : undefined;
}

export function normalizePolityPage(page?: number) {
  return Number.isSafeInteger(page) && page !== undefined && page >= 0
    ? page
    : 0;
}

export function normalizePolityPageSize(size?: number) {
  if (!Number.isSafeInteger(size) || size === undefined)
    return defaultPolityPageSize;
  return Math.min(Math.max(size, 1), maximumPolityPageSize);
}

function projectPolitySummary(response: PolityResponse): PolitySummary {
  return {
    constitutionVersion: response.constitutionVersion,
    id: response.id,
    institutionName: response.institutionName,
    name: response.name,
    status: response.status,
    visibility: response.visibility,
  };
}

function formatDateTime(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function thresholdLabel(
  threshold: GovernmentResponse["procedures"][number]["threshold"],
) {
  return {
    majority_of_eligible: "Majority of eligible members",
    office_election_result: "Election result",
    simple_majority_cast: "Simple majority of votes cast",
    two_thirds_cast: "Two thirds of votes cast",
    two_thirds_eligible: "Two thirds of eligible members",
  }[threshold];
}

function effectLabel(effectType: MotionResponse["effectType"]) {
  return {
    adopt_resolution: "Ordinary resolution",
    amend_constitution: "Constitutional amendment",
    apply_sanction: "Sanction",
    disband_polity: "Disbandment",
    elect_office: "Office election",
    grant_appeal: "Appeal",
    vacate_office_term: "Office term review",
    void_official_act: "Constitutional review",
  }[effectType];
}

function projectMotion(
  response: MotionResponse,
  government: GovernmentResponse,
  record: readonly OfficialRecordResponse[],
  locale: string,
): Motion {
  const procedure = government.procedures.find(
    ({ name }) => name === response.procedureName,
  );
  if (!procedure)
    throw new Error(
      "Motion procedure is missing from the polity government response.",
    );
  const tally = response.tally;
  const electionTally = response.electionTally;
  const participation = tally
    ? {
        cast: tally.yes + tally.no + tally.abstain,
        eligible: tally.eligible,
        quorumMet: tally.quorumMet,
        quorumRequired: tally.quorumRequired,
      }
    : electionTally
      ? {
          cast: electionTally.participation,
          eligible: electionTally.eligible,
          quorumMet: electionTally.quorumMet,
          quorumRequired: electionTally.quorumRequired,
        }
      : undefined;
  const recordEntry = record.find(
    ({ motionId }) => motionId === response.id,
  )?.entryNumber;
  const certification = response.certification;

  return {
    actionKind: response.officeElection ? "candidacy" : "vote",
    body: response.body,
    category: effectLabel(response.effectType),
    closesAtLabel: formatDateTime(response.votingClosesAt, locale),
    ...(response.currentVote ? { currentVote: response.currentVote } : {}),
    id: response.id,
    introducedBy: response.introducedByName,
    ...(participation ? { participation } : {}),
    procedure: {
      electorate:
        procedure.electorate === "active_members"
          ? `All ${government.activeMemberCount} active members`
          : "Current office holders",
      name: procedure.name,
      notice: `${procedure.minimumNoticeHours} hours`,
      threshold: thresholdLabel(procedure.threshold),
    },
    ...(certification && recordEntry
      ? {
          result: {
            no: certification.noCount ?? 0,
            outcome: certification.passed ? "Adopted" : "Rejected",
            recordEntry,
            yes: certification.yesCount ?? 0,
          },
        }
      : {}),
    status: response.status,
    title: response.title,
  };
}

async function requestUnknown(
  path: string,
  { acceptedLanguage, signal }: RequestOptions,
  params?: object,
) {
  return httpClient.request<unknown>({
    acceptedLanguage,
    method: "GET",
    params,
    signal,
    url: path,
  });
}

export async function listPolities({
  acceptedLanguage,
  page,
  query,
  signal,
  size,
}: ListPolitiesOptions): Promise<PageResult<PolitySummary>> {
  const response = parsePolityPage(
    await requestUnknown(
      "/polities",
      { acceptedLanguage, signal },
      {
        page: normalizePolityPage(page),
        query: normalizePolityQuery(query),
        size: normalizePolityPageSize(size),
      },
    ),
  );
  return { ...response, content: response.content.map(projectPolitySummary) };
}

async function getWorkspaceResources(
  polityId: string,
  options: RequestOptions,
) {
  const encodedId = encodeURIComponent(polityId);
  try {
    const [polity, actions, government, motions, record] = await Promise.all([
      requestUnknown(`/polities/${encodedId}`, options).then(
        parsePolityResponse,
      ),
      requestUnknown(`/polities/${encodedId}/actions`, options).then(
        parsePolityActions,
      ),
      requestUnknown(`/polities/${encodedId}/government`, options).then(
        parseGovernment,
      ),
      requestUnknown(`/polities/${encodedId}/motions`, options, {
        page: 0,
        size: 100,
      }).then(parseMotionPage),
      requestUnknown(`/polities/${encodedId}/record`, options, {
        page: 0,
        size: 100,
      }).then(parseOfficialRecordPage),
    ]);
    return {
      actions,
      government,
      motions: motions.content,
      polity,
      record: record.content,
    };
  } catch (error) {
    if (hasHttpResponseStatus(error, 404))
      throw new ResourceNotFoundError("Polity", polityId);
    throw error;
  }
}

export async function getPolity(
  polityId: string,
  options: RequestOptions,
): Promise<Polity> {
  const resources = await getWorkspaceResources(polityId, options);
  const motions = resources.motions.map((motion) =>
    projectMotion(
      motion,
      resources.government,
      resources.record,
      options.acceptedLanguage,
    ),
  );
  const attention: Polity["attention"] = [
    ...resources.motions
      .filter(
        ({ actions, status }) =>
          status === "voting" && actions.castVote.available,
      )
      .map((motion) => ({
        description: motion.body,
        dueLabel: `Closes ${formatDateTime(motion.votingClosesAt, options.acceptedLanguage)}`,
        id: `vote-${motion.id}`,
        kind: "vote" as const,
        target: { kind: "motion" as const, motionId: motion.id },
        title: `Vote on ${motion.title}`,
      })),
    ...resources.motions
      .filter(
        ({ actions, status }) =>
          status === "voting" && actions.respondCandidacy.available,
      )
      .map((motion) => ({
        description: `Confirm whether you accept your nomination${motion.officeElection ? ` for ${motion.officeElection.officeName}` : ""}.`,
        dueLabel: `Respond by ${formatDateTime(motion.votingClosesAt, options.acceptedLanguage)}`,
        id: `candidacy-${motion.id}`,
        kind: "candidacy" as const,
        target: { kind: "motion" as const, motionId: motion.id },
        title: "Respond to your nomination",
      })),
    ...(["ready", "disbanded"].includes(resources.actions.readiness.status)
      ? []
      : [
          {
            description: resources.actions.readiness.statusMessage,
            dueLabel: "No deadline",
            id: "complete-formation",
            kind: "formation" as const,
            target: { actionId: "invite-member", kind: "action" as const },
            title: "Finish forming the polity",
          },
        ]),
  ];

  return {
    attention,
    constitutionVersion: resources.polity.constitutionVersion,
    id: resources.polity.id,
    memberCount: resources.government.activeMemberCount,
    motions,
    name: resources.polity.name,
    readiness:
      resources.actions.readiness.status === "ready"
        ? "ready"
        : resources.actions.readiness.status === "disbanded"
          ? "unavailable"
          : "forming",
    readinessMessage: resources.actions.readiness.statusMessage,
    recentActivity: resources.record.map((entry) => ({
      id: entry.id,
      label: `Official record No. ${entry.entryNumber}`,
      timeLabel: formatDateTime(entry.occurredAt, options.acceptedLanguage),
      title: entry.title,
    })),
    status: resources.polity.status,
    visibility: resources.polity.visibility,
  };
}

export async function getPolityActions(
  polityId: string,
  options: RequestOptions,
) {
  try {
    return parsePolityActions(
      await requestUnknown(
        `/polities/${encodeURIComponent(polityId)}/actions`,
        options,
      ),
    );
  } catch (error) {
    if (hasHttpResponseStatus(error, 404))
      throw new ResourceNotFoundError("Polity actions", polityId);
    throw error;
  }
}

export async function listPolityMotionResponses(
  polityId: string,
  options: RequestOptions,
) {
  return parseMotionPage(
    await requestUnknown(
      `/polities/${encodeURIComponent(polityId)}/motions`,
      options,
      { page: 0, size: 100 },
    ),
  ).content;
}

export async function getPolityMotion(
  polityId: string,
  motionId: string,
  options: RequestOptions,
) {
  const encodedPolityId = encodeURIComponent(polityId);
  try {
    const [motion, government, record] = await Promise.all([
      requestUnknown(
        `/polities/${encodedPolityId}/motions/${encodeURIComponent(motionId)}`,
        options,
      ).then(parseMotionResponse),
      requestUnknown(`/polities/${encodedPolityId}/government`, options).then(
        parseGovernment,
      ),
      requestUnknown(`/polities/${encodedPolityId}/record`, options, {
        page: 0,
        size: 100,
      }).then(parseOfficialRecordPage),
    ]);
    return projectMotion(
      motion,
      government,
      record.content,
      options.acceptedLanguage,
    );
  } catch (error) {
    if (hasHttpResponseStatus(error, 404))
      throw new ResourceNotFoundError("Motion", motionId);
    throw error;
  }
}

export { parseMotionResponse };
