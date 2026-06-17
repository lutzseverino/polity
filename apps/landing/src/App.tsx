import { AppTooltipProvider } from "@/components/app/app-tooltip";
import { LandingPage } from "@/features/landing/LandingPage";
import "@/i18n";
import { ThemeProvider } from "@/theme/ThemeProvider";

export default function App() {
  return (
    <ThemeProvider>
      <AppTooltipProvider>
        <LandingPage />
      </AppTooltipProvider>
    </ThemeProvider>
  );
}
