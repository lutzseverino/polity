import { msg } from "@lingui/core/macro";
import { describe, expect, it } from "vitest";

import { resolveShellContext } from "@/app/shell/shell-route-context";

const translate = (message: { message?: string }) => message.message ?? "";

describe("resolveShellContext", () => {
  it("builds a dynamic workspace breadcrumb trail from route metadata", () => {
    const context = resolveShellContext(
      [
        {
          loaderData: { shellLabel: "Thursday Assembly" },
          params: { polityId: "11111111-1111-4111-8111-111111111111" },
          shell: {
            back: { label: msg`All polities`, target: { to: "/polities" } },
            level: "workspace",
            section: "polities",
            target: { params: "polityId", to: "/polities/$polityId" },
          },
        },
        {
          params: { polityId: "11111111-1111-4111-8111-111111111111" },
          shell: {
            label: msg`Motions`,
            target: {
              params: "polityId",
              to: "/polities/$polityId/motions",
            },
          },
        },
      ],
      translate,
    );

    expect(context.title).toBe("Motions");
    expect(context.breadcrumbs.map((breadcrumb) => breadcrumb.label)).toEqual([
      "Polities",
      "Thursday Assembly",
      "Motions",
    ]);
    expect(context.back?.label).toBe("All polities");
    expect(context.polityId).toBe("11111111-1111-4111-8111-111111111111");
  });

  it("inherits the workspace title through a label-less index route", () => {
    const context = resolveShellContext(
      [
        {
          loaderData: { shellLabel: "Thursday Assembly" },
          params: { polityId: "11111111-1111-4111-8111-111111111111" },
          shell: {
            level: "workspace",
            section: "polities",
            target: { params: "polityId", to: "/polities/$polityId" },
          },
        },
        {
          params: { polityId: "11111111-1111-4111-8111-111111111111" },
          shell: { showPrimaryAction: false },
        },
      ],
      translate,
    );

    expect(context).toMatchObject({
      level: "workspace",
      showPrimaryAction: false,
      title: "Thursday Assembly",
    });
    expect(context.breadcrumbs.map((breadcrumb) => breadcrumb.label)).toEqual([
      "Polities",
      "Thursday Assembly",
    ]);
  });

  it("lets a focused detail route override inherited compact behavior", () => {
    const context = resolveShellContext(
      [
        {
          loaderData: { shellLabel: "Thursday Assembly" },
          params: { polityId: "11111111-1111-4111-8111-111111111111" },
          shell: {
            level: "workspace",
            section: "polities",
            target: { params: "polityId", to: "/polities/$polityId" },
          },
        },
        {
          loaderData: { shellLabel: "Shared Thursday Dinner" },
          params: {
            motionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
            polityId: "11111111-1111-4111-8111-111111111111",
          },
          shell: {
            back: {
              label: msg`All motions`,
              target: {
                params: "polityId",
                to: "/polities/$polityId/motions",
              },
            },
            compactLabel: msg`Motion`,
            compactNavigation: "hidden",
            level: "detail",
            showPrimaryAction: false,
          },
        },
      ],
      translate,
    );

    expect(context).toMatchObject({
      back: { label: "All motions" },
      compactNavigation: "hidden",
      level: "detail",
      showPrimaryAction: false,
      title: "Motion",
    });
  });
});
