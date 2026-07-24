import { setupWorker } from "msw/browser";
import { createAccountScenarioHandlers } from "@/mocks/scenarios/account";
import { createGoverningJourneyScenarioHandlers } from "@/mocks/scenarios/governing-journey";
import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";
import {
  createSessionScenarioHandlers,
  materializeBrowserSessionScenarioCsrf,
} from "@/mocks/scenarios/session";
import { handleUnhandledBrowserRequest } from "@/mocks/unhandled-request";

const worker = setupWorker(
  ...createSessionScenarioHandlers(),
  ...createAccountScenarioHandlers(),
  ...createMembershipInvitationScenarioHandlers(),
  ...createGoverningJourneyScenarioHandlers(),
);

export function startBrowserApiMocking() {
  materializeBrowserSessionScenarioCsrf();
  return worker.start({ onUnhandledRequest: handleUnhandledBrowserRequest });
}
