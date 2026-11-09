"use client";

import Image from "next/image";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { AppHeader } from "@/components/app-header";
import { AuthGuard } from "@/components/auth-guard";
import { resolveApiAssetUrl } from "@/lib/api/config";
import { getTemplates } from "@/lib/api/templates";

export default function Home() {
  const [categoryCode, setCategoryCode] = useState("");
  const templates = useQuery({
    queryKey: ["templates", categoryCode],
    queryFn: () => getTemplates(categoryCode || undefined),
  });

  return (
    <AuthGuard>
      <div className="app-shell">
        <AppHeader />
        <main className="content">
          <section className="page-heading">
            <div className="page-heading-copy">
              <p className="eyebrow">Creative templates</p>
              <h1>从一个灵感开始创作</h1>
              <p>挑选喜欢的视觉模板，上传你的图片，快速生成专属作品。</p>
            </div>
          </section>

          {templates.data && (
            <div className="category-tabs" role="group" aria-label="模板分类">
              <button className={!categoryCode ? "active" : ""} onClick={() => setCategoryCode("")}>全部作品</button>
              {templates.data.categories.map((category) => (
                <button key={category.code} className={categoryCode === category.code ? "active" : ""}
                        onClick={() => setCategoryCode(category.code)}>{category.name}</button>
              ))}
            </div>
          )}

          {templates.isPending && <p className="state-panel" role="status">正在加载模板…</p>}
          {templates.isError && (
            <div className="state-panel" role="alert">
              <p>模板加载失败，请检查后端服务后重试。</p>
              <button type="button" onClick={() => void templates.refetch()}>重新加载</button>
            </div>
          )}
          {templates.data?.items.length === 0 && <p className="state-panel">当前分类暂无模板。</p>}
          {templates.data && templates.data.items.length > 0 && (
            <section className="template-grid" aria-label="模板列表">
              {templates.data.items.map((template) => (
                <article className="template-card" key={template.id}>
                  <Image src={resolveApiAssetUrl(template.coverUrl)} alt={template.title} width={640} height={800} unoptimized />
                  <div className="template-card-body">
                    <span className="category-label">{template.categoryName}</span>
                    <h2>{template.title}</h2>
                    <p>{template.description}</p>
                    <div className="card-footer">
                      <span>{template.requiredImageCount} 张图片</span>
                      <Link href={`/templates/${template.id}`}>立即创作</Link>
                    </div>
                  </div>
                </article>
              ))}
            </section>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
