"use client";

import Image from "next/image";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { AppHeader } from "@/components/app-header";
import { AuthGuard } from "@/components/auth-guard";
import { useAuth } from "@/components/auth-provider";
import { resolveApiAssetUrl } from "@/lib/api/config";
import { getTasks, Task, TaskStage, TaskStatus } from "@/lib/api/tasks";
import { mergeTaskSnapshots, shouldPollTasks } from "@/lib/tasks/task-state";

const statusText: Record<TaskStatus, string> = {
  QUEUED: "等待处理",
  PROCESSING: "处理中",
  SUCCEEDED: "制作成功",
  FAILED: "制作失败",
};

const stageText: Record<TaskStage, string> = {
  MATERIAL_CHECK: "素材检查",
  GENERATING: "模拟生成",
  FINALIZING: "结果整理",
};

export default function TasksPage() {
  const { user } = useAuth();
  const tasks = useQuery<Task[]>({
    queryKey: ["tasks", user?.id],
    queryFn: ({ signal }) => getTasks(undefined, signal),
    enabled: Boolean(user),
    refetchInterval: (query) => shouldPollTasks(query.state.data) ? 2500 : false,
    refetchIntervalInBackground: false,
    structuralSharing: (oldData: unknown, newData: unknown) =>
      mergeTaskSnapshots(oldData as Task[] | undefined, newData as Task[]),
  });

  return (
    <AuthGuard>
      <div className="app-shell">
        <AppHeader />
        <main className="content">
          <section className="page-heading">
            <p className="eyebrow">My Productions</p>
            <h1>我的制作</h1>
            <p>未完成任务会自动更新；全部结束后停止轮询，刷新页面也能恢复个人任务历史。</p>
            <button type="button" className="secondary-button refresh-button"
                    disabled={tasks.isFetching} onClick={() => void tasks.refetch()}>
              {tasks.isFetching ? "正在刷新…" : "刷新状态"}
            </button>
          </section>
          {tasks.isPending && <p className="state-panel" role="status">正在加载任务…</p>}
          {tasks.isError && !tasks.data && (
            <div className="state-panel" role="alert">
              <p>任务加载失败，请稍后重试。</p>
              <button type="button" onClick={() => void tasks.refetch()}>重新加载</button>
            </div>
          )}
          {tasks.isError && tasks.data && (
            <div className="query-warning" role="alert">
              <span>暂时无法获取最新状态，下面保留的是上次成功获取的任务信息。</span>
              <button type="button" onClick={() => void tasks.refetch()}>手动刷新</button>
            </div>
          )}
          {tasks.data?.length === 0 && (
            <div className="state-panel"><p>还没有制作任务。</p><Link href="/">选择模板</Link></div>
          )}
          {tasks.data && tasks.data.length > 0 && (
            <section className="task-list" aria-label="制作任务列表">
              {tasks.data.map((task) => (
                <article className="task-card" key={task.id}>
                  <div className="task-visuals">
                    <figure>
                      <Image src={resolveApiAssetUrl(task.template.coverUrl)} alt={`${task.template.title}模板`}
                             width={240} height={135} unoptimized />
                      <figcaption>使用模板</figcaption>
                    </figure>
                    {task.status === "SUCCEEDED" && task.resultUrl && (
                      <figure className="task-result">
                        <Image src={resolveApiAssetUrl(task.resultUrl)} alt={`任务 ${task.id} 制作结果`}
                               width={480} height={320} unoptimized />
                        <figcaption>制作结果</figcaption>
                      </figure>
                    )}
                  </div>
                  <div className="task-copy">
                    <span className={`task-status status-${task.status.toLowerCase()}`}>{statusText[task.status]}</span>
                    <h2>{task.template.title}</h2>
                    <p>任务 #{task.id} · 创建于 {new Date(task.createdAt).toLocaleString("zh-CN")}</p>
                    {task.stage && <p className="task-stage">当前阶段：{stageText[task.stage]}</p>}
                    {task.failure && (
                      <p className="slot-error" role="status">
                        {task.failure.stage ? `${stageText[task.failure.stage]}：` : ""}{task.failure.message}
                      </p>
                    )}
                    {task.status === "SUCCEEDED" && task.resultUrl && (
                      <a className="result-link" href={resolveApiAssetUrl(task.resultUrl)} target="_blank"
                         rel="noreferrer">在新窗口查看结果</a>
                    )}
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
