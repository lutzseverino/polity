import { Trans, useLingui } from "@lingui/react/macro";
import { Check, ShieldCheck } from "lucide-react";
import { type FormEvent, useState } from "react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppButton } from "@/components/app/AppButton";
import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppText } from "@/components/app/AppText";
import type { Motion, VoteChoice } from "@/domains/motion";
import { useCastVote } from "@/features/cast-vote/api/cast-vote-mutation";
import { cn } from "@/lib/utils";

type CastVotePanelProps = Readonly<{
  motion: Motion;
  polityId: string;
}>;

export function CastVotePanel({ motion, polityId }: CastVotePanelProps) {
  const { t } = useLingui();
  const castVote = useCastVote();
  const voteChoices: readonly Readonly<{
    description: string;
    label: string;
    value: VoteChoice;
  }>[] = [
    { description: t`Support the motion`, label: t`Yes`, value: "yes" },
    { description: t`Oppose the motion`, label: t`No`, value: "no" },
    {
      description: t`Participate without choosing a side`,
      label: t`Abstain`,
      value: "abstain",
    },
  ];
  const [recordedVote, setRecordedVote] = useState<VoteChoice | undefined>(
    motion.currentVote,
  );
  const [selectedVote, setSelectedVote] = useState<VoteChoice | undefined>(
    motion.currentVote,
  );
  const [showConfirmation, setShowConfirmation] = useState(false);

  function submitVote(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedVote) {
      return;
    }

    castVote.mutate(
      { choice: selectedVote, motionId: motion.id, polityId },
      {
        onSuccess: ({ choice }) => {
          setRecordedVote(choice);
          setShowConfirmation(true);
        },
      },
    );
  }

  return (
    <AppCard>
      <AppCardHeader>
        <AppText
          as="div"
          className="mb-1 flex items-center gap-2"
          variant="strong"
        >
          <ShieldCheck aria-hidden="true" className="size-4" />
          <Trans>Official vote</Trans>
        </AppText>
        <AppCardTitle>
          <Trans>How do you vote?</Trans>
        </AppCardTitle>
        <AppCardDescription>
          <Trans>
            Your vote determines the motion. Reactions and comments never count
            toward the result.
          </Trans>
        </AppCardDescription>
      </AppCardHeader>
      <AppCardContent>
        <form className="space-y-4" onSubmit={submitVote}>
          <fieldset className="grid gap-2">
            <legend className="sr-only">
              <Trans>Vote choice</Trans>
            </legend>
            {voteChoices.map((choice) => {
              const isSelected = selectedVote === choice.value;

              return (
                <button
                  aria-label={`${choice.label}: ${choice.description}`}
                  aria-pressed={isSelected}
                  className={cn(
                    "focus-indicator flex min-h-14 w-full items-center justify-between gap-4 rounded-lg border px-3 py-2.5 text-left transition-colors hover:bg-muted",
                    isSelected && "border-foreground bg-muted",
                  )}
                  key={choice.value}
                  onClick={() => {
                    setSelectedVote(choice.value);
                    setShowConfirmation(false);
                    castVote.reset();
                  }}
                  type="button"
                >
                  <span>
                    <AppText as="span" className="block" variant="strong">
                      {choice.label}
                    </AppText>
                    <AppText as="span" className="block" variant="caption">
                      {choice.description}
                    </AppText>
                  </span>
                  {isSelected ? (
                    <Check aria-hidden="true" className="size-4" />
                  ) : null}
                </button>
              );
            })}
          </fieldset>

          <AppButton
            className="w-full sm:w-auto"
            disabled={
              castVote.isPending ||
              !selectedVote ||
              selectedVote === recordedVote
            }
            type="submit"
          >
            {recordedVote ? (
              <Trans>Update vote</Trans>
            ) : (
              <Trans>Record vote</Trans>
            )}
          </AppButton>
        </form>

        {showConfirmation ? (
          <AppAlert className="mt-4">
            <Check aria-hidden="true" />
            <AppAlertTitle>
              <Trans>Your vote was recorded</Trans>
            </AppAlertTitle>
            <AppAlertDescription>
              <Trans>
                You voted {recordedVote}. You can change it until voting closes{" "}
                {motion.closesAtLabel.toLowerCase()}.
              </Trans>
            </AppAlertDescription>
          </AppAlert>
        ) : null}
      </AppCardContent>
    </AppCard>
  );
}
