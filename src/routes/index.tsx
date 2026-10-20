import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  ssr: false,
  beforeLoad: () => {
    throw redirect({ to: "/dashboard" });
  },
  head: () => ({
    meta: [
      { title: "Workforce — Employee & Bulk Import Admin" },
      { name: "description", content: "Manage employees, departments and bulk CSV imports." },
      { property: "og:title", content: "Workforce — Employee & Bulk Import Admin" },
      { property: "og:description", content: "Manage employees, departments and bulk CSV imports." },
    ],
  }),
});
