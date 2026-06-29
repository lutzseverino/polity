import type { ComponentProps } from "react";

import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldTitle,
} from "@/components/ui/field";

type AppFieldOrientation = "horizontal" | "responsive" | "vertical";

type AppFieldProps = Readonly<
  ComponentProps<"fieldset"> & {
    orientation?: AppFieldOrientation;
  }
>;
type AppFieldDescriptionProps = Readonly<ComponentProps<"p">>;
type AppFieldErrorProps = Readonly<
  ComponentProps<"div"> & {
    errors?: Array<{ message?: string } | undefined>;
  }
>;
type AppFieldGroupProps = Readonly<ComponentProps<"div">>;
type AppFieldLabelProps = Readonly<ComponentProps<"label">>;
type AppFieldTitleProps = Readonly<ComponentProps<"div">>;

function AppField(props: AppFieldProps) {
  return <Field {...props} />;
}

function AppFieldDescription(props: AppFieldDescriptionProps) {
  return <FieldDescription {...props} />;
}

function AppFieldError(props: AppFieldErrorProps) {
  return <FieldError {...props} />;
}

function AppFieldGroup(props: AppFieldGroupProps) {
  return <FieldGroup {...props} />;
}

function AppFieldLabel(props: AppFieldLabelProps) {
  return <FieldLabel {...props} />;
}

function AppFieldTitle(props: AppFieldTitleProps) {
  return <FieldTitle {...props} />;
}

export {
  AppField,
  AppFieldDescription,
  AppFieldError,
  AppFieldGroup,
  AppFieldLabel,
  AppFieldTitle,
};
