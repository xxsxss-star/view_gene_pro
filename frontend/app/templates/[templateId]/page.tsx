"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { AppHeader } from "@/components/app-header";
import { AuthGuard } from "@/components/auth-guard";
import { ApiClientError } from "@/lib/api/client";
import { resolveApiAssetUrl } from "@/lib/api/config";
import { getTemplate } from "@/lib/api/templates";
import { UploadSlots } from "@/components/upload-slots";
import { getTask, TaskStage, TaskStatus } from "@/lib/api/tasks";

const taskStatusText: Record<TaskStatus, string> = {
  QUEUED: "等待处理",
  PROCESSING: "正在生成",
  SUCCEEDED: "制作完成",
  FAILED: "制作失败",
};

const taskStageText: Record<TaskStage, string> = {
  MATERIAL_CHECK: "正在检查素材",
  GENERATING: "正在生成作品",
  FINALIZING: "正在整理结果",
};

function readableMimeType(type: string): string {
  return type.replace("image/", "").toUpperCase();
}

function readableFileSize(bytes: number): string {
  return `${Math.round(bytes / 1024 / 1024)} MB`;
}

export default function TemplateDetailPage() {
  const params = useParams<{ templateId: string }>();
  const templateId = params.templateId;
  const [createdTaskId, setCreatedTaskId] = useState<number | null>(null);
  const template = useQuery({
    queryKey: ["template", templateId],
    queryFn: () => getTemplate(templateId),
  });
  const createdTask = useQuery({
    queryKey: ["task-preview", createdTaskId],
    queryFn: () => getTask(createdTaskId!),
    enabled: createdTaskId !== null,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === "QUEUED" || status === "PROCESSING" ? 1200 : false;
    },
    refetchIntervalInBackground: false,
  });
  const notFound = template.error instanceof ApiClientError && template.error.status === 404;

  return (
    <AuthGuard>
      <div className="app-shell">
        <AppHeader />
        <main className="content">
          <Link className="back-link" href="/">← 返回模板</Link>
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
            <article className="studio-layout">
              <section className="studio-panel studio-sidebar">
                <span className="category-label">{template.data.categoryName}</span>
                <h1>{template.data.title}</h1>
                <p className="detail-description">{template.data.description}</p>
                <div className="detail-image">
                  <Image src={resolveApiAssetUrl(template.data.coverUrl)} alt={template.data.title}
                         width={960} height={540} unoptimized priority />
                </div>
                <dl className="requirements">
                  <div><dt>图片数量</dt><dd>{template.data.requiredImageCount} 张</dd></div>
                  <div><dt>支持格式</dt><dd>{template.data.acceptedMimeTypes.map(readableMimeType).join("、")}</dd></div>
                  <div><dt>单张上限</dt><dd>{readableFileSize(template.data.maxFileSizeBytes)}</dd></div>
                </dl>
                <UploadSlots
                  templateId={template.data.id}
                  requiredImageCount={template.data.requiredImageCount}
                  acceptedMimeTypes={template.data.acceptedMimeTypes}
                  maxFileSizeBytes={template.data.maxFileSizeBytes}
                  onTaskCreated={setCreatedTaskId}
                />
              </section>
              <section className="studio-panel studio-history">
                <div className="studio-history-header">
                  <h2>创作预览</h2>
                  <Link className="result-link" href="/tasks">查看全部历史</Link>
                </div>
                {!createdTaskId && (
                  <div className="history-placeholder">
                    <div><strong>作品将在这里生成</strong><span>上传所需图片并开始制作，结果会直接显示在当前页面。</span></div>
                  </div>
                )}
                {createdTaskId && (createdTask.isPending || createdTask.data?.status === "QUEUED" || createdTask.data?.status === "PROCESSING") && (
                  <div className="preview-progress" role="status">
                    <span className="preview-spinner" aria-hidden="true" />
                    <strong>{createdTask.data ? taskStatusText[createdTask.data.status] : "正在获取任务状态"}</strong>
                    <p>{createdTask.data?.stage ? taskStageText[createdTask.data.stage] : "任务已提交，请稍候…"}</p>
                    <small>任务 #{createdTaskId}</small>
                  </div>
                )}
                {createdTask.data?.status === "SUCCEEDED" && createdTask.data.resultUrl && (
                  <a className="preview-result" href={resolveApiAssetUrl(createdTask.data.resultUrl)} target="_blank" rel="noreferrer">
                    <Image src={resolveApiAssetUrl(createdTask.data.resultUrl)} alt={`${createdTask.data.template.title}制作结果`}
                           width={960} height={1200} unoptimized />
                    <span>点击图片在新窗口查看结果</span>
                  </a>
                )}
                {createdTask.data?.status === "FAILED" && (
                  <div className="preview-failed" role="alert">
                    <strong>{taskStatusText.FAILED}</strong>
                    <p>{createdTask.data.failure?.message ?? "任务处理失败，请稍后重试。"}</p>
                  </div>
                )}
                {createdTask.isError && (
                  <div className="preview-failed" role="alert">
                    <strong>暂时无法获取任务状态</strong>
                    <button type="button" className="secondary-button" onClick={() => void createdTask.refetch()}>重新查询</button>
                  </div>
                )}
              </section>
            </article>
          )}
        </main>
      </div>
    </AuthGuard>
  );
}
