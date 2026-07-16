# App Component Wrappers

This reference is authoritative for wrappers under `src/components/app/` and for access to the
registry-managed primitives under `src/components/ui/`.

## Ownership

| Layer | Responsibility |
| --- | --- |
| `src/components/ui/` | Registry-managed primitive implementation; read-only application dependency |
| `src/components/app/` | Owner-neutral application behavior, semantics, styling conventions, and primitive access boundary |
| `src/app/`, `src/routes/`, `src/domains/`, `src/features/` | Product composition using app components; no direct registry imports |

Only `src/components/app/` may import from `src/components/ui/`. Updating a registry primitive is a
registry operation, not an application-wrapper edit.

## Wrapper Shape

An app wrapper adds the smallest application-owned behavior or style that is actually shared.

- Preserve the primitive's composition model. Keep compound parts, slots, `render` props, controlled
  state, and caller-owned content available.
- Wrap only the part that needs application behavior. Re-export unchanged parts directly with
  `export { PrimitivePart as AppPart } from "..."`.
- Derive wrapper props from the wrapped primitive with `ComponentProps` and forward them unchanged.
- Merge an application-owned default with caller props only when the default is a genuine invariant.
- Keep labels, content, actions, dimensions, dismissal effects, route behavior, and feature styling at
  the caller unless every use of the wrapper must share them.
- Do not recreate primitive state, context, lifecycle, or accessibility behavior when prop forwarding
  or composition can extend the primitive instead.
- Do not introduce a wrapper for a hypothetical future convention. A wrapper is earned by a current
  application boundary, repeated use, or an explicit application-wide rule.

## Styling

Move style into an app wrapper when it represents an owner-neutral application convention. Leave it
with the consumer when it describes feature layout, content density, route context, or a one-off
presentation.

Wrappers that add classes must continue to accept `className` and merge it with the application
default. A wrapper must not make ordinary primitive customization require new wrapper-specific props.

## Responsive Dialog Example

`AppDialog` preserves the registry dialog composition API. Its trigger, title, description, footer,
header, and close components are direct aliases. The root and content select a centered dialog on
desktop or a draggable bottom drawer on mobile. Its default close affordance is application-owned so
it has the shared touch target and localized accessible label; callers can disable it with the
primitive's existing `showCloseButton` prop and compose `AppDialogClose` themselves.

```tsx
function AppDialog({ onOpenChange, ...props }: AppDialogProps) {
  const isMobile = useIsMobile();

  return isMobile ? (
    <Drawer showSwipeHandle {...props} onOpenChange={onOpenChange} />
  ) : (
    <Dialog {...props} onOpenChange={onOpenChange} />
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
        <DialogClose render={<AppButton size="icon-lg" variant="ghost" />}>
          <X aria-hidden="true" />
          <span className="sr-only">
            <Trans>Close</Trans>
          </span>
        </DialogClose>
      ) : null}
    </>
  );

  return isMobile ? (
    <DrawerContent {...props}>
      {content}
    </DrawerContent>
  ) : (
    <DialogContent showCloseButton={false} {...props}>
      {content}
    </DialogContent>
  );
}

export {
  DialogClose as AppDialogClose,
  DialogTitle as AppDialogTitle,
  DialogTrigger as AppDialogTrigger,
} from "@/components/ui/dialog";
```

The root adapts the differing dialog and drawer open-change event detail types while forwarding their
shared props. The caller still composes the trigger, content, title, and description and still owns
content sizing and dismissal side effects. It can opt out of the default close affordance and
compose `AppDialogClose` directly when needed.

## Verification

- `pnpm check:architecture` rejects direct registry imports outside `components/app` and
  `components/ui`, including every product owner.
- Wrapper tests cover only the behavior added by the wrapper and confirm that the underlying
  composition remains usable.
