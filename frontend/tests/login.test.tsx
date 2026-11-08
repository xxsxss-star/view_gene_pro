import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ApiClientError } from "@/lib/api/client";
import LoginPage from "@/app/login/page";
import { useAuth } from "@/components/auth-provider";

const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));
vi.mock("@/components/auth-provider", () => ({ useAuth: vi.fn() }));

const mockedUseAuth = vi.mocked(useAuth);

beforeEach(() => {
  replace.mockReset();
  mockedUseAuth.mockReturnValue({ user: null, isRestoring: false, signIn: vi.fn(), signOut: vi.fn() });
});

function fillAndSubmit() {
  fireEvent.change(screen.getByLabelText("用户名"), { target: { value: "demo_user" } });
  fireEvent.change(screen.getByLabelText("密码"), { target: { value: "demo123" } });
  fireEvent.click(screen.getByRole("button", { name: "登录" }));
}

describe("LoginPage", () => {
  it("disables the button while login is pending and redirects after success", async () => {
    let resolveLogin!: () => void;
    const signIn = vi.fn(() => new Promise<void>((resolve) => { resolveLogin = resolve; }));
    mockedUseAuth.mockReturnValue({ user: null, isRestoring: false, signIn, signOut: vi.fn() });
    render(<LoginPage />);
    fillAndSubmit();
    expect(screen.getByRole("button", { name: "登录中…" })).toBeDisabled();
    resolveLogin();
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
  });

  it("shows one message for invalid credentials", async () => {
    const signIn = vi.fn().mockRejectedValue(new ApiClientError(401, "INVALID_CREDENTIALS", "bad"));
    mockedUseAuth.mockReturnValue({ user: null, isRestoring: false, signIn, signOut: vi.fn() });
    render(<LoginPage />);
    fillAndSubmit();
    expect(await screen.findByRole("alert")).toHaveTextContent("用户名或密码错误");
    expect(replace).not.toHaveBeenCalled();
  });
});
