import type { MessageDescriptor } from "@lingui/core";
import { msg } from "@lingui/core/macro";
import type { PolityActionAvailability } from "@/domains/polity";

type LaunchableActionAvailabilityKey = keyof Pick<
  PolityActionAvailability,
  | "introduceAmendment"
  | "introduceAppeal"
  | "introduceConstitutionalReview"
  | "introduceDisbandment"
  | "introduceMotion"
  | "introduceOfficeElection"
  | "introduceOfficeTermReview"
  | "introduceSanction"
  | "inviteMembers"
  | "requestCertification"
  | "resignMembership"
>;

type ActionDefinitionShape = Readonly<{
  availabilityKey: LaunchableActionAvailabilityKey;
  description: MessageDescriptor;
  goal: MessageDescriptor;
  id: string;
  keywords: readonly MessageDescriptor[];
  label: MessageDescriptor;
  deemphasized?: boolean;
}>;

const definedActions = [
  {
    availabilityKey: "introduceMotion",
    description: msg`Put a decision before the polity for an official vote.`,
    goal: msg`Make a group decision`,
    id: "propose-resolution",
    keywords: [
      msg`motion`,
      msg`decision`,
      msg`vote`,
      msg`proposal`,
      msg`approve an idea`,
      msg`spend money`,
      msg`adopt a policy`,
    ],
    label: msg`Propose a Resolution`,
  },
  {
    availabilityKey: "inviteMembers",
    description: msg`Ask someone to become a member of this polity.`,
    goal: msg`Invite someone to join`,
    id: "invite-member",
    keywords: [
      msg`person`,
      msg`citizen`,
      msg`membership`,
      msg`join`,
      msg`add member`,
    ],
    label: msg`Invite a Member`,
  },
  {
    availabilityKey: "introduceOfficeElection",
    description: msg`Open nominations and elect someone to an office.`,
    goal: msg`Choose people for an office`,
    id: "start-election",
    keywords: [
      msg`office`,
      msg`candidate`,
      msg`nomination`,
      msg`elect`,
      msg`choose a leader`,
      msg`fill a position`,
    ],
    label: msg`Start an Election`,
  },
  {
    availabilityKey: "introduceAmendment",
    description: msg`Propose a change to the polity’s governing rules.`,
    goal: msg`Change how this polity works`,
    id: "amend-constitution",
    keywords: [
      msg`rules`,
      msg`government`,
      msg`procedure`,
      msg`power`,
      msg`constitution`,
      msg`change our rules`,
      msg`change an office`,
    ],
    label: msg`Amend the Constitution`,
  },
  {
    availabilityKey: "introduceSanction",
    description: msg`Ask the polity to issue a warning or temporary suspension.`,
    goal: msg`Address harmful conduct`,
    id: "propose-sanction",
    keywords: [
      msg`sanction`,
      msg`warning`,
      msg`suspension`,
      msg`misconduct`,
      msg`discipline a member`,
      msg`someone broke the rules`,
    ],
    label: msg`Propose a Sanction`,
  },
  {
    availabilityKey: "introduceAppeal",
    description: msg`Ask the polity to overturn an active sanction.`,
    goal: msg`Challenge a sanction`,
    id: "appeal-sanction",
    keywords: [
      msg`appeal`,
      msg`sanction`,
      msg`suspension`,
      msg`overturn punishment`,
      msg`challenge a warning`,
    ],
    label: msg`Open an Appeal`,
  },
  {
    availabilityKey: "introduceOfficeTermReview",
    description: msg`Ask the polity to review and potentially vacate an active office term.`,
    goal: msg`Review someone in office`,
    id: "review-office-term",
    keywords: [
      msg`office`,
      msg`term`,
      msg`remove an officer`,
      msg`vacate a position`,
      msg`recall a leader`,
    ],
    label: msg`Review an Office Term`,
  },
  {
    availabilityKey: "introduceConstitutionalReview",
    description: msg`Request review of whether an act follows the constitution.`,
    goal: msg`Challenge an official act`,
    id: "request-review",
    keywords: [
      msg`justice`,
      msg`challenge`,
      msg`constitutional`,
      msg`void a decision`,
      msg`unconstitutional`,
      msg`official record`,
    ],
    label: msg`Request Constitutional Review`,
  },
  {
    availabilityKey: "requestCertification",
    description: msg`Finalize a motion whose voting or election period has ended.`,
    goal: msg`Certify a completed decision`,
    id: "certify-motion",
    keywords: [
      msg`certify`,
      msg`finalize a vote`,
      msg`close a motion`,
      msg`record the result`,
      msg`apply the outcome`,
    ],
    label: msg`Certify a Motion`,
  },
  {
    availabilityKey: "introduceDisbandment",
    description: msg`Propose permanently closing the polity through its constitutional procedure.`,
    goal: msg`Close this polity`,
    id: "disband-polity",
    keywords: [
      msg`disband`,
      msg`close`,
      msg`end the group`,
      msg`dissolve`,
      msg`shut down`,
    ],
    label: msg`Propose Disbandment`,
    deemphasized: true,
  },
  {
    availabilityKey: "resignMembership",
    description: msg`End your membership and give up your participation rights in this polity.`,
    goal: msg`Leave this polity`,
    id: "resign-membership",
    keywords: [
      msg`resign`,
      msg`leave`,
      msg`quit membership`,
      msg`exit the group`,
      msg`stop participating`,
    ],
    label: msg`Resign Membership`,
    deemphasized: true,
  },
] as const satisfies readonly ActionDefinitionShape[];

export type ActionId = (typeof definedActions)[number]["id"];
export type ActionDefinition = Omit<ActionDefinitionShape, "id"> &
  Readonly<{ id: ActionId }>;

const actionDefinitions: readonly ActionDefinition[] = definedActions;

export function findActionDefinition(actionId: string | null) {
  return actionDefinitions.find((action) => action.id === actionId);
}

export function filterActionDefinitions(
  query: string,
  translate: (message: MessageDescriptor) => string,
) {
  const normalize = (value: string) =>
    value
      .toLocaleLowerCase()
      .normalize("NFD")
      .replace(/\p{Diacritic}/gu, "")
      .trim();
  const normalizedQuery = normalize(query);

  if (!normalizedQuery) {
    return actionDefinitions;
  }

  const queryTokens = normalizedQuery.split(/\s+/);

  return actionDefinitions
    .map((action, index) => {
      const fields = [
        { text: normalize(translate(action.goal)), weight: 5 },
        { text: normalize(translate(action.label)), weight: 4 },
        ...action.keywords.map((keyword) => ({
          text: normalize(translate(keyword)),
          weight: 3,
        })),
        { text: normalize(translate(action.description)), weight: 1 },
      ];
      const tokenScores = queryTokens.map((token) =>
        Math.max(
          0,
          ...fields.map((field) =>
            field.text.includes(token) ? field.weight : 0,
          ),
        ),
      );

      if (tokenScores.some((score) => score === 0)) {
        return null;
      }

      const phraseBonus = fields.some((field) =>
        field.text.includes(normalizedQuery),
      )
        ? 10
        : 0;

      return {
        action,
        index,
        score: tokenScores.reduce((total, score) => total + score, phraseBonus),
      };
    })
    .filter(
      (
        result,
      ): result is Readonly<{
        action: ActionDefinition;
        index: number;
        score: number;
      }> => result !== null,
    )
    .sort((left, right) => right.score - left.score || left.index - right.index)
    .map((result) => result.action);
}
