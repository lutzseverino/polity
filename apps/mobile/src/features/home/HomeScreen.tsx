import { StyleSheet, View } from "react-native";

import { AppText } from "@/components/ui/AppText";
import { PosterButton } from "@/components/ui/PosterButton";
import { Screen } from "@/components/ui/Screen";
import { useDesignTheme } from "@/design/useDesignTheme";

const foundations = [
  "Constitute a group",
  "Keep motions legible",
  "Make the record durable",
] as const;

export function HomeScreen() {
  const theme = useDesignTheme();

  return (
    <Screen>
      <View style={styles.container}>
        <View style={styles.brandRow}>
          <View
            style={[
              styles.brandMark,
              { backgroundColor: theme.colors.primary },
            ]}
          />
          <AppText variant="label">decreos mobile</AppText>
        </View>

        <View style={styles.hero}>
          <AppText variant="display">
            Every group deserves a constitution.
          </AppText>
          <AppText style={styles.lede} tone="muted">
            The native app starts from the same design grammar as the landing
            page, but grows through product features.
          </AppText>
        </View>

        <View style={[styles.panel, { backgroundColor: theme.colors.card }]}>
          {foundations.map((item, index) => (
            <View key={item} style={styles.recordRow}>
              <AppText tone="primary" variant="label">
                No. {String(index + 1).padStart(3, "0")}
              </AppText>
              <AppText>{item}</AppText>
            </View>
          ))}
        </View>

        <View style={styles.actions}>
          <PosterButton label="Start a polity" />
          <PosterButton label="Sign in" variant="secondary" />
        </View>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  actions: {
    gap: 12,
  },
  brandMark: {
    height: 10,
    width: 10,
  },
  brandRow: {
    alignItems: "center",
    flexDirection: "row",
    gap: 10,
  },
  container: {
    flex: 1,
    gap: 32,
    justifyContent: "center",
    paddingHorizontal: 24,
    paddingVertical: 32,
  },
  hero: {
    gap: 18,
  },
  lede: {
    maxWidth: 520,
  },
  panel: {
    borderRadius: 0,
    gap: 1,
    padding: 16,
  },
  recordRow: {
    gap: 4,
    paddingVertical: 12,
  },
});
