import { forwardRef } from "react";
import {
  Pressable,
  type PressableProps,
  StyleSheet,
  type View,
} from "react-native";

import { useDesignTheme } from "@/design/useDesignTheme";

import { AppText } from "./AppText";

type PosterButtonProps = Omit<PressableProps, "children"> & {
  label: string;
  variant?: "primary" | "secondary";
};

export const PosterButton = forwardRef<View, PosterButtonProps>(
  ({ label, style, variant = "primary", ...props }, ref) => {
    const theme = useDesignTheme();
    const isPrimary = variant === "primary";

    return (
      <Pressable
        ref={ref}
        style={(state) => [
          styles.button,
          {
            backgroundColor: isPrimary ? theme.colors.primary : "transparent",
            borderColor: isPrimary ? theme.colors.primary : theme.colors.ink,
            opacity: state.pressed ? 0.78 : 1,
          },
          typeof style === "function" ? style(state) : style,
        ]}
        {...props}
      >
        <AppText tone={isPrimary ? "inverse" : "default"} variant="label">
          {label}
        </AppText>
      </Pressable>
    );
  },
);

PosterButton.displayName = "PosterButton";

const styles = StyleSheet.create({
  button: {
    alignItems: "center",
    borderRadius: 0,
    borderWidth: 2,
    minHeight: 46,
    justifyContent: "center",
    paddingHorizontal: 18,
  },
});
