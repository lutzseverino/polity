import { useTranslation } from "react-i18next";
import { type Language, languageNames, languages } from "@/i18n";
import {
  AppSelect,
  AppSelectContent,
  AppSelectGroup,
  AppSelectItem,
  AppSelectLabel,
  AppSelectTrigger,
  AppSelectValue,
} from "./app-select";

export function LanguagePicker() {
  const { i18n, t } = useTranslation("common");
  const currentLanguage = languages.includes(i18n.language as Language)
    ? (i18n.language as Language)
    : "en";

  function updateLanguage(value: string) {
    if (languages.includes(value as Language)) {
      void i18n.changeLanguage(value);
    }
  }

  return (
    <AppSelect onValueChange={updateLanguage} value={currentLanguage}>
      <AppSelectTrigger
        aria-label={t("language.label")}
        className="w-[7.5rem]"
        size="sm"
        treatment="utility"
      >
        <AppSelectValue />
      </AppSelectTrigger>
      <AppSelectContent align="end" position="popper">
        <AppSelectGroup>
          <AppSelectLabel>{t("language.label")}</AppSelectLabel>
          {languages.map((language) => (
            <AppSelectItem key={language} treatment="utility" value={language}>
              {languageNames[language]}
            </AppSelectItem>
          ))}
        </AppSelectGroup>
      </AppSelectContent>
    </AppSelect>
  );
}
