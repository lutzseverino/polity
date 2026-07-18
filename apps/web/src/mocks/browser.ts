import { setupWorker } from "msw/browser";

import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";
import { handleUnhandledBrowserRequest } from "@/mocks/unhandled-request";

const worker = setupWorker(...createMembershipInvitationScenarioHandlers());

export function startBrowserApiMocking() {
  return worker.start({ onUnhandledRequest: handleUnhandledBrowserRequest });
}
