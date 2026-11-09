const DEFAULT_API_BASE_URL = "http://localhost:8080/api/v1";

export const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? DEFAULT_API_BASE_URL;

export function resolveApiAssetUrl(path: string): string {
  if (/^https?:\/\//.test(path)) return path;
  try {
    const origin = /^https?:\/\//.test(apiBaseUrl)
      ? new URL(apiBaseUrl).origin
      : typeof window !== "undefined"
        ? window.location.origin
        : "http://localhost";
    return new URL(path, origin).toString();
  } catch {
    return path;
  }
}
