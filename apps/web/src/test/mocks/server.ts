import { setupServer } from "msw/node";
import { createAccountScenarioHandlers } from "@/mocks/scenarios/account";
import { createGoverningJourneyScenarioHandlers } from "@/mocks/scenarios/governing-journey";
import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";
import { createSessionScenarioHandlers } from "@/mocks/scenarios/session";

export const apiMockServer = setupServer(
  ...createSessionScenarioHandlers(),
  ...createAccountScenarioHandlers(),
  ...createMembershipInvitationScenarioHandlers(),
  ...createGoverningJourneyScenarioHandlers(),
);

export function resetDefaultApiMockHandlers() {
  apiMockServer.resetHandlers(
    ...createSessionScenarioHandlers(),
    ...createAccountScenarioHandlers(),
    ...createMembershipInvitationScenarioHandlers(),
    ...createGoverningJourneyScenarioHandlers(),
  );
}
