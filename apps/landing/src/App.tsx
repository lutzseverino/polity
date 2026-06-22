import { AppTooltipProvider } from "@/components/app/app-tooltip";
import "@/i18n";
import { LandingPage } from "@/pages/landing/landing-page";
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
