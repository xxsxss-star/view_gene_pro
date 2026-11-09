"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/components/auth-provider";
import { ApiClientError } from "@/lib/api/client";

export default function LoginPage() {
  const { user, isRestoring, signIn } = useAuth();
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { if (!isRestoring && user) router.replace("/"); }, [isRestoring, router, user]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await signIn(username, password);
      router.replace("/");
    } catch (caught) {
      setError(caught instanceof ApiClientError && caught.code === "INVALID_CREDENTIALS"
        ? "用户名或密码错误"
        : caught instanceof ApiClientError && caught.code === "USER_DISABLED"
          ? "该用户已被禁用"
          : "登录失败，请稍后重试");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-showcase" aria-hidden="true">
        <Link className="brand" href="/">
          <span className="brand-mark">VG</span><span>View Gene Pro</span>
        </Link>
        <div className="login-copy">
          <p className="eyebrow">Make something remarkable</p>
          <h1>让每张图片都有新表达</h1>
          <p>选择灵感模板，上传你的素材，用更简单的方式完成视觉创作。</p>
        </div>
      </section>
      <section className="login-form-wrap">
        <div className="login-card" aria-labelledby="login-title">
          <p className="eyebrow">Welcome back</p>
          <h2 id="login-title">登录你的账号</h2>
          <p className="muted">继续探索模板和管理你的创作。</p>
          <form onSubmit={submit}>
            <label htmlFor="username">用户名</label>
            <input id="username" name="username" autoComplete="username" required maxLength={64}
                   placeholder="请输入用户名" value={username} onChange={(event) => setUsername(event.target.value)} />
            <label htmlFor="password">密码</label>
            <input id="password" name="password" type="password" autoComplete="current-password" required maxLength={128}
                   placeholder="请输入密码" value={password} onChange={(event) => setPassword(event.target.value)} />
            {error && <p className="form-error" role="alert">{error}</p>}
            <button type="submit" disabled={isSubmitting}>{isSubmitting ? "登录中…" : "登录"}</button>
          </form>
        </div>
      </section>
    </main>
  );
}
