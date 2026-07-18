import { setupWorker } from "msw/browser";
import { createGoverningJourneyScenarioHandlers } from "@/mocks/scenarios/governing-journey";
import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";
import {
  createSessionScenarioHandlers,
  materializeBrowserSessionScenarioCsrf,
} from "@/mocks/scenarios/session";
import { handleUnhandledBrowserRequest } from "@/mocks/unhandled-request";

const worker = setupWorker(
  ...createSessionScenarioHandlers(),
  ...createMembershipInvitationScenarioHandlers(),
  ...createGoverningJourneyScenarioHandlers(),
);

export function startBrowserApiMocking() {
  materializeBrowserSessionScenarioCsrf();
  return worker.start({ onUnhandledRequest: handleUnhandledBrowserRequest });
}
