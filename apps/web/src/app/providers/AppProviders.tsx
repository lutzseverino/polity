import { I18nProvider } from "@lingui/react";
import type { ReactNode } from "react";

import { i18n } from "@/app/i18n/i18n";

type AppProvidersProps = Readonly<{
  children: ReactNode;
}>;

export function AppProviders({ children }: AppProvidersProps) {
  return <I18nProvider i18n={i18n}>{children}</I18nProvider>;
}
