"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "./auth-provider";

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const { user, isRestoring } = useAuth();
  const router = useRouter();
  useEffect(() => { if (!isRestoring && !user) router.replace("/login"); }, [isRestoring, router, user]);
  if (isRestoring) return <main><p role="status">正在恢复登录状态…</p></main>;
  if (!user) return <main><p role="status">正在前往登录页…</p></main>;
  return children;
}
