import { AppLanguagePicker } from "@/components/app/app-language-picker";
import { AppThemeSwitcher } from "@/components/app/app-theme-switcher";

export function LandingHeaderControls() {
  return (
    <div className="flex items-center gap-2">
      <AppLanguagePicker />
      <AppThemeSwitcher />
    </div>
  );
}
