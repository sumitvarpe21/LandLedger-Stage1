import { createFileRoute } from "@tanstack/react-router";
import { ClientOnly } from "@tanstack/react-router";
import { lazy, Suspense, useCallback, useState } from "react";
import { AppShell, StatusBadge } from "@/components/AppShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/lib/auth";
import { useRemoteData } from "@/lib/useRemoteData";
import * as api from "@/services/api";
import { run } from "@/lib/run";

const ParcelMap = lazy(() => import("@/components/ParcelMap"));

export const Route = createFileRoute("/applications")({
  head: () => ({
    meta: [
      { title: "Land Applications — LandLedger" },
      { name: "description", content: "Submit and review land registration applications." },
      { property: "og:title", content: "Land Applications — LandLedger" },
      { property: "og:description", content: "Submit and review land registration applications." },
    ],
  }),
  component: () => (
    <AppShell title="Land registration applications">
      <Apps />
    </AppShell>
  ),
});

const EMPTY: api.LandInput = {
  parcelId: "",
  surveyNumber: "",
  village: "",
  district: "",
  state: "",
  area: 1,
  landType: "Agricultural",
};
const FIELDS: [keyof api.LandInput, string][] = [
  ["parcelId", "Parcel ID"],
  ["surveyNumber", "Survey number"],
  ["village", "Village"],
  ["district", "District"],
  ["state", "State"],
  ["area", "Area (hectares)"],
];

function LandForm({
  initial,
  onSubmit,
  label,
}: {
  initial: api.LandInput;
  onSubmit: (v: api.LandInput) => Promise<boolean>;
  label: string;
}) {
  const [f, setF] = useState(initial);
  return (
    <form
      className="grid gap-3 sm:grid-cols-2"
      onSubmit={(e) => {
        e.preventDefault();
        void onSubmit({
          ...f,
          area: Number(f.area),
          latitude: f.latitude === undefined ? undefined : Number(f.latitude),
          longitude: f.longitude === undefined ? undefined : Number(f.longitude),
        }).then((success) => {
          if (success) setF(EMPTY);
        });
      }}
    >
      {FIELDS.map(([k, l]) => (
        <div key={k}>
          <Label>{l}</Label>
          <Input
            required
            type={k === "area" ? "number" : "text"}
            step={k === "area" ? "0.01" : undefined}
            min={k === "area" ? "0.01" : undefined}
            value={f[k] ?? ""}
            onChange={(e) =>
              setF({ ...f, [k]: k === "area" ? Number(e.target.value) : e.target.value })
            }
          />
        </div>
      ))}
      <div>
        <Label>Land type</Label>
        <select
          className="h-9 w-full rounded-md border bg-card px-3 text-sm"
          value={f.landType}
          onChange={(e) => setF({ ...f, landType: e.target.value })}
        >
          {["Agricultural", "Residential", "Commercial", "Industrial"].map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
      </div>
      <div>
        <Label>Latitude (optional)</Label>
        <Input
          type="number"
          step="any"
          min="-90"
          max="90"
          value={f.latitude ?? ""}
          onChange={(e) =>
            setF({ ...f, latitude: e.target.value ? Number(e.target.value) : undefined })
          }
        />
      </div>
      <div>
        <Label>Longitude (optional)</Label>
        <Input
          type="number"
          step="any"
          min="-180"
          max="180"
          value={f.longitude ?? ""}
          onChange={(e) =>
            setF({ ...f, longitude: e.target.value ? Number(e.target.value) : undefined })
          }
        />
      </div>
      <p className="text-xs text-muted-foreground sm:col-span-2">
        Coordinates are optional and are supplied by the applicant; no land boundaries are generated
        or verified.
      </p>
      <div className="flex items-end">
        <Button type="submit">{label}</Button>
      </div>
    </form>
  );
}

function Apps() {
  const { user } = useAuth();
  const [editing, setEditing] = useState<string | null>(null);
  const [reasons, setReasons] = useState<Record<string, string>>({});
  const [approvalDraft, setApprovalDraft] = useState<{
    applicationId: string;
    boundary: api.BoundaryPoint[];
  } | null>(null);
  const [q, setQ] = useState("");
  const loadApplications = useCallback(() => api.listApplications(), []);
  const { data, loading, error, reload } = useRemoteData(loadApplications);
  const canReview = user!.role === "OFFICER" || user!.role === "ADMIN";
  const apps = (data ?? []).filter(
    (a) =>
      !q ||
      `${a.id} ${a.parcelId} ${a.village} ${a.district}`.toLowerCase().includes(q.toLowerCase()),
  );
  return (
    <div className="space-y-6">
      {user!.role === "CITIZEN" && (
        <Card>
          <CardHeader>
            <CardTitle className="font-display">New application</CardTitle>
          </CardHeader>
          <CardContent>
            <LandForm
              initial={EMPTY}
              label="Submit application"
              onSubmit={async (v) => {
                const ok = await run(() => api.submitApplication(v), "Application submitted");
                if (ok) await reload();
                return ok;
              }}
            />
          </CardContent>
        </Card>
      )}
      <Input
        placeholder="Search by application, parcel, village, district…"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        className="max-w-md"
      />
      <div className="space-y-3">
        {loading && (
          <p role="status" className="text-muted-foreground">
            Loading applications…
          </p>
        )}
        {error && (
          <div role="alert" className="text-destructive">
            Could not load applications: {error}{" "}
            <Button variant="outline" size="sm" onClick={() => void reload().catch(() => {})}>
              Retry
            </Button>
          </div>
        )}
        {!loading && !error && apps.length === 0 && (
          <p className="text-muted-foreground">No applications.</p>
        )}
        {apps.map((a) => (
          <Card key={a.id}>
            <CardContent className="space-y-3 pt-6">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <span className="font-mono text-sm">{a.id}</span> · <b>{a.parcelId}</b> · Survey{" "}
                  {a.surveyNumber}
                </div>
                <StatusBadge s={a.status} />
              </div>
              <p className="text-sm text-muted-foreground">
                {a.village}, {a.district}, {a.state} · {a.area} ha · {a.landType}
                {user!.role !== "CITIZEN" && ` · Applicant: ${a.applicantName}`}
                {a.reviewedBy && ` · Reviewed by ${a.reviewedBy}`} · Updated{" "}
                {new Date(a.updatedAt).toLocaleString()}
              </p>
              {a.rejectionReason && (
                <p className="rounded bg-destructive/10 p-2 text-sm text-destructive">
                  Reason: {a.rejectionReason}
                </p>
              )}
              {user!.role === "CITIZEN" &&
                a.status === "REJECTED" &&
                (editing === a.id ? (
                  <LandForm
                    initial={a}
                    label="Resubmit"
                    onSubmit={async (v) => {
                      const ok = await run(() => api.resubmitApplication(a.id, v), "Resubmitted");
                      if (ok) {
                        setEditing(null);
                        await reload();
                      }
                      return ok;
                    }}
                  />
                ) : (
                  <Button size="sm" variant="outline" onClick={() => setEditing(a.id)}>
                    Correct & resubmit
                  </Button>
                ))}
              {canReview && (a.status === "PENDING" || a.status === "UNDER_REVIEW") && (
                <div className="space-y-2">
                  <Textarea
                    placeholder="Rejection reason (required to reject)"
                    value={reasons[a.id] ?? ""}
                    onChange={(e) => setReasons({ ...reasons, [a.id]: e.target.value })}
                  />
                  <div className="flex gap-2">
                    {a.status === "PENDING" && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() =>
                          void run(async () => {
                            await api.setApplicationStatus(a.id, "UNDER_REVIEW");
                            await reload();
                          }, "Marked under review")
                        }
                      >
                        Mark under review
                      </Button>
                    )}
                    {user!.role === "ADMIN" && (
                      <Button
                        size="sm"
                        variant={approvalDraft?.applicationId === a.id ? "default" : "outline"}
                        onClick={() =>
                          setApprovalDraft(
                            approvalDraft?.applicationId === a.id
                              ? null
                              : { applicationId: a.id, boundary: [] },
                          )
                        }
                      >
                        {approvalDraft?.applicationId === a.id
                          ? "Cancel parcel boundary"
                          : "Draw boundary to approve"}
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="destructive"
                      onClick={() =>
                        void run(async () => {
                          await api.setApplicationStatus(a.id, "REJECTED", reasons[a.id]);
                          await reload();
                        }, "Rejected")
                      }
                    >
                      Reject
                    </Button>
                  </div>
                  {user!.role === "ADMIN" && approvalDraft?.applicationId === a.id && (
                    <div className="space-y-2 rounded-md border p-3">
                      <p className="text-sm text-muted-foreground">
                        Click at least three points on the map to outline this parcel before
                        registering it.
                      </p>
                      <div className="h-72 overflow-hidden rounded-md border">
                        <ClientOnly fallback={<div className="h-full animate-pulse bg-muted" />}>
                          <Suspense fallback={<div className="h-full animate-pulse bg-muted" />}>
                            <ParcelMap
                              parcels={[]}
                              mineId={user!.id}
                              onSelect={() => {}}
                              drawMode
                              draftBoundary={approvalDraft.boundary}
                              onBoundaryChange={(boundary) =>
                                setApprovalDraft({ applicationId: a.id, boundary })
                              }
                            />
                          </Suspense>
                        </ClientOnly>
                      </div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span role="status" className="text-sm text-muted-foreground">
                          {approvalDraft.boundary.length} boundary points
                        </span>
                        <Button
                          size="sm"
                          type="button"
                          variant="outline"
                          disabled={!approvalDraft.boundary.length}
                          onClick={() =>
                            setApprovalDraft({
                              applicationId: a.id,
                              boundary: approvalDraft.boundary.slice(0, -1),
                            })
                          }
                        >
                          Undo last point
                        </Button>
                        <Button
                          size="sm"
                          type="button"
                          variant="outline"
                          disabled={!approvalDraft.boundary.length}
                          onClick={() => setApprovalDraft({ applicationId: a.id, boundary: [] })}
                        >
                          Clear boundary
                        </Button>
                        <Button
                          size="sm"
                          type="button"
                          disabled={approvalDraft.boundary.length < 3}
                          onClick={() =>
                            void run(async () => {
                              await api.setApplicationStatus(
                                a.id,
                                "APPROVED",
                                undefined,
                                approvalDraft.boundary,
                              );
                              setApprovalDraft(null);
                              await reload();
                            }, "Approved — parcel registered")
                          }
                        >
                          Approve & register parcel
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              )}
              {user!.role === "CITIZEN" && (
                <Button size="sm" variant="ghost" onClick={() => window.print()}>
                  Print receipt
                </Button>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
