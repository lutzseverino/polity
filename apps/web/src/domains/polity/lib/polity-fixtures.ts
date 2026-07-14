import type { Polity } from "@/domains/polity/lib/polity";

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
      target: { kind: "polity" },
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

export function findPolityMotionFixture(polityId: string, motionId: string) {
  return findPolityFixture(polityId)?.motions.find(
    (motion) => motion.id === motionId,
  );
}

export function findPolityFixture(polityId: string) {
  return polities.find((polity) => polity.id === polityId);
}

export function listPolityFixtures() {
  return polities;
}
