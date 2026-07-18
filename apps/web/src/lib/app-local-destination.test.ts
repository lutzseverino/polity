import { describe, expect, it } from "vitest";

import { readAppLocalDestination } from "@/lib/app-local-destination";

describe("app-local return destinations", () => {
  it.each([
    ["/polities", "/polities"],
    ["/inbox?category=updates#latest", "/inbox?category=updates#latest"],
    ["https://attacker.test", undefined],
    ["//attacker.test/path", undefined],
    ["/\\attacker.test/path", undefined],
    ["javascript:alert(1)", undefined],
    ["sign-in", undefined],
    ["/sign-in", undefined],
    ["/sign-in/", undefined],
    ["/sign-in//", undefined],
  ])("normalizes %s", (value, expected) => {
    expect(readAppLocalDestination(value)).toBe(expected);
  });
});
