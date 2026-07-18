import { setupWorker } from "msw/browser";
import { createGoverningJourneyScenarioHandlers } from "@/mocks/scenarios/governing-journey";
import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";
import { handleUnhandledBrowserRequest } from "@/mocks/unhandled-request";

const worker = setupWorker(
  ...createMembershipInvitationScenarioHandlers(),
  ...createGoverningJourneyScenarioHandlers(),
);

export function startBrowserApiMocking() {
  return worker.start({ onUnhandledRequest: handleUnhandledBrowserRequest });
}
