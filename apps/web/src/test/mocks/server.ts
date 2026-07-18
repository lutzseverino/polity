import { setupServer } from "msw/node";
import { createGoverningJourneyScenarioHandlers } from "@/mocks/scenarios/governing-journey";
import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";

export const apiMockServer = setupServer(
  ...createMembershipInvitationScenarioHandlers(),
  ...createGoverningJourneyScenarioHandlers(),
);

export function resetDefaultApiMockHandlers() {
  apiMockServer.resetHandlers(
    ...createMembershipInvitationScenarioHandlers(),
    ...createGoverningJourneyScenarioHandlers(),
  );
}
