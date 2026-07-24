import type {
  GrantConvergence,
  PolityAccount,
} from "@/domains/account/lib/account";
import { isUuid } from "@/lib/uuid";

type UnknownRecord = Record<string, unknown>;

function requiredRecord(value: unknown, message: string): UnknownRecord {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error(message);
  }
  return value as UnknownRecord;
}

function requiredString(value: unknown, message: string) {
  if (typeof value !== "string" || !value) throw new Error(message);
  return value;
}

function requiredUuid(value: unknown, message: string) {
  if (!isUuid(value)) throw new Error(message);
  return value;
}

function parseGrantConvergence(
  value: unknown,
  message: string,
): GrantConvergence {
  const response = requiredRecord(value, message);
  const receiptId = requiredUuid(response.receiptId, message);

  if (response.status === "failed") {
    return {
      failureCode: requiredString(response.failureCode, message),
      receiptId,
      status: "failed",
    };
  }
  if (response.status !== "applied" && response.status !== "pending") {
    throw new Error(message);
  }
  if (response.failureCode !== undefined && response.failureCode !== null) {
    throw new Error(message);
  }

  return { receiptId, status: response.status };
}

export function parsePolityAccount(value: unknown): PolityAccount {
  const message = "Invalid Polity account response.";
  const response = requiredRecord(value, message);

  return {
    grants: parseGrantConvergence(response.grants, message),
    userId: requiredUuid(response.userId, message),
  };
}
