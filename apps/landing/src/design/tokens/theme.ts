import { designTokens } from "@polity/design";

export const designTheme = designTokens;

export type DesignTheme = typeof designTheme;
export type DesignThemeMode = keyof DesignTheme["colorModes"];
