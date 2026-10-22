import { Link, useNavigate } from "@tanstack/react-router";
import { useEffect, type ReactNode } from "react";
import { LayoutDashboard, FileText, Map, ArrowLeftRight, Users, LogOut, Landmark } from "lucide-react";
import { useAuth } from "@/lib/auth";
import type { Role } from "@/services/api";

const NAV: { to: "/dashboard" | "/applications" | "/parcels" | "/transfers" | "/admin"; label: string; icon: typeof Map; roles: Role[] }[] = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard, roles: ["CITIZEN", "OFFICER", "ADMIN"] },
  { to: "/applications", label: "Applications", icon: FileText, roles: ["CITIZEN", "OFFICER", "ADMIN"] },
  { to: "/parcels", label: "GeoLedger Map", icon: Map, roles: ["CITIZEN", "OFFICER", "ADMIN"] },
  { to: "/transfers", label: "Transfers", icon: ArrowLeftRight, roles: ["CITIZEN", "OFFICER", "ADMIN"] },
  { to: "/admin", label: "Users & Audit", icon: Users, roles: ["ADMIN"] },
];

export function AppShell({ title, roles, children }: { title: string; roles?: Role[]; children: ReactNode }) {
  const { user, ready, signOut } = useAuth();
  const nav = useNavigate();
  useEffect(() => { if (ready && !user) nav({ to: "/" }) }, [ready, user, nav]);
  if (!ready || !user) return <div className="min-h-screen bg-background" />;
  const allowed = !roles || roles.includes(user.role);
  return (
    <div className="flex min-h-screen bg-background">
      <aside className="hidden w-60 shrink-0 flex-col bg-sidebar text-sidebar-foreground md:flex">
        <div className="flex items-center gap-2 px-5 py-6"><Landmark className="h-6 w-6 text-sidebar-primary" /><span className="font-display text-xl font-bold">LandLedger</span></div>
        <nav className="flex-1 space-y-1 px-3">
          {NAV.filter((n) => n.roles.includes(user.role)).map((n) => (
            <Link key={n.to} to={n.to} className="flex items-center gap-3 rounded-md px-3 py-2 text-sm hover:bg-sidebar-accent" activeProps={{ className: "bg-sidebar-accent text-sidebar-primary" }}>
              <n.icon className="h-4 w-4" />{n.label}
            </Link>
          ))}
        </nav>
        <div className="border-t border-sidebar-border p-4 text-sm">
          <div className="font-medium">{user.name}</div>
          <div className="stamp text-sidebar-primary">{user.role}</div>
          <button onClick={() => { signOut(); nav({ to: "/" }) }} className="mt-3 flex items-center gap-2 opacity-80 hover:opacity-100"><LogOut className="h-4 w-4" />Log out</button>
        </div>
      </aside>
      <main className="flex-1 overflow-x-hidden">
        <header className="flex flex-wrap items-center justify-between gap-3 border-b bg-card px-6 py-4">
          <h1 className="text-2xl font-bold">{title}</h1>
          <div className="flex items-center gap-2">
            <div className="flex gap-2 md:hidden">
              {NAV.filter((n) => n.roles.includes(user.role)).map((n) => <Link key={n.to} to={n.to}><n.icon className="h-5 w-5" /></Link>)}
              <button onClick={() => { signOut(); nav({ to: "/" }) }}><LogOut className="h-5 w-5" /></button>
            </div>
          </div>
        </header>
        <div className="p-6">{allowed ? children : <p className="text-destructive">403 — your role cannot access this page.</p>}</div>
      </main>
    </div>
  );
}

export function StatusBadge({ s }: { s: string }) {
  const cls: Record<string, string> = {
    PENDING: "bg-warning/20 text-accent-foreground", UNDER_REVIEW: "bg-info/15 text-info",
    APPROVED: "bg-success/15 text-success", REJECTED: "bg-destructive/15 text-destructive",
  };
  return <span className={`stamp rounded px-2 py-1 ${cls[s] ?? "bg-muted"}`}>{s.replace("_", " ")}</span>;
}
