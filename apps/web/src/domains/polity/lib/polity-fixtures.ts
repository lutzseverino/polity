import type {
  ActionAvailability,
  Polity,
  PolityActionAvailability,
} from "@/domains/polity/lib/polity";

const available: ActionAvailability = { available: true };

function unavailable(
  reason: string,
  reasonMessage: string,
): ActionAvailability {
  return { available: false, reason, reasonMessage };
}

const thursdayAssembly: Polity = {
  attention: [
    {
      description:
        "Choose whether the assembly should adopt a recurring shared dinner.",
      dueLabel: "Closes today at 19:00",
      id: "vote-shared-dinner",
      kind: "vote",
      target: { kind: "motion", motionId: "shared-dinner" },
      title: "Vote on Shared Thursday Dinner",
    },
    {
      description: "Confirm whether you accept your nomination for Tribune.",
      dueLabel: "Respond by tomorrow",
      id: "tribune-candidacy",
      kind: "candidacy",
      target: { kind: "motion", motionId: "tribune-election" },
      title: "Respond to your nomination",
    },
  ],
  constitutionVersion: 2,
  id: "thursday-assembly",
  memberCount: 8,
  motions: [
    {
      actionKind: "vote",
      body: "The Thursday Assembly will hold a shared dinner on the first Thursday of every month. The host rotates alphabetically among volunteers, and the assembly budget may reimburse up to €80 for ingredients.",
      category: "Ordinary resolution",
      closesAtLabel: "Today at 19:00",
      id: "shared-dinner",
      introducedBy: "Mira Chen",
      participation: {
        cast: 5,
        eligible: 8,
        quorumMet: true,
        quorumRequired: 4,
      },
      procedure: {
        electorate: "All 8 active members",
        name: "Ordinary resolution",
        notice: "24 hours",
        threshold: "Simple majority of votes cast",
      },
      status: "voting",
      title: "Shared Thursday Dinner",
    },
    {
      actionKind: "candidacy",
      body: "Elect one Tribune for the next 90-day term.",
      category: "Office election",
      closesAtLabel: "Tomorrow at 18:00",
      id: "tribune-election",
      introducedBy: "System",
      participation: {
        cast: 2,
        eligible: 8,
        quorumMet: false,
        quorumRequired: 4,
      },
      procedure: {
        electorate: "All 8 active members",
        name: "Office election",
        notice: "24 hours",
        threshold: "Ranked-choice result",
      },
      status: "voting",
      title: "Elect the next Tribune",
    },
    {
      actionKind: "vote",
      body: "Reserve €240 from the assembly fund for the autumn cabin weekend.",
      category: "Ordinary resolution",
      closesAtLabel: "Closed 8 July",
      id: "autumn-cabin-budget",
      introducedBy: "Jon Bell",
      participation: {
        cast: 7,
        eligible: 8,
        quorumMet: true,
        quorumRequired: 4,
      },
      procedure: {
        electorate: "All 8 active members",
        name: "Ordinary resolution",
        notice: "24 hours",
        threshold: "Simple majority of votes cast",
      },
      result: {
        no: 2,
        outcome: "Adopted",
        recordEntry: 41,
        yes: 5,
      },
      status: "enacted",
      title: "Autumn cabin budget",
    },
  ],
  name: "The Thursday Assembly",
  readiness: "ready",
  readinessMessage: "Government is operating normally.",
  recentActivity: [
    {
      id: "record-43",
      label: "Motion No. 43",
      timeLabel: "2 hours ago",
      title: "Shared Thursday Dinner opened for voting",
    },
    {
      id: "record-42",
      label: "Office election",
      timeLabel: "Yesterday",
      title: "Tribune election opened",
    },
    {
      id: "record-41",
      label: "Official record No. 41",
      timeLabel: "4 days ago",
      title: "Autumn cabin budget adopted",
    },
  ],
  role: "Citizen",
  visibility: "public",
};

const neighbourhoodTable: Polity = {
  attention: [
    {
      description:
        "Invite one more person so elections and ordinary decisions can run reliably.",
      dueLabel: "No deadline",
      id: "complete-formation",
      kind: "formation",
      target: { actionId: "invite-member", kind: "action" },
      title: "Finish forming the polity",
    },
  ],
  constitutionVersion: 1,
  id: "neighbourhood-table",
  memberCount: 2,
  motions: [],
  name: "Neighbourhood Table",
  readiness: "forming",
  readinessMessage: "One more standing member is needed for full government.",
  recentActivity: [
    {
      id: "neighbour-record-4",
      label: "Member admitted",
      timeLabel: "Yesterday",
      title: "Alex Rivera joined the polity",
    },
  ],
  role: "Founding citizen",
  visibility: "private",
};

const weekendCouncil: Polity = {
  attention: [],
  constitutionVersion: 1,
  id: "weekend-council",
  memberCount: 5,
  motions: [],
  name: "Weekend Council",
  readiness: "ready",
  readinessMessage: "Government is operating normally.",
  recentActivity: [],
  role: "Citizen",
  visibility: "public",
};

const polities: readonly Polity[] = [
  thursdayAssembly,
  neighbourhoodTable,
  weekendCouncil,
];

const readyActions: PolityActionAvailability = {
  constitutionalHealth: {
    diagnostics: [],
    status: "healthy",
    statusMessage: "Every core constitutional path is operating.",
  },
  introduceAmendment: available,
  introduceAppeal: available,
  introduceConstitutionalReview: available,
  introduceDisbandment: available,
  introduceMotion: available,
  introduceOfficeElection: available,
  introduceOfficeTermReview: available,
  introduceSanction: available,
  inviteMembers: available,
  readiness: {
    diagnostics: [],
    status: "ready",
    statusMessage: "Government is operating normally.",
  },
  requestCertification: available,
  resignMembership: available,
};

const actionAvailabilityByPolityId: Readonly<
  Record<string, PolityActionAvailability>
> = {
  "neighbourhood-table": {
    ...readyActions,
    constitutionalHealth: {
      diagnostics: [
        {
          code: "ordinary_governance_unavailable",
          message: "Ordinary decisions need one more standing member.",
        },
        {
          code: "office_election_path_unavailable",
          message: "Office elections need one more standing member.",
        },
      ],
      status: "degraded",
      statusMessage: "Formation is still in progress.",
    },
    introduceAppeal: unavailable(
      "procedure_electorate_below_minimum",
      "An appeal needs one more eligible member.",
    ),
    introduceConstitutionalReview: unavailable(
      "procedure_electorate_below_minimum",
      "A constitutional review needs one more eligible member.",
    ),
    introduceMotion: unavailable(
      "procedure_electorate_below_minimum",
      "Invite one more standing member before opening an ordinary decision.",
    ),
    introduceOfficeElection: unavailable(
      "procedure_electorate_below_minimum",
      "Finish forming this polity before starting an election.",
    ),
    introduceOfficeTermReview: unavailable(
      "procedure_electorate_below_minimum",
      "An office-term review needs one more eligible member.",
    ),
    introduceSanction: unavailable(
      "procedure_electorate_below_minimum",
      "A sanction proceeding needs one more eligible member and a working appeal path.",
    ),
    readiness: {
      diagnostics: [
        {
          code: "needs_more_standing_members",
          message: "One more standing member is needed for full government.",
        },
      ],
      status: "provisional",
      statusMessage: "Invite one more person to finish forming the government.",
    },
    requestCertification: unavailable(
      "certification_not_open",
      "There is no motion ready to certify.",
    ),
    resignMembership: unavailable(
      "provisional_founder_resignation_unavailable",
      "The provisional founder cannot leave until the government finishes forming.",
    ),
  },
  "thursday-assembly": readyActions,
  "weekend-council": {
    ...readyActions,
    constitutionalHealth: {
      diagnostics: [
        {
          code: "constitutional_review_path_unavailable",
          message: "The constitution has no active review procedure.",
        },
      ],
      status: "degraded",
      statusMessage: "Most constitutional paths are operating.",
    },
    introduceConstitutionalReview: unavailable(
      "procedure_missing",
      "This polity has no active constitutional review procedure.",
    ),
  },
};

export function findPolityMotionFixture(polityId: string, motionId: string) {
  return findPolityFixture(polityId)?.motions.find(
    (motion) => motion.id === motionId,
  );
}

export function findPolityFixture(polityId: string) {
  return polities.find((polity) => polity.id === polityId);
}

export function findPolityActionAvailabilityFixture(polityId: string) {
  return actionAvailabilityByPolityId[polityId];
}

export function listPolityFixtures(query?: string) {
  if (!query) {
    return polities;
  }

  const normalizedQuery = query.toLocaleLowerCase();

  return polities.filter((polity) =>
    polity.name.toLocaleLowerCase().includes(normalizedQuery),
  );
}
