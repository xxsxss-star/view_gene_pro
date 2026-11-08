const DEFAULT_API_BASE_URL = "http://localhost:8080/api/v1";

export const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? DEFAULT_API_BASE_URL;

export function resolveApiAssetUrl(path: string): string {
  if (/^https?:\/\//.test(path)) return path;
  try {
    return new URL(path, new URL(apiBaseUrl).origin).toString();
  } catch {
    return path;
  }
}
