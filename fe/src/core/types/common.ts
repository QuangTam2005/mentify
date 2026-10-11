/**
 * Common Architectural Utility Types
 */

export type EntityId = string;

export type Nullable<T> = T | null;

export type Optional<T> = T | undefined;

export type AsyncState<T> = {
  data: Nullable<T>;
  isLoading: boolean;
  error: Nullable<string>;
};
