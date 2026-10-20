const API_BASE_URL = (
  import.meta.env["VITE_API_BASE_URL"]?.trim() || "http://localhost:8081/api"
).replace(/\/+$/, "");
const ACCESS_TOKEN_KEY = "landledger-access-token";

export function setAccessToken(token: string | null) {
  if (typeof window === "undefined") return;
  if (token) {
    window.sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
  } else {
    window.sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  }
}

export interface HealthResponse {
  status: "UP";
  service: string;
}

export class BackendHttpError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = "BackendHttpError";
  }
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    const headers = new Headers(init?.headers);
    if (!headers.has("Accept")) {
      headers.set("Accept", "application/json");
    }
    if (typeof window !== "undefined") {
      const token = window.sessionStorage.getItem(ACCESS_TOKEN_KEY);
      if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
      }
    }
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers,
    });
  } catch (error) {
    const detail = error instanceof Error ? `: ${error.message}` : "";
    throw new Error(`Could not reach the backend at ${API_BASE_URL}${detail}`);
  }

  if (!response.ok) {
    const detail = await response.text();
    let message = detail;
    if (detail) {
      try {
        const payload: unknown = JSON.parse(detail);
        if (
          typeof payload === "object" &&
          payload !== null &&
          "error" in payload &&
          typeof payload.error === "string"
        ) {
          message = payload.error;
        }
      } catch {
        message = detail;
      }
    }
    throw new BackendHttpError(
      message || `Backend request failed (${response.status} ${response.statusText})`,
      response.status,
    );
  }

  try {
    return (await response.json()) as T;
  } catch {
    throw new Error("Backend returned an invalid JSON response");
  }
}

export async function getHealth(): Promise<HealthResponse> {
  const result = await request<unknown>("/health");
  if (
    typeof result !== "object" ||
    result === null ||
    !("status" in result) ||
    result.status !== "UP" ||
    !("service" in result) ||
    typeof result.service !== "string"
  ) {
    throw new Error("Backend returned an unexpected health response");
  }
  return { status: "UP", service: result.service };
}
