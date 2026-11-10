import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import TasksPage from "@/app/tasks/page";
import * as taskApi from "@/lib/api/tasks";

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: vi.fn() }) }));
const authState = vi.hoisted(() => ({ userId: 1 }));
vi.mock("@/components/auth-provider", () => ({
  useAuth: () => ({
    user: { id: authState.userId, username: `demo_user_${authState.userId}`, displayName: "演示用户" },
    isRestoring: false,
    signIn: vi.fn(),
    signOut: vi.fn(),
  }),
}));
vi.mock("@/lib/api/tasks", () => ({ getTasks: vi.fn() }));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const view = render(<QueryClientProvider client={client}><TasksPage /></QueryClientProvider>);
  return { ...view, client };
}

beforeEach(() => {
  authState.userId = 1;
  vi.mocked(taskApi.getTasks).mockReset();
});

describe("task list", () => {
  it("keeps each task status, stage, failure, and result scoped to its own card", async () => {
    vi.mocked(taskApi.getTasks).mockResolvedValue([
      {
        id: 9101,
        template: { id: "queued", title: "等待模板", coverUrl: "/queued.svg" },
        status: "QUEUED", stage: null, resultUrl: null, failure: null,
        createdAt: "2026-11-08T00:00:04Z", startedAt: null, completedAt: null,
        updatedAt: "2026-11-08T00:00:04Z",
      },
      {
        id: 9102,
        template: { id: "processing", title: "处理模板", coverUrl: "/processing.svg" },
        status: "PROCESSING", stage: "GENERATING", resultUrl: null, failure: null,
        createdAt: "2026-11-08T00:00:03Z", startedAt: "2026-11-08T00:00:04Z", completedAt: null,
        updatedAt: "2026-11-08T00:00:05Z",
      },
      {
        id: 9103,
        template: { id: "failed", title: "失败模板", coverUrl: "/failed.svg" },
        status: "FAILED", stage: "MATERIAL_CHECK", resultUrl: null,
        failure: { stage: "MATERIAL_CHECK", code: "BAD_ASSET", message: "素材不可用" },
        createdAt: "2026-11-08T00:00:02Z", startedAt: "2026-11-08T00:00:03Z",
        completedAt: "2026-11-08T00:00:04Z", updatedAt: "2026-11-08T00:00:04Z",
      },
      {
        id: 9104,
        template: { id: "success", title: "成功模板", coverUrl: "/success.svg" },
        status: "SUCCEEDED", stage: "FINALIZING", resultUrl: "/success-result.svg", failure: null,
        createdAt: "2026-11-08T00:00:01Z", startedAt: "2026-11-08T00:00:02Z",
        completedAt: "2026-11-08T00:00:06Z", updatedAt: "2026-11-08T00:00:06Z",
      },
    ]);

    renderPage();
    await screen.findByRole("heading", { name: "成功模板" });
    const byTaskId = (id: number) => screen.getByText(new RegExp(`任务 #${id}`)).closest("article")!;

    expect(within(byTaskId(9101)).getByText("等待处理")).toBeInTheDocument();
    expect(within(byTaskId(9101)).queryByText(/当前阶段/)).not.toBeInTheDocument();
    expect(within(byTaskId(9102)).getByText("处理中")).toBeInTheDocument();
    expect(within(byTaskId(9102)).getByText("当前阶段：模拟生成")).toBeInTheDocument();
    expect(within(byTaskId(9103)).getByText("制作失败")).toBeInTheDocument();
    expect(within(byTaskId(9103)).getByText("素材检查：素材不可用")).toBeInTheDocument();
    expect(within(byTaskId(9103)).queryByRole("img", { name: /制作结果/ })).not.toBeInTheDocument();
    expect(within(byTaskId(9104)).getByText("制作成功")).toBeInTheDocument();
    expect(within(byTaskId(9104)).getByRole("img", { name: "任务 9104 制作结果" })).toHaveAttribute(
      "src", "http://localhost:8080/success-result.svg",
    );
    expect(within(byTaskId(9104)).queryByText("素材不可用")).not.toBeInTheDocument();
  });

  it("renders tasks returned by the current-user endpoint", async () => {
    vi.mocked(taskApi.getTasks).mockResolvedValue([{
      id: 9001,
      template: { id: "travel-collage", title: "旅行拼图", coverUrl: "/images/templates/travel.svg" },
      status: "QUEUED",
      stage: null,
      resultUrl: null,
      failure: null,
      createdAt: "2026-11-08T00:00:00Z",
      startedAt: null,
      completedAt: null,
      updatedAt: "2026-11-08T00:00:00Z",
    }]);
    renderPage();
    expect(await screen.findByRole("heading", { name: "旅行拼图" })).toBeInTheDocument();
    expect(screen.getByText("等待处理")).toBeInTheDocument();
    expect(screen.getByText(/任务 #9001/)).toBeInTheDocument();
    expect(taskApi.getTasks).toHaveBeenCalledOnce();
  });

  it("shows an empty state", async () => {
    vi.mocked(taskApi.getTasks).mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText("还没有制作任务。")).toBeInTheDocument();
  });

  it("shows the successful result preview and processing stage", async () => {
    vi.mocked(taskApi.getTasks).mockResolvedValue([{
      id: 9002,
      template: { id: "travel", title: "旅行明信片", coverUrl: "/images/templates/travel.svg" },
      status: "SUCCEEDED",
      stage: "FINALIZING",
      resultUrl: "/images/results/travel-result.svg",
      failure: null,
      createdAt: "2026-11-08T00:00:00Z",
      startedAt: "2026-11-08T00:00:01Z",
      completedAt: "2026-11-08T00:00:05Z",
      updatedAt: "2026-11-08T00:00:05Z",
    }]);
    renderPage();
    expect(await screen.findByRole("img", { name: "任务 9002 制作结果" })).toHaveAttribute(
      "src", "http://localhost:8080/images/results/travel-result.svg",
    );
    expect(screen.getByText("当前阶段：结果整理")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "在新窗口查看任务 9002 的制作结果" })).toBeInTheDocument();
  });

  it("keeps the last task data when a refresh request fails", async () => {
    vi.mocked(taskApi.getTasks)
      .mockResolvedValueOnce([{
        id: 9003,
        template: { id: "social", title: "社交故事", coverUrl: "/cover.svg" },
        status: "SUCCEEDED", stage: "FINALIZING", resultUrl: "/result.svg", failure: null,
        createdAt: "2026-11-08T00:00:00Z", startedAt: null, completedAt: null,
        updatedAt: "2026-11-08T00:00:05Z",
      }])
      .mockRejectedValueOnce(new Error("offline"));
    renderPage();
    expect(await screen.findByRole("heading", { name: "社交故事" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "刷新状态" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("暂时无法获取最新状态");
    expect(screen.getByRole("heading", { name: "社交故事" })).toBeInTheDocument();
    await waitFor(() => expect(taskApi.getTasks).toHaveBeenCalledTimes(2));
  });

  it("does not render an old response after the authenticated user changes", async () => {
    let resolveOld!: (tasks: Awaited<ReturnType<typeof taskApi.getTasks>>) => void;
    const oldRequest = new Promise<Awaited<ReturnType<typeof taskApi.getTasks>>>((resolve) => { resolveOld = resolve; });
    vi.mocked(taskApi.getTasks)
      .mockReturnValueOnce(oldRequest)
      .mockResolvedValueOnce([{
        id: 22, template: { id: "new", title: "新用户任务", coverUrl: "/new.svg" },
        status: "SUCCEEDED", stage: "FINALIZING", resultUrl: "/new-result.svg", failure: null,
        createdAt: "2026-11-08T00:00:02Z", startedAt: null, completedAt: null,
        updatedAt: "2026-11-08T00:00:03Z",
      }]);
    const view = renderPage();
    await waitFor(() => expect(taskApi.getTasks).toHaveBeenCalledOnce());

    authState.userId = 2;
    view.rerender(<QueryClientProvider client={view.client}><TasksPage /></QueryClientProvider>);
    expect(await screen.findByRole("heading", { name: "新用户任务" })).toBeInTheDocument();
    resolveOld([{
      id: 11, template: { id: "old", title: "旧用户任务", coverUrl: "/old.svg" },
      status: "SUCCEEDED", stage: "FINALIZING", resultUrl: "/old-result.svg", failure: null,
      createdAt: "2026-11-08T00:00:01Z", startedAt: null, completedAt: null,
      updatedAt: "2026-11-08T00:00:04Z",
    }]);
    await waitFor(() => expect(screen.queryByRole("heading", { name: "旧用户任务" })).not.toBeInTheDocument());
  });

  it("starts polling for an unfinished task and stops after it reaches a terminal state", async () => {
    vi.mocked(taskApi.getTasks)
      .mockResolvedValueOnce([{
        id: 9201, template: { id: "work", title: "轮询任务", coverUrl: "/work.svg" },
        status: "PROCESSING", stage: "GENERATING", resultUrl: null, failure: null,
        createdAt: "2026-11-08T00:00:00Z", startedAt: "2026-11-08T00:00:01Z", completedAt: null,
        updatedAt: "2026-11-08T00:00:02Z",
      }])
      .mockResolvedValueOnce([{
        id: 9201, template: { id: "work", title: "轮询任务", coverUrl: "/work.svg" },
        status: "SUCCEEDED", stage: "FINALIZING", resultUrl: "/work-result.svg", failure: null,
        createdAt: "2026-11-08T00:00:00Z", startedAt: "2026-11-08T00:00:01Z",
        completedAt: "2026-11-08T00:00:05Z", updatedAt: "2026-11-08T00:00:05Z",
      }]);

    renderPage();
    expect(await screen.findByText("处理中")).toBeInTheDocument();
    expect(taskApi.getTasks).toHaveBeenCalledTimes(1);

    await waitFor(() => expect(taskApi.getTasks).toHaveBeenCalledTimes(2), { timeout: 4000 });
    expect(await screen.findByText("制作成功")).toBeInTheDocument();

    await act(async () => { await new Promise((resolve) => setTimeout(resolve, 3000)); });
    expect(taskApi.getTasks).toHaveBeenCalledTimes(2);
  }, 8000);

  it("keeps polling after a query error and recovers with the next successful response", async () => {
    const processingTask = {
      id: 9202, template: { id: "recover", title: "恢复任务", coverUrl: "/recover.svg" },
      status: "PROCESSING" as const, stage: "GENERATING" as const, resultUrl: null, failure: null,
      createdAt: "2026-11-08T00:00:00Z", startedAt: "2026-11-08T00:00:01Z", completedAt: null,
      updatedAt: "2026-11-08T00:00:02Z",
    };
    vi.mocked(taskApi.getTasks)
      .mockResolvedValueOnce([processingTask])
      .mockRejectedValueOnce(new Error("temporary outage"))
      .mockResolvedValueOnce([{ ...processingTask, status: "SUCCEEDED", stage: "FINALIZING",
        resultUrl: "/recovered.svg", completedAt: "2026-11-08T00:00:06Z", updatedAt: "2026-11-08T00:00:06Z" }]);

    renderPage();
    expect(await screen.findByText("处理中")).toBeInTheDocument();
    await waitFor(() => expect(taskApi.getTasks).toHaveBeenCalledTimes(2), { timeout: 4000 });
    expect(await screen.findByRole("alert")).toHaveTextContent("暂时无法获取最新状态");
    expect(screen.getByText("处理中")).toBeInTheDocument();

    await waitFor(() => expect(taskApi.getTasks).toHaveBeenCalledTimes(3), { timeout: 4000 });
    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
    expect(screen.getByText("制作成功")).toBeInTheDocument();
  }, 8000);

  it("aborts the previous user's in-flight request when the authenticated user changes", async () => {
    const signals: AbortSignal[] = [];
    vi.mocked(taskApi.getTasks).mockImplementation((_status, signal) => {
      if (signal) signals.push(signal);
      return new Promise(() => undefined);
    });
    const view = renderPage();
    await waitFor(() => expect(signals).toHaveLength(1));
    expect(signals[0].aborted).toBe(false);

    authState.userId = 2;
    view.rerender(<QueryClientProvider client={view.client}><TasksPage /></QueryClientProvider>);
    await waitFor(() => expect(signals).toHaveLength(2));
    expect(signals[0].aborted).toBe(true);
    expect(signals[1].aborted).toBe(false);
  });
});
