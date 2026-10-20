# LandLedger — Stage 1

A land-registry application with three roles:

- **Citizen** — submit a land registration application, track its status, view
  registered parcels and ownership history, request a plot transfer.
- **Officer (Registrar)** — review the queue, verify details, mark applications
  under review or reject with a reason, and decide ownership transfers.
- **Admin** — draw and register parcel boundaries for registered citizens,
  approve applications to register parcels, manage user accounts, and view every
  record, audit log and system notices.

Approved applications create a PostgreSQL parcel and ownership-history record.
Registration review, transfers, profiles, notifications, users and audit logs
are stored by the Spring Boot backend. There are no seeded demo accounts or
browser-stored registry records.

## Run it

See **[RUN-ON-YOUR-LAPTOP.md](RUN-ON-YOUR-LAPTOP.md)** for the full step-by-step
(Windows). In short:

```sh
npm install
npm run dev      # then open http://localhost:8080
```

Configure PostgreSQL and the backend first; see
[landledger-backend/.env.example](../landledger-backend/.env.example) and
[RUN-ON-YOUR-LAPTOP.md](RUN-ON-YOUR-LAPTOP.md). The first administrator is
created only from credentials configured locally in the backend `.env` file.
Public registration creates Citizen accounts; an administrator assigns
Officer roles. Only administrators can create parcels, including by approving
a citizen's registration application. Admin-created parcel boundaries are
visible on the map, and signed-in users can inspect parcel details and named
ownership history by selecting a boundary.

The frontend stores only the current-session authentication token in browser
session storage. All user accounts and registry records are fetched from the
backend.

## Stack

TanStack Start (React 19 + TypeScript), Tailwind CSS v4, React Leaflet for the map,
Recharts for the admin charts.

## Where things live

```
src/
  routes/        one file per page (sign in, dashboard, applications,
                 parcels, transfers, admin)
  services/
    api.ts       REST operations for authentication and registry features.
    backend.ts   Shared HTTP client and Spring Boot health endpoint integration.
  lib/
    auth.tsx     who is signed in, and which role they hold
  components/    shared pieces (map, page shell, UI primitives)
```

Parcel locations are optional applicant-submitted coordinates. The GIS map
shows parcel boundaries drawn by an administrator and point markers for records
that only have coordinates. Boundary-derived area is an estimate; map geometry
does not indicate official verification.
