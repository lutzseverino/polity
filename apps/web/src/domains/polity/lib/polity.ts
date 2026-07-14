import type { Motion } from "@/domains/motion";

type AttentionItem = Readonly<{
  description: string;
  dueLabel: string;
  id: string;
  kind: "candidacy" | "formation" | "vote";
  target:
    | Readonly<{ kind: "motion"; motionId: string }>
    | Readonly<{ kind: "polity" }>;
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
  readiness: "forming" | "ready";
  readinessMessage: string;
  recentActivity: readonly OfficialActivity[];
  role: string;
  visibility: "private" | "public";
}>;
