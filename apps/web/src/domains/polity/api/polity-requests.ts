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
import type {
  Polity,
  PolityGovernment,
  PolityOfficialRecordEntry,
  PolitySummary,
} from "@/domains/polity/lib/polity";
import type { PageResult } from "@/lib/pagination";
import { ResourceNotFoundError } from "@/lib/resource-not-found";

const defaultPolityPageSize = 50;
const httpClient = createHttpClient();
const maximumPolityPageSize = 100;

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
    slug: response.slug,
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
  threshold: GovernmentResponse["constitution"]["procedures"][number]["threshold"],
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
  const procedure = government.constitution.procedures.find(
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
    actionAvailability: response.officeElection
      ? response.actions.respondCandidacy
      : response.actions.castVote,
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
          ? `All ${government.formation.activeMemberCount} active members`
          : "Current office holders",
      name: procedure.name,
      notice: `${procedure.minimumNoticeHours} hours`,
      threshold: thresholdLabel(procedure.threshold),
    },
    ...(certification && recordEntry !== undefined
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

export function reconcileMotionResponse(
  motion: Motion,
  response: MotionResponse,
  locale: string,
): Motion {
  const {
    currentVote: _currentVote,
    participation: _participation,
    result: _result,
    ...stableMotion
  } = motion;
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
  const certification = response.certification;
  const result = certification &&
    motion.result && {
      no: certification.noCount ?? 0,
      outcome: certification.passed ? "Adopted" : "Rejected",
      recordEntry: motion.result.recordEntry,
      yes: certification.yesCount ?? 0,
    };

  return {
    ...stableMotion,
    actionAvailability: response.officeElection
      ? response.actions.respondCandidacy
      : response.actions.castVote,
    actionKind: response.officeElection ? "candidacy" : "vote",
    body: response.body,
    category: effectLabel(response.effectType),
    closesAtLabel: formatDateTime(response.votingClosesAt, locale),
    ...(response.currentVote ? { currentVote: response.currentVote } : {}),
    introducedBy: response.introducedByName,
    ...(participation ? { participation } : {}),
    ...(result ? { result } : {}),
    status: response.status,
    title: response.title,
  };
}

function projectGovernment(
  government: GovernmentResponse,
  actions: ReturnType<typeof parsePolityActions>,
  locale: string,
): PolityGovernment {
  return {
    constitution: {
      body: government.constitution.body,
      ratifiedAtLabel: formatDateTime(
        government.constitution.ratifiedAt,
        locale,
      ),
      title: government.constitution.title,
      version: government.constitution.version,
    },
    formation: government.formation,
    health: actions.constitutionalHealth,
    institutions: government.constitution.institutions,
    offices: government.constitution.offices,
    procedures: government.constitution.procedures,
    readiness: actions.readiness,
  };
}

function projectOfficialRecordEntry(
  entry: OfficialRecordResponse,
  locale: string,
): PolityOfficialRecordEntry {
  return {
    actorName: entry.actorName,
    body: entry.body,
    constitutionVersion: entry.constitutionVersion,
    entryNumber: entry.entryNumber,
    id: entry.id,
    ...(entry.motionId ? { motionId: entry.motionId } : {}),
    occurredAtLabel: formatDateTime(entry.occurredAt, locale),
    title: entry.title,
  };
}

function flattenCompletePageSet<Item>(
  pages: readonly PageResult<Item>[],
  message: string,
) {
  const firstPage = pages[0];
  if (!firstPage) throw new Error(message);
  const metadata = firstPage.page;
  const stablePageSet = pages.every(
    (page, index) =>
      page.page.number === index &&
      page.page.size === metadata.size &&
      page.page.totalElements === metadata.totalElements &&
      page.page.totalPages === metadata.totalPages,
  );
  const content = pages.flatMap((page) => page.content);
  if (!stablePageSet || content.length !== metadata.totalElements) {
    throw new Error(message);
  }
  return content;
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

export async function listAllPolities(options: RequestOptions) {
  const firstPage = await listPolities({
    ...options,
    page: 0,
    size: maximumPolityPageSize,
  });
  const remainingPages = await Promise.all(
    Array.from(
      { length: Math.max(firstPage.page.totalPages - 1, 0) },
      (_, index) =>
        listPolities({
          ...options,
          page: index + 1,
          size: maximumPolityPageSize,
        }),
    ),
  );
  return flattenCompletePageSet(
    [firstPage, ...remainingPages],
    "Invalid polity page response.",
  );
}

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu;
const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/u;

export async function getPolityReference(
  polityReference: string,
  options: RequestOptions,
): Promise<PolitySummary> {
  const byId = uuidPattern.test(polityReference);
  const bySlug =
    polityReference.length <= 80 && slugPattern.test(polityReference);
  if (!byId && !bySlug) {
    throw new ResourceNotFoundError("Polity", polityReference);
  }

  const path = byId
    ? `/polities/${encodeURIComponent(polityReference)}`
    : `/polities/by-slug/${encodeURIComponent(polityReference)}`;
  try {
    return projectPolitySummary(
      parsePolityResponse(await requestUnknown(path, options)),
    );
  } catch (error) {
    if (hasHttpResponseStatus(error, 404)) {
      throw new ResourceNotFoundError("Polity", polityReference);
    }
    throw error;
  }
}

async function requestAllMotionResponses(
  polityId: string,
  options: RequestOptions,
) {
  const path = `/polities/${encodeURIComponent(polityId)}/motions`;
  const firstPage = parseMotionPage(
    await requestUnknown(path, options, {
      page: 0,
      size: maximumPolityPageSize,
    }),
  );
  const remainingPages = await Promise.all(
    Array.from(
      { length: Math.max(firstPage.page.totalPages - 1, 0) },
      async (_, index) =>
        parseMotionPage(
          await requestUnknown(path, options, {
            page: index + 1,
            size: maximumPolityPageSize,
          }),
        ),
    ),
  );
  return flattenCompletePageSet(
    [firstPage, ...remainingPages],
    "Invalid motion page response.",
  );
}

async function requestAllOfficialRecordEntries(
  polityId: string,
  options: RequestOptions,
) {
  const path = `/polities/${encodeURIComponent(polityId)}/record`;
  const firstPage = parseOfficialRecordPage(
    await requestUnknown(path, options, {
      page: 0,
      size: maximumPolityPageSize,
    }),
  );
  const remainingPages = await Promise.all(
    Array.from(
      { length: Math.max(firstPage.page.totalPages - 1, 0) },
      async (_, index) =>
        parseOfficialRecordPage(
          await requestUnknown(path, options, {
            page: index + 1,
            size: maximumPolityPageSize,
          }),
        ),
    ),
  );
  return flattenCompletePageSet(
    [firstPage, ...remainingPages],
    "Invalid official record page response.",
  );
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
      requestAllMotionResponses(polityId, options),
      requestAllOfficialRecordEntries(polityId, options),
    ]);
    return {
      actions,
      government,
      motions,
      polity,
      record,
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
        ({ actions, currentVote, status }) =>
          status === "voting" &&
          actions.castVote.available &&
          currentVote === undefined,
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
    memberCount: resources.government.formation.activeMemberCount,
    motions,
    name: resources.polity.name,
    slug: resources.polity.slug,
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

export async function getPolityGovernment(
  polityId: string,
  options: RequestOptions,
) {
  const encodedId = encodeURIComponent(polityId);
  try {
    const [government, actions] = await Promise.all([
      requestUnknown(`/polities/${encodedId}/government`, options).then(
        parseGovernment,
      ),
      requestUnknown(`/polities/${encodedId}/actions`, options).then(
        parsePolityActions,
      ),
    ]);
    return projectGovernment(government, actions, options.acceptedLanguage);
  } catch (error) {
    if (hasHttpResponseStatus(error, 404))
      throw new ResourceNotFoundError("Polity government", polityId);
    throw error;
  }
}

export async function getPolityOfficialRecord(
  polityId: string,
  options: RequestOptions,
) {
  try {
    return (await requestAllOfficialRecordEntries(polityId, options)).map(
      (entry) => projectOfficialRecordEntry(entry, options.acceptedLanguage),
    );
  } catch (error) {
    if (hasHttpResponseStatus(error, 404))
      throw new ResourceNotFoundError("Polity official record", polityId);
    throw error;
  }
}

export async function listPolityMotionResponses(
  polityId: string,
  options: RequestOptions,
) {
  return requestAllMotionResponses(polityId, options);
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
      requestAllOfficialRecordEntries(polityId, options),
    ]);
    return projectMotion(motion, government, record, options.acceptedLanguage);
  } catch (error) {
    if (hasHttpResponseStatus(error, 404))
      throw new ResourceNotFoundError("Motion", motionId);
    throw error;
  }
}

export { parseMotionResponse };
