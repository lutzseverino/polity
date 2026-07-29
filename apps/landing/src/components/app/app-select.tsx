import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from "@polity/ui/select";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

type AppSelectTreatment = "default" | "utility";

type AppSelectProps = Readonly<
  Omit<ComponentProps<typeof Select<string>>, "onValueChange"> & {
    onValueChange?: (value: string) => void;
  }
>;
type AppSelectContentProps = Readonly<
  ComponentProps<typeof SelectContent> & {
    position?: "popper";
  }
>;
type AppSelectItemProps = Readonly<
  ComponentProps<typeof SelectItem> & {
    treatment?: AppSelectTreatment;
  }
>;
type AppSelectTriggerProps = Readonly<
  ComponentProps<typeof SelectTrigger> & {
    treatment?: AppSelectTreatment;
  }
>;

function AppSelect({ onValueChange, ...props }: AppSelectProps) {
  return (
    <Select<string>
      {...props}
      onValueChange={(value) => {
        if (value !== null) {
          onValueChange?.(value);
        }
      }}
    />
  );
}

function AppSelectContent({
  position: _position,
  ...props
}: AppSelectContentProps) {
  return <SelectContent {...props} />;
}

function AppSelectItem({
  className,
  treatment = "default",
  ...props
}: AppSelectItemProps) {
  return (
    <SelectItem
      className={cn(
        treatment === "utility" && "font-mono text-[0.65rem] tracking-[0.16em]",
        className,
      )}
      {...props}
    />
  );
}

function AppSelectTrigger({
  className,
  treatment = "default",
  ...props
}: AppSelectTriggerProps) {
  return (
    <SelectTrigger
      className={cn(
        treatment === "utility" &&
          "bg-background/75 font-mono text-[0.65rem] tracking-[0.16em]",
        className,
      )}
      {...props}
    />
  );
}

export {
  SelectGroup as AppSelectGroup,
  SelectLabel as AppSelectLabel,
  SelectSeparator as AppSelectSeparator,
  SelectValue as AppSelectValue,
} from "@polity/ui/select";
export { AppSelect, AppSelectContent, AppSelectItem, AppSelectTrigger };
