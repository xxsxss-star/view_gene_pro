"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
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
    <main>
      <section className="login-card" aria-labelledby="login-title">
        <p className="eyebrow">View Gene Pro</p>
        <h1 id="login-title">登录</h1>
        <p className="muted">使用系统预置账号进入图片素材制作网站。</p>
        <form onSubmit={submit}>
          <label htmlFor="username">用户名</label>
          <input id="username" name="username" autoComplete="username" required maxLength={64}
                 value={username} onChange={(event) => setUsername(event.target.value)} />
          <label htmlFor="password">密码</label>
          <input id="password" name="password" type="password" autoComplete="current-password" required maxLength={128}
                 value={password} onChange={(event) => setPassword(event.target.value)} />
          {error && <p className="form-error" role="alert">{error}</p>}
          <button type="submit" disabled={isSubmitting}>{isSubmitting ? "登录中…" : "登录"}</button>
        </form>
      </section>
    </main>
  );
}
