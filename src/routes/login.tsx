import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, API_URL, setSession } from "@/lib/api";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Sign in — Workforce" },
      { name: "description", content: "Sign in to the Workforce admin dashboard as Admin or HR." },
      { property: "og:title", content: "Sign in — Workforce" },
      { property: "og:description", content: "Sign in to the Workforce admin dashboard." },
    ],
  }),
  component: Login,
});

function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const s = await api.login(username, password);
      setSession(s);
      navigate({ to: "/dashboard" });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="hidden flex-col justify-between bg-sidebar p-12 text-sidebar-foreground lg:flex">
        <div className="font-display text-xl font-semibold text-sidebar-accent-foreground">Workforce</div>
        <div>
          <h1 className="font-display text-5xl font-semibold leading-tight text-sidebar-accent-foreground">
            Your people,
            <br />
            <span className="text-sidebar-primary">organised at scale.</span>
          </h1>
          <p className="mt-4 max-w-md opacity-80">
            Manage employees and departments, and import thousands of records in one upload — with every
            rejected row explained.
          </p>
        </div>
        <div className="text-xs opacity-60">API: {API_URL}</div>
      </div>
      <div className="flex items-center justify-center p-6">
        <form onSubmit={submit} className="w-full max-w-sm space-y-5">
          <div>
            <h2 className="text-2xl font-semibold">Sign in</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Demo accounts: <b>admin / admin123</b> or <b>hr / hr123</b>
            </p>
          </div>
          <div className="space-y-2">
            <Label htmlFor="u">Username</Label>
            <Input id="u" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </div>
          <div className="space-y-2">
            <Label htmlFor="p">Password</Label>
            <Input id="p" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          {error && <p className="text-sm text-destructive">{error}</p>}
          <Button type="submit" className="w-full" disabled={busy}>
            {busy ? "Signing in…" : "Sign in"}
          </Button>
        </form>
      </div>
    </div>
  );
}
