import { HttpResponse, http, type RequestHandler } from "msw";

type AccountScenarioMode = "failed" | "immediate" | "pending-to-applied";

type AccountScenarioOptions = Readonly<{
  initiallyProvisioned?: boolean;
  mode?: AccountScenarioMode;
  pendingReadsBeforeApplied?: number;
}>;

const accountUserId = "99999999-9999-4999-8999-999999999999";
const grantReceiptId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";

function accountResponse(status: "applied" | "failed" | "pending") {
  return {
    grants: {
      failureCode: status === "failed" ? "grant_application_failed" : null,
      receiptId: grantReceiptId,
      status,
    },
    userId: accountUserId,
  };
}

function notFound() {
  return HttpResponse.json(
    {
      error: {
        code: "polity_account_not_found",
        message: "Polity account not found.",
      },
    },
    { status: 404 },
  );
}

export function createAccountScenarioHandlers({
  initiallyProvisioned = false,
  mode = "immediate",
  pendingReadsBeforeApplied = 1,
}: AccountScenarioOptions = {}): RequestHandler[] {
  let provisioned = initiallyProvisioned;
  let pendingReads = 0;
  let status: "applied" | "failed" | "pending" =
    mode === "failed"
      ? "failed"
      : mode === "pending-to-applied"
        ? "pending"
        : "applied";

  return [
    http.get("/api/v1/polity/account", () => {
      if (!provisioned) return notFound();
      if (status === "pending" && pendingReads >= pendingReadsBeforeApplied) {
        status = "applied";
      } else if (status === "pending") {
        pendingReads += 1;
      }
      return HttpResponse.json(accountResponse(status));
    }),
    http.post("/api/v1/polity/account", () => {
      const created = !provisioned;
      provisioned = true;
      return HttpResponse.json(accountResponse(status), {
        status: created ? 201 : 200,
      });
    }),
  ];
}
