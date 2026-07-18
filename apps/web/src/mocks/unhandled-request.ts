type UnhandledRequestPrinter = Readonly<{
  error: () => void;
}>;

export function handleUnhandledBrowserRequest(
  request: Request,
  print: UnhandledRequestPrinter,
) {
  const url = new URL(request.url);

  if (
    url.origin === window.location.origin &&
    url.pathname.startsWith("/api/")
  ) {
    print.error();
  }
}
