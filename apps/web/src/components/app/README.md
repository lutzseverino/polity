# App-owned components

This directory owns conventional wrappers and reusable product components composed from the
registry-managed primitives in `../ui/`.

Create a component here only when the application needs shared behavior, semantics, or composition.
Domain and feature presentation stays with its owner. Package each app component in a named folder,
export it through that folder's `index.ts`, and keep registry primitives untouched.

## Dialog convention

Product dialogs use the compound `AppDialog` components. See the authoritative
[app component wrapper reference](../../../docs/reference/app-component-wrappers.md) for the minimal
wrapper shape, draggable mobile-drawer behavior, composition rules, and registry import boundary.

## Link surface convention

Use `AppLinkSurface` when an entire product surface navigates to one destination. It owns the
keyboard focus boundary, intent preloading, and the named interaction group. Pair it with
`AppLinkSurfaceIndicator` when the surface needs the shared directional hover and focus cue; feature
and domain owners still control the surface's content and layout.

## Tabs convention

Product tab sets use the compound `AppTabs` exports. Routes may render `AppTabsTrigger` through a
router link so URLs, history, and intent preloading remain route-owned while the registry component
owns tab semantics, focus movement, and active-state styling.
