import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Badge, Button, Card, CardDescription, CardTitle } from "@nexora/ui";
import type { AxiosError } from "axios";

import { useAuth } from "@/features/auth/auth-context";

function resolveErrorMessage(error: unknown) {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? "We could not complete that request.";
}

function AuthPageShell({
  badge,
  title,
  description,
  children,
  footer
}: {
  badge: string;
  title: string;
  description: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <div className="mx-auto grid min-h-[calc(100vh-7rem)] max-w-7xl gap-6 px-6 pb-12 pt-6 lg:grid-cols-[0.95fr_1.05fr] lg:px-8">
      <Card className="flex flex-col justify-between bg-slate-950 text-white">
        <div>
          {badge ? <Badge className="border-white/15 bg-white/10 text-white">{badge}</Badge> : null}
          <h1 className="mt-6 font-display text-5xl leading-tight text-white">
            Secure collaboration for every workspace in your portfolio.
          </h1>
          <p className="mt-6 max-w-xl text-base leading-8 text-slate-300">
            Nexora handles workspace-aware identity, JWT sessions, approval-ready permissions, and
            premium onboarding flows for agency-grade teams.
          </p>
        </div>
        <div className="grid gap-3 text-sm text-slate-300">
          <div className="rounded-3xl border border-white/10 bg-white/5 px-5 py-4">
            Role-aware access with workspace switching and invite acceptance.
          </div>
          <div className="rounded-3xl border border-white/10 bg-white/5 px-5 py-4">
            Refresh-token rotation, Redis-backed revocation, and gateway enforcement.
          </div>
        </div>
      </Card>

      <Card className="flex flex-col justify-center">
        {badge ? <Badge>{badge}</Badge> : null}
        <CardTitle className="mt-6 text-4xl">{title}</CardTitle>
        <CardDescription className="mt-3 max-w-xl text-base leading-7">
          {description}
        </CardDescription>
        <div className="mt-8">{children}</div>
        {footer ? <div className="mt-6 text-sm text-slate-600">{footer}</div> : null}
      </Card>
    </div>
  );
}

function Field({
  label,
  type = "text",
  value,
  onChange,
  placeholder,
  readOnly = false
}: {
  label: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  readOnly?: boolean;
}) {
  return (
    <label className="grid gap-2">
      <span className="text-sm font-semibold text-slate-700">{label}</span>
      <input
        className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-200"
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        readOnly={readOnly}
        type={type}
        value={value}
      />
    </label>
  );
}

function Notice({
  tone = "default",
  children
}: {
  tone?: "default" | "error" | "success";
  children: ReactNode;
}) {
  const toneClass =
    tone === "error"
      ? "border-rose-200 bg-rose-50 text-rose-700"
      : tone === "success"
        ? "border-emerald-200 bg-emerald-50 text-emerald-700"
        : "border-sky-200 bg-sky-50 text-sky-800";

  return <div className={`rounded-2xl border px-4 py-3 text-sm ${toneClass}`}>{children}</div>;
}

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login, pendingInviteToken } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const nextPath = searchParams.get("next") ?? "/app/dashboard";

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await login({ email, password });
      navigate(
        pendingInviteToken
          ? `/auth/invite/accept?token=${encodeURIComponent(pendingInviteToken)}`
          : nextPath,
        { replace: true }
      );
    } catch (submissionError) {
      setError(resolveErrorMessage(submissionError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthPageShell
      badge=""
      title="Sign in to your workspace"
      description="Use your Nexora credentials to access workspace memberships, protected product routes, and invitation workflows."
      footer={
        <>
          New to Nexora? <Link className="font-semibold text-sky-700" to="/auth/signup">Create an account</Link>
        </>
      }
    >
      <form className="grid gap-4" onSubmit={handleSubmit}>
        {pendingInviteToken ? (
          <Notice>An invite is waiting for you. Sign in first and we will route you back to accept it.</Notice>
        ) : null}
        {error ? <Notice tone="error">{error}</Notice> : null}
        <Field label="Email address" placeholder="you@example.com" onChange={setEmail} value={email} />
        <Field label="Password" placeholder="Enter your password" onChange={setPassword} type="password" value={password} />
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
          <Link className="text-sm font-semibold text-sky-700" to="/auth/password/request">
            Forgot your password?
          </Link>
          <Button disabled={submitting} type="submit">
            {submitting ? "Signing in..." : "Sign In"}
          </Button>
        </div>
      </form>
    </AuthPageShell>
  );
}

export function SignUpPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { signUp } = useAuth();
  const [displayName, setDisplayName] = useState("");
  const [workspaceName, setWorkspaceName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const response = await signUp({
        displayName,
        workspaceName,
        email,
        password,
        locale: "en",
        timezone: "Asia/Calcutta"
      });

      const nextPath = searchParams.get("next") ?? "/app/dashboard";
      const verificationToken = response.developmentTokens.emailVerificationToken;
      navigate(
        verificationToken
          ? `/auth/verify?token=${encodeURIComponent(verificationToken)}&next=${encodeURIComponent(nextPath)}`
          : nextPath,
        { replace: true }
      );
    } catch (submissionError) {
      setError(resolveErrorMessage(submissionError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthPageShell
      badge=""
      title="Create your Nexora organization"
      description="Provision your first workspace, owner role, and refresh-rotated session in one flow."
      footer={
        <>
          Already registered? <Link className="font-semibold text-sky-700" to="/auth/login">Sign in</Link>
        </>
      }
    >
      <form className="grid gap-4" onSubmit={handleSubmit}>
        {error ? <Notice tone="error">{error}</Notice> : null}
        <Field label="Display name" placeholder="Your name" onChange={setDisplayName} value={displayName} />
        <Field label="Workspace name" placeholder="Your workspace" onChange={setWorkspaceName} value={workspaceName} />
        <Field label="Email address" placeholder="you@example.com" onChange={setEmail} value={email} />
        <Field label="Password" placeholder="Create a password" onChange={setPassword} type="password" value={password} />
        <div className="flex justify-end pt-2">
          <Button disabled={submitting} type="submit">
            {submitting ? "Creating workspace..." : "Create Account"}
          </Button>
        </div>
      </form>
    </AuthPageShell>
  );
}

export function RequestPasswordResetPage() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const { requestPasswordReset } = useAuth();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const response = await requestPasswordReset(email);
      if (response.challengeToken) {
        setMessage(`Development reset token: ${response.challengeToken}`);
      } else {
        setMessage("If an account exists for that email, a password reset challenge has been created.");
      }
    } catch (submissionError) {
      setError(resolveErrorMessage(submissionError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthPageShell
      badge="Password Recovery"
      title="Reset your password"
      description="Request a one-time password reset challenge for your Nexora account."
      footer={<Link className="font-semibold text-sky-700" to="/auth/login">Back to sign in</Link>}
    >
      <form className="grid gap-4" onSubmit={handleSubmit}>
        {message ? <Notice tone="success">{message}</Notice> : null}
        {error ? <Notice tone="error">{error}</Notice> : null}
        <Field label="Email address" placeholder="you@example.com" onChange={setEmail} value={email} />
        <div className="flex justify-end">
          <Button disabled={submitting} type="submit">
            {submitting ? "Requesting..." : "Request Reset"}
          </Button>
        </div>
      </form>
    </AuthPageShell>
  );
}

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { resetPassword } = useAuth();
  const [newPassword, setNewPassword] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const token = searchParams.get("token") ?? "";

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await resetPassword(token, newPassword);
      setMessage("Your password has been updated. Redirecting you to sign in.");
      window.setTimeout(() => navigate("/auth/login", { replace: true }), 900);
    } catch (submissionError) {
      setError(resolveErrorMessage(submissionError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthPageShell
      badge="Password Recovery"
      title="Choose a new password"
      description="Complete the password reset challenge and restore access to your workspace."
      footer={<Link className="font-semibold text-sky-700" to="/auth/login">Return to sign in</Link>}
    >
      <form className="grid gap-4" onSubmit={handleSubmit}>
        {message ? <Notice tone="success">{message}</Notice> : null}
        {error ? <Notice tone="error">{error}</Notice> : null}
        <Field label="Reset token" onChange={() => {}} readOnly value={token} />
        <Field label="New password" placeholder="Enter new password" onChange={setNewPassword} type="password" value={newPassword} />
        <div className="flex justify-end">
          <Button disabled={submitting || token.length === 0} type="submit">
            {submitting ? "Updating..." : "Set New Password"}
          </Button>
        </div>
      </form>
    </AuthPageShell>
  );
}

export function VerifyEmailPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { confirmEmailVerification } = useAuth();
  const [state, setState] = useState<"idle" | "working" | "done" | "error">("idle");
  const [message, setMessage] = useState<string | null>(null);

  const token = searchParams.get("token") ?? "";
  const nextPath = searchParams.get("next") ?? "/app/dashboard";

  useEffect(() => {
    if (!token || state !== "idle") {
      return;
    }

    setState("working");
    void confirmEmailVerification(token)
      .then(() => {
        setState("done");
        setMessage("Your email address is now verified. Redirecting you to the product workspace.");
        window.setTimeout(() => navigate(nextPath, { replace: true }), 900);
      })
      .catch((error) => {
        setState("error");
        setMessage(resolveErrorMessage(error));
      });
  }, [confirmEmailVerification, navigate, nextPath, state, token]);

  return (
    <AuthPageShell
      badge="Email Verification"
      title="Confirm your identity"
      description="This challenge activates your Nexora account and unlocks verified workspace ownership."
      footer={<Link className="font-semibold text-sky-700" to="/auth/login">Return to sign in</Link>}
    >
      <div className="grid gap-4">
        {message ? (
          <Notice tone={state === "done" ? "success" : state === "error" ? "error" : "default"}>
            {message}
          </Notice>
        ) : null}
        {!token ? (
          <Notice tone="error">The verification token is missing from this link.</Notice>
        ) : null}
        <div className="rounded-3xl border border-slate-200 bg-slate-50 px-5 py-4 text-sm text-slate-600">
          Token: <span className="font-semibold text-slate-950">{token || "Missing token"}</span>
        </div>
      </div>
    </AuthPageShell>
  );
}

export function AcceptInvitePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { acceptInvite, isAuthenticated, rememberInviteToken } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const inviteToken = searchParams.get("token") ?? "";

  useEffect(() => {
    if (!inviteToken) {
      return;
    }
    rememberInviteToken(inviteToken);
  }, [inviteToken, rememberInviteToken]);

  async function handleAccept() {
    if (!inviteToken) {
      setError("The invite token is missing from this link.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await acceptInvite(inviteToken);
      navigate("/app/dashboard", { replace: true });
    } catch (submissionError) {
      setError(resolveErrorMessage(submissionError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthPageShell
      badge="Workspace Invite"
      title="Join an invited workspace"
      description="Accept a workspace membership and rotate your active session into the invited organization."
      footer={
        !isAuthenticated ? (
          <>
            Sign in first: <Link className="font-semibold text-sky-700" to={`/auth/login?next=${encodeURIComponent(`/auth/invite/accept?token=${inviteToken}`)}`}>Continue to sign in</Link>
          </>
        ) : null
      }
    >
      <div className="grid gap-4">
        {error ? <Notice tone="error">{error}</Notice> : null}
        {!inviteToken ? <Notice tone="error">The invite token is missing from this link.</Notice> : null}
        {!isAuthenticated ? (
          <Notice>
            Your invite has been saved for this browser session. Sign in, then return here to accept it.
          </Notice>
        ) : (
          <Notice tone="success">
            You are authenticated. Accepting this invite will switch your current workspace context.
          </Notice>
        )}
        <div className="flex justify-end">
          <Button disabled={!isAuthenticated || !inviteToken || submitting} onClick={handleAccept} type="button">
            {submitting ? "Accepting..." : "Accept Invite"}
          </Button>
        </div>
      </div>
    </AuthPageShell>
  );
}
