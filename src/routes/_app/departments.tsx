import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, getSession, type Department } from "@/lib/api";
import { ErrorPanel } from "@/components/app/StatusBadge";

export const Route = createFileRoute("/_app/departments")({
  head: () => ({
    meta: [
      { title: "Departments — Workforce" },
      { name: "description", content: "Create and manage company departments." },
      { property: "og:title", content: "Departments — Workforce" },
      { property: "og:description", content: "Create and manage company departments." },
    ],
  }),
  component: Departments,
});

function Departments() {
  const qc = useQueryClient();
  const isAdmin = getSession()?.role === "ADMIN";
  const { data, error } = useQuery({ queryKey: ["departments"], queryFn: api.departments });
  const [editing, setEditing] = useState<Department | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ["departments"] });
    qc.invalidateQueries({ queryKey: ["summary"] });
  };
  const save = useMutation({
    mutationFn: () =>
      editing ? api.updateDepartment(editing.id, { name, description }) : api.createDepartment({ name, description }),
    onSuccess: () => {
      toast.success("Department saved");
      setEditing(null);
      setName("");
      setDescription("");
      refresh();
    },
    onError: (e) => toast.error(e.message),
  });
  const del = useMutation({
    mutationFn: api.deleteDepartment,
    onSuccess: () => { toast.success("Department deleted"); refresh(); },
    onError: (e) => toast.error(e.message),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-semibold">Departments</h1>
        <p className="text-muted-foreground">{isAdmin ? "Admins can add, rename and remove departments." : "Read-only for HR."}</p>
      </div>
      {error && <ErrorPanel error={error} />}
      {isAdmin && (
        <form
          className="flex flex-wrap gap-3 rounded-xl border bg-card p-4 shadow-card"
          onSubmit={(e) => { e.preventDefault(); save.mutate(); }}
        >
          <Input className="min-w-48 flex-1" placeholder="Department name" value={name} onChange={(e) => setName(e.target.value)} required />
          <Input className="min-w-48 flex-[2]" placeholder="Description (optional)" value={description} onChange={(e) => setDescription(e.target.value)} />
          <Button type="submit" disabled={save.isPending}>{editing ? "Update" : "Add department"}</Button>
          {editing && <Button type="button" variant="outline" onClick={() => { setEditing(null); setName(""); setDescription(""); }}>Cancel</Button>}
        </form>
      )}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.map((d) => (
          <div key={d.id} className="rounded-xl border bg-card p-5 shadow-card">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-semibold">{d.name}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{d.description || "—"}</p>
              </div>
              {isAdmin && (
                <div className="flex">
                  <Button variant="ghost" size="icon" aria-label="Edit" onClick={() => { setEditing(d); setName(d.name); setDescription(d.description ?? ""); }}>
                    <Pencil className="size-4" />
                  </Button>
                  <Button variant="ghost" size="icon" aria-label="Delete" onClick={() => confirm(`Delete ${d.name}?`) && del.mutate(d.id)}>
                    <Trash2 className="size-4 text-destructive" />
                  </Button>
                </div>
              )}
            </div>
            <div className="mt-4 font-display text-2xl font-semibold">{d.employeeCount}<span className="ml-1 text-sm font-normal text-muted-foreground">employees</span></div>
          </div>
        ))}
      </div>
    </div>
  );
}
