import type { MessageDescriptor } from "@lingui/core";
import { msg } from "@lingui/core/macro";

export type ActionDefinition = Readonly<{
  description: MessageDescriptor;
  id: string;
  keywords: readonly MessageDescriptor[];
  label: MessageDescriptor;
  unavailableIn?: readonly string[];
  unavailableReason?: MessageDescriptor;
}>;

const actionDefinitions: readonly ActionDefinition[] = [
  {
    description: msg`Put a decision before the polity for an official vote.`,
    id: "propose-resolution",
    keywords: [msg`motion`, msg`decision`, msg`vote`, msg`proposal`],
    label: msg`Propose a Resolution`,
  },
  {
    description: msg`Open nominations and elect someone to an office.`,
    id: "start-election",
    keywords: [msg`office`, msg`candidate`, msg`nomination`, msg`elect`],
    label: msg`Start an Election`,
    unavailableIn: ["neighbourhood-table"],
    unavailableReason: msg`Finish forming this polity before starting an election.`,
  },
  {
    description: msg`Ask someone to become a member of this polity.`,
    id: "invite-member",
    keywords: [msg`person`, msg`citizen`, msg`membership`, msg`join`],
    label: msg`Invite a Member`,
  },
  {
    description: msg`Propose a change to the polity’s governing rules.`,
    id: "amend-constitution",
    keywords: [msg`rules`, msg`government`, msg`procedure`, msg`power`],
    label: msg`Amend the Constitution`,
  },
  {
    description: msg`Request review of whether an act follows the constitution.`,
    id: "request-review",
    keywords: [msg`justice`, msg`challenge`, msg`constitutional`, msg`appeal`],
    label: msg`Request Constitutional Review`,
    unavailableIn: ["neighbourhood-table", "weekend-council"],
    unavailableReason: msg`This polity has no active constitutional review procedure.`,
  },
];

export function findActionDefinition(actionId: string | null) {
  return actionDefinitions.find((action) => action.id === actionId);
}

export function filterActionDefinitions(
  query: string,
  translate: (message: MessageDescriptor) => string,
) {
  const normalizedQuery = query.trim().toLocaleLowerCase();

  if (!normalizedQuery) {
    return actionDefinitions;
  }

  return actionDefinitions.filter((action) =>
    [action.label, action.description, ...action.keywords]
      .map(translate)
      .join(" ")
      .toLocaleLowerCase()
      .includes(normalizedQuery),
  );
}
