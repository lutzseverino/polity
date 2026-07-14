export class ResourceNotFoundError extends Error {
  readonly resourceId: string;
  readonly resourceType: string;

  constructor(resourceType: string, resourceId: string) {
    super(`${resourceType} ${resourceId} was not found.`);
    this.name = "ResourceNotFoundError";
    this.resourceId = resourceId;
    this.resourceType = resourceType;
  }
}

export function isResourceNotFoundError(
  error: unknown,
): error is ResourceNotFoundError {
  return error instanceof ResourceNotFoundError;
}
