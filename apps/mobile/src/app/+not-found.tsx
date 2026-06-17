import { Link } from "expo-router";
import { StyleSheet, View } from "react-native";

import { AppText } from "@/components/ui/AppText";
import { PosterButton } from "@/components/ui/PosterButton";
import { useDesignTheme } from "@/design/useDesignTheme";

export default function NotFoundScreen() {
  const theme = useDesignTheme();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.paper }]}>
      <AppText tone="muted">No official record found at this address.</AppText>
      <Link asChild href="/">
        <PosterButton label="Return home" />
      </Link>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: "center",
    flex: 1,
    gap: 20,
    justifyContent: "center",
    padding: 24,
  },
});
