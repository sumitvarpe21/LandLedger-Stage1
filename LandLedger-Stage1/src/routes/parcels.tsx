import { createFileRoute } from "@tanstack/react-router";
import { ClientOnly } from "@tanstack/react-router";
import { lazy, Suspense, useCallback, useState } from "react";
import { AppShell } from "@/components/AppShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth";
import * as api from "@/services/api";
import { run } from "@/lib/run";
import { useRemoteData } from "@/lib/useRemoteData";

const ParcelMap = lazy(() => import("@/components/ParcelMap"));

const EMPTY_DRAFT: api.ParcelCreateInput = {
  land: {
    parcelId: "",
    surveyNumber: "",
    village: "",
    district: "",
    state: "",
    area: 1,
    landType: "Agricultural",
  },
  ownerEmail: "",
  boundary: [],
};

function boundaryAreaHectares(boundary: api.BoundaryPoint[]): number {
  if (boundary.length < 3) return 0;
  const earthRadiusMeters = 6_371_000;
  let area = 0;
  for (let index = 0; index < boundary.length; index += 1) {
    const current = boundary[index];
    const next = boundary[(index + 1) % boundary.length];
    if (!current || !next) continue;
    const currentLatitude = (current.latitude * Math.PI) / 180;
    const nextLatitude = (next.latitude * Math.PI) / 180;
    const longitudeDelta = ((next.longitude - current.longitude) * Math.PI) / 180;
    area += longitudeDelta * (2 + Math.sin(currentLatitude) + Math.sin(nextLatitude));
  }
  return Math.abs((area * earthRadiusMeters * earthRadiusMeters) / 2) / 10_000;
}

export const Route = createFileRoute("/parcels")({
  head: () => ({
    meta: [
      { title: "GeoLedger Map — LandLedger" },
      { name: "description", content: "Search and explore land parcels on an interactive map." },
      { property: "og:title", content: "GeoLedger Map — LandLedger" },
      {
        property: "og:description",
        content: "Search and explore land parcels on an interactive map.",
      },
    ],
  }),
  component: () => (
    <AppShell title="GeoLedger map">
      <Parcels />
    </AppShell>
  ),
});

function Parcels() {
  const { user } = useAuth();
  const [q, setQ] = useState("");
  const [mine, setMine] = useState(false);
  const [sel, setSel] = useState<string>();
  const [to, setTo] = useState("");
  const [drawMode, setDrawMode] = useState(false);
  const [draft, setDraft] = useState(EMPTY_DRAFT);
  const loadParcels = useCallback(() => api.listParcels(q, mine), [q, mine]);
  const { data, loading, error, reload } = useRemoteData(loadParcels);
  const parcels = data ?? [];
  const p = parcels.find((x) => x.parcelId === sel);
  const fallback = <div className="h-full animate-pulse bg-muted" />;
  return (
    <div className="space-y-6">
      {user!.role === "ADMIN" && (
        <Card>
          <CardHeader>
            <CardTitle className="font-display">Register parcel</CardTitle>
          </CardHeader>
          <CardContent>
            <form
              className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3"
              onSubmit={(event) => {
                event.preventDefault();
                void run(async () => {
                  const parcel = await api.createParcel({
                    ...draft,
                    land: {
                      ...draft.land,
                      area: Number(draft.land.area),
                    },
                    boundary: draft.boundary,
                  });
                  setDraft(EMPTY_DRAFT);
                  setDrawMode(false);
                  setSel(parcel.parcelId);
                  await reload();
                }, "Parcel registered");
              }}
            >
              {(
                [
                  ["parcelId", "Parcel ID", "text"],
                  ["surveyNumber", "Survey number", "text"],
                  ["village", "Village", "text"],
                  ["district", "District", "text"],
                  ["state", "State", "text"],
                  ["area", "Area (hectares)", "number"],
                ] as const
              ).map(([key, label, type]) => (
                <div key={key}>
                  <Label htmlFor={`parcel-${key}`}>{label}</Label>
                  <Input
                    id={`parcel-${key}`}
                    required
                    type={type}
                    min={key === "area" ? "0.000001" : undefined}
                    step={key === "area" ? "any" : undefined}
                    value={draft.land[key]}
                    onChange={(event) =>
                      setDraft({
                        ...draft,
                        land: {
                          ...draft.land,
                          [key]: key === "area" ? Number(event.target.value) : event.target.value,
                        },
                      })
                    }
                  />
                </div>
              ))}
              <div>
                <Label htmlFor="parcel-land-type">Land type</Label>
                <select
                  id="parcel-land-type"
                  className="h-9 w-full rounded-md border bg-card px-3 text-sm"
                  value={draft.land.landType}
                  onChange={(event) =>
                    setDraft({ ...draft, land: { ...draft.land, landType: event.target.value } })
                  }
                >
                  {["Agricultural", "Residential", "Commercial", "Industrial"].map((type) => (
                    <option key={type}>{type}</option>
                  ))}
                </select>
              </div>
              <div>
                <Label htmlFor="parcel-owner-email">Owner citizen email</Label>
                <Input
                  id="parcel-owner-email"
                  required
                  type="email"
                  value={draft.ownerEmail}
                  onChange={(event) => setDraft({ ...draft, ownerEmail: event.target.value })}
                />
              </div>
              <div>
                <Label htmlFor="parcel-latitude">Latitude (optional)</Label>
                <Input
                  id="parcel-latitude"
                  type="number"
                  step="any"
                  min="-90"
                  max="90"
                  value={draft.land.latitude ?? ""}
                  onChange={(event) =>
                    setDraft({
                      ...draft,
                      land: {
                        ...draft.land,
                        latitude: event.target.value ? Number(event.target.value) : undefined,
                      },
                    })
                  }
                />
              </div>
              <div>
                <Label htmlFor="parcel-longitude">Longitude (optional)</Label>
                <Input
                  id="parcel-longitude"
                  type="number"
                  step="any"
                  min="-180"
                  max="180"
                  value={draft.land.longitude ?? ""}
                  onChange={(event) =>
                    setDraft({
                      ...draft,
                      land: {
                        ...draft.land,
                        longitude: event.target.value ? Number(event.target.value) : undefined,
                      },
                    })
                  }
                />
              </div>
              <div className="flex items-end">
                <Button type="submit" disabled={draft.boundary.length < 3}>
                  Create parcel
                </Button>
              </div>
            </form>
            <p className="mt-3 text-xs text-muted-foreground">
              Draw the parcel boundary on the map below. Area is estimated from the outline; verify
              it against official records. The owner must be an active citizen.
            </p>
          </CardContent>
        </Card>
      )}
      <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
        <div className="space-y-3">
          <div className="flex flex-wrap gap-2">
            <Input
              placeholder="Parcel ID, survey no., village or district"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              className="max-w-sm"
            />
            {user!.role === "CITIZEN" && (
              <Button variant={mine ? "default" : "outline"} onClick={() => setMine(!mine)}>
                My parcels only
              </Button>
            )}
          </div>
          {user!.role === "ADMIN" && (
            <div className="flex flex-wrap items-center gap-2">
              <Button
                type="button"
                variant={drawMode ? "default" : "outline"}
                onClick={() => setDrawMode(!drawMode)}
              >
                {drawMode ? "Stop drawing boundary" : "Draw parcel boundary"}
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={!draft.boundary.length}
                onClick={() => {
                  const boundary = draft.boundary.slice(0, -1);
                  setDraft({
                    ...draft,
                    boundary,
                    land: {
                      ...draft.land,
                      area: boundary.length >= 3 ? boundaryAreaHectares(boundary) : 1,
                    },
                  });
                }}
              >
                Undo last point
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={!draft.boundary.length}
                onClick={() =>
                  setDraft({
                    ...draft,
                    boundary: [],
                    land: { ...draft.land, area: 1 },
                  })
                }
              >
                Clear boundary
              </Button>
              <span className="text-sm text-muted-foreground" role="status">
                {drawMode
                  ? `Click the map to add boundary points (${draft.boundary.length} added; 3 minimum, 500 maximum).`
                  : `${draft.boundary.length} boundary points selected.`}
              </span>
            </div>
          )}
          {loading && (
            <p role="status" className="text-sm text-muted-foreground">
              Loading parcels…
            </p>
          )}
          {error && (
            <div role="alert" className="text-sm text-destructive">
              Could not load parcels: {error}{" "}
              <Button variant="outline" size="sm" onClick={() => void reload().catch(() => {})}>
                Retry
              </Button>
            </div>
          )}
          <div className="h-[560px] overflow-hidden rounded-lg border">
            <ClientOnly fallback={fallback}>
              <Suspense fallback={fallback}>
                <ParcelMap
                  parcels={parcels}
                  selected={sel}
                  mineId={user!.id}
                  onSelect={setSel}
                  drawMode={user!.role === "ADMIN" && drawMode}
                  draftBoundary={draft.boundary}
                  onBoundaryChange={(boundary) =>
                    setDraft((current) => ({
                      ...current,
                      boundary,
                      land: {
                        ...current.land,
                        area: boundary.length >= 3 ? boundaryAreaHectares(boundary) : 1,
                      },
                    }))
                  }
                />
              </Suspense>
            </ClientOnly>
          </div>
          <div className="flex flex-wrap gap-2">
            {parcels.map((x) => (
              <button
                key={x.parcelId}
                onClick={() => setSel(x.parcelId)}
                className={`rounded border px-2 py-1 font-mono text-xs ${x.parcelId === sel ? "bg-primary text-primary-foreground" : "bg-card"}`}
              >
                {x.parcelId}
              </button>
            ))}
          </div>
        </div>
        <Card className="h-fit">
          <CardHeader>
            <CardTitle className="font-display">{p ? p.parcelId : "Select a parcel"}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            {!p && (
              <p className="text-muted-foreground">Click a plot on the map to see its details.</p>
            )}
            {p && (
              <>
                <dl className="grid grid-cols-2 gap-2">
                  {[
                    ["Survey", p.surveyNumber],
                    ["Village", p.village],
                    ["District", p.district],
                    ["State", p.state],
                    ["Area", `${p.area.toFixed(2)} ha`],
                    ["Type", p.landType],
                    ["Owner", p.ownerName ?? "Not available"],
                  ].map(([k, v]) => (
                    <div key={k}>
                      <dt className="stamp text-muted-foreground">{k}</dt>
                      <dd>{v}</dd>
                    </div>
                  ))}
                </dl>
                <div>
                  <p className="stamp mb-2 text-muted-foreground">Ownership history</p>
                  <ol className="space-y-2 border-l-2 border-accent pl-3">
                    {p.history.map((h) => (
                      <li key={`${h.date}-${h.event}`}>
                        <b>{h.ownerName}</b> — {h.event}
                        <div className="text-xs text-muted-foreground">
                          {new Date(h.date).toLocaleDateString()} · Tx ref:{" "}
                          {h.txRef ?? "NOT CONFIGURED"} · Verification: NOT CONNECTED
                        </div>
                      </li>
                    ))}
                  </ol>
                </div>
                {user!.role === "CITIZEN" && p.ownerId === user!.id && (
                  <div className="space-y-2 border-t pt-3">
                    <p className="stamp text-muted-foreground">Request ownership transfer</p>
                    <Input
                      type="email"
                      placeholder="Recipient citizen email"
                      value={to}
                      onChange={(e) => setTo(e.target.value)}
                    />
                    <Button
                      size="sm"
                      onClick={() =>
                        void run(async () => {
                          await api.requestTransfer(p.parcelId, to);
                          setTo("");
                        }, "Transfer request sent to registrar")
                      }
                    >
                      Submit request
                    </Button>
                  </div>
                )}
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
