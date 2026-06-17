import { Monitor, Moon, Sun } from "lucide-react";
import { useTranslation } from "react-i18next";

import { type ThemeMode, themeModes, useTheme } from "@/theme/ThemeProvider";

import { AppToggleGroup, AppToggleGroupItem } from "./app-toggle-group";
import { AppTooltip } from "./app-tooltip";

const themeIcons = {
  light: Sun,
  dark: Moon,
  system: Monitor,
} satisfies Record<ThemeMode, typeof Sun>;

export function AppThemeSwitcher() {
  const { t } = useTranslation("common");
  const { setTheme, theme } = useTheme();

  function updateTheme(value: string) {
    if (themeModes.includes(value as ThemeMode)) {
      setTheme(value as ThemeMode);
    }
  }

  return (
    <AppToggleGroup
      aria-label={t("theme.label")}
      className="border bg-background/75 p-0.5"
      onValueChange={updateTheme}
      spacing={0}
      type="single"
      value={theme}
    >
      {themeModes.map((mode) => {
        const Icon = themeIcons[mode];
        const label = t(`theme.options.${mode}`);

        return (
          <AppTooltip key={mode} label={label}>
            <AppToggleGroupItem
              aria-label={label}
              size="sm"
              value={mode}
              variant="outline"
            >
              <Icon aria-hidden="true" data-icon="inline-start" />
            </AppToggleGroupItem>
          </AppTooltip>
        );
      })}
    </AppToggleGroup>
  );
}
