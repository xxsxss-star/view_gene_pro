"use client";

import { AuthGuard } from "@/components/auth-guard";
import { useAuth } from "@/components/auth-provider";

export default function Home() {
  const { user, signOut } = useAuth();
  return (
    <AuthGuard>
      <main>
        <section className="placeholder" aria-labelledby="page-title">
          <p className="eyebrow">View Gene Pro</p>
          <h1 id="page-title">图片素材制作网站</h1>
          <p>你好，{user?.displayName}。认证闭环已经就绪，模板业务将在下一阶段接入。</p>
          <button type="button" className="secondary-button" onClick={() => void signOut()}>退出登录</button>
        </section>
      </main>
    </AuthGuard>
  );
}
