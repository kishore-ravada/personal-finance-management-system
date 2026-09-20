# Personal Finance Management System

A secure, full-stack Personal Finance Management Web Application built with **Java 21**, **Spring Boot 3**, **Spring Security**, **JWT**, **MySQL**, and **React** containerized with **Docker Compose**.

---

## 🌟 Overview

The Personal Finance Management System allows registered users to manage their financial accounts, track income and expenses, enforce category budgets, track progress toward savings goals, view interactive financial analytics, and download official CSV and PDF financial reports.

---

## ✨ Features

- **Authentication & User Profile**: Secure registration with BCrypt password hashing, JWT authentication, profile updates, and password changes.
- **Account Management**: Support for BANK, CASH, WALLET, CREDIT_CARD, and SAVINGS account types with atomic balance tracking.
- **Category Management**: User-isolated income and expense categories (auto-seeded with defaults upon registration).
- **Transaction Ledger**: Record income and expenses with automatic account balance recalculation, multi-criteria filtering, search, pagination, and sorting.
- **Category Budgets**: Monthly category spending limits with usage percentage tracking, remaining amount calculation, and over-budget warnings.
- **Savings Goals**: Track target vs current savings with percentage completion indicators and target dates.
- **Interactive Dashboard**: Financial summary cards (Total Balance, Monthly Income, Expenses, Savings), Doughnut & Bar charts (Chart.js), budget alerts, and recent transaction log.
- **Financial Reports & Exports**: Multi-month summaries, category spending breakdowns, and one-click PDF (OpenPDF) and CSV (Apache Commons CSV) report exports.
- **Data Isolation**: Strict user-level security context filtering on all endpoints to prevent data leakage between users.

---

## 🛠️ Technology Stack

### Backend
- **Java 21**
- **Spring Boot 3.2.5**
- **Spring Data JPA & Hibernate**
- **Spring Security & JWT** (`jjwt` 0.12.5)
- **Bean Validation**
- **MySQL 8.0**
- **OpenPDF** (PDF Report Exporter)
- **Apache Commons CSV** (CSV Exporter)
- **JUnit 5 & Mockito** (Unit & Integration Testing)
- **SpringDoc OpenAPI / Swagger UI**

### Frontend
- **React 18 & Vite**
- **React Router v6**
- **Axios** (with JWT request/response interceptors)
- **Chart.js & React-Chartjs-2**
- **Lucide React** (Modern Icons)
- **Nginx** (Production Web Server & API Reverse Proxy)

### Orchestration
- **Docker & Docker Compose**

---

## 📐 Architecture & Conceptual Model

```
Browser / React Frontend (Port 3000 / 80)
            │
            │ REST APIs / JSON (JWT Authorization Header)
            ▼
   Nginx / Spring Boot Backend (Port 8080)
      ├── Controllers (@RestController)
      ├── Services (@Service, @Transactional)
      └── Repositories (Spring Data JPA)
            │
            ▼
     MySQL Docker Container (Port 3306)
            │
            ▼
     Persistent Docker Volume (mysql_data)
```

### Entity Relationship Model

```
User (1) ───< Accounts (N)
User (1) ───< Categories (N)
User (1) ───< Transactions (N) ───> Account & Category
User (1) ───< Budgets (N) ────────> Category
User (1) ───< Savings Goals (N)
```

---

## 🚀 Quick Start & Installation

### Option A: Running with Docker Compose (Recommended)

1. Clone or navigate to the project directory:
   ```bash
   cd personal-finance-management
   ```

2. Copy the template environment configuration:
   ```bash
   cp .env.example .env
   ```

3. Build and launch all services (`mysql`, `backend`, `frontend`):
   ```bash
   docker compose up --build
   ```

4. Access the web application:
   - **Frontend UI**: [http://localhost:3000](http://localhost:3000) or [http://localhost](http://localhost)
   - **Backend API**: [http://localhost:8080/api](http://localhost:8080/api)
   - **Swagger OpenAPI Docs**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### Option B: Local Development Setup

#### 1. Start Docker MySQL
```bash
docker compose up -d mysql
```

#### 2. Start Spring Boot Backend
```bash
cd backend
mvn spring-boot:run
```

#### 3. Start React Frontend
```bash
cd frontend
npm install
npm run dev
```

---

## 🧪 Testing

### Backend Tests
Execute unit and integration tests (using H2 in-memory MySQL mode):
```bash
cd backend
mvn test "-Dspring.profiles.active=test"
```

### Frontend Tests
Run Vitest non-interactive test suite:
```bash
cd frontend
npm run test:run
```

---

## 🔐 API Documentation Overview

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register new user & seed default categories | No |
| `POST` | `/api/auth/login` | Authenticate user & return JWT token | No |
| `GET` | `/api/users/me` | Get current user profile | Yes |
| `PUT` | `/api/users/me` | Update current user profile | Yes |
| `PUT` | `/api/users/me/password` | Change user password | Yes |
| `GET` | `/api/accounts` | List user financial accounts | Yes |
| `POST` | `/api/accounts` | Create financial account | Yes |
| `GET` | `/api/categories` | List user categories (optional `?type=EXPENSE`) | Yes |
| `POST` | `/api/categories` | Create category | Yes |
| `GET` | `/api/transactions` | Paginated/filtered transaction ledger | Yes |
| `POST` | `/api/transactions` | Record income/expense transaction | Yes |
| `PUT` | `/api/transactions/{id}` | Update transaction & recalculate balance | Yes |
| `DELETE` | `/api/transactions/{id}` | Delete transaction & reverse balance | Yes |
| `GET` | `/api/budgets` | List monthly category budgets | Yes |
| `POST` | `/api/budgets` | Set category budget limit | Yes |
| `GET` | `/api/savings-goals` | List user savings goals | Yes |
| `POST` | `/api/savings-goals` | Create savings goal | Yes |
| `GET` | `/api/dashboard` | Aggregated financial summary & chart metrics | Yes |
| `GET` | `/api/reports/monthly` | Multi-month financial summary | Yes |
| `GET` | `/api/reports/export/csv` | Download transactions as CSV file | Yes |
| `GET` | `/api/reports/export/pdf` | Download official financial report PDF | Yes |

---

## 🌐 Production Cloud Deployment Guide

1. **Environment Variables**: Configure secrets in environment variables (`JWT_SECRET`, `MYSQL_PASSWORD`, etc.) on your server.
2. **Reverse Proxy & SSL**: Configure Nginx / Traefik with Let's Encrypt SSL certificate for HTTPS enforcement.
3. **Data Persistence**: Ensure `mysql_data` Docker volume is mapped to persistent block storage.

---

## 📄 License

This application is provided under the MIT License.
