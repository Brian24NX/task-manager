# Task Manager

A full-stack daily task manager built with modern Vue and Spring Boot.

## Stack

- **Frontend:** Vue 3 + Vite
- **Backend:** Spring Boot 4 + Spring Data JPA
- **Database:** H2 (in-memory)

## Features

- Create, edit, and delete tasks
- Track status (`TODO`, `IN_PROGRESS`, `DONE`)
- Mark tasks as done quickly
- Filter tasks by due date
- REST API + SPA frontend

## Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

If `mvnw` is unavailable in your environment:

```bash
mvn spring-boot:run
```

Backend will run at `http://localhost:8080`.

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend will run at `http://localhost:5173`.

## API endpoints

- `GET /api/tasks`
- `GET /api/tasks?dueDate=YYYY-MM-DD`
- `POST /api/tasks`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}/status?status=TODO|IN_PROGRESS|DONE`
- `DELETE /api/tasks/{id}`
