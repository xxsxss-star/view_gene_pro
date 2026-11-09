"use client";

import Link from "next/link";
import { useAuth } from "./auth-provider";

export function AppHeader() {
  const { user, signOut } = useAuth();
  return (
    <header className="app-header">
      <Link className="brand" href="/">
        <span className="brand-mark">VG</span>
        <span>View Gene Pro</span>
      </Link>
      <nav aria-label="主导航">
        <Link className="nav-link" href="/">探索模板</Link>
        <Link className="nav-link" href="/tasks">创作历史</Link>
      </nav>
      <div className="account">
        <span className="avatar" aria-hidden="true">{user?.displayName?.slice(0, 1) ?? "U"}</span>
        <span>{user?.displayName}</span>
        <button type="button" className="text-button" onClick={() => void signOut()}>退出</button>
      </div>
    </header>
  );
}
