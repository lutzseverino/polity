import { createLink, type LinkComponent } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { type AnchorHTMLAttributes, forwardRef } from "react";

import { cn } from "@/lib/utils";

interface AppBackLinkBaseProps
  extends AnchorHTMLAttributes<HTMLAnchorElement> {}

const AppBackLinkBase = forwardRef<HTMLAnchorElement, AppBackLinkBaseProps>(
  ({ children, className, ...props }, ref) => (
    <a
      className={cn(
        "focus-indicator inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground focus-visible:rounded",
        className,
      )}
      ref={ref}
      {...props}
    >
      <ArrowLeft aria-hidden="true" className="size-4" />
      {children}
    </a>
  ),
);

const CreatedAppBackLink = createLink(AppBackLinkBase);

export const AppBackLink: LinkComponent<typeof AppBackLinkBase> = (props) => (
  <CreatedAppBackLink preload="intent" {...props} />
);
