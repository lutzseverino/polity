import { Tooltip, TooltipContent, TooltipTrigger } from "@polity/ui/tooltip";
import type { ReactElement } from "react";

type AppTooltipProps = Readonly<{
  children: ReactElement;
  label: string;
}>;

function AppTooltip({ children, label }: AppTooltipProps) {
  return (
    <Tooltip>
      <TooltipTrigger render={children} />
      <TooltipContent side="bottom">{label}</TooltipContent>
    </Tooltip>
  );
}

export { TooltipProvider as AppTooltipProvider } from "@polity/ui/tooltip";
export { AppTooltip };
