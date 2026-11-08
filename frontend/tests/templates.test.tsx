import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import Home from "@/app/page";
import TemplateDetailPage from "@/app/templates/[templateId]/page";
import { ApiClientError } from "@/lib/api/client";
import * as templateApi from "@/lib/api/templates";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  useParams: () => ({ templateId: "missing" }),
}));
vi.mock("@/components/auth-provider", () => ({
  useAuth: () => ({
    user: { id: 1, username: "demo_user", displayName: "演示用户" },
    isRestoring: false,
    signIn: vi.fn(),
    signOut: vi.fn(),
  }),
}));
vi.mock("@/lib/api/templates", () => ({ getTemplates: vi.fn(), getTemplate: vi.fn() }));

const categories = [
  { code: "TRAVEL", name: "旅行纪念" },
  { code: "SOCIAL", name: "社交分享" },
];
const travel = {
  id: "travel-postcard", title: "旅行明信片", categoryCode: "TRAVEL", categoryName: "旅行纪念",
  coverUrl: "/images/templates/travel-postcard.svg", description: "旅行模板", requiredImageCount: 1,
};
const social = {
  id: "social-story", title: "三图社交故事", categoryCode: "SOCIAL", categoryName: "社交分享",
  coverUrl: "/images/templates/social-story.svg", description: "社交模板", requiredImageCount: 3,
};

function renderWithQuery(ui: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

beforeEach(() => {
  replace.mockReset();
  vi.mocked(templateApi.getTemplates).mockReset();
  vi.mocked(templateApi.getTemplate).mockReset();
});

describe("template browsing", () => {
  it("switches category and renders data returned by the API", async () => {
    vi.mocked(templateApi.getTemplates).mockImplementation(async (categoryCode?: string) => ({
      categories,
      items: categoryCode === "SOCIAL" ? [social] : [travel, social],
      total: categoryCode === "SOCIAL" ? 1 : 2,
    }));
    renderWithQuery(<Home />);
    expect(await screen.findByRole("heading", { name: "旅行明信片" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "社交分享" }));
    expect(await screen.findByRole("heading", { name: "三图社交故事" })).toBeInTheDocument();
    await waitFor(() => expect(templateApi.getTemplates).toHaveBeenLastCalledWith("SOCIAL"));
    expect(screen.queryByRole("heading", { name: "旅行明信片" })).not.toBeInTheDocument();
  });

  it("shows an empty state", async () => {
    vi.mocked(templateApi.getTemplates).mockResolvedValue({ categories, items: [], total: 0 });
    renderWithQuery(<Home />);
    expect(await screen.findByText("当前分类暂无模板。")).toBeInTheDocument();
  });

  it("shows an API error and can retry", async () => {
    vi.mocked(templateApi.getTemplates)
      .mockRejectedValueOnce(new ApiClientError(500, "INTERNAL_ERROR", "failed"))
      .mockResolvedValueOnce({ categories, items: [travel], total: 1 });
    renderWithQuery(<Home />);
    expect(await screen.findByRole("alert")).toHaveTextContent("模板加载失败");
    fireEvent.click(screen.getByRole("button", { name: "重新加载" }));
    expect(await screen.findByRole("heading", { name: "旅行明信片" })).toBeInTheDocument();
  });

  it("renders a dedicated not-found state for detail 404", async () => {
    vi.mocked(templateApi.getTemplate).mockRejectedValue(
      new ApiClientError(404, "TEMPLATE_NOT_FOUND", "模板不存在"),
    );
    renderWithQuery(<TemplateDetailPage />);
    expect(await screen.findByRole("heading", { name: "模板不存在" })).toBeInTheDocument();
    expect(screen.getByText("该模板可能已被删除或停用。")).toBeInTheDocument();
  });
});
