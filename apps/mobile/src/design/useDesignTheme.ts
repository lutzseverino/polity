import { designTokens, nativeColorModes } from "@polity/design";
import { useColorScheme } from "react-native";

export function useDesignTheme() {
  const scheme = useColorScheme();
  const mode = scheme === "dark" ? "dark" : "light";

  return {
    ...designTokens,
    colors: nativeColorModes[mode],
    mode,
  };
}
