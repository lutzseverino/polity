type BrowserApiMockingOption = Readonly<{
  development: boolean;
  value?: string;
}>;

type StartApplicationOptions = Readonly<{
  beforeRender?: () => Promise<unknown> | unknown;
  render: () => void;
}>;

export function isBrowserApiMockingEnabled({
  development,
  value,
}: BrowserApiMockingOption) {
  return development && value === "true";
}

export async function startApplication({
  beforeRender,
  render,
}: StartApplicationOptions) {
  await beforeRender?.();
  render();
}
