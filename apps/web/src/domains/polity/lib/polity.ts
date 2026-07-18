import type { Motion } from "@/domains/motion";

export type ActionAvailability = Readonly<{
  available: boolean;
  reason?: string;
  reasonMessage?: string;
}>;

export type PolityActionAvailability = Readonly<{
  constitutionalHealth: Readonly<{
    diagnostics: readonly Readonly<{ code: string; message: string }>[];
    status: "critical" | "degraded" | "healthy";
    statusMessage: string;
  }>;
  introduceAmendment: ActionAvailability;
  introduceAppeal: ActionAvailability;
  introduceConstitutionalReview: ActionAvailability;
  introduceDisbandment: ActionAvailability;
  introduceMotion: ActionAvailability;
  introduceOfficeElection: ActionAvailability;
  introduceOfficeTermReview: ActionAvailability;
  introduceSanction: ActionAvailability;
  inviteMembers: ActionAvailability;
  readiness: Readonly<{
    diagnostics: readonly Readonly<{ code: string; message: string }>[];
    status:
      | "blocked"
      | "disbanded"
      | "forming_offices"
      | "provisional"
      | "ready";
    statusMessage: string;
  }>;
  requestCertification: ActionAvailability;
  resignMembership: ActionAvailability;
}>;

type AttentionItem = Readonly<{
  description: string;
  dueLabel: string;
  id: string;
  kind: "candidacy" | "formation" | "vote";
  target:
    | Readonly<{ actionId: string; kind: "action" }>
    | Readonly<{ kind: "motion"; motionId: string }>;
  title: string;
}>;

type OfficialActivity = Readonly<{
  id: string;
  label: string;
  timeLabel: string;
  title: string;
}>;

export type Polity = Readonly<{
  attention: readonly AttentionItem[];
  constitutionVersion: number;
  id: string;
  memberCount: number;
  motions: readonly Motion[];
  name: string;
  readiness: "forming" | "ready" | "unavailable";
  readinessMessage: string;
  recentActivity: readonly OfficialActivity[];
  status: "active" | "disbanded";
  visibility: "private" | "public";
}>;

export type PolitySummary = Readonly<{
  constitutionVersion: number;
  id: string;
  institutionName: string;
  name: string;
  status: "active" | "disbanded";
  visibility: "private" | "public";
}>;
