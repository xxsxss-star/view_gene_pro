"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { AppHeader } from "@/components/app-header";
import { AuthGuard } from "@/components/auth-guard";
import { ApiClientError } from "@/lib/api/client";
import { resolveApiAssetUrl } from "@/lib/api/config";
import { getTemplate } from "@/lib/api/templates";

function readableMimeType(type: string): string {
  return type.replace("image/", "").toUpperCase();
}

function readableFileSize(bytes: number): string {
  return `${Math.round(bytes / 1024 / 1024)} MB`;
}

export default function TemplateDetailPage() {
  const params = useParams<{ templateId: string }>();
  const templateId = params.templateId;
  const template = useQuery({
    queryKey: ["template", templateId],
    queryFn: () => getTemplate(templateId),
  });
  const notFound = template.error instanceof ApiClientError && template.error.status === 404;

  return (
    <AuthGuard>
      <div className="app-shell">
        <AppHeader />
        <main className="content">
          <Link className="back-link" href="/">← 返回模板列表</Link>
          {template.isPending && <p className="state-panel" role="status">正在加载模板详情…</p>}
          {notFound && (
            <section className="state-panel" role="alert">
              <h1>模板不存在</h1>
              <p>该模板可能已被删除或停用。</p>
              <Link href="/">查看其他模板</Link>
            </section>
          )}
          {template.isError && !notFound && (
            <section className="state-panel" role="alert">
              <p>模板详情加载失败。</p>
              <button type="button" onClick={() => void template.refetch()}>重新加载</button>
            </section>
          )}
          {template.data && (
            <article className="template-detail">
              <div className="detail-image">
                <Image src={resolveApiAssetUrl(template.data.coverUrl)} alt={template.data.title}
                       width={960} height={540} unoptimized priority />
              </div>
              <div className="detail-copy">
                <span className="category-label">{template.data.categoryName}</span>
                <h1>{template.data.title}</h1>
                <p>{template.data.description}</p>
                <dl className="requirements">
                  <div><dt>图片数量</dt><dd>{template.data.requiredImageCount} 张</dd></div>
                  <div><dt>支持格式</dt><dd>{template.data.acceptedMimeTypes.map(readableMimeType).join("、")}</dd></div>
                  <div><dt>单张上限</dt><dd>{readableFileSize(template.data.maxFileSizeBytes)}</dd></div>
                </dl>
                <button type="button" disabled>上传功能将在下一阶段开放</button>
              </div>
            </article>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
