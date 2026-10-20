import { cn } from "@/lib/utils";
import type { JobStatus } from "@/lib/api";

const styles: Record<string, string> = {
  COMPLETED: "bg-success/15 text-success",
  FAILED: "bg-destructive/15 text-destructive",
  STARTED: "bg-warning/25 text-warning-foreground",
  STARTING: "bg-warning/25 text-warning-foreground",
  STOPPED: "bg-muted text-muted-foreground",
};

export function StatusBadge({ status }: { status: JobStatus }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold",
        styles[status] ?? "bg-muted text-muted-foreground",
      )}
    >
      <span className="size-1.5 rounded-full bg-current" />
      {status}
    </span>
  );
}

export function ErrorPanel({ error }: { error: unknown }) {
  return (
    <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
      {error instanceof Error ? error.message : "Something went wrong"}
    </div>
  );
}
