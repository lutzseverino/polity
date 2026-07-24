export type GrantConvergence =
  | Readonly<{
      receiptId: string;
      status: "applied" | "pending";
    }>
  | Readonly<{
      failureCode: string;
      receiptId: string;
      status: "failed";
    }>;

export type PolityAccount = Readonly<{
  grants: GrantConvergence;
  userId: string;
}>;
