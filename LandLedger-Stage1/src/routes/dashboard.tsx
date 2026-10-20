import { createFileRoute } from "@tanstack/react-router";
import { useCallback, useState } from "react";
import { Bell } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";
import * as api from "@/services/api";
import { run } from "@/lib/run";
import { useRemoteData } from "@/lib/useRemoteData";

export const Route = createFileRoute("/dashboard")({
  head: () => ({
    meta: [
      { title: "Dashboard — LandLedger" },
      { name: "description", content: "Your LandLedger overview." },
      { property: "og:title", content: "Dashboard — LandLedger" },
      { property: "og:description", content: "Your LandLedger overview." },
    ],
  }),
  component: () => (
    <AppShell title="Dashboard">
      <Dash />
    </AppShell>
  ),
});

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <Card>
      <CardContent className="pt-6">
        <p className="stamp text-muted-foreground">{label}</p>
        <p className="font-display text-4xl font-bold text-primary">{value}</p>
      </CardContent>
    </Card>
  );
}

function Dash() {
  const { user, updateUser } = useAuth();
  const [p, setP] = useState({ name: user!.name, phone: user!.phone });
  const loadDashboard = useCallback(async () => {
    const [applications, transfers, stats, parcels, notices] = await Promise.all([
      api.listApplications(),
      api.listTransfers(),
      api.stats(),
      api.listParcels("", true),
      api.listNotices(),
    ]);
    return { applications, transfers, stats, parcels, notices };
  }, []);
  const { data, loading, error, reload } = useRemoteData(loadDashboard);
  const apps = data?.applications ?? [];
  const tr = data?.transfers ?? [];
  const s = data?.stats ?? { parcels: 0, pending: 0, transfers: 0, users: 0 };
  const notices = data?.notices ?? [];
  return (
    <div className="space-y-6">
      {loading && (
        <p role="status" className="text-muted-foreground">
          Loading dashboard…
        </p>
      )}
      {error && (
        <div role="alert" className="text-destructive">
          Could not load dashboard data: {error}{" "}
          <Button variant="outline" size="sm" onClick={() => void reload().catch(() => {})}>
            Retry
          </Button>
        </div>
      )}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {user!.role === "CITIZEN" ? (
          <>
            <Stat label="My parcels" value={data?.parcels.length ?? 0} />
            <Stat label="Applications" value={apps.length} />
            <Stat
              label="Pending"
              value={
                apps.filter((a) => a.status === "PENDING" || a.status === "UNDER_REVIEW").length
              }
            />
            <Stat label="Transfers" value={tr.length} />
          </>
        ) : (
          <>
            <Stat label="Registered parcels" value={s.parcels} />
            <Stat label="Applications to review" value={s.pending} />
            <Stat label="Pending transfers" value={s.transfers} />
            <Stat label="Users" value={s.users} />
          </>
        )}
      </div>
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 font-display">
              <Bell className="h-4 w-4" />
              Notifications
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {!loading && notices.length === 0 && (
              <p className="text-sm text-muted-foreground">No updates yet.</p>
            )}
            {notices.slice(0, 8).map((n) => (
              <div key={n.id} className="border-l-2 border-accent pl-3 text-sm">
                {n.text}
                <div className="text-xs text-muted-foreground">
                  {new Date(n.at).toLocaleString()}
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="font-display">Profile</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Input value={p.name} onChange={(e) => setP({ ...p, name: e.target.value })} />
            <Input value={p.phone} onChange={(e) => setP({ ...p, phone: e.target.value })} />
            <p className="text-sm text-muted-foreground">
              {user!.email} · role <span className="stamp">{user!.role}</span> (cannot be
              self-edited)
            </p>
            <Button
              onClick={() =>
                void run(async () => {
                  updateUser(await api.updateProfile(p.name, p.phone));
                }, "Profile saved")
              }
            >
              Save
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
