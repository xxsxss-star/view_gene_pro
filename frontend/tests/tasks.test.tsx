import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
    expect(screen.getByRole("link", { name: "在新窗口查看结果" })).toBeInTheDocument();
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
});
