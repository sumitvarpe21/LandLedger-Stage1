import { useCallback, useEffect, useState } from "react";
import { getHealth, type HealthResponse } from "@/services/backend";
import { Button } from "@/components/ui/button";

type HealthState =
  | { status: "checking" }
  | { status: "connected"; result: HealthResponse }
  | { status: "unavailable"; message: string };

export function BackendHealth() {
  const [health, setHealth] = useState<HealthState>({ status: "checking" });

  const checkHealth = useCallback(async () => {
    setHealth({ status: "checking" });
    try {
      setHealth({ status: "connected", result: await getHealth() });
    } catch (error) {
      setHealth({
        status: "unavailable",
        message: error instanceof Error ? error.message : "An unexpected error occurred",
      });
    }
  }, []);

  useEffect(() => {
    void checkHealth();
  }, [checkHealth]);

  return (
    <section
      aria-live="polite"
      className="space-y-2 rounded-md border bg-card p-3 text-sm"
      data-testid="backend-health"
    >
      <p className="stamp text-muted-foreground">Backend connectivity</p>
      {health.status === "checking" && <p>Checking Spring Boot backend…</p>}
      {health.status === "connected" && (
        <p className="text-success">
          Connected — {health.result.service} ({health.result.status})
        </p>
      )}
      {health.status === "unavailable" && (
        <div role="alert" className="space-y-2">
          <p className="text-destructive">Backend unavailable: {health.message}</p>
          <Button type="button" variant="outline" size="sm" onClick={() => void checkHealth()}>
            Retry connection
          </Button>
        </div>
      )}
      <p className="text-xs text-muted-foreground">
        Sign-in and registry records still use local demonstration data.
      </p>
    </section>
  );
}
