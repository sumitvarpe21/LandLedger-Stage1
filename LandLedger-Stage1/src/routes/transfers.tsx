import { createFileRoute } from "@tanstack/react-router";
import { useCallback, useState } from "react";
import { AppShell, StatusBadge } from "@/components/AppShell";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";
import * as api from "@/services/api";
import { run } from "@/lib/run";
import { useRemoteData } from "@/lib/useRemoteData";

export const Route = createFileRoute("/transfers")({
  head: () => ({
    meta: [
      { title: "Ownership Transfers — LandLedger" },
      { name: "description", content: "Track and review land ownership transfer requests." },
      { property: "og:title", content: "Ownership Transfers — LandLedger" },
      { property: "og:description", content: "Track and review land ownership transfer requests." },
    ],
  }),
  component: () => (
    <AppShell title="Ownership transfers">
      <Transfers />
    </AppShell>
  ),
});

function Transfers() {
  const { user } = useAuth();
  const [reason, setReason] = useState<Record<string, string>>({});
  const loadTransfers = useCallback(() => api.listTransfers(), []);
  const { data, loading, error, reload } = useRemoteData(loadTransfers);
  const list = data ?? [];
  return (
    <div className="space-y-3">
      {user!.role === "CITIZEN" && (
        <p className="text-sm text-muted-foreground">
          Start a transfer from a parcel you own on the GeoLedger map.
        </p>
      )}
      {loading && (
        <p role="status" className="text-muted-foreground">
          Loading transfers…
        </p>
      )}
      {error && (
        <div role="alert" className="text-destructive">
          Could not load transfers: {error}{" "}
          <Button variant="outline" size="sm" onClick={() => void reload().catch(() => {})}>
            Retry
          </Button>
        </div>
      )}
      {!loading && !error && list.length === 0 && (
        <p className="text-muted-foreground">No transfer requests.</p>
      )}
      {list.map((t) => {
        return (
          <Card key={t.id}>
            <CardContent className="space-y-2 pt-6">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <span className="font-mono text-sm">{t.id}</span> · Parcel <b>{t.parcelId}</b>
                </div>
                <StatusBadge s={t.status} />
              </div>
              <p className="text-sm text-muted-foreground">
                From {t.fromName} → {t.toEmail} · Requested {new Date(t.createdAt).toLocaleString()}
                {t.decidedAt && ` · Decided ${new Date(t.decidedAt).toLocaleString()}`}
              </p>
              {t.reason && <p className="text-sm text-destructive">Reason: {t.reason}</p>}
              {user!.role === "OFFICER" && t.status === "PENDING" && (
                <div className="space-y-2 rounded bg-muted p-3">
                  <p className="text-sm">
                    Requested owner on record: <b>{t.fromName}</b>. The backend checks current
                    ownership when this request is decided.
                  </p>
                  <Input
                    placeholder="Rejection reason (required to reject)"
                    value={reason[t.id] ?? ""}
                    onChange={(e) => setReason({ ...reason, [t.id]: e.target.value })}
                  />
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={() =>
                        void run(async () => {
                          await api.decideTransfer(t.id, "APPROVED");
                          await reload();
                        }, "Transfer approved")
                      }
                    >
                      Approve
                    </Button>
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() =>
                        void run(async () => {
                          await api.decideTransfer(t.id, "REJECTED", reason[t.id]);
                          await reload();
                        }, "Transfer rejected")
                      }
                    >
                      Reject
                    </Button>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
