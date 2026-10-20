// Typed client for the Spring Boot REST API (see backend/README.md for the contract).
export const API_URL = (import.meta.env["VITE_API_URL"] as string | undefined) ?? "http://localhost:8081/api";

const TOKEN_KEY = "wf_auth";

export type Role = "ADMIN" | "HR";
export interface AuthSession {
  token: string;
  username: string;
  role: Role;
}

export function getSession(): AuthSession | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(TOKEN_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  } catch {
    return null;
  }
}
export function setSession(s: AuthSession | null) {
  if (s) localStorage.setItem(TOKEN_KEY, JSON.stringify(s));
  else localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fieldErrors?: Record<string, string>,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = getSession();
  const headers = new Headers(init.headers);
  if (session) headers.set("Authorization", `Bearer ${session.token}`);
  if (init.body && !(init.body instanceof FormData)) headers.set("Content-Type", "application/json");
  let res: Response;
  try {
    res = await fetch(`${API_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError(0, `Cannot reach the API at ${API_URL}. Is the backend running?`);
  }
  if (res.status === 401 && session) {
    setSession(null);
    window.location.href = "/login";
  }
  if (!res.ok) {
    let body: { message?: string; fieldErrors?: Record<string, string> } = {};
    try {
      body = await res.json();
    } catch {
      /* empty */
    }
    throw new ApiError(res.status, body.message ?? `Request failed (${res.status})`, body.fieldErrors);
  }
  if (res.status === 204) return undefined as T;
  const ct = res.headers.get("content-type") ?? "";
  return (ct.includes("json") ? res.json() : res.blob()) as Promise<T>;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
export interface Department {
  id: number;
  name: string;
  description?: string | null;
  employeeCount: number;
}
export interface Employee {
  id: number;
  name: string;
  email: string;
  departmentId: number;
  departmentName: string;
  role: string;
  salary: number;
  joiningDate: string;
}
export type EmployeeInput = Omit<Employee, "id" | "departmentName">;
export type JobStatus = "STARTING" | "STARTED" | "COMPLETED" | "FAILED" | "STOPPED" | "ABANDONED" | "UNKNOWN";
export interface ImportJob {
  id: number;
  fileName: string;
  status: JobStatus;
  totalRecords: number;
  processedRecords: number;
  successCount: number;
  failureCount: number;
  createdBy: string;
  startedAt: string | null;
  finishedAt: string | null;
  errorMessage?: string | null;
}
export interface Summary {
  totalEmployees: number;
  totalDepartments: number;
  totalImports: number;
  averageSalary: number;
  employeesByDepartment: { name: string; count: number }[];
  recentImports: ImportJob[];
}

export interface EmployeeQuery {
  page: number;
  size: number;
  sort: string;
  search?: string;
  departmentId?: number | undefined;
}

const qs = (o: Record<string, string | number | undefined>) =>
  new URLSearchParams(
    Object.entries(o).filter(([, v]) => v !== undefined && v !== "") as [string, string][],
  ).toString();

export const api = {
  login: (username: string, password: string) =>
    request<AuthSession>("/auth/login", { method: "POST", body: JSON.stringify({ username, password }) }),
  summary: () => request<Summary>("/dashboard/summary"),
  employees: (q: EmployeeQuery) => request<Page<Employee>>(`/employees?${qs({ ...q })}`),
  createEmployee: (e: EmployeeInput) => request<Employee>("/employees", { method: "POST", body: JSON.stringify(e) }),
  updateEmployee: (id: number, e: EmployeeInput) =>
    request<Employee>(`/employees/${id}`, { method: "PUT", body: JSON.stringify(e) }),
  deleteEmployee: (id: number) => request<void>(`/employees/${id}`, { method: "DELETE" }),
  departments: () => request<Department[]>("/departments"),
  createDepartment: (d: { name: string; description?: string }) =>
    request<Department>("/departments", { method: "POST", body: JSON.stringify(d) }),
  updateDepartment: (id: number, d: { name: string; description?: string }) =>
    request<Department>(`/departments/${id}`, { method: "PUT", body: JSON.stringify(d) }),
  deleteDepartment: (id: number) => request<void>(`/departments/${id}`, { method: "DELETE" }),
  imports: (page: number, size = 10) => request<Page<ImportJob>>(`/imports?${qs({ page, size, sort: "id,desc" })}`),
  importJob: (id: number) => request<ImportJob>(`/imports/${id}`),
  uploadImport: (file: File) => {
    const fd = new FormData();
    fd.append("file", file);
    return request<ImportJob>("/imports", { method: "POST", body: fd });
  },
  restartImport: (id: number) => request<ImportJob>(`/imports/${id}/restart`, { method: "POST" }),
  downloadErrors: async (id: number) => {
    const blob = await request<Blob>(`/imports/${id}/errors.csv`);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `import-${id}-rejected.csv`;
    a.click();
    URL.revokeObjectURL(url);
  },
};

export const isRunning = (s: JobStatus) => s === "STARTING" || s === "STARTED";
export const money = (n: number) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(n);
