import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from "react";
import type {
  AuthSessionResponse,
  ChallengeCompletionResponse,
  ChallengeDispatchResponse,
  LoginRequest,
  SignUpRequest
} from "@nexora/contracts";

import { apiClient, authorizationHeader } from "@/lib/api";

type AuthStatus = "loading" | "authenticated" | "anonymous";

type StoredTokens = {
  accessToken: string;
  refreshToken: string;
};

type AuthContextValue = {
  status: AuthStatus;
  session: AuthSessionResponse | null;
  isAuthenticated: boolean;
  login: (request: LoginRequest) => Promise<AuthSessionResponse>;
  signUp: (request: SignUpRequest) => Promise<AuthSessionResponse>;
  refreshSession: (workspaceId?: string) => Promise<AuthSessionResponse>;
  logout: () => Promise<void>;
  switchWorkspace: (workspaceId: string) => Promise<AuthSessionResponse>;
  acceptInvite: (inviteToken: string) => Promise<AuthSessionResponse>;
  requestEmailVerification: (email: string) => Promise<ChallengeDispatchResponse>;
  confirmEmailVerification: (token: string) => Promise<ChallengeCompletionResponse>;
  requestPasswordReset: (email: string) => Promise<ChallengeDispatchResponse>;
  resetPassword: (token: string, newPassword: string) => Promise<ChallengeCompletionResponse>;
  rememberInviteToken: (token: string | null) => void;
  pendingInviteToken: string | null;
};

const TOKENS_STORAGE_KEY = "nexora.auth.tokens";
const INVITE_STORAGE_KEY = "nexora.auth.pendingInvite";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [session, setSession] = useState<AuthSessionResponse | null>(null);
  const [pendingInviteToken, setPendingInviteToken] = useState<string | null>(() =>
    readStorage(INVITE_STORAGE_KEY)
  );

  useEffect(() => {
    const storedTokens = readTokens();
    if (!storedTokens?.refreshToken) {
      setStatus("anonymous");
      return;
    }

    void refreshWithToken(storedTokens.refreshToken)
      .then((response) => {
        setSession(response);
        setStatus("authenticated");
      })
      .catch(() => {
        clearSession();
      });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      session,
      isAuthenticated: status === "authenticated" && session?.session != null,
      async login(request) {
        const response = await apiClient
          .post<AuthSessionResponse>("/api/v1/auth/login", request)
          .then(({ data }) => data);

        applySession(response, setSession, setStatus);
        return response;
      },
      async signUp(request) {
        const response = await apiClient
          .post<AuthSessionResponse>("/api/v1/auth/signup", request)
          .then(({ data }) => data);

        applySession(response, setSession, setStatus);
        return response;
      },
      async refreshSession(workspaceId) {
        const storedTokens = requireTokens();
        const response = await refreshWithToken(storedTokens.refreshToken, workspaceId);
        applySession(response, setSession, setStatus);
        return response;
      },
      async logout() {
        const storedTokens = readTokens();
        try {
          if (storedTokens?.accessToken) {
            await apiClient.post(
              "/api/v1/auth/logout",
              {},
              {
                headers: authorizationHeader(storedTokens.accessToken)
              }
            );
          }
        } finally {
          clearSession(setSession, setStatus);
        }
      },
      async switchWorkspace(workspaceId) {
        const storedTokens = requireTokens();
        const response = await apiClient
          .post<AuthSessionResponse>(
            "/api/v1/auth/workspaces/switch",
            {
              workspaceId,
              refreshToken: storedTokens.refreshToken
            },
            {
              headers: authorizationHeader(storedTokens.accessToken)
            }
          )
          .then(({ data }) => data);

        applySession(response, setSession, setStatus);
        return response;
      },
      async acceptInvite(inviteToken) {
        const storedTokens = requireTokens();
        const response = await apiClient
          .post<AuthSessionResponse>(
            "/api/v1/auth/invites/accept",
            {
              inviteToken,
              refreshToken: storedTokens.refreshToken
            },
            {
              headers: authorizationHeader(storedTokens.accessToken)
            }
          )
          .then(({ data }) => data);

        applySession(response, setSession, setStatus);
        writeStorage(INVITE_STORAGE_KEY, null);
        setPendingInviteToken(null);
        return response;
      },
      requestEmailVerification(email) {
        return apiClient
          .post<ChallengeDispatchResponse>("/api/v1/auth/verification/request", { email })
          .then(({ data }) => data);
      },
      confirmEmailVerification(token) {
        return apiClient
          .post<ChallengeCompletionResponse>("/api/v1/auth/verification/confirm", { token })
          .then(({ data }) => data);
      },
      requestPasswordReset(email) {
        return apiClient
          .post<ChallengeDispatchResponse>("/api/v1/auth/password/request-reset", { email })
          .then(({ data }) => data);
      },
      resetPassword(token, newPassword) {
        return apiClient
          .post<ChallengeCompletionResponse>("/api/v1/auth/password/reset", { token, newPassword })
          .then(({ data }) => data);
      },
      rememberInviteToken(token) {
        writeStorage(INVITE_STORAGE_KEY, token);
        setPendingInviteToken(token);
      },
      pendingInviteToken
    }),
    [pendingInviteToken, session, status]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return value;
}

function applySession(
  nextSession: AuthSessionResponse,
  setSession: (session: AuthSessionResponse | null) => void,
  setStatus: (status: AuthStatus) => void
) {
  if (nextSession.session) {
    writeStorage(TOKENS_STORAGE_KEY, JSON.stringify(nextSession.session));
    setSession(nextSession);
    setStatus("authenticated");
    return;
  }

  setSession(nextSession);
  setStatus("anonymous");
}

async function refreshWithToken(refreshToken: string, workspaceId?: string) {
  return apiClient
    .post<AuthSessionResponse>("/api/v1/auth/refresh", {
      refreshToken,
      workspaceId
    })
    .then(({ data }) => data);
}

function requireTokens() {
  const storedTokens = readTokens();
  if (!storedTokens) {
    throw new Error("The current browser session is not authenticated");
  }
  return storedTokens;
}

function clearSession(
  setSession?: (session: AuthSessionResponse | null) => void,
  setStatus?: (status: AuthStatus) => void
) {
  writeStorage(TOKENS_STORAGE_KEY, null);
  setSession?.(null);
  setStatus?.("anonymous");
}

function readTokens(): StoredTokens | null {
  const rawValue = readStorage(TOKENS_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    const parsed = JSON.parse(rawValue) as StoredTokens;
    if (!parsed.accessToken || !parsed.refreshToken) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function readStorage(key: string) {
  if (typeof window === "undefined") {
    return null;
  }
  return window.sessionStorage.getItem(key);
}

function writeStorage(key: string, value: string | null) {
  if (typeof window === "undefined") {
    return;
  }

  if (value == null) {
    window.sessionStorage.removeItem(key);
    return;
  }

  window.sessionStorage.setItem(key, value);
}

// ── Route Guards ────────────────────────────────────────────────────────────
// Merged from route-guards.tsx — auth-aware navigation guards

import { Navigate, Outlet, useLocation } from "react-router-dom";

export function RequireAuth() {
  const { isAuthenticated, status } = useAuth();
  const location = useLocation();

  if (status === "loading") {
    return (
      <div className="mx-auto flex min-h-[60vh] max-w-3xl items-center justify-center px-6 text-center">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.28em] text-sky-700">
            Securing your workspace
          </p>
          <h1 className="mt-4 font-display text-4xl text-slate-950">Loading your Nexora session</h1>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    const next = `${location.pathname}${location.search}`;
    return <Navigate replace to={`/auth/login?next=${encodeURIComponent(next)}`} />;
  }

  return <Outlet />;
}

export function RedirectAuthenticated() {
  const { isAuthenticated, status, pendingInviteToken } = useAuth();

  if (status === "loading") {
    return null;
  }

  if (isAuthenticated) {
    const destination = pendingInviteToken
      ? `/auth/invite/accept?token=${encodeURIComponent(pendingInviteToken)}`
      : "/app/dashboard";
    return <Navigate replace to={destination} />;
  }

  return <Outlet />;
}
