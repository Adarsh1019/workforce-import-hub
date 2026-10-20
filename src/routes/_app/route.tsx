import { createFileRoute, Link, Outlet, redirect, useNavigate } from "@tanstack/react-router";
import { LayoutDashboard, Users, Building2, Upload, LogOut, Menu } from "lucide-react";
import { useState } from "react";
import { getSession, setSession } from "@/lib/api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/_app")({
  ssr: false,
  beforeLoad: () => {
    const session = getSession();
    if (!session) throw redirect({ to: "/login" });
    return { session };
  },
  component: AppLayout,
});

const nav = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/employees", label: "Employees", icon: Users },
  { to: "/departments", label: "Departments", icon: Building2 },
  { to: "/imports", label: "Bulk Import", icon: Upload },
] as const;

function AppLayout() {
  const { session } = Route.useRouteContext();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const sidebar = (
    <aside className="flex h-full w-64 flex-col bg-sidebar text-sidebar-foreground">
      <div className="flex items-center gap-2 px-6 py-6">
        <div className="grid size-9 place-items-center rounded-lg bg-sidebar-primary font-display text-lg font-bold text-sidebar-primary-foreground">
          W
        </div>
        <div>
          <div className="font-display text-base font-semibold text-sidebar-accent-foreground">Workforce</div>
          <div className="text-xs opacity-70">Management Suite</div>
        </div>
      </div>
      <nav className="flex-1 space-y-1 px-3">
        {nav.map((n) => (
          <Link
            key={n.to}
            to={n.to}
            onClick={() => setOpen(false)}
            className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors hover:bg-sidebar-accent"
            activeProps={{ className: "bg-sidebar-accent text-sidebar-primary" }}
          >
            <n.icon className="size-4" />
            {n.label}
          </Link>
        ))}
      </nav>
      <div className="border-t border-sidebar-border p-4">
        <div className="mb-3 text-sm">
          <div className="font-semibold text-sidebar-accent-foreground">{session.username}</div>
          <div className="text-xs uppercase tracking-wider text-sidebar-primary">{session.role}</div>
        </div>
        <button
          onClick={() => {
            setSession(null);
            navigate({ to: "/login" });
          }}
          className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm hover:bg-sidebar-accent"
        >
          <LogOut className="size-4" /> Sign out
        </button>
      </div>
    </aside>
  );

  return (
    <div className="flex min-h-screen">
      <div className="sticky top-0 hidden h-screen lg:block">{sidebar}</div>
      <div
        className={cn("fixed inset-0 z-40 bg-foreground/40 lg:hidden", open ? "block" : "hidden")}
        onClick={() => setOpen(false)}
      />
      <div
        className={cn(
          "fixed inset-y-0 left-0 z-50 transition-transform lg:hidden",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        {sidebar}
      </div>
      <div className="min-w-0 flex-1">
        <header className="flex items-center gap-3 border-b bg-card px-4 py-3 lg:hidden">
          <button onClick={() => setOpen(true)} aria-label="Open menu">
            <Menu className="size-5" />
          </button>
          <span className="font-display font-semibold">Workforce</span>
        </header>
        <main className="mx-auto max-w-7xl p-4 sm:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
