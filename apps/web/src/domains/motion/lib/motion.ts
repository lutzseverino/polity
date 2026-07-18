export type VoteChoice = "yes" | "no" | "abstain";

export type MotionStatus = "voting" | "enacted" | "rejected";

export type Motion = Readonly<{
  actionKind: "candidacy" | "vote";
  body: string;
  category: string;
  closesAtLabel: string;
  currentVote?: VoteChoice;
  id: string;
  introducedBy: string;
  participation?: Readonly<{
    cast: number;
    eligible: number;
    quorumMet: boolean;
    quorumRequired: number;
  }>;
  procedure: Readonly<{
    electorate: string;
    name: string;
    notice: string;
    threshold: string;
  }>;
  result?: Readonly<{
    no: number;
    outcome: string;
    recordEntry: number;
    yes: number;
  }>;
  status: MotionStatus;
  title: string;
}>;
