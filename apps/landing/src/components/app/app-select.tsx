import type { ComponentProps } from "react";

import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

type AppSelectTreatment = "default" | "utility";

type AppSelectProps = Readonly<ComponentProps<typeof Select>>;
type AppSelectContentProps = Readonly<ComponentProps<typeof SelectContent>>;
type AppSelectGroupProps = Readonly<ComponentProps<typeof SelectGroup>>;
type AppSelectItemProps = Readonly<
  ComponentProps<typeof SelectItem> & {
    treatment?: AppSelectTreatment;
  }
>;
type AppSelectLabelProps = Readonly<ComponentProps<typeof SelectLabel>>;
type AppSelectSeparatorProps = Readonly<ComponentProps<typeof SelectSeparator>>;
type AppSelectTriggerProps = Readonly<
  ComponentProps<typeof SelectTrigger> & {
    treatment?: AppSelectTreatment;
  }
>;
type AppSelectValueProps = Readonly<ComponentProps<typeof SelectValue>>;

function AppSelect(props: AppSelectProps) {
  return <Select {...props} />;
}

function AppSelectContent(props: AppSelectContentProps) {
  return <SelectContent {...props} />;
}

function AppSelectGroup(props: AppSelectGroupProps) {
  return <SelectGroup {...props} />;
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

function AppSelectLabel(props: AppSelectLabelProps) {
  return <SelectLabel {...props} />;
}

function AppSelectSeparator(props: AppSelectSeparatorProps) {
  return <SelectSeparator {...props} />;
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

function AppSelectValue(props: AppSelectValueProps) {
  return <SelectValue {...props} />;
}

export {
  AppSelect,
  AppSelectContent,
  AppSelectGroup,
  AppSelectItem,
  AppSelectLabel,
  AppSelectSeparator,
  AppSelectTrigger,
  AppSelectValue,
};
