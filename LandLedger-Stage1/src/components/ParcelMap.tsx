import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Polygon,
  Polyline,
  Tooltip,
  useMap,
  useMapEvents,
} from "react-leaflet";
import { useEffect } from "react";
import type { BoundaryPoint, Parcel } from "@/services/api";

type Position = [number, number];

function MapClick({
  enabled,
  onBoundaryChange,
  boundary,
}: {
  enabled: boolean;
  onBoundaryChange: ((boundary: BoundaryPoint[]) => void) | undefined;
  boundary: BoundaryPoint[];
}) {
  useMapEvents({
    click(event) {
      if (enabled && onBoundaryChange && boundary.length < 500) {
        onBoundaryChange([
          ...boundary,
          { latitude: event.latlng.lat, longitude: event.latlng.lng },
        ]);
      }
    },
  });
  return null;
}

function Fit({ parcels, selected }: { parcels: Parcel[]; selected?: string | undefined }) {
  const map = useMap();
  useEffect(() => {
    const points = (selected ? parcels.filter((p) => p.parcelId === selected) : parcels).flatMap(
      (parcel): Position[] =>
        parcel.boundary.length
          ? parcel.boundary.map(({ latitude, longitude }) => [latitude, longitude])
          : parcel.latitude != null && parcel.longitude != null
            ? [[parcel.latitude, parcel.longitude]]
            : [],
    );
    if (points.length) map.fitBounds(points, { padding: [40, 40], maxZoom: 16 });
  }, [parcels, selected, map]);
  return null;
}

export default function ParcelMap({
  parcels,
  selected,
  mineId,
  onSelect,
  drawMode = false,
  draftBoundary = [],
  onBoundaryChange,
}: {
  parcels: Parcel[];
  selected?: string | undefined;
  mineId?: string | undefined;
  onSelect: (id: string) => void;
  drawMode?: boolean;
  draftBoundary?: BoundaryPoint[];
  onBoundaryChange?: (boundary: BoundaryPoint[]) => void;
}) {
  const css = getComputedStyle(document.documentElement);
  const c = (v: string) => css.getPropertyValue(v).trim();
  const firstDraftPoint = draftBoundary[0];
  return (
    <MapContainer center={[18.52, 73.85]} zoom={13} className="h-full w-full">
      <TileLayer
        attribution="&copy; OpenStreetMap contributors"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <MapClick enabled={drawMode} boundary={draftBoundary} onBoundaryChange={onBoundaryChange} />
      <Fit parcels={parcels} selected={selected} />
      {parcels.map((p) => {
        if (p.boundary.length < 3) return null;
        const col =
          p.parcelId === selected
            ? c("--destructive")
            : p.ownerId === mineId
              ? c("--accent")
              : c("--primary");
        return (
          <Polygon
            key={p.parcelId}
            positions={p.boundary.map(({ latitude, longitude }) => [latitude, longitude])}
            pathOptions={{
              color: col,
              fillColor: col,
              fillOpacity: p.parcelId === selected ? 0.45 : 0.25,
              weight: 2,
            }}
            eventHandlers={{ click: () => onSelect(p.parcelId) }}
          >
            <Tooltip>
              {p.parcelId} · {p.village}
            </Tooltip>
          </Polygon>
        );
      })}
      {draftBoundary.length === 1 && firstDraftPoint && (
        <CircleMarker
          center={[firstDraftPoint.latitude, firstDraftPoint.longitude]}
          radius={5}
          pathOptions={{ color: c("--destructive"), fillColor: c("--destructive") }}
        />
      )}
      {draftBoundary.length === 2 && (
        <Polyline
          positions={draftBoundary.map(({ latitude, longitude }) => [latitude, longitude])}
          pathOptions={{ color: c("--destructive"), dashArray: "6 6" }}
        />
      )}
      {draftBoundary.length >= 3 && (
        <Polygon
          positions={draftBoundary.map(({ latitude, longitude }) => [latitude, longitude])}
          pathOptions={{
            color: c("--destructive"),
            fillColor: c("--destructive"),
            fillOpacity: 0.2,
            dashArray: "6 6",
          }}
        />
      )}
      {parcels.map((p) => {
        if (p.latitude == null || p.longitude == null) return null;
        const col =
          p.parcelId === selected
            ? c("--destructive")
            : p.ownerId === mineId
              ? c("--accent")
              : c("--primary");
        return (
          <CircleMarker
            key={p.parcelId}
            center={[p.latitude, p.longitude]}
            radius={8}
            pathOptions={{
              color: col,
              fillColor: col,
              fillOpacity: p.parcelId === selected ? 0.8 : 0.55,
              weight: 2,
            }}
            eventHandlers={{ click: () => onSelect(p.parcelId) }}
          >
            <Tooltip>
              {p.parcelId} · {p.village}
            </Tooltip>
          </CircleMarker>
        );
      })}
    </MapContainer>
  );
}
