export function readAppLocalDestination(value: unknown) {
  if (typeof value !== "string" || !value.startsWith("/")) return undefined;

  try {
    const base = new URL("https://polity.local");
    const destination = new URL(value, base);
    if (destination.origin !== base.origin) return undefined;
    if (/^\/sign-in\/?$/.test(destination.pathname)) return undefined;
    return `${destination.pathname}${destination.search}${destination.hash}`;
  } catch {
    return undefined;
  }
}
