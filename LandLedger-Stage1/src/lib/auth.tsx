import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import * as api from "@/services/api";
import { BackendHttpError } from "@/services/backend";

interface Ctx {
  user: api.PublicUser | null;
  ready: boolean;
  authError: string | null;
  signIn: (user: api.PublicUser) => void;
  updateUser: (user: api.PublicUser) => void;
  signOut: () => void;
}

const AuthCtx = createContext<Ctx>({
  user: null,
  ready: false,
  authError: null,
  signIn: () => {},
  updateUser: () => {},
  signOut: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<api.PublicUser | null>(null);
  const [ready, setReady] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    if (!api.hasSession()) {
      setReady(true);
      return;
    }
    void api.getCurrentUser().then(
      (currentUser) => {
        if (!active) return;
        setUser(currentUser);
        setAuthError(null);
        setReady(true);
      },
      (error: unknown) => {
        if (!active) return;
        if (error instanceof BackendHttpError && error.status === 401) {
          api.signOut();
        }
        setAuthError(error instanceof Error ? error.message : "Could not verify your session");
        setReady(true);
      },
    );
    return () => {
      active = false;
    };
  }, []);

  const signIn = (nextUser: api.PublicUser) => {
    setUser(nextUser);
    setAuthError(null);
  };
  const updateUser = (nextUser: api.PublicUser) => setUser(nextUser);
  const signOut = () => {
    api.signOut();
    setUser(null);
    setAuthError(null);
  };

  return (
    <AuthCtx.Provider value={{ user, ready, authError, signIn, updateUser, signOut }}>
      {children}
    </AuthCtx.Provider>
  );
}

export const useAuth = () => useContext(AuthCtx);
