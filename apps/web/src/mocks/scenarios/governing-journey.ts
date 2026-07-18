import { HttpResponse, http, type RequestHandler } from "msw";

const available = { available: true } as const;
const assemblyInstitutionId = "44444444-4444-4444-8444-444444444444";
const ordinaryProcedureId = "66666666-6666-4666-8666-666666666661";
const electionProcedureId = "66666666-6666-4666-8666-666666666662";
const tribuneOfficeId = "77777777-7777-4777-8777-777777777777";
const unavailable = (reason: string, reasonMessage: string) => ({
  available: false,
  reason,
  reasonMessage,
});

function actionSet(readiness: "provisional" | "ready" = "ready") {
  const ready = readiness === "ready";
  return {
    constitutionalHealth: {
      diagnostics: ready
        ? []
        : [
            {
              code: "ordinary_governance_unavailable",
              message: "Ordinary decisions need one more standing member.",
            },
          ],
      status: ready ? "healthy" : "degraded",
      statusMessage: ready
        ? "Every core constitutional path is operating."
        : "Formation is still in progress.",
    },
    introduceAmendment: available,
    introduceAppeal: ready
      ? available
      : unavailable(
          "procedure_electorate_below_minimum",
          "An appeal needs one more eligible member.",
        ),
    introduceConstitutionalReview: ready
      ? available
      : unavailable(
          "procedure_electorate_below_minimum",
          "A constitutional review needs one more eligible member.",
        ),
    introduceDisbandment: available,
    introduceMotion: ready
      ? available
      : unavailable(
          "procedure_electorate_below_minimum",
          "Invite one more standing member before opening an ordinary decision.",
        ),
    introduceOfficeElection: ready
      ? available
      : unavailable(
          "procedure_electorate_below_minimum",
          "Finish forming this polity before starting an election.",
        ),
    introduceOfficeTermReview: ready
      ? available
      : unavailable(
          "procedure_electorate_below_minimum",
          "An office-term review needs one more eligible member.",
        ),
    introduceSanction: ready
      ? available
      : unavailable(
          "procedure_electorate_below_minimum",
          "A sanction proceeding needs one more eligible member.",
        ),
    inviteMembers: available,
    readiness: {
      diagnostics: ready
        ? []
        : [
            {
              code: "needs_more_standing_members",
              message:
                "One more standing member is needed for full government.",
            },
          ],
      status: readiness,
      statusMessage: ready
        ? "Government is operating normally."
        : "Invite one more person to finish forming the government.",
    },
    requestCertification: ready
      ? available
      : unavailable(
          "certification_not_open",
          "There is no motion ready to certify.",
        ),
    resignMembership: ready
      ? available
      : unavailable(
          "provisional_founder_resignation_unavailable",
          "The provisional founder cannot leave until formation is complete.",
        ),
  };
}

const polities = [
  {
    constitutionVersion: 2,
    createdAt: "2025-01-10T12:00:00.000Z",
    id: "11111111-1111-4111-8111-111111111111",
    institutionName: "Assembly",
    jurisdictionName: "The Thursday Assembly",
    name: "The Thursday Assembly",
    status: "active",
    visibility: "public",
  },
  {
    constitutionVersion: 1,
    createdAt: "2025-05-14T12:00:00.000Z",
    id: "22222222-2222-4222-8222-222222222222",
    institutionName: "Assembly",
    jurisdictionName: "Neighbourhood Table",
    name: "Neighbourhood Table",
    status: "active",
    visibility: "private",
  },
  {
    constitutionVersion: 1,
    createdAt: "2025-06-02T12:00:00.000Z",
    id: "33333333-3333-4333-8333-333333333333",
    institutionName: "Council",
    jurisdictionName: "Weekend Council",
    name: "Weekend Council",
    status: "active",
    visibility: "public",
  },
] as const;

const memberCounts: Readonly<Record<string, number>> = {
  "22222222-2222-4222-8222-222222222222": 2,
  "11111111-1111-4111-8111-111111111111": 8,
  "33333333-3333-4333-8333-333333333333": 5,
};

function procedure(
  id: string,
  name: string,
  threshold: "office_election_result" | "simple_majority_cast",
  effectType: "adopt_resolution" | "elect_office",
) {
  return {
    code: id,
    effectType,
    electorate: "active_members",
    id,
    institutionId: assemblyInstitutionId,
    minimumElectorCount: 1,
    minimumNoticeHours: 24,
    name,
    quorumDenominator: 2,
    quorumNumerator: 1,
    threshold,
    votingPeriodHours: 48,
  };
}

function government(polityId: string) {
  const activeMemberCount = memberCounts[polityId] ?? 0;
  return {
    constitution: {
      body: "A constitutional council republic.",
      id: polityId,
      institutions: [
        {
          id: assemblyInstitutionId,
          jurisdictionId: polityId,
          kind: "assembly",
          name: "Assembly",
        },
      ],
      offices: [
        {
          code: "tribune",
          description:
            "Coordinates meetings and represents the assembly between sessions.",
          id: tribuneOfficeId,
          jurisdictionId: polityId,
          name: "Tribune",
          seatCount: 1,
          termLengthDays: 365,
        },
      ],
      powers: [],
      procedures: [
        procedure(
          ordinaryProcedureId,
          "Ordinary resolution",
          "simple_majority_cast",
          "adopt_resolution",
        ),
        procedure(
          electionProcedureId,
          "Office election",
          "office_election_result",
          "elect_office",
        ),
      ],
      ratifiedAt: "2025-01-10T12:00:00.000Z",
      status: "ratified",
      templateParams: {},
      title: "Constitution",
      version:
        polities.find(({ id }) => id === polityId)?.constitutionVersion ?? 1,
    },
    formation: {
      activeMemberCount,
      complete: activeMemberCount >= 3,
      minimumFullGovernmentMembers: 3,
      standingMemberCount: activeMemberCount,
    },
    jurisdictions: [
      {
        id: polityId,
        kind: "root",
        name: polities.find(({ id }) => id === polityId)?.name ?? polityId,
      },
    ],
  };
}

function motionActions(kind: "candidacy" | "vote", availableNow = true) {
  return {
    castElectionBallot: unavailable(
      "office_election_ballot_required",
      "A ballot is not available yet.",
    ),
    castVote:
      kind === "vote" && availableNow
        ? available
        : unavailable("motion_not_voting", "Voting is not available."),
    requestCertification: unavailable(
      "certification_not_open",
      "Certification is not open.",
    ),
    respondCandidacy:
      kind === "candidacy" && availableNow
        ? available
        : unavailable(
            "candidacy_response_closed",
            "Your response has been recorded.",
          ),
  };
}

function hoursFrom(now: Date, hours: number) {
  return new Date(now.getTime() + hours * 60 * 60 * 1_000).toISOString();
}

type MockCandidate = {
  membershipId: string;
  name: string;
  respondedAt?: string;
  status: string;
};
type MockMotion = Record<string, unknown> & {
  currentVote?: "abstain" | "no" | "yes";
  id: string;
  officeElection?: Record<string, unknown> & { candidates: MockCandidate[] };
  tally?: {
    abstain: number;
    eligible: number;
    no: number;
    outcomeReason: string;
    quorumMet: boolean;
    quorumRequired: number;
    yes: number;
  };
};

function initialMotions(now: Date): MockMotion[] {
  const common = {
    certificationOpensAt: hoursFrom(now, 49),
    constitutionVersion: 2,
    openedAt: hoursFrom(now, -2),
    votingOpensAt: hoursFrom(now, -2),
  };
  return [
    {
      ...common,
      actions: motionActions("vote"),
      body: "The Thursday Assembly will hold a shared dinner on the first Thursday of every month. The host rotates alphabetically among volunteers, and the assembly budget may reimburse up to €80 for ingredients.",
      effectType: "adopt_resolution",
      id: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
      introducedByName: "Mira Chen",
      procedureName: "Ordinary resolution",
      status: "voting",
      tally: {
        abstain: 1,
        eligible: 8,
        no: 1,
        outcomeReason: "passed",
        quorumMet: true,
        quorumRequired: 4,
        yes: 3,
      },
      title: "Shared Thursday Dinner",
      votingClosesAt: hoursFrom(now, 8),
    },
    {
      ...common,
      actions: motionActions("candidacy"),
      body: "Elect one Tribune for the next 90-day term.",
      effectType: "elect_office",
      electionTally: {
        candidates: [],
        decisive: false,
        eligible: 8,
        method: "ranked_choice",
        outcomeReason: "no_decisive_result",
        participation: 2,
        passed: false,
        quorumMet: false,
        quorumRequired: 4,
        rounds: [],
        seatsAvailable: 1,
        seatsFilled: 0,
        winners: [],
      },
      id: "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2",
      introducedByName: "System",
      officeElection: {
        candidates: [
          {
            membershipId: "88888888-8888-4888-8888-888888888888",
            name: "Alex Rivera",
            status: "pending",
          },
        ],
        method: "ranked_choice",
        officeCode: "tribune",
        officeId: tribuneOfficeId,
        officeName: "Tribune",
        seatsAvailable: 1,
      },
      procedureName: "Office election",
      status: "voting",
      title: "Elect the next Tribune",
      votingClosesAt: hoursFrom(now, 30),
    },
    {
      ...common,
      actions: motionActions("vote", false),
      body: "Reserve €240 from the assembly fund for the autumn cabin weekend.",
      certification: {
        abstainCount: 0,
        certifiedAt: hoursFrom(now, -96),
        eligibleCount: 8,
        modality: "yes_no",
        noCount: 2,
        outcomeReason: "passed",
        passed: true,
        quorumMet: true,
        quorumRequired: 4,
        thresholdMet: true,
        yesCount: 5,
      },
      effectType: "adopt_resolution",
      id: "cccccccc-cccc-4ccc-8ccc-ccccccccccc3",
      introducedByName: "Jon Bell",
      procedureName: "Ordinary resolution",
      status: "enacted",
      tally: {
        abstain: 0,
        eligible: 8,
        no: 2,
        outcomeReason: "passed",
        quorumMet: true,
        quorumRequired: 4,
        yes: 5,
      },
      title: "Autumn Cabin Budget",
      votingClosesAt: hoursFrom(now, -120),
    },
  ];
}

function records(now: Date) {
  return [
    {
      actorName: "Mira Chen",
      body: "Voting opened.",
      constitutionVersion: 2,
      entryNumber: 43,
      id: "dddddddd-dddd-4ddd-8ddd-ddddddddddd1",
      motionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
      occurredAt: hoursFrom(now, -2),
      title: "Shared Thursday Dinner opened for voting",
      type: "motion_introduced",
    },
    {
      actorName: "System",
      body: "Election opened.",
      constitutionVersion: 2,
      entryNumber: 42,
      id: "dddddddd-dddd-4ddd-8ddd-ddddddddddd2",
      motionId: "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2",
      occurredAt: hoursFrom(now, -24),
      title: "Tribune election opened",
      type: "motion_introduced",
    },
    {
      actorName: "Jon Bell",
      body: "The motion was adopted.",
      constitutionVersion: 2,
      entryNumber: 41,
      id: "dddddddd-dddd-4ddd-8ddd-ddddddddddd3",
      motionId: "cccccccc-cccc-4ccc-8ccc-ccccccccccc3",
      occurredAt: hoursFrom(now, -96),
      title: "Autumn cabin budget adopted",
      type: "resolution_adopted",
    },
  ];
}

function page<T>(
  content: readonly T[],
  number = 0,
  size = 100,
  totalElements = content.length,
) {
  return {
    content,
    page: {
      number,
      size,
      totalElements,
      totalPages: totalElements === 0 ? 0 : Math.ceil(totalElements / size),
    },
  };
}

function notFound() {
  return HttpResponse.json(
    { error: { code: "not_found", message: "Resource not found." } },
    { status: 404 },
  );
}

export function createGoverningJourneyScenarioHandlers({
  now = new Date(),
}: Readonly<{ now?: Date }> = {}): RequestHandler[] {
  let motions = initialMotions(now);
  const officialRecords = records(now);

  function polityMotions(polityId: string) {
    return polityId === "11111111-1111-4111-8111-111111111111" ? motions : [];
  }

  return [
    http.get("/api/v1/polities", ({ request }) => {
      const url = new URL(request.url);
      const query = url.searchParams.get("query")?.trim().toLocaleLowerCase();
      const number = Math.max(Number(url.searchParams.get("page") ?? 0), 0);
      const size = Math.max(Number(url.searchParams.get("size") ?? 50), 1);
      const matching = query
        ? polities.filter(({ name }) =>
            name.toLocaleLowerCase().includes(query),
          )
        : polities;
      const start = number * size;
      return HttpResponse.json(
        page(
          matching.slice(start, start + size),
          number,
          size,
          matching.length,
        ),
      );
    }),
    http.get("/api/v1/polities/:polityId", ({ params }) => {
      const polity = polities.find(({ id }) => id === String(params.polityId));
      return polity ? HttpResponse.json(polity) : notFound();
    }),
    http.get("/api/v1/polities/:polityId/actions", ({ params }) => {
      const polityId = String(params.polityId);
      if (!polities.some(({ id }) => id === polityId)) return notFound();
      return HttpResponse.json(
        actionSet(
          polityId === "22222222-2222-4222-8222-222222222222"
            ? "provisional"
            : "ready",
        ),
      );
    }),
    http.get("/api/v1/polities/:polityId/government", ({ params }) => {
      const polityId = String(params.polityId);
      return polities.some(({ id }) => id === polityId)
        ? HttpResponse.json(government(polityId))
        : notFound();
    }),
    http.get("/api/v1/polities/:polityId/motions", ({ params, request }) => {
      const polityId = String(params.polityId);
      if (!polities.some(({ id }) => id === polityId)) return notFound();
      const url = new URL(request.url);
      const number = Math.max(Number(url.searchParams.get("page") ?? 0), 0);
      const size = Math.max(Number(url.searchParams.get("size") ?? 100), 1);
      const all = polityMotions(polityId);
      return HttpResponse.json(
        page(
          all.slice(number * size, number * size + size),
          number,
          size,
          all.length,
        ),
      );
    }),
    http.get("/api/v1/polities/:polityId/motions/:motionId", ({ params }) => {
      const motion = polityMotions(String(params.polityId)).find(
        ({ id }) => id === String(params.motionId),
      );
      return motion ? HttpResponse.json(motion) : notFound();
    }),
    http.get("/api/v1/polities/:polityId/record", ({ params }) => {
      const polityId = String(params.polityId);
      if (!polities.some(({ id }) => id === polityId)) return notFound();
      const content =
        polityId === "11111111-1111-4111-8111-111111111111"
          ? officialRecords
          : [];
      return HttpResponse.json(page(content));
    }),
    http.put(
      "/api/v1/polities/:polityId/motions/:motionId/votes",
      async ({ params, request }) => {
        const motionIndex = motions.findIndex(
          ({ id }) => id === String(params.motionId),
        );
        if (
          String(params.polityId) !== "11111111-1111-4111-8111-111111111111" ||
          motionIndex < 0
        )
          return notFound();
        const body = (await request.json()) as { choice?: unknown };
        if (
          !(["yes", "no", "abstain"] as const).includes(body.choice as "yes")
        ) {
          return HttpResponse.json(
            {
              error: { code: "invalid_vote", message: "Choose a valid vote." },
            },
            { status: 400 },
          );
        }
        const motion = motions[motionIndex];
        if (!motion.tally) return notFound();
        const previous = motion.currentVote;
        const tally = { ...motion.tally };
        if (previous) tally[previous] -= 1;
        const choice = body.choice as "yes" | "no" | "abstain";
        tally[choice] += 1;
        const updated = { ...motion, currentVote: choice, tally };
        motions = motions.map((candidate, index) =>
          index === motionIndex ? updated : candidate,
        );
        return HttpResponse.json(updated);
      },
    ),
    http.put(
      "/api/v1/polities/:polityId/motions/:motionId/candidacy",
      async ({ params, request }) => {
        const motionIndex = motions.findIndex(
          ({ id }) => id === String(params.motionId),
        );
        if (
          String(params.polityId) !== "11111111-1111-4111-8111-111111111111" ||
          motionIndex < 0
        )
          return notFound();
        const body = (await request.json()) as { accepted?: unknown };
        if (typeof body.accepted !== "boolean") {
          return HttpResponse.json(
            {
              error: {
                code: "invalid_candidacy",
                message: "Choose a valid response.",
              },
            },
            { status: 400 },
          );
        }
        const motion = motions[motionIndex];
        if (!motion.officeElection) return notFound();
        const updated = {
          ...motion,
          actions: {
            ...motionActions("candidacy", false),
            respondCandidacy: unavailable(
              "candidacy_response_closed",
              body.accepted
                ? "You accepted the nomination."
                : "You declined the nomination.",
            ),
          },
          officeElection: {
            ...motion.officeElection,
            candidates: motion.officeElection.candidates.map((candidate) => ({
              ...candidate,
              respondedAt: now.toISOString(),
              status: body.accepted ? "accepted" : "declined",
            })),
          },
        };
        motions = motions.map((candidate, index) =>
          index === motionIndex ? updated : candidate,
        );
        return HttpResponse.json(updated);
      },
    ),
  ];
}
