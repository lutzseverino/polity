import type { PolityActionAvailability } from "@/domains/polity/lib/polity";
import type { PageResult } from "@/lib/pagination";

export type PolityResponse = Readonly<{
  constitutionVersion: number;
  createdAt: string;
  id: string;
  institutionName: string;
  jurisdictionName: string;
  name: string;
  status: "active" | "disbanded";
  visibility: "private" | "public";
}>;

export type ProcedureResponse = Readonly<{
  electorate: "active_members" | "office_holders";
  minimumNoticeHours: number;
  name: string;
  threshold:
    | "majority_of_eligible"
    | "office_election_result"
    | "simple_majority_cast"
    | "two_thirds_cast"
    | "two_thirds_eligible";
}>;

export type GovernmentResponse = Readonly<{
  activeMemberCount: number;
  procedures: readonly ProcedureResponse[];
}>;

type ActionAvailabilityResponse = Readonly<{
  available: boolean;
  reason?: string;
  reasonMessage?: string;
}>;

type MotionActions = Readonly<{
  castElectionBallot: ActionAvailabilityResponse;
  castVote: ActionAvailabilityResponse;
  requestCertification: ActionAvailabilityResponse;
  respondCandidacy: ActionAvailabilityResponse;
}>;

type EffectType =
  | "adopt_resolution"
  | "amend_constitution"
  | "apply_sanction"
  | "disband_polity"
  | "elect_office"
  | "grant_appeal"
  | "vacate_office_term"
  | "void_official_act";

export type MotionResponse = Readonly<{
  actions: MotionActions;
  body: string;
  certification?: Readonly<{
    noCount?: number;
    passed: boolean;
    yesCount?: number;
  }>;
  currentVote?: "abstain" | "no" | "yes";
  electionTally?: Readonly<{
    eligible: number;
    participation: number;
    quorumMet: boolean;
    quorumRequired: number;
  }>;
  effectType: EffectType;
  id: string;
  introducedByName: string;
  officeElection?: Readonly<{
    officeName: string;
  }>;
  openedAt: string;
  procedureName: string;
  status: "enacted" | "rejected" | "voting";
  tally?: Readonly<{
    abstain: number;
    eligible: number;
    no: number;
    quorumMet: boolean;
    quorumRequired: number;
    yes: number;
  }>;
  title: string;
  votingClosesAt: string;
}>;

export type OfficialRecordResponse = Readonly<{
  entryNumber: number;
  id: string;
  motionId?: string;
  occurredAt: string;
  title: string;
  type: string;
}>;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function requiredRecord(value: unknown, message: string) {
  if (!isRecord(value)) throw new Error(message);
  return value;
}

function requiredString(value: unknown, message: string) {
  if (typeof value !== "string" || value.length === 0) throw new Error(message);
  return value;
}

function requiredUuid(value: unknown, message: string) {
  const uuid = requiredString(value, message);
  if (
    !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(
      uuid,
    )
  ) {
    throw new Error(message);
  }
  return uuid;
}

function requiredDateTime(value: unknown, message: string) {
  const dateTime = requiredString(value, message);
  if (
    !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/u.test(
      dateTime,
    ) ||
    !Number.isFinite(Date.parse(dateTime))
  )
    throw new Error(message);
  return dateTime;
}

function requiredNumber(value: unknown, message: string) {
  if (typeof value !== "number" || !Number.isFinite(value))
    throw new Error(message);
  return value;
}

function requiredBoolean(value: unknown, message: string) {
  if (typeof value !== "boolean") throw new Error(message);
  return value;
}

function enumValue<const Value extends string>(
  value: unknown,
  values: readonly Value[],
  message: string,
) {
  if (typeof value !== "string" || !values.includes(value as Value)) {
    throw new Error(message);
  }
  return value as Value;
}

function parsePage<Response, Product>(
  value: unknown,
  message: string,
  parseItem: (candidate: unknown) => Response,
  project: (response: Response) => Product,
): PageResult<Product> {
  const response = requiredRecord(value, message);
  const page = requiredRecord(response.page, message);
  if (!Array.isArray(response.content)) throw new Error(message);

  return {
    content: response.content.map(parseItem).map(project),
    page: {
      number: requiredNumber(page.number, message),
      size: requiredNumber(page.size, message),
      totalElements: requiredNumber(page.totalElements, message),
      totalPages: requiredNumber(page.totalPages, message),
    },
  };
}

export function parsePolityResponse(value: unknown): PolityResponse {
  const message = "Invalid polity response.";
  const response = requiredRecord(value, message);
  return {
    constitutionVersion: requiredNumber(response.constitutionVersion, message),
    createdAt: requiredDateTime(response.createdAt, message),
    id: requiredUuid(response.id, message),
    institutionName: requiredString(response.institutionName, message),
    jurisdictionName: requiredString(response.jurisdictionName, message),
    name: requiredString(response.name, message),
    status: enumValue(response.status, ["active", "disbanded"], message),
    visibility: enumValue(response.visibility, ["private", "public"], message),
  };
}

export function parsePolityPage(value: unknown) {
  return parsePage(
    value,
    "Invalid polity page response.",
    parsePolityResponse,
    (item) => item,
  );
}

function parseAvailability(value: unknown, message: string) {
  const response = requiredRecord(value, message);
  const result: {
    available: boolean;
    reason?: string;
    reasonMessage?: string;
  } = {
    available: requiredBoolean(response.available, message),
  };
  if (response.reason !== undefined)
    result.reason = requiredString(response.reason, message);
  if (response.reasonMessage !== undefined) {
    result.reasonMessage = requiredString(response.reasonMessage, message);
  }
  return result;
}

function parseDiagnostics(value: unknown, message: string) {
  if (!Array.isArray(value)) throw new Error(message);
  return value.map((candidate) => {
    const diagnostic = requiredRecord(candidate, message);
    return {
      code: requiredString(diagnostic.code, message),
      message: requiredString(diagnostic.message, message),
    };
  });
}

export function parsePolityActions(value: unknown): PolityActionAvailability {
  const message = "Invalid polity action availability response.";
  const response = requiredRecord(value, message);
  const readiness = requiredRecord(response.readiness, message);
  const health = requiredRecord(response.constitutionalHealth, message);
  const actionNames = [
    "introduceAmendment",
    "introduceAppeal",
    "introduceConstitutionalReview",
    "introduceDisbandment",
    "introduceMotion",
    "introduceOfficeElection",
    "introduceOfficeTermReview",
    "introduceSanction",
    "inviteMembers",
    "requestCertification",
    "resignMembership",
  ] as const;
  const actions = Object.fromEntries(
    actionNames.map((name) => [
      name,
      parseAvailability(response[name], message),
    ]),
  ) as unknown as Omit<
    PolityActionAvailability,
    "constitutionalHealth" | "readiness"
  >;
  return {
    ...actions,
    constitutionalHealth: {
      diagnostics: parseDiagnostics(health.diagnostics, message),
      status: enumValue(
        health.status,
        ["critical", "degraded", "healthy"],
        message,
      ),
      statusMessage: requiredString(health.statusMessage, message),
    },
    readiness: {
      diagnostics: parseDiagnostics(readiness.diagnostics, message),
      status: enumValue(
        readiness.status,
        ["blocked", "disbanded", "forming_offices", "provisional", "ready"],
        message,
      ),
      statusMessage: requiredString(readiness.statusMessage, message),
    },
  };
}

export function parseGovernment(value: unknown): GovernmentResponse {
  const message = "Invalid polity government response.";
  const response = requiredRecord(value, message);
  const constitution = requiredRecord(response.constitution, message);
  const formation = requiredRecord(response.formation, message);
  if (!Array.isArray(constitution.procedures)) throw new Error(message);
  return {
    activeMemberCount: requiredNumber(formation.activeMemberCount, message),
    procedures: constitution.procedures.map((candidate) => {
      const procedure = requiredRecord(candidate, message);
      return {
        electorate: enumValue(
          procedure.electorate,
          ["active_members", "office_holders"],
          message,
        ),
        minimumNoticeHours: requiredNumber(
          procedure.minimumNoticeHours,
          message,
        ),
        name: requiredString(procedure.name, message),
        threshold: enumValue(
          procedure.threshold,
          [
            "majority_of_eligible",
            "office_election_result",
            "simple_majority_cast",
            "two_thirds_cast",
            "two_thirds_eligible",
          ],
          message,
        ),
      };
    }),
  };
}

function parseMotionActions(value: unknown, message: string): MotionActions {
  const actions = requiredRecord(value, message);
  return {
    castElectionBallot: parseAvailability(actions.castElectionBallot, message),
    castVote: parseAvailability(actions.castVote, message),
    requestCertification: parseAvailability(
      actions.requestCertification,
      message,
    ),
    respondCandidacy: parseAvailability(actions.respondCandidacy, message),
  };
}

export function parseMotionResponse(value: unknown): MotionResponse {
  const message = "Invalid motion response.";
  const response = requiredRecord(value, message);
  const currentVote =
    response.currentVote === undefined
      ? undefined
      : enumValue(response.currentVote, ["abstain", "no", "yes"], message);
  const tally =
    response.tally === undefined
      ? undefined
      : (() => {
          const value = requiredRecord(response.tally, message);
          return {
            abstain: requiredNumber(value.abstain, message),
            eligible: requiredNumber(value.eligible, message),
            no: requiredNumber(value.no, message),
            quorumMet: requiredBoolean(value.quorumMet, message),
            quorumRequired: requiredNumber(value.quorumRequired, message),
            yes: requiredNumber(value.yes, message),
          };
        })();
  const electionTally =
    response.electionTally === undefined
      ? undefined
      : (() => {
          const value = requiredRecord(response.electionTally, message);
          return {
            eligible: requiredNumber(value.eligible, message),
            participation: requiredNumber(value.participation, message),
            quorumMet: requiredBoolean(value.quorumMet, message),
            quorumRequired: requiredNumber(value.quorumRequired, message),
          };
        })();
  const officeElection =
    response.officeElection === undefined
      ? undefined
      : (() => {
          const value = requiredRecord(response.officeElection, message);
          return { officeName: requiredString(value.officeName, message) };
        })();
  const certification =
    response.certification === undefined
      ? undefined
      : (() => {
          const value = requiredRecord(response.certification, message);
          return {
            passed: requiredBoolean(value.passed, message),
            ...(value.noCount === undefined
              ? {}
              : { noCount: requiredNumber(value.noCount, message) }),
            ...(value.yesCount === undefined
              ? {}
              : { yesCount: requiredNumber(value.yesCount, message) }),
          };
        })();
  return {
    actions: parseMotionActions(response.actions, message),
    body: requiredString(response.body, message),
    effectType: enumValue(
      response.effectType,
      [
        "adopt_resolution",
        "amend_constitution",
        "apply_sanction",
        "disband_polity",
        "elect_office",
        "grant_appeal",
        "vacate_office_term",
        "void_official_act",
      ],
      message,
    ),
    id: requiredUuid(response.id, message),
    introducedByName: requiredString(response.introducedByName, message),
    openedAt: requiredDateTime(response.openedAt, message),
    procedureName: requiredString(response.procedureName, message),
    status: enumValue(
      response.status,
      ["enacted", "rejected", "voting"],
      message,
    ),
    title: requiredString(response.title, message),
    votingClosesAt: requiredDateTime(response.votingClosesAt, message),
    ...(currentVote ? { currentVote } : {}),
    ...(tally ? { tally } : {}),
    ...(electionTally ? { electionTally } : {}),
    ...(officeElection ? { officeElection } : {}),
    ...(certification ? { certification } : {}),
  };
}

export function parseMotionPage(value: unknown) {
  return parsePage(
    value,
    "Invalid motion page response.",
    parseMotionResponse,
    (item) => item,
  );
}

export function parseOfficialRecordPage(value: unknown) {
  const parseItem = (candidate: unknown): OfficialRecordResponse => {
    const message = "Invalid official record response.";
    const item = requiredRecord(candidate, message);
    return {
      entryNumber: requiredNumber(item.entryNumber, message),
      id: requiredUuid(item.id, message),
      ...(item.motionId === undefined
        ? {}
        : { motionId: requiredUuid(item.motionId, message) }),
      occurredAt: requiredDateTime(item.occurredAt, message),
      title: requiredString(item.title, message),
      type: requiredString(item.type, message),
    };
  };
  return parsePage(
    value,
    "Invalid official record page response.",
    parseItem,
    (item) => item,
  );
}
