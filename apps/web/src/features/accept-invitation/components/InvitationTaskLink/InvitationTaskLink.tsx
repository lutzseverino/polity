import { Link } from "@tanstack/react-router";
import type { ReactNode } from "react";

type InvitationTaskLinkProps = Readonly<{
  children: ReactNode;
  className?: string;
  invitationId: string;
}>;

export function InvitationTaskLink({
  children,
  className,
  invitationId,
}: InvitationTaskLinkProps) {
  return (
    <Link
      className={className}
      mask={{
        params: { invitationId },
        to: "/polities/invitations/$invitationId",
        unmaskOnReload: true,
      }}
      resetScroll={false}
      search={(previous) => ({
        ...previous,
        task: {
          invitationId,
          kind: "invitation-response" as const,
        },
      })}
      to="."
    >
      {children}
    </Link>
  );
}
