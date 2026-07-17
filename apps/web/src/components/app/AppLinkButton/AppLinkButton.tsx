import { createLink, type LinkComponent } from "@tanstack/react-router";
import type { VariantProps } from "class-variance-authority";
import { type AnchorHTMLAttributes, forwardRef } from "react";

import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface AppLinkButtonBaseProps
  extends AnchorHTMLAttributes<HTMLAnchorElement>,
    VariantProps<typeof buttonVariants> {}

const AppLinkButtonBase = forwardRef<HTMLAnchorElement, AppLinkButtonBaseProps>(
  ({ className, size = "default", variant = "default", ...props }, ref) => (
    <a
      className={cn(buttonVariants({ className, size, variant }))}
      data-slot="button"
      ref={ref}
      {...props}
    />
  ),
);

const CreatedAppLinkButton = createLink(AppLinkButtonBase);

export const AppLinkButton: LinkComponent<typeof AppLinkButtonBase> = (
  props,
) => <CreatedAppLinkButton preload="intent" {...props} />;
