import { createFileRoute } from "@tanstack/react-router";
import { useCallback } from "react";
import { AppShell } from "@/components/AppShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";
import * as api from "@/services/api";
import { run } from "@/lib/run";
import { useRemoteData } from "@/lib/useRemoteData";

export const Route = createFileRoute("/admin")({
  head: () => ({
    meta: [
      { title: "Users & Audit — LandLedger" },
      { name: "description", content: "Manage user roles and view the audit log." },
      { property: "og:title", content: "Users & Audit — LandLedger" },
      { property: "og:description", content: "Manage user roles and view the audit log." },
    ],
  }),
  component: () => (
    <AppShell title="Users & audit log" roles={["ADMIN"]}>
      <Admin />
    </AppShell>
  ),
});

function Admin() {
  const { user } = useAuth();
  const loadAdmin = useCallback(async () => {
    const [users, audit] = await Promise.all([api.listUsers(), api.listAudit()]);
    return { users, audit };
  }, []);
  const { data, loading, error, reload } = useRemoteData(loadAdmin);
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      {loading && (
        <p role="status" className="text-muted-foreground">
          Loading administrator data…
        </p>
      )}
      {error && (
        <div role="alert" className="text-destructive">
          Could not load administrator data: {error}{" "}
          <Button variant="outline" size="sm" onClick={() => void reload().catch(() => {})}>
            Retry
          </Button>
        </div>
      )}
      <Card>
        <CardHeader>
          <CardTitle className="font-display">Users</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {(data?.users ?? []).map((u) => (
            <div
              key={u.id}
              className="flex flex-wrap items-center justify-between gap-2 border-b pb-3 text-sm"
            >
              <div>
                <b>{u.name}</b>
                <div className="text-muted-foreground">{u.email}</div>
              </div>
              <div className="flex items-center gap-2">
                <select
                  disabled={u.id === user!.id}
                  className="h-8 rounded border bg-card px-2 text-xs"
                  value={u.role}
                  onChange={(e) =>
                    void run(async () => {
                      await api.updateUser(u.id, { role: e.target.value as api.Role });
                      await reload();
                    }, "Role updated")
                  }
                >
                  {["CITIZEN", "OFFICER", "ADMIN"].map((r) => (
                    <option key={r}>{r}</option>
                  ))}
                </select>
                <Button
                  size="sm"
                  variant={u.active ? "outline" : "default"}
                  disabled={u.id === user!.id}
                  onClick={() =>
                    void run(
                      async () => {
                        await api.updateUser(u.id, { active: !u.active });
                        await reload();
                      },
                      u.active ? "Deactivated" : "Activated",
                    )
                  }
                >
                  {u.active ? "Deactivate" : "Activate"}
                </Button>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle className="font-display">Audit log</CardTitle>
        </CardHeader>
        <CardContent className="max-h-[600px] space-y-2 overflow-auto text-sm">
          {(data?.audit ?? []).map((a) => (
            <div key={a.id} className="border-l-2 border-primary pl-3">
              <span className="font-mono text-xs">{a.actor}</span> — {a.action}
              <div className="text-xs text-muted-foreground">{new Date(a.at).toLocaleString()}</div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
