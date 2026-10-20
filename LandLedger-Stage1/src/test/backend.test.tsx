import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { BackendHealth } from "@/components/BackendHealth";

describe("backend health", () => {
  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("displays the health result returned by Spring Boot", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ status: "UP", service: "LandLedger Backend" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<BackendHealth />);

    await waitFor(() =>
      expect(screen.getByText(/Connected — LandLedger Backend \(UP\)/)).toBeTruthy(),
    );
    expect(fetchMock.mock.calls[0]?.[0]).toBe("http://localhost:8081/api/health");
    expect(new Headers(fetchMock.mock.calls[0]?.[1]?.headers).get("Accept")).toBe(
      "application/json",
    );
  });

  it("shows a visible error when the backend cannot be reached", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    render(<BackendHealth />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Backend unavailable: Could not reach the backend at http://localhost:8081/api: Failed to fetch",
    );
  });

  it("does not report an unexpected health response as connected", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: "UNKNOWN" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    render(<BackendHealth />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Backend unavailable: Backend returned an unexpected health response",
    );
  });
});
