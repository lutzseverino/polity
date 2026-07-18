type DevelopmentApiSourceOptions = Readonly<{
  apiMockingValue?: string;
  apiUrlValue?: string;
}>;

export type DevelopmentApiSource = Readonly<{
  apiMockingEnabled: boolean;
  apiUrl?: URL;
}>;

export function resolveDevelopmentApiSource({
  apiMockingValue,
  apiUrlValue,
}: DevelopmentApiSourceOptions): DevelopmentApiSource {
  const apiUrl = apiUrlValue ? new URL(apiUrlValue) : undefined;

  if (apiUrl && apiUrl.pathname.replace(/\/$/, "") !== "/api/v1") {
    throw new Error("VITE_API_URL must target the /api/v1 service base path.");
  }

  return {
    apiMockingEnabled: apiMockingValue === "true" || !apiUrl,
    apiUrl,
  };
}
