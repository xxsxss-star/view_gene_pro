import { apiRequest } from "./client";

export interface User { id: number; username: string; displayName: string }
interface Envelope<T> { data: T; requestId: string; timestamp: string }
export interface LoginResult {
  accessToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
  user: User;
}

export async function login(username: string, password: string): Promise<LoginResult> {
  const response = await apiRequest<Envelope<LoginResult>>("/auth/login", {
    method: "POST", body: JSON.stringify({ username, password }),
  }, false);
  return response.data;
}

export async function getCurrentUser(): Promise<User> {
  return (await apiRequest<Envelope<User>>("/auth/me")).data;
}

export async function logout(): Promise<void> {
  await apiRequest<void>("/auth/logout", { method: "POST" });
}
