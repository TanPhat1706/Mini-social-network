# MiniSocialNetwork

![React](https://img.shields.io/badge/React-19.2-61DAFB?logo=react)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![License](https://img.shields.io/badge/License-Not%20specified-red)

MiniSocialNetwork is a modern full-stack social networking platform designed for campus/community engagement. It combines social features such as authentication, profiles, posts, comments, friendships, real-time chat, notifications, shop/gacha cosmetics, mini-games, and admin moderation in a single integrated experience.

## 1. Project Title & Description

MiniSocialNetwork is a capstone-style social platform that brings together social interaction and gamification. The project is built with a React frontend, a Spring Boot backend, and SQL Server for persistence, with Docker support for fast local deployment.

## 2. Core Features

- User authentication and authorization with JWT
- User profile management and avatar/cosmetic customization
- Social feed with posts, comments, reactions, and media upload
- Friendship system with requests, approvals, and suggestions
- Real-time chat and notifications via WebSocket
- Shop, gacha, and cosmetic inventory system
- Mini-games such as Snake and Tic-Tac-Toe
- Admin dashboard for managing users, posts, and blacklist keywords

## 3. Tech Stack

### Frontend
- React 19 + TypeScript
- Vite
- Material UI
- Axios, React Router, SockJS/STOMP

### Backend
- Java 17
- Spring Boot 3.2
- Spring Security, Spring Data JPA, Validation
- JWT Authentication
- WebSocket for real-time messaging
- Swagger / OpenAPI

### Database
- Microsoft SQL Server
- Dockerized database initialization via SQL scripts

### DevOps
- Docker Compose
- Jenkins pipeline support
- Nginx for frontend serving

## 4. Folder Structure

```text
mini-social-network/
├── backend/           # Spring Boot API service
│   ├── src/main/java/  # Application code
│   ├── src/main/resources/  # Config files and properties
│   └── pom.xml        # Maven dependencies and build config
├── frontend/          # React + Vite client app
│   ├── src/           # Components, pages, hooks, API wrappers
│   └── package.json   # Frontend dependencies and scripts
├── db/                # SQL scripts and DB setup files
├── docker-compose.yml # Container orchestration for full stack
└── README.md          # Project documentation
```

## 5. Getting Started

### Prerequisites

- Docker Desktop
- Node.js 20+ and npm
- JDK 17
- Maven (or use the provided Maven wrapper)

### Environment Variables

The backend uses environment variables for database, JWT, mail, file upload, and AWS/S3 configuration. A typical setup looks like:

```env
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=MiniSocialDB;encrypt=true;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=minisocialnetwork
SPRING_DATASOURCE_PASSWORD=2026
JWT_SECRET=your-secret-key
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
CORS_ALLOWED_ORIGINS=http://localhost:5173
SERVER_PORT=8080
```

For the frontend, set the API base URL before running locally:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_API_URL=http://localhost:8080/api/auth
VITE_API_TIMEOUT=10000
```

### Installation & Running

#### Option 1: Run with Docker Compose

```bash
docker compose up --build
```

Available services:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- SQL Server: localhost:1433

#### Option 2: Run locally

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## 6. API Documentation

API documentation is available through Swagger UI once the backend is running:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 7. Contributors & License

- Contributors: [Le Hong Phat](https://github.com/pht1412), [Vo Tan Phat](https://github.com/TanPhat1706)
- License: No license.