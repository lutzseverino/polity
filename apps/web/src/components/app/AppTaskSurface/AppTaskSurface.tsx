import { Trans } from "@lingui/react/macro";
import { X } from "lucide-react";
import { type ReactNode, useEffect, useState } from "react";

import { AppButton } from "@/components/app/AppButton";
import { Dialog, DialogClose, DialogContent } from "@/components/ui/dialog";
import { Drawer, DrawerClose, DrawerContent } from "@/components/ui/drawer";

const desktopMediaQuery = "(min-width: 640px)";

type AppTaskSurfaceProps = Readonly<{
  children: ReactNode;
  describedBy: string;
  labelledBy: string;
  onDismiss: () => void;
  onOpenChangeComplete?: (open: boolean) => void;
  open: boolean;
}>;

function useDesktopTaskSurface() {
  const [isDesktop, setIsDesktop] = useState(
    () =>
      typeof window !== "undefined" &&
      typeof window.matchMedia === "function" &&
      window.matchMedia(desktopMediaQuery).matches,
  );

  useEffect(() => {
    if (typeof window.matchMedia !== "function") {
      return undefined;
    }

    const mediaQuery = window.matchMedia(desktopMediaQuery);
    const updateSurface = () => setIsDesktop(mediaQuery.matches);

    updateSurface();
    mediaQuery.addEventListener("change", updateSurface);

    return () => mediaQuery.removeEventListener("change", updateSurface);
  }, []);

  return isDesktop;
}

function CloseLabel() {
  return (
    <>
      <X aria-hidden="true" />
      <span className="sr-only">
        <Trans>Close</Trans>
      </span>
    </>
  );
}

export function AppTaskSurface({
  children,
  describedBy,
  labelledBy,
  onDismiss,
  onOpenChangeComplete,
  open,
}: AppTaskSurfaceProps) {
  const isDesktop = useDesktopTaskSurface();
  const [renderedOpen, setRenderedOpen] = useState(false);

  useEffect(() => {
    const animationFrame = window.requestAnimationFrame(() => {
      setRenderedOpen(open);
    });

    return () => window.cancelAnimationFrame(animationFrame);
  }, [open]);

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      onDismiss();
    }
  };

  if (isDesktop) {
    return (
      <Dialog
        onOpenChange={handleOpenChange}
        onOpenChangeComplete={onOpenChangeComplete}
        open={renderedOpen}
      >
        <DialogContent
          aria-describedby={describedBy}
          aria-labelledby={labelledBy}
          className="max-h-[calc(100dvh-2rem)] max-w-xl overflow-hidden p-0 sm:max-w-xl"
          showCloseButton={false}
        >
          {children}
          <DialogClose
            render={
              <AppButton
                className="absolute top-3 right-3"
                size="icon-lg"
                variant="ghost"
              />
            }
          >
            <CloseLabel />
          </DialogClose>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Drawer
      onOpenChange={handleOpenChange}
      onOpenChangeComplete={onOpenChangeComplete}
      open={renderedOpen}
      showSwipeHandle
    >
      <DrawerContent
        aria-describedby={describedBy}
        aria-labelledby={labelledBy}
        className="[--drawer-content-max-height:calc(100dvh-1rem)]"
      >
        {children}
        <DrawerClose
          render={
            <AppButton
              className="absolute top-3 right-3"
              size="icon-lg"
              variant="ghost"
            />
          }
        >
          <CloseLabel />
        </DrawerClose>
      </DrawerContent>
    </Drawer>
  );
}
