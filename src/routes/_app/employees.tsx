import { createFileRoute } from "@tanstack/react-router";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { ArrowUpDown, Pencil, Plus, Search, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { api, ApiError, getSession, money, type Employee, type EmployeeInput } from "@/lib/api";
import { ErrorPanel } from "@/components/app/StatusBadge";

export const Route = createFileRoute("/_app/employees")({
  head: () => ({
    meta: [
      { title: "Employees — Workforce" },
      { name: "description", content: "Search, filter, sort and manage employee records." },
      { property: "og:title", content: "Employees — Workforce" },
      { property: "og:description", content: "Search, filter, sort and manage employee records." },
    ],
  }),
  component: Employees,
});

const cols: { key: keyof Employee; label: string; sortKey: string }[] = [
  { key: "name", label: "Name", sortKey: "name" },
  { key: "departmentName", label: "Department", sortKey: "department.name" },
  { key: "role", label: "Role", sortKey: "role" },
  { key: "salary", label: "Salary", sortKey: "salary" },
  { key: "joiningDate", label: "Joined", sortKey: "joiningDate" },
];

function Employees() {
  const qc = useQueryClient();
  const isAdmin = getSession()?.role === "ADMIN";
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sort, setSort] = useState("name,asc");
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [departmentId, setDepartmentId] = useState<number | undefined>();
  const [editing, setEditing] = useState<Employee | "new" | null>(null);

  useEffect(() => {
    const t = setTimeout(() => {
      setSearch(searchInput);
      setPage(0);
    }, 300);
    return () => clearTimeout(t);
  }, [searchInput]);

  const depts = useQuery({ queryKey: ["departments"], queryFn: api.departments });
  const q = { page, size, sort, search, departmentId };
  const list = useQuery({ queryKey: ["employees", q], queryFn: () => api.employees(q), placeholderData: keepPreviousData });

  const del = useMutation({
    mutationFn: api.deleteEmployee,
    onSuccess: () => {
      toast.success("Employee deleted");
      qc.invalidateQueries({ queryKey: ["employees"] });
    },
    onError: (e) => toast.error(e.message),
  });

  const toggleSort = (k: string) => {
    const [cur, dir] = sort.split(",");
    setSort(`${k},${cur === k && dir === "asc" ? "desc" : "asc"}`);
  };

  const data = list.data;
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold">Employees</h1>
          <p className="text-muted-foreground">{data?.totalElements.toLocaleString() ?? "—"} records</p>
        </div>
        <Button onClick={() => setEditing("new")}>
          <Plus className="size-4" /> Add employee
        </Button>
      </div>

      <div className="flex flex-wrap gap-3">
        <div className="relative min-w-60 flex-1">
          <Search className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            className="pl-9"
            placeholder="Search name, email or role…"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
        </div>
        <select
          className="h-9 rounded-md border bg-card px-3 text-sm"
          value={departmentId ?? ""}
          onChange={(e) => {
            setDepartmentId(e.target.value ? Number(e.target.value) : undefined);
            setPage(0);
          }}
        >
          <option value="">All departments</option>
          {depts.data?.map((d) => (
            <option key={d.id} value={d.id}>{d.name}</option>
          ))}
        </select>
      </div>

      {list.error && <ErrorPanel error={list.error} />}

      <div className="overflow-x-auto rounded-xl border bg-card shadow-card">
        <table className="w-full text-sm">
          <thead className="bg-muted/60 text-left text-xs uppercase tracking-wider text-muted-foreground">
            <tr>
              {cols.map((c) => (
                <th key={c.key} className="px-4 py-3 font-semibold">
                  <button className="inline-flex items-center gap-1" onClick={() => toggleSort(c.sortKey)}>
                    {c.label} <ArrowUpDown className="size-3" />
                  </button>
                </th>
              ))}
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y">
            {data?.content.map((e) => (
              <tr key={e.id} className="hover:bg-muted/40">
                <td className="px-4 py-3">
                  <div className="font-medium">{e.name}</div>
                  <div className="text-xs text-muted-foreground">{e.email}</div>
                </td>
                <td className="px-4 py-3">{e.departmentName}</td>
                <td className="px-4 py-3">{e.role}</td>
                <td className="px-4 py-3 tabular-nums">{money(e.salary)}</td>
                <td className="px-4 py-3 tabular-nums">{e.joiningDate}</td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  <Button variant="ghost" size="icon" onClick={() => setEditing(e)} aria-label="Edit">
                    <Pencil className="size-4" />
                  </Button>
                  {isAdmin && (
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label="Delete"
                      onClick={() => confirm(`Delete ${e.name}?`) && del.mutate(e.id)}
                    >
                      <Trash2 className="size-4 text-destructive" />
                    </Button>
                  )}
                </td>
              </tr>
            ))}
            {data?.content.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-muted-foreground">No employees found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {data && (
        <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
          <div className="flex items-center gap-2">
            Rows
            <select className="h-8 rounded-md border bg-card px-2" value={size} onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}>
              {[10, 25, 50].map((s) => <option key={s}>{s}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-muted-foreground">Page {data.number + 1} of {Math.max(1, data.totalPages)}</span>
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <Button variant="outline" size="sm" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      )}

      {editing && (
        <EmployeeDialog
          employee={editing === "new" ? null : editing}
          departments={depts.data ?? []}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}

function EmployeeDialog({
  employee,
  departments,
  onClose,
}: {
  employee: Employee | null;
  departments: { id: number; name: string }[];
  onClose: () => void;
}) {
  const qc = useQueryClient();
  const [form, setForm] = useState<EmployeeInput>({
    name: employee?.name ?? "",
    email: employee?.email ?? "",
    departmentId: employee?.departmentId ?? departments[0]?.id ?? 0,
    role: employee?.role ?? "",
    salary: employee?.salary ?? 50000,
    joiningDate: employee?.joiningDate ?? new Date().toISOString().slice(0, 10),
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const save = useMutation({
    mutationFn: () => (employee ? api.updateEmployee(employee.id, form) : api.createEmployee(form)),
    onSuccess: () => {
      toast.success(employee ? "Employee updated" : "Employee created");
      qc.invalidateQueries({ queryKey: ["employees"] });
      qc.invalidateQueries({ queryKey: ["summary"] });
      onClose();
    },
    onError: (e) => {
      if (e instanceof ApiError && e.fieldErrors) setErrors(e.fieldErrors);
      toast.error(e.message);
    },
  });
  const field = (k: keyof EmployeeInput, label: string, type = "text") => (
    <div className="space-y-1.5">
      <Label htmlFor={k}>{label}</Label>
      <Input
        id={k}
        type={type}
        value={String(form[k])}
        onChange={(e) => setForm({ ...form, [k]: type === "number" ? Number(e.target.value) : e.target.value })}
        required
      />
      {errors[k] && <p className="text-xs text-destructive">{errors[k]}</p>}
    </div>
  );
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{employee ? "Edit employee" : "New employee"}</DialogTitle>
        </DialogHeader>
        <form
          id="emp-form"
          className="grid gap-4 sm:grid-cols-2"
          onSubmit={(e) => {
            e.preventDefault();
            save.mutate();
          }}
        >
          {field("name", "Full name")}
          {field("email", "Email", "email")}
          <div className="space-y-1.5">
            <Label htmlFor="dept">Department</Label>
            <select
              id="dept"
              className="h-9 w-full rounded-md border bg-card px-3 text-sm"
              value={form.departmentId}
              onChange={(e) => setForm({ ...form, departmentId: Number(e.target.value) })}
            >
              {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
          </div>
          {field("role", "Role")}
          {field("salary", "Salary (USD)", "number")}
          {field("joiningDate", "Joining date", "date")}
        </form>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button type="submit" form="emp-form" disabled={save.isPending}>{save.isPending ? "Saving…" : "Save"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
