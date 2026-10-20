import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Users, Building2, Upload, Wallet } from "lucide-react";
import { api, money } from "@/lib/api";
import { ErrorPanel, StatusBadge } from "@/components/app/StatusBadge";

export const Route = createFileRoute("/_app/dashboard")({
  head: () => ({
    meta: [
      { title: "Dashboard — Workforce" },
      { name: "description", content: "Workforce headcount, departments and import activity at a glance." },
      { property: "og:title", content: "Dashboard — Workforce" },
      { property: "og:description", content: "Headcount, departments and import activity at a glance." },
    ],
  }),
  component: Dashboard,
});

function Dashboard() {
  const { data, error, isLoading } = useQuery({ queryKey: ["summary"], queryFn: api.summary });
  const max = Math.max(1, ...(data?.employeesByDepartment.map((d) => d.count) ?? [1]));
  const cards = [
    { label: "Employees", value: data?.totalEmployees.toLocaleString(), icon: Users },
    { label: "Departments", value: data?.totalDepartments, icon: Building2 },
    { label: "Import jobs", value: data?.totalImports, icon: Upload },
    { label: "Avg. salary", value: data ? money(data.averageSalary) : undefined, icon: Wallet },
  ];
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-semibold">Dashboard</h1>
        <p className="text-muted-foreground">Overview of your workforce.</p>
      </div>
      {error && <ErrorPanel error={error} />}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((c) => (
          <div key={c.label} className="rounded-xl border bg-card p-5 shadow-card">
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              {c.label}
              <c.icon className="size-4 text-primary" />
            </div>
            <div className="mt-3 font-display text-3xl font-semibold">{isLoading ? "…" : (c.value ?? "—")}</div>
          </div>
        ))}
      </div>
      <div className="grid gap-6 lg:grid-cols-5">
        <section className="rounded-xl border bg-card p-6 shadow-card lg:col-span-2">
          <h2 className="mb-4 font-semibold">Headcount by department</h2>
          <div className="space-y-3">
            {data?.employeesByDepartment.map((d) => (
              <div key={d.name}>
                <div className="mb-1 flex justify-between text-sm">
                  <span>{d.name}</span>
                  <span className="text-muted-foreground">{d.count}</span>
                </div>
                <div className="h-2 rounded-full bg-muted">
                  <div className="h-2 rounded-full bg-primary" style={{ width: `${(d.count / max) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
        </section>
        <section className="rounded-xl border bg-card p-6 shadow-card lg:col-span-3">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-semibold">Recent imports</h2>
            <Link to="/imports" className="text-sm text-primary hover:underline">View all</Link>
          </div>
          <div className="divide-y">
            {data?.recentImports.length === 0 && <p className="text-sm text-muted-foreground">No imports yet.</p>}
            {data?.recentImports.map((j) => (
              <Link
                key={j.id}
                to="/imports/$id"
                params={{ id: String(j.id) }}
                className="flex items-center justify-between py-3 text-sm hover:bg-muted/50"
              >
                <div>
                  <div className="font-medium">{j.fileName}</div>
                  <div className="text-xs text-muted-foreground">
                    {j.successCount} ok · {j.failureCount} rejected
                  </div>
                </div>
                <StatusBadge status={j.status} />
              </Link>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
