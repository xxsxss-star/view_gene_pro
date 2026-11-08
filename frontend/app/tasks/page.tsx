"use client";

import Image from "next/image";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { AppHeader } from "@/components/app-header";
import { AuthGuard } from "@/components/auth-guard";
import { resolveApiAssetUrl } from "@/lib/api/config";
import { getTasks, TaskStatus } from "@/lib/api/tasks";

const statusText: Record<TaskStatus, string> = {
  QUEUED: "等待处理",
  PROCESSING: "处理中",
  SUCCEEDED: "制作成功",
  FAILED: "制作失败",
};

export default function TasksPage() {
  const tasks = useQuery({ queryKey: ["tasks"], queryFn: () => getTasks() });

  return (
    <AuthGuard>
      <div className="app-shell">
        <AppHeader />
        <main className="content">
          <section className="page-heading">
            <p className="eyebrow">My Productions</p>
            <h1>我的制作</h1>
            <p>阶段六只负责受理和查询，新建任务会稳定显示为“等待处理”。</p>
          </section>
          {tasks.isPending && <p className="state-panel" role="status">正在加载任务…</p>}
          {tasks.isError && (
            <div className="state-panel" role="alert">
              <p>任务加载失败，请稍后重试。</p>
              <button type="button" onClick={() => void tasks.refetch()}>重新加载</button>
            </div>
          )}
          {tasks.data?.length === 0 && (
            <div className="state-panel"><p>还没有制作任务。</p><Link href="/">选择模板</Link></div>
          )}
          {tasks.data && tasks.data.length > 0 && (
            <section className="task-list" aria-label="制作任务列表">
              {tasks.data.map((task) => (
                <article className="task-card" key={task.id}>
                  <Image src={resolveApiAssetUrl(task.template.coverUrl)} alt="" width={240} height={135} unoptimized />
                  <div>
                    <span className={`task-status status-${task.status.toLowerCase()}`}>{statusText[task.status]}</span>
                    <h2>{task.template.title}</h2>
                    <p>任务 #{task.id} · 创建于 {new Date(task.createdAt).toLocaleString("zh-CN")}</p>
                    {task.failure && <p className="slot-error">{task.failure.message}</p>}
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
