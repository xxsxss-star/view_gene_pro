"use client";

import Link from "next/link";
import { useAuth } from "./auth-provider";

export function AppHeader() {
  const { user, signOut } = useAuth();
  return (
    <header className="app-header">
      <Link className="brand" href="/">View Gene Pro</Link>
      <nav aria-label="主导航">
        <Link href="/">模板</Link>
        <Link href="/tasks">我的制作</Link>
      </nav>
      <div className="account">
        <span>{user?.displayName}</span>
        <button type="button" className="text-button" onClick={() => void signOut()}>退出</button>
      </div>
    </header>
  );
}
