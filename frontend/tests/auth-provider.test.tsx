import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { AuthGuard } from "@/components/auth-guard";
import { AuthProvider, useAuth } from "@/components/auth-provider";
import { ApiClientError } from "@/lib/api/client";
import * as authApi from "@/lib/api/auth";

const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));
vi.mock("@/lib/api/auth", () => ({ getCurrentUser: vi.fn(), login: vi.fn(), logout: vi.fn() }));

function UserProbe() {
  const { user, isRestoring } = useAuth();
  return <p>{isRestoring ? "restoring" : user?.displayName ?? "anonymous"}</p>;
}

function renderAuth(content: React.ReactNode = <UserProbe />) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><AuthProvider>{content}</AuthProvider></QueryClientProvider>);
}

beforeEach(() => {
  localStorage.clear();
  replace.mockReset();
  vi.mocked(authApi.getCurrentUser).mockReset();
});

describe("AuthProvider", () => {
  it("restores the current user from a stored token", async () => {
    localStorage.setItem("view-gene.access-token", "stored-token");
    vi.mocked(authApi.getCurrentUser).mockResolvedValue({ id: 1, username: "demo_user", displayName: "演示用户" });
    renderAuth();
    expect(await screen.findByText("演示用户")).toBeInTheDocument();
    expect(authApi.getCurrentUser).toHaveBeenCalledOnce();
  });

  it("clears an invalid token and lets the guard redirect to login", async () => {
    localStorage.setItem("view-gene.access-token", "expired-token");
    vi.mocked(authApi.getCurrentUser).mockRejectedValue(new ApiClientError(401, "TOKEN_EXPIRED", "expired"));
    renderAuth(<AuthGuard><p>private</p></AuthGuard>);
    await waitFor(() => expect(localStorage.getItem("view-gene.access-token")).toBeNull());
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(screen.queryByText("private")).not.toBeInTheDocument();
  });
});
