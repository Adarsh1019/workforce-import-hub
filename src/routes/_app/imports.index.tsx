import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useState } from "react";
import { FileUp, Download } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { api, isRunning } from "@/lib/api";
import { ErrorPanel, StatusBadge } from "@/components/app/StatusBadge";

export const Route = createFileRoute("/_app/imports/")({
  head: () => ({
    meta: [
      { title: "Bulk Import — Workforce" },
      { name: "description", content: "Upload employee CSV files and track import job history." },
      { property: "og:title", content: "Bulk Import — Workforce" },
      { property: "og:description", content: "Upload employee CSV files and track import job history." },
    ],
  }),
  component: Imports,
});

function Imports() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [drag, setDrag] = useState(false);
  const [page, setPage] = useState(0);

  const list = useQuery({
    queryKey: ["imports", page],
    queryFn: () => api.imports(page),
    refetchInterval: (q) => (q.state.data?.content.some((j) => isRunning(j.status)) ? 2000 : false),
  });
  const upload = useMutation({
    mutationFn: (f: File) => api.uploadImport(f),
    onSuccess: (job) => {
      toast.success("Import started");
      qc.invalidateQueries({ queryKey: ["imports"] });
      navigate({ to: "/imports/$id", params: { id: String(job.id) } });
    },
    onError: (e) => toast.error(e.message),
  });

  const pick = (f?: File) => {
    if (!f) return;
    if (!f.name.toLowerCase().endsWith(".csv")) {
      toast.error("Please choose a .csv file");
      return;
    }
    setFile(f);
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold">Bulk Import</h1>
          <p className="text-muted-foreground">Import thousands of employees from a CSV file.</p>
        </div>
        <a href="/samples/employees_sample.csv" download>
          <Button variant="outline"><Download className="size-4" /> Sample CSV (3,000 rows)</Button>
        </a>
      </div>

      <div
        onDragOver={(e) => { e.preventDefault(); setDrag(true); }}
        onDragLeave={() => setDrag(false)}
        onDrop={(e) => { e.preventDefault(); setDrag(false); pick(e.dataTransfer.files[0]); }}
        className={`rounded-xl border-2 border-dashed bg-card p-10 text-center transition-colors ${drag ? "border-primary bg-primary/5" : ""}`}
      >
        <FileUp className="mx-auto size-10 text-primary" />
        <p className="mt-3 font-medium">{file ? file.name : "Drag a CSV here, or browse"}</p>
        <p className="mt-1 text-xs text-muted-foreground">
          Columns: <code>name,email,department,role,salary,joiningDate</code> (date as YYYY-MM-DD)
        </p>
        <input ref={input} type="file" accept=".csv" className="hidden" onChange={(e) => pick(e.target.files?.[0])} />
        <div className="mt-5 flex justify-center gap-3">
          <Button variant="outline" onClick={() => input.current?.click()}>Browse</Button>
          <Button disabled={!file || upload.isPending} onClick={() => file && upload.mutate(file)}>
            {upload.isPending ? "Uploading…" : "Start import"}
          </Button>
        </div>
      </div>

      <section>
        <h2 className="mb-3 text-xl font-semibold">Import history</h2>
        {list.error && <ErrorPanel error={list.error} />}
        <div className="overflow-x-auto rounded-xl border bg-card shadow-card">
          <table className="w-full text-sm">
            <thead className="bg-muted/60 text-left text-xs uppercase tracking-wider text-muted-foreground">
              <tr>
                {["#", "File", "Status", "Progress", "Success", "Failed", "Started", "By"].map((h) => (
                  <th key={h} className="px-4 py-3 font-semibold">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y">
              {list.data?.content.map((j) => (
                <tr key={j.id} className="hover:bg-muted/40">
                  <td className="px-4 py-3">
                    <Link to="/imports/$id" params={{ id: String(j.id) }} className="font-medium text-primary hover:underline">#{j.id}</Link>
                  </td>
                  <td className="px-4 py-3">{j.fileName}</td>
                  <td className="px-4 py-3"><StatusBadge status={j.status} /></td>
                  <td className="w-40 px-4 py-3">
                    <Progress value={j.totalRecords ? (j.processedRecords / j.totalRecords) * 100 : 0} />
                    <div className="mt-1 text-xs text-muted-foreground">{j.processedRecords}/{j.totalRecords}</div>
                  </td>
                  <td className="px-4 py-3 text-success">{j.successCount}</td>
                  <td className="px-4 py-3 text-destructive">{j.failureCount}</td>
                  <td className="px-4 py-3 text-xs">{j.startedAt ? new Date(j.startedAt).toLocaleString() : "—"}</td>
                  <td className="px-4 py-3">{j.createdBy}</td>
                </tr>
              ))}
              {list.data?.content.length === 0 && (
                <tr><td colSpan={8} className="px-4 py-10 text-center text-muted-foreground">No imports yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
        {list.data && list.data.totalPages > 1 && (
          <div className="mt-3 flex justify-end gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
            <Button variant="outline" size="sm" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage(page + 1)}>Next</Button>
          </div>
        )}
      </section>
    </div>
  );
}
