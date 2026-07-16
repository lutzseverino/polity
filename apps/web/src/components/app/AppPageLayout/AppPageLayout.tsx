import type { ComponentProps } from "react";

import { cn } from "@/lib/utils";

type AppPageMeasure = "focused" | "narrow" | "standard" | "wide";

const pageMeasureClassName: Readonly<Record<AppPageMeasure, string>> = {
  focused: "mx-auto max-w-3xl",
  narrow: "mx-auto max-w-2xl",
  standard: "mx-auto max-w-4xl",
  wide: "max-w-none",
};

type AppPageLayoutProps = ComponentProps<"div"> &
  Readonly<{
    measure: AppPageMeasure;
  }>;

export function AppPageLayout({
  className,
  measure,
  ...props
}: AppPageLayoutProps) {
  return (
    <div
      {...props}
      className={cn(
        "flex w-full flex-col gap-6",
        pageMeasureClassName[measure],
        className,
      )}
      data-measure={measure}
      data-slot="page-layout"
    />
  );
}
