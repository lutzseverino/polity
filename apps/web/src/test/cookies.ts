export function setTestCookie(value: string) {
  // biome-ignore lint/suspicious/noDocumentCookie: tests exercise the browser cookie transport contract.
  document.cookie = value;
}
