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

function AppSelect(props: ComponentProps<typeof Select>) {
  return <Select {...props} />;
}

function AppSelectGroup(props: ComponentProps<typeof SelectGroup>) {
  return <SelectGroup {...props} />;
}

function AppSelectValue(props: ComponentProps<typeof SelectValue>) {
  return <SelectValue {...props} />;
}

function AppSelectTrigger({
  className,
  ...props
}: ComponentProps<typeof SelectTrigger>) {
  return (
    <SelectTrigger className={cn("bg-background/75", className)} {...props} />
  );
}

function AppSelectContent(props: ComponentProps<typeof SelectContent>) {
  return <SelectContent {...props} />;
}

function AppSelectLabel(props: ComponentProps<typeof SelectLabel>) {
  return <SelectLabel {...props} />;
}

function AppSelectItem(props: ComponentProps<typeof SelectItem>) {
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
