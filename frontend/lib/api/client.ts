import { apiBaseUrl } from "./config";
import { getAccessToken } from "@/lib/auth/token-storage";

interface ApiErrorBody { error?: { code?: string; message?: string } }

export class ApiClientError extends Error {
  constructor(public readonly status: number, public readonly code: string, message: string) {
    super(message);
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}, authenticated = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  if (authenticated) {
    const token = getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }
  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers });
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as ApiErrorBody;
    const error = new ApiClientError(response.status, body.error?.code ?? "REQUEST_FAILED",
      body.error?.message ?? "请求失败，请稍后重试");
    if (authenticated && (response.status === 401 || error.code === "USER_DISABLED") && typeof window !== "undefined") {
      window.dispatchEvent(new Event("view-gene:auth-invalid"));
    }
    throw error;
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
