import { setupServer } from "msw/node";
import { createGoverningJourneyScenarioHandlers } from "@/mocks/scenarios/governing-journey";
import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";
import { createSessionScenarioHandlers } from "@/mocks/scenarios/session";

export const apiMockServer = setupServer(
  ...createSessionScenarioHandlers(),
  ...createMembershipInvitationScenarioHandlers(),
  ...createGoverningJourneyScenarioHandlers(),
);

export function resetDefaultApiMockHandlers() {
  apiMockServer.resetHandlers(
    ...createSessionScenarioHandlers(),
    ...createMembershipInvitationScenarioHandlers(),
    ...createGoverningJourneyScenarioHandlers(),
  );
}
