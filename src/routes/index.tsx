import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { ArrowRight, Building2, Check, Sparkles, Upload, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { getSession } from "@/lib/api";

export const Route = createFileRoute("/")({
  ssr: false,
  component: StartPage,
  head: () => ({
    meta: [
      { title: "Employee CRM — Workforce management" },
      { name: "description", content: "A clear, simple workspace for managing employees, departments and imports." },
      { property: "og:title", content: "Employee CRM — Workforce management" },
      { property: "og:description", content: "Manage your people and workforce data in one place." },
    ],
  }),
});

function StartPage() {
  const navigate = useNavigate();

  function start() {
    navigate({ to: getSession() ? "/dashboard" : "/login" });
  }

  return (
    <main className="min-h-screen overflow-hidden bg-background">
      <header className="mx-auto flex max-w-7xl items-center justify-between px-6 py-5 sm:px-10">
        <a href="/" className="flex items-center gap-3" aria-label="Employee CRM home">
          <span className="grid size-10 place-items-center rounded-xl bg-primary text-primary-foreground">
            <Users className="size-5" />
          </span>
          <span>
            <span className="block font-display text-base font-semibold">Employee CRM</span>
            <span className="block text-xs text-muted-foreground">Workforce management</span>
          </span>
        </a>
        <Button onClick={start} variant="outline" className="gap-2 rounded-full px-5">
          Start <ArrowRight className="size-4" />
        </Button>
      </header>

      <section className="mx-auto grid max-w-7xl items-center gap-14 px-6 pb-20 pt-10 sm:px-10 lg:grid-cols-[1fr_0.9fr] lg:gap-20 lg:pb-28 lg:pt-16">
        <div className="max-w-2xl">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border bg-card px-3 py-1.5 text-sm text-muted-foreground shadow-sm">
            <Sparkles className="size-4 text-primary" />
            A better way to manage your people
          </div>
          <h1 className="font-display text-5xl font-semibold leading-[1.08] tracking-tight sm:text-6xl">
            Your people,
            <br />
            <span className="text-primary">all in one place.</span>
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-8 text-muted-foreground">
            Keep employee records organised, stay on top of departments, and bring bulk imports into one simple workspace.
          </p>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:items-center">
            <Button onClick={start} size="lg" className="gap-2 rounded-full px-7">
              Start using Employee CRM <ArrowRight className="size-4" />
            </Button>
            <span className="text-sm text-muted-foreground">Employee records · Departments · CSV imports</span>
          </div>
          <div className="mt-10 flex flex-wrap gap-x-6 gap-y-3 text-sm text-muted-foreground">
            {["One organised workspace", "Easy bulk importing", "Clear import results"].map((item) => (
              <span key={item} className="inline-flex items-center gap-2">
                <Check className="size-4 text-primary" /> {item}
              </span>
            ))}
          </div>
        </div>

        <div className="relative mx-auto w-full max-w-xl">
          <div className="absolute -inset-6 rounded-[2rem] bg-primary/5 blur-2xl" />
          <div className="relative rounded-2xl border bg-card p-5 shadow-[0_24px_80px_-32px_rgba(20,55,60,0.35)] sm:p-7">
            <div className="flex items-center justify-between border-b pb-5">
              <div>
                <p className="text-sm text-muted-foreground">Your workspace</p>
                <h2 className="mt-1 font-display text-xl font-semibold">People overview</h2>
              </div>
              <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">CRM</span>
            </div>
            <div className="grid grid-cols-2 gap-3 py-5">
              {[
                { label: "Employees", icon: Users },
                { label: "Departments", icon: Building2 },
              ].map(({ label, icon: Icon }) => (
                <div key={label} className="rounded-xl border bg-background p-4">
                  <Icon className="size-4 text-primary" />
                  <p className="mt-5 font-display text-lg font-semibold">{label}</p>
                  <p className="mt-1 text-xs text-muted-foreground">Manage your workforce</p>
                </div>
              ))}
            </div>
            <div className="flex items-center gap-4 rounded-xl bg-sidebar p-4 text-sidebar-foreground">
              <span className="grid size-11 shrink-0 place-items-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                <Upload className="size-5" />
              </span>
              <div>
                <p className="font-medium">Bulk employee imports</p>
                <p className="mt-1 text-sm opacity-70">Upload a CSV and review every result.</p>
              </div>
              <ArrowRight className="ml-auto size-4 opacity-70" />
            </div>
          </div>
          <div className="absolute -bottom-5 -left-5 hidden items-center gap-3 rounded-xl border bg-card px-4 py-3 shadow-card sm:flex">
            <span className="grid size-9 place-items-center rounded-full bg-primary/10 text-primary"><Check className="size-4" /></span>
            <span className="text-sm font-medium">Everything in one place</span>
          </div>
        </div>
      </section>
    </main>
  );
}
