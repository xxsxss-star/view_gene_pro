const TOKEN_KEY = "view-gene.access-token";
const TOKEN_EVENT = "view-gene:token-change";

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  window.localStorage.setItem(TOKEN_KEY, token);
  window.dispatchEvent(new Event(TOKEN_EVENT));
}

export function clearAccessToken(): void {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(TOKEN_KEY);
    window.dispatchEvent(new Event(TOKEN_EVENT));
  }
}

export function subscribeToAccessToken(onStoreChange: () => void): () => void {
  window.addEventListener(TOKEN_EVENT, onStoreChange);
  window.addEventListener("storage", onStoreChange);
  return () => {
    window.removeEventListener(TOKEN_EVENT, onStoreChange);
    window.removeEventListener("storage", onStoreChange);
  };
}
