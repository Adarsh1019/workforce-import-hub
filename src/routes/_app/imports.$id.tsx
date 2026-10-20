import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Download, RotateCcw } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { api, isRunning } from "@/lib/api";
import { ErrorPanel, StatusBadge } from "@/components/app/StatusBadge";

export const Route = createFileRoute("/_app/imports/$id")({
  head: () => ({
    meta: [
      { title: "Import job — Workforce" },
      { name: "description", content: "Live progress and results of a bulk employee import." },
      { property: "og:title", content: "Import job — Workforce" },
      { property: "og:description", content: "Live progress and results of a bulk employee import." },
    ],
  }),
  component: ImportDetail,
});

function ImportDetail() {
  const id = Number(Route.useParams().id);
  const qc = useQueryClient();
  const { data: j, error } = useQuery({
    queryKey: ["import", id],
    queryFn: () => api.importJob(id),
    refetchInterval: (q) => (q.state.data && isRunning(q.state.data.status) ? 1500 : false),
  });
  const restart = useMutation({
    mutationFn: () => api.restartImport(id),
    onSuccess: () => { toast.success("Job restarted"); qc.invalidateQueries({ queryKey: ["import", id] }); },
    onError: (e) => toast.error(e.message),
  });
  const pct = j?.totalRecords ? Math.round((j.processedRecords / j.totalRecords) * 100) : 0;
  const canRestart = j && (j.status === "FAILED" || j.status === "STOPPED");

  return (
    <div className="space-y-6">
      <Link to="/imports" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Import history
      </Link>
      {error && <ErrorPanel error={error} />}
      {j && (
        <>
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="text-3xl font-semibold">Import #{j.id}</h1>
              <p className="text-muted-foreground">{j.fileName} · uploaded by {j.createdBy}</p>
            </div>
            <div className="flex gap-2">
              {canRestart && (
                <Button variant="outline" onClick={() => restart.mutate()} disabled={restart.isPending}>
                  <RotateCcw className="size-4" /> Restart job
                </Button>
              )}
              {j.failureCount > 0 && (
                <Button onClick={() => api.downloadErrors(id).catch((e) => toast.error(e.message))}>
                  <Download className="size-4" /> Rejected rows
                </Button>
              )}
            </div>
          </div>
          <div className="rounded-xl border bg-card p-6 shadow-card">
            <div className="mb-3 flex items-center justify-between">
              <StatusBadge status={j.status} />
              <span className="font-display text-2xl font-semibold">{pct}%</span>
            </div>
            <Progress value={pct} />
            {j.errorMessage && <p className="mt-3 text-sm text-destructive">{j.errorMessage}</p>}
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[
              ["Total records", j.totalRecords, ""],
              ["Processed", j.processedRecords, ""],
              ["Imported", j.successCount, "text-success"],
              ["Rejected", j.failureCount, "text-destructive"],
            ].map(([label, v, cls]) => (
              <div key={label as string} className="rounded-xl border bg-card p-5 shadow-card">
                <div className="text-sm text-muted-foreground">{label}</div>
                <div className={`mt-2 font-display text-3xl font-semibold ${cls}`}>{Number(v).toLocaleString()}</div>
              </div>
            ))}
          </div>
          <div className="text-sm text-muted-foreground">
            Started {j.startedAt ? new Date(j.startedAt).toLocaleString() : "—"} · Finished{" "}
            {j.finishedAt ? new Date(j.finishedAt).toLocaleString() : "—"}
          </div>
        </>
      )}
    </div>
  );
}
