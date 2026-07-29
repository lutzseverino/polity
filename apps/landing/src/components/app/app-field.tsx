import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

type AppFieldOrientation = "horizontal" | "responsive" | "vertical";

type AppFieldProps = Readonly<ComponentProps<"fieldset">> & {
  orientation?: AppFieldOrientation;
};
const orientationClassName = {
  horizontal:
    "flex-row items-center has-[>[data-slot=field-content]]:items-start *:data-[slot=field-label]:flex-auto",
  responsive:
    "flex-col *:w-full @md/field-group:flex-row @md/field-group:items-center @md/field-group:*:w-auto @md/field-group:has-[>[data-slot=field-content]]:items-start @md/field-group:*:data-[slot=field-label]:flex-auto",
  vertical: "flex-col *:w-full [&>.sr-only]:w-auto",
} satisfies Record<AppFieldOrientation, string>;

function AppField({
  className,
  orientation = "vertical",
  ...props
}: AppFieldProps) {
  return (
    <fieldset
      className={cn(
        "group/field m-0 flex min-w-0 w-full gap-2 border-0 p-0 data-[invalid=true]:text-destructive",
        orientationClassName[orientation],
        className,
      )}
      data-orientation={orientation}
      data-slot="field"
      {...props}
    />
  );
}

export {
  FieldDescription as AppFieldDescription,
  FieldError as AppFieldError,
  FieldGroup as AppFieldGroup,
  FieldLabel as AppFieldLabel,
  FieldTitle as AppFieldTitle,
} from "@polity/ui/field";
export { AppField };
