type PageMetadata = Readonly<{
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}>;

export type PageResult<T> = Readonly<{
  content: readonly T[];
  page: PageMetadata;
}>;
