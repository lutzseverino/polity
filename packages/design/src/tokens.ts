import tokens from "./tokens.json";

export const designTokens = tokens;
export const colorModes = designTokens.colorModes;
export const spacing = designTokens.spacing;

export type DesignTokens = typeof designTokens;
export type ColorMode = keyof DesignTokens["colorModes"];
export type SemanticColor = keyof DesignTokens["colorModes"]["light"];
export type WebColorMode = Record<SemanticColor, string>;
export type NativeColorMode = Record<SemanticColor, string>;

export const webColorModes: Record<ColorMode, WebColorMode> = {
  dark: Object.fromEntries(
    Object.entries(colorModes.dark).map(([name, value]) => [name, value.css]),
  ) as WebColorMode,
  light: Object.fromEntries(
    Object.entries(colorModes.light).map(([name, value]) => [name, value.css]),
  ) as WebColorMode,
};

export const nativeColorModes: Record<ColorMode, NativeColorMode> = {
  dark: Object.fromEntries(
    Object.entries(colorModes.dark).map(([name, value]) => [
      name,
      value.native,
    ]),
  ) as NativeColorMode,
  light: Object.fromEntries(
    Object.entries(colorModes.light).map(([name, value]) => [
      name,
      value.native,
    ]),
  ) as NativeColorMode,
};
