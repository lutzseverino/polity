import type { ReactNode } from "react";

import type { ActionId } from "@/features/launch-action/lib/action-definitions";

export type PolityOption = Readonly<{
  id: string;
  name: string;
  slug: string;
}>;

export type ActionLauncherVariant = "compact" | "surface";

export type ActionLauncherActionLinkProps = Readonly<{
  actionId: ActionId;
  children: ReactNode;
  className: string;
  onSelect?: () => void;
  polityId: string;
}>;

export type ActionLauncherEmptyActionLinkProps = Readonly<{
  children: ReactNode;
  kind: "explore-polities" | "found-polity";
}>;
