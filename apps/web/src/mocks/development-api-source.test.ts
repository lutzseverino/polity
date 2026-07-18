import { describe, expect, it } from "vitest";

import { resolveDevelopmentApiSource } from "@/mocks/development-api-source";

describe("development API source", () => {
  it("uses browser mocking when no live API URL is configured", () => {
    expect(resolveDevelopmentApiSource({})).toEqual({
      apiMockingEnabled: true,
      apiUrl: undefined,
    });
  });

  it("uses the live proxy when its URL is configured", () => {
    const source = resolveDevelopmentApiSource({
      apiMockingValue: "false",
      apiUrlValue: "http://localhost:8084/api/v1",
    });

    expect(source.apiMockingEnabled).toBe(false);
    expect(source.apiUrl?.href).toBe("http://localhost:8084/api/v1");
  });

  it("lets the explicit flag force mocking over a configured URL", () => {
    const source = resolveDevelopmentApiSource({
      apiMockingValue: "true",
      apiUrlValue: "http://localhost:8084/api/v1",
    });

    expect(source.apiMockingEnabled).toBe(true);
  });

  it("rejects a live URL that does not target the service base path", () => {
    expect(() =>
      resolveDevelopmentApiSource({
        apiUrlValue: "http://localhost:8084/wrong",
      }),
    ).toThrow("VITE_API_URL must target the /api/v1 service base path.");
  });
});
