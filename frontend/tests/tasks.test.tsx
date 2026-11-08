import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import TasksPage from "@/app/tasks/page";
import * as taskApi from "@/lib/api/tasks";

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: vi.fn() }) }));
vi.mock("@/components/auth-provider", () => ({
  useAuth: () => ({
    user: { id: 1, username: "demo_user", displayName: "演示用户" },
    isRestoring: false,
    signIn: vi.fn(),
    signOut: vi.fn(),
  }),
}));
vi.mock("@/lib/api/tasks", () => ({ getTasks: vi.fn() }));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><TasksPage /></QueryClientProvider>);
}

beforeEach(() => vi.mocked(taskApi.getTasks).mockReset());

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
});
