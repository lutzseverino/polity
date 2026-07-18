import { setupServer } from "msw/node";

import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";

export const apiMockServer = setupServer(
  ...createMembershipInvitationScenarioHandlers(),
);

export function resetDefaultApiMockHandlers() {
  apiMockServer.resetHandlers(...createMembershipInvitationScenarioHandlers());
}
