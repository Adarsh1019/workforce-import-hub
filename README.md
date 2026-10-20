# Workforce Import Hub

Build a simple, full-stack *Workforce Management & Bulk Import System* using React, TypeScript, Tailwind CSS, Spring Boot (Java), Spring Batch, and PostgreSQL.

*Core features:*

- Employee CRUD with name, email, department, role, salary, and joining date.

- Department management, search, filtering, sorting, and pagination.

- Simple login with admin and HR roles.

- CSV upload to import thousands of employees using Spring Batch with chunk processing, validation, and duplicate prevention.

- Import job dashboard showing status, total records, processed records, successes, and failures.

- Download rejected CSV rows with error reasons and support safe job restarts.

*UI:* Create a clean, modern, responsive admin dashboard with sidebar navigation, summary cards, employee table, upload page, and import history.

*Backend:* Use layered architecture (controller, service, repository), REST APIs, DTO validation, global exception handling, Spring Security, JPA/Hibernate, and PostgreSQL. Store batch job metadata and import errors in the database.

*Production basics:* Add unit/integration tests, Docker Compose, database migrations, Swagger/OpenAPI documentation, and a README.

Keep the project simple, modular, and beginner-friendly. Use realistic sample data. Build the frontend and backend as separate applications with clear API integration. Do not use mock APIs for features that should work. If Lovable cannot implement or run the Java/Spring Boot backend directly, build the complete frontend and API integration structure, then provide the backend implementation requirements separately.

This project was built with [Lovable](https://lovable.dev).

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/0a6fbe03-8823-41ba-9520-5d79c5e5901c).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
