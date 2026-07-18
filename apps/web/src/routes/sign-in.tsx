import { createFileRoute, useRouter } from "@tanstack/react-router";

import { AppCard, AppCardContent } from "@/components/app/AppCard";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import { SignInWorkflow } from "@/features/sign-in";

export const Route = createFileRoute("/sign-in")({
  component: SignInRoute,
});

function SignInRoute() {
  const router = useRouter();
  const { returnTo } = Route.useSearch();

  return (
    <AppPageLayout measure="narrow">
      <AppCard>
        <AppCardContent>
          <SignInWorkflow
            onSignedIn={() => router.history.replace(returnTo ?? "/polities")}
          />
        </AppCardContent>
      </AppCard>
    </AppPageLayout>
  );
}
