import { describe, expect, it } from "vitest";

import {
  politiesQueryOptions,
  polityMotionQueryOptions,
  polityQueryOptions,
} from "@/domains/polity/api/polity-queries";

describe("polity queries", () => {
  it("includes locale in every localized cache identity", () => {
    const english = [
      politiesQueryOptions({ locale: "en" }).queryKey,
      polityQueryOptions({ locale: "en", polityId: "polity-1" }).queryKey,
      polityMotionQueryOptions({
        locale: "en",
        motionId: "motion-1",
        polityId: "polity-1",
      }).queryKey,
    ];
    const spanish = [
      politiesQueryOptions({ locale: "es" }).queryKey,
      polityQueryOptions({ locale: "es", polityId: "polity-1" }).queryKey,
      polityMotionQueryOptions({
        locale: "es",
        motionId: "motion-1",
        polityId: "polity-1",
      }).queryKey,
    ];

    expect(english).not.toEqual(spanish);
  });
});
