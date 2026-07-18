import type { PolityActionAvailability } from "@/domains/polity/lib/polity";
import type { PageResult } from "@/lib/pagination";

export type PolityResponse = Readonly<{
  constitutionVersion: number;
  createdAt: string;
  id: string;
  institutionName: string;
  jurisdictionName: string;
  name: string;
  slug: string;
  status: "active" | "disbanded";
  visibility: "private" | "public";
}>;

export type ProcedureResponse = Readonly<{
  electorate: "active_members" | "office_holders";
  id: string;
  minimumNoticeHours: number;
  name: string;
  votingPeriodHours: number;
  threshold:
    | "majority_of_eligible"
    | "office_election_result"
    | "simple_majority_cast"
    | "two_thirds_cast"
    | "two_thirds_eligible";
}>;

export type GovernmentResponse = Readonly<{
  constitution: Readonly<{
    body: string;
    id: string;
    institutions: readonly Readonly<{
      id: string;
      kind: "assembly" | "council" | "judiciary";
      name: string;
    }>[];
    offices: readonly Readonly<{
      description: string;
      id: string;
      name: string;
      seatCount: number;
      termLengthDays: number;
    }>[];
    procedures: readonly ProcedureResponse[];
    ratifiedAt: string;
    title: string;
    version: number;
  }>;
  formation: Readonly<{
    activeMemberCount: number;
    complete: boolean;
    minimumFullGovernmentMembers: number;
    standingMemberCount: number;
  }>;
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
  actorName: string;
  body: string;
  constitutionVersion: number;
  entryNumber: number;
  id: string;
  motionId?: string;
  occurredAt: string;
  title: string;
}>;

const effectTypes = [
  "adopt_resolution",
  "amend_constitution",
  "apply_sanction",
  "disband_polity",
  "elect_office",
  "grant_appeal",
  "vacate_office_term",
  "void_official_act",
] as const;

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

function requiredSlug(value: unknown, message: string) {
  const slug = requiredString(value, message);
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/u.test(slug) || slug.length > 80) {
    throw new Error(message);
  }
  return slug;
}

function requiredDateTime(value: unknown, message: string) {
  const dateTime = requiredString(value, message);
  const components =
    /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?(?:Z|[+-](\d{2}):(\d{2}))$/u.exec(
      dateTime,
    );
  if (!components) throw new Error(message);

  const [
    ,
    yearValue,
    monthValue,
    dayValue,
    hourValue,
    minuteValue,
    secondValue,
  ] = components;
  const year = Number(yearValue);
  const month = Number(monthValue);
  const day = Number(dayValue);
  const hour = Number(hourValue);
  const minute = Number(minuteValue);
  const second = Number(secondValue);
  const offsetHour = Number(components[7] ?? 0);
  const offsetMinute = Number(components[8] ?? 0);
  const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const daysInMonth = [
    31,
    leapYear ? 29 : 28,
    31,
    30,
    31,
    30,
    31,
    31,
    30,
    31,
    30,
    31,
  ];
  if (
    month < 1 ||
    month > 12 ||
    day < 1 ||
    day > (daysInMonth[month - 1] ?? 0) ||
    hour > 23 ||
    minute > 59 ||
    second > 59 ||
    offsetHour > 23 ||
    offsetMinute > 59 ||
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

function requiredInteger(value: unknown, minimum: number, message: string) {
  if (
    typeof value !== "number" ||
    !Number.isSafeInteger(value) ||
    value < minimum
  )
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
  const number = requiredInteger(page.number, 0, message);
  const size = requiredInteger(page.size, 1, message);
  const totalElements = requiredInteger(page.totalElements, 0, message);
  const totalPages = requiredInteger(page.totalPages, 0, message);
  const expectedTotalPages =
    totalElements === 0 ? 0 : Math.ceil(totalElements / size);
  if (totalPages !== expectedTotalPages) throw new Error(message);

  return {
    content: response.content.map(parseItem).map(project),
    page: {
      number,
      size,
      totalElements,
      totalPages,
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
    slug: requiredSlug(response.slug, message),
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
  if (
    !Array.isArray(constitution.institutions) ||
    !Array.isArray(constitution.offices) ||
    !Array.isArray(constitution.procedures)
  )
    throw new Error(message);
  return {
    constitution: {
      body: requiredString(constitution.body, message),
      id: requiredUuid(constitution.id, message),
      institutions: constitution.institutions.map((candidate) => {
        const institution = requiredRecord(candidate, message);
        return {
          id: requiredUuid(institution.id, message),
          kind: enumValue(
            institution.kind,
            ["assembly", "council", "judiciary"],
            message,
          ),
          name: requiredString(institution.name, message),
        };
      }),
      offices: constitution.offices.map((candidate) => {
        const office = requiredRecord(candidate, message);
        return {
          description: requiredString(office.description, message),
          id: requiredUuid(office.id, message),
          name: requiredString(office.name, message),
          seatCount: requiredInteger(office.seatCount, 1, message),
          termLengthDays: requiredInteger(office.termLengthDays, 1, message),
        };
      }),
      procedures: constitution.procedures.map((candidate) => {
        const procedure = requiredRecord(candidate, message);
        return {
          electorate: enumValue(
            procedure.electorate,
            ["active_members", "office_holders"],
            message,
          ),
          id: requiredUuid(procedure.id, message),
          minimumNoticeHours: requiredInteger(
            procedure.minimumNoticeHours,
            0,
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
          votingPeriodHours: requiredInteger(
            procedure.votingPeriodHours,
            1,
            message,
          ),
        };
      }),
      ratifiedAt: requiredDateTime(constitution.ratifiedAt, message),
      title: requiredString(constitution.title, message),
      version: requiredInteger(constitution.version, 1, message),
    },
    formation: {
      activeMemberCount: requiredInteger(
        formation.activeMemberCount,
        0,
        message,
      ),
      complete: requiredBoolean(formation.complete, message),
      minimumFullGovernmentMembers: requiredInteger(
        formation.minimumFullGovernmentMembers,
        1,
        message,
      ),
      standingMemberCount: requiredInteger(
        formation.standingMemberCount,
        0,
        message,
      ),
    },
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
    effectType: enumValue(response.effectType, effectTypes, message),
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
      actorName: requiredString(item.actorName, message),
      body: requiredString(item.body, message),
      constitutionVersion: requiredInteger(
        item.constitutionVersion,
        1,
        message,
      ),
      entryNumber: requiredInteger(item.entryNumber, 1, message),
      id: requiredUuid(item.id, message),
      ...(item.motionId === undefined
        ? {}
        : { motionId: requiredUuid(item.motionId, message) }),
      occurredAt: requiredDateTime(item.occurredAt, message),
      title: requiredString(item.title, message),
    };
  };
  return parsePage(
    value,
    "Invalid official record page response.",
    parseItem,
    (item) => item,
  );
}
