import { useTranslation } from "react-i18next";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { type Language, languageNames, languages } from "@/i18n";

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
    <Select onValueChange={updateLanguage} value={currentLanguage}>
      <SelectTrigger
        aria-label={t("language.label")}
        className="w-[7.5rem] bg-background/75 font-mono text-[0.65rem] tracking-[0.16em]"
        size="sm"
      >
        <SelectValue />
      </SelectTrigger>
      <SelectContent align="end" position="popper">
        <SelectGroup>
          <SelectLabel>{t("language.label")}</SelectLabel>
          {languages.map((language) => (
            <SelectItem
              className="font-mono text-[0.65rem] tracking-[0.16em]"
              key={language}
              value={language}
            >
              {languageNames[language]}
            </SelectItem>
          ))}
        </SelectGroup>
      </SelectContent>
    </Select>
  );
}
