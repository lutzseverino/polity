import { useTranslation } from "react-i18next";

import {
  AppCard,
  AppCardContent,
  AppCardFooter,
  AppCardHeader,
} from "@/components/app/app-card";

export function OfficialRecordPlate() {
  const { t } = useTranslation("landing");

  return (
    <AppCard
      className="relative w-full max-w-[20rem] border-[3px] border-secondary bg-secondary text-secondary-foreground"
      data-hero-plate
    >
      <AppCardHeader className="flex flex-row items-center justify-between border-secondary-foreground/20 px-3 pt-2 pb-2! font-mono text-[0.58rem] leading-none tracking-[0.24em] text-secondary-foreground/85 uppercase">
        <span data-hero-mark>{t("hero.plate.official")}</span>
        <span data-hero-mark>{t("hero.plate.number")}</span>
        <span data-hero-mark>{t("hero.plate.record")}</span>
      </AppCardHeader>
      <AppCardContent className="relative aspect-[16/9] overflow-hidden p-0">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_42%,color-mix(in_oklch,var(--secondary-foreground)_34%,transparent),transparent_68%)]" />
        <div className="absolute inset-0 text-secondary-foreground opacity-70 texture-halftone [--dot:4px]" />
        <div className="absolute inset-0 text-secondary-foreground opacity-25 texture-halftone [--dot:11px]" />
      </AppCardContent>
      <AppCardFooter
        className="block rounded-none border-secondary-foreground/20 bg-secondary px-3 py-2 text-center font-mono text-[0.58rem] leading-none tracking-[0.34em] text-secondary-foreground/85 uppercase"
        data-hero-mark
      >
        {t("hero.plate.founded")}
      </AppCardFooter>
    </AppCard>
  );
}
