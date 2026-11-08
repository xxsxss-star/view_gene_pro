"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useSyncExternalStore } from "react";
import * as authApi from "@/lib/api/auth";
import { ApiClientError } from "@/lib/api/client";
import { clearAccessToken, getAccessToken, setAccessToken, subscribeToAccessToken } from "@/lib/auth/token-storage";

interface AuthContextValue {
  user: authApi.User | null;
  isRestoring: boolean;
  signIn(username: string, password: string): Promise<void>;
  signOut(): Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const token = useSyncExternalStore(subscribeToAccessToken, getAccessToken, () => null);

  const invalidateSession = useCallback(async () => {
    clearAccessToken();
    await queryClient.cancelQueries();
    queryClient.clear();
  }, [queryClient]);

  const currentUser = useQuery({
    queryKey: ["auth", "me"],
    queryFn: async () => {
      try {
        return await authApi.getCurrentUser();
      } catch (error) {
        if (error instanceof ApiClientError && [401, 403].includes(error.status)) {
          await invalidateSession();
        }
        throw error;
      }
    },
    enabled: Boolean(token),
  });

  useEffect(() => {
    const handleInvalidSession = () => { void invalidateSession(); };
    window.addEventListener("view-gene:auth-invalid", handleInvalidSession);
    return () => window.removeEventListener("view-gene:auth-invalid", handleInvalidSession);
  }, [invalidateSession]);

  const signIn = useCallback(async (username: string, password: string) => {
    const result = await authApi.login(username, password);
    setAccessToken(result.accessToken);
    queryClient.setQueryData(["auth", "me"], result.user);
  }, [queryClient]);

  const signOut = useCallback(async () => {
    try { if (token) await authApi.logout(); }
    finally { await invalidateSession(); router.replace("/login"); }
  }, [invalidateSession, router, token]);

  const isRestoring = Boolean(token) && currentUser.isPending;
  const user = token ? (currentUser.data ?? null) : null;
  return <AuthContext.Provider value={{ user, isRestoring, signIn, signOut }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
