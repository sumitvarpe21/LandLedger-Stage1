import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { Landmark, MapPinned, ShieldCheck, History } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { BackendHealth } from "@/components/BackendHealth";
import { useAuth } from "@/lib/auth";
import * as api from "@/services/api";
import { run } from "@/lib/run";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "LandLedger — Sign in to the Land Registry" },
      {
        name: "description",
        content: "Register land, track applications and explore parcels on an interactive GIS map.",
      },
      { property: "og:title", content: "LandLedger — Land Registry" },
      {
        property: "og:description",
        content: "Land registration, GIS parcel map and ownership history verification.",
      },
    ],
  }),
  component: Index,
});

function Index() {
  const { user, ready, authError, signIn } = useAuth();
  const nav = useNavigate();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [f, setF] = useState({ name: "", email: "", password: "", phone: "" });
  useEffect(() => {
    if (ready && user) nav({ to: "/dashboard" });
  }, [ready, user, nav]);
  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    void run(
      async () => {
        const authenticatedUser =
          mode === "login"
            ? await api.login(f.email, f.password)
            : await api.register(f.name, f.email, f.password, f.phone);
        signIn(authenticatedUser);
        nav({ to: "/dashboard" });
      },
      mode === "login" ? "Signed in" : "Account created",
    );
  };
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <section className="ledger-grid relative flex flex-col justify-between bg-sidebar p-10 text-sidebar-foreground">
        <div className="flex items-center gap-2">
          <Landmark className="h-7 w-7 text-sidebar-primary" />
          <span className="font-display text-2xl font-bold">LandLedger</span>
        </div>
        <div className="max-w-lg">
          <p className="stamp text-sidebar-primary">Land registry · Stage 1</p>
          <h1 className="mt-3 text-5xl font-bold leading-tight">
            Every parcel, every owner, on the record.
          </h1>
          <ul className="mt-8 space-y-3 text-sm opacity-90">
            <li className="flex gap-3">
              <ShieldCheck className="h-5 w-5 text-sidebar-primary" />
              Citizen, Officer and Admin roles with enforced permissions
            </li>
            <li className="flex gap-3">
              <MapPinned className="h-5 w-5 text-sidebar-primary" />
              Interactive GIS map of registered parcels
            </li>
            <li className="flex gap-3">
              <History className="h-5 w-5 text-sidebar-primary" />
              Ownership history ready for blockchain verification
            </li>
          </ul>
        </div>
        <p className="stamp opacity-60">Prototype UI · Blockchain not connected</p>
      </section>
      <section className="flex items-center justify-center p-8">
        <form onSubmit={submit} className="w-full max-w-sm space-y-4">
          <h2 className="text-3xl font-bold">
            {mode === "login" ? "Sign in" : "Create citizen account"}
          </h2>
          {mode === "register" && (
            <>
              <div>
                <Label>Full name</Label>
                <Input
                  required
                  value={f.name}
                  onChange={(e) => setF({ ...f, name: e.target.value })}
                />
              </div>
              <div>
                <Label>Phone</Label>
                <Input
                  required
                  value={f.phone}
                  onChange={(e) => setF({ ...f, phone: e.target.value })}
                />
              </div>
            </>
          )}
          <div>
            <Label>Email</Label>
            <Input
              type="email"
              required
              value={f.email}
              onChange={(e) => setF({ ...f, email: e.target.value })}
            />
          </div>
          <div>
            <Label>Password</Label>
            <Input
              type="password"
              required
              minLength={mode === "register" ? 8 : undefined}
              maxLength={72}
              value={f.password}
              onChange={(e) => setF({ ...f, password: e.target.value })}
            />
          </div>
          <Button type="submit" className="w-full">
            {mode === "login" ? "Sign in" : "Register"}
          </Button>
          <button
            type="button"
            className="w-full text-sm text-muted-foreground underline"
            onClick={() => setMode(mode === "login" ? "register" : "login")}
          >
            {mode === "login" ? "New citizen? Register" : "Have an account? Sign in"}
          </button>
          {authError && (
            <p role="alert" className="text-sm text-destructive">
              Could not restore your sign-in: {authError}
            </p>
          )}
          {mode === "login" && (
            <p className="rounded-md border bg-card p-3 text-sm text-muted-foreground">
              Sign in with an account registered in LandLedger. New accounts are citizens; officer
              and administrator accounts are assigned securely.
            </p>
          )}
          <BackendHealth />
        </form>
      </section>
    </div>
  );
}
