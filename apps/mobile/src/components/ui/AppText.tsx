import type { ReactNode } from "react";
import { StyleSheet, Text, type TextStyle } from "react-native";

import { useDesignTheme } from "@/design/useDesignTheme";

type AppTextProps = {
  children: ReactNode;
  style?: TextStyle;
  tone?: "default" | "muted" | "primary" | "inverse";
  variant?: "body" | "display" | "label" | "title";
};

export function AppText({
  children,
  style,
  tone = "default",
  variant = "body",
}: AppTextProps) {
  const theme = useDesignTheme();

  return (
    <Text
      style={[
        styles.base,
        variant === "display" && styles.display,
        variant === "title" && styles.title,
        variant === "label" && styles.label,
        {
          color:
            tone === "muted"
              ? theme.colors.mutedForeground
              : tone === "primary"
                ? theme.colors.primary
                : tone === "inverse"
                  ? theme.colors.primaryForeground
                  : theme.colors.ink,
        },
        style,
      ]}
    >
      {children}
    </Text>
  );
}

const styles = StyleSheet.create({
  base: {
    fontSize: 16,
    lineHeight: 24,
  },
  display: {
    fontSize: 52,
    fontWeight: "900",
    letterSpacing: 0,
    lineHeight: 54,
    textTransform: "uppercase",
  },
  label: {
    fontSize: 11,
    fontWeight: "700",
    letterSpacing: 2.2,
    lineHeight: 14,
    textTransform: "uppercase",
  },
  title: {
    fontSize: 28,
    fontWeight: "800",
    letterSpacing: 0,
    lineHeight: 34,
  },
});
