import { cva, type VariantProps } from "class-variance-authority";
import type { HTMLAttributes } from "react";

import { cn } from "@/lib/utils";

const appTextVariants = cva("", {
  variants: {
    variant: {
      body: "text-sm leading-6",
      caption: "text-xs text-muted-foreground",
      captionStrong: "text-xs font-medium text-muted-foreground",
      contentTitle: "text-pretty text-xl font-semibold",
      eyebrow: "text-sm font-medium text-muted-foreground",
      metric: "text-xl font-semibold",
      pageTitle:
        "text-pretty text-2xl font-semibold tracking-tight sm:text-3xl",
      prose: "text-base leading-7",
      sectionTitle: "text-pretty text-lg font-semibold",
      strong: "text-sm font-medium",
      subsectionTitle: "text-pretty text-base font-semibold",
      supporting: "text-sm leading-6 text-muted-foreground",
    },
  },
  defaultVariants: {
    variant: "body",
  },
});

type TextElement =
  | "dd"
  | "div"
  | "dt"
  | "h1"
  | "h2"
  | "h3"
  | "p"
  | "small"
  | "span";

type AppTextProps = HTMLAttributes<HTMLElement> &
  VariantProps<typeof appTextVariants> &
  Readonly<{
    as?: TextElement;
  }>;

export function AppText({
  as: Component = "p",
  className,
  variant,
  ...props
}: AppTextProps) {
  return (
    <Component
      className={cn(appTextVariants({ variant }), className)}
      {...props}
    />
  );
}
