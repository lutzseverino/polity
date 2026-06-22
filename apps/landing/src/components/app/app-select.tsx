import type { ComponentProps } from "react";

import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

type AppSelectProps = Readonly<ComponentProps<typeof Select>>;
type AppSelectGroupProps = Readonly<ComponentProps<typeof SelectGroup>>;
type AppSelectValueProps = Readonly<ComponentProps<typeof SelectValue>>;
type AppSelectTriggerProps = Readonly<ComponentProps<typeof SelectTrigger>>;
type AppSelectContentProps = Readonly<ComponentProps<typeof SelectContent>>;
type AppSelectLabelProps = Readonly<ComponentProps<typeof SelectLabel>>;
type AppSelectItemProps = Readonly<ComponentProps<typeof SelectItem>>;

function AppSelect(props: AppSelectProps) {
  return <Select {...props} />;
}

function AppSelectGroup(props: AppSelectGroupProps) {
  return <SelectGroup {...props} />;
}

function AppSelectValue(props: AppSelectValueProps) {
  return <SelectValue {...props} />;
}

function AppSelectTrigger({ className, ...props }: AppSelectTriggerProps) {
  return (
    <SelectTrigger className={cn("bg-background/75", className)} {...props} />
  );
}

function AppSelectContent(props: AppSelectContentProps) {
  return <SelectContent {...props} />;
}

function AppSelectLabel(props: AppSelectLabelProps) {
  return <SelectLabel {...props} />;
}

function AppSelectItem(props: AppSelectItemProps) {
  return <SelectItem {...props} />;
}

export {
  AppSelect,
  AppSelectContent,
  AppSelectGroup,
  AppSelectItem,
  AppSelectLabel,
  AppSelectTrigger,
  AppSelectValue,
};
