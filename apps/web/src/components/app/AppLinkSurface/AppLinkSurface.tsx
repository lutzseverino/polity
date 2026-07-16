import { createLink, type LinkComponent } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import {
  type AnchorHTMLAttributes,
  type ComponentProps,
  forwardRef,
} from "react";

import { cn } from "@/lib/utils";

const AppLinkSurfaceBase = forwardRef<
  HTMLAnchorElement,
  AnchorHTMLAttributes<HTMLAnchorElement>
>(({ className, ...props }, ref) => (
  <a
    className={cn(
      "focus-indicator group/link-surface block min-w-0 rounded-xl",
      className,
    )}
    data-slot="link-surface"
    ref={ref}
    {...props}
  />
));

const CreatedAppLinkSurface = createLink(AppLinkSurfaceBase);

export const AppLinkSurface: LinkComponent<typeof AppLinkSurfaceBase> = (
  props,
) => <CreatedAppLinkSurface preload="intent" {...props} />;

export function AppLinkSurfaceIndicator({
  className,
  ...props
}: ComponentProps<typeof ArrowRight>) {
  return (
    <ArrowRight
      {...props}
      aria-hidden="true"
      className={cn(
        "size-4 shrink-0 text-muted-foreground transition-colors group-hover/link-surface:text-foreground group-focus-visible/link-surface:text-foreground motion-safe:transition-transform motion-safe:group-hover/link-surface:translate-x-0.5 motion-safe:group-focus-visible/link-surface:translate-x-0.5",
        className,
      )}
      data-slot="link-surface-indicator"
    />
  );
}
