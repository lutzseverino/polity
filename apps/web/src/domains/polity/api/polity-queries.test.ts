import { describe, expect, it } from "vitest";

import {
  politiesQueryOptions,
  polityActionsQueryOptions,
  polityGovernmentQueryOptions,
  polityMotionQueryOptions,
  polityOptionsQueryOptions,
  polityQueryOptions,
  polityRecordQueryOptions,
} from "@/domains/polity/api/polity-queries";

describe("polity queries", () => {
  it("includes locale in every localized cache identity", () => {
    const english = [
      politiesQueryOptions({ locale: "en" }).queryKey,
      polityActionsQueryOptions({
        locale: "en",
        polityId: "polity-1",
      }).queryKey,
      polityGovernmentQueryOptions({
        locale: "en",
        polityId: "polity-1",
      }).queryKey,
      polityQueryOptions({ locale: "en", polityId: "polity-1" }).queryKey,
      polityMotionQueryOptions({
        locale: "en",
        motionId: "motion-1",
        polityId: "polity-1",
      }).queryKey,
      polityOptionsQueryOptions({ locale: "en" }).queryKey,
      polityRecordQueryOptions({
        locale: "en",
        polityId: "polity-1",
      }).queryKey,
    ];
    const spanish = [
      politiesQueryOptions({ locale: "es" }).queryKey,
      polityActionsQueryOptions({
        locale: "es",
        polityId: "polity-1",
      }).queryKey,
      polityGovernmentQueryOptions({
        locale: "es",
        polityId: "polity-1",
      }).queryKey,
      polityQueryOptions({ locale: "es", polityId: "polity-1" }).queryKey,
      polityMotionQueryOptions({
        locale: "es",
        motionId: "motion-1",
        polityId: "polity-1",
      }).queryKey,
      polityOptionsQueryOptions({ locale: "es" }).queryKey,
      polityRecordQueryOptions({
        locale: "es",
        polityId: "polity-1",
      }).queryKey,
    ];

    expect(english).not.toEqual(spanish);
  });

  it("uses the normalized polity query in the list cache identity", () => {
    expect(
      politiesQueryOptions({ locale: "en", query: "  assembly  " }).queryKey,
    ).toEqual(
      politiesQueryOptions({ locale: "en", query: "assembly" }).queryKey,
    );
    expect(
      politiesQueryOptions({ locale: "en", query: "assembly" }).queryKey,
    ).not.toEqual(politiesQueryOptions({ locale: "en" }).queryKey);
  });

  it("uses normalized pagination in the list cache identity", () => {
    expect(
      politiesQueryOptions({ locale: "en", page: -1, size: 0 }).queryKey,
    ).toEqual(
      politiesQueryOptions({ locale: "en", page: 0, size: 1 }).queryKey,
    );
    expect(
      politiesQueryOptions({ locale: "en", page: 1, size: 12 }).queryKey,
    ).not.toEqual(
      politiesQueryOptions({ locale: "en", page: 0, size: 12 }).queryKey,
    );
  });
});
