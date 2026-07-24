import type { PolityAccount } from "@/domains/account/lib/account";

export class AccountGrantFailedError extends Error {
  readonly account: PolityAccount;

  constructor(account: PolityAccount) {
    super("Polity account grants failed to converge.");
    this.account = account;
    this.name = "AccountGrantFailedError";
  }
}

export function isAccountGrantFailedError(
  error: unknown,
): error is AccountGrantFailedError {
  return error instanceof AccountGrantFailedError;
}
