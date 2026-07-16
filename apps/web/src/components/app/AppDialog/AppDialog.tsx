import { Trans } from "@lingui/react/macro";
import { X } from "lucide-react";
import { type ComponentProps, useEffect, useState } from "react";

import { AppButton } from "@/components/app/AppButton";
import { Dialog, DialogClose, DialogContent } from "@/components/ui/dialog";
import { Drawer, DrawerContent } from "@/components/ui/drawer";
import { useIsMobile } from "@/components/ui/hooks/use-mobile";

type DialogOpenChangeDetails = Parameters<
  NonNullable<ComponentProps<typeof Dialog>["onOpenChange"]>
>[1];
type DrawerOpenChangeDetails = Parameters<
  NonNullable<ComponentProps<typeof Drawer>["onOpenChange"]>
>[1];
type AppDialogProps = Omit<ComponentProps<typeof Dialog>, "onOpenChange"> & {
  onOpenChange?: (
    open: boolean,
    eventDetails: DialogOpenChangeDetails | DrawerOpenChangeDetails,
  ) => void;
};
type AppDialogContentProps = ComponentProps<typeof DialogContent> &
  ComponentProps<typeof DrawerContent>;

function AppDialog({ onOpenChange, open, ...props }: AppDialogProps) {
  const isMobile = useIsMobile();
  const [renderedOpen, setRenderedOpen] = useState(false);

  useEffect(() => {
    if (open === undefined) {
      return undefined;
    }

    const animationFrame = window.requestAnimationFrame(() => {
      setRenderedOpen(open);
    });

    return () => window.cancelAnimationFrame(animationFrame);
  }, [open]);

  return isMobile ? (
    <Drawer
      {...props}
      onOpenChange={(open, eventDetails) => onOpenChange?.(open, eventDetails)}
      open={open === undefined ? undefined : renderedOpen}
      showSwipeHandle
    />
  ) : (
    <Dialog
      {...props}
      onOpenChange={(open, eventDetails) => onOpenChange?.(open, eventDetails)}
      open={open}
    />
  );
}

function AppDialogContent({
  children,
  showCloseButton = true,
  ...props
}: AppDialogContentProps) {
  const isMobile = useIsMobile();
  const content = (
    <>
      {children}
      {showCloseButton ? (
        <DialogClose
          render={
            <AppButton
              className="absolute top-3 right-3"
              size="icon-lg"
              variant="ghost"
            />
          }
        >
          <X aria-hidden="true" />
          <span className="sr-only">
            <Trans>Close</Trans>
          </span>
        </DialogClose>
      ) : null}
    </>
  );

  return isMobile ? (
    <DrawerContent {...props}>{content}</DrawerContent>
  ) : (
    <DialogContent showCloseButton={false} {...props}>
      {content}
    </DialogContent>
  );
}

export {
  DialogClose as AppDialogClose,
  DialogDescription as AppDialogDescription,
  DialogFooter as AppDialogFooter,
  DialogHeader as AppDialogHeader,
  DialogTitle as AppDialogTitle,
  DialogTrigger as AppDialogTrigger,
} from "@/components/ui/dialog";
export { AppDialog, AppDialogContent };
