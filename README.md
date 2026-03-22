# EcoDana Project - SQL Server Edition

## Introduction

EcoDana is a web application built with Spring Boot, designed to connect vehicle owners and renters with a focus on eco-friendly transportation solutions. This project provides a platform for efficient vehicle management, booking, and rental.

## Key Features

- **User Management:** Registration, login, and user profile management.
- **Vehicle Management:** Allows vehicle owners to add, update, and manage rental vehicle information.
- **Booking:** Users can search, view details, and book vehicles.
- **Payment:** Secure payment integration to handle transactions (VNPAY, PayOS).
- **Notifications:** Sends email notifications for booking confirmations and other updates.
- **Image Upload:** Uses Cloudinary for managing vehicle images.
- **AI Integration:** Integration with Cloudflare AI for advanced features.

## Technologies Used

- **Backend:**
  - Java 21
  - Spring Boot 3.5.x
  - Spring Data JPA
  - Spring Security (OAuth2)
- **Frontend:**
  - Thymeleaf & Vanilla CSS
- **Database:**
  - **Microsoft SQL Server 2022+**
- **Third-Party Services:**
  - Cloudinary (Images)
  - Cloudflare AI (LLM)
  - VNPAY & PayOS (Payments)
  - OCR Space (Document Scanning)

## Setup Instructions

### 1. Database Setup (SQL Server)

1.  **Create Database**: Create a new database in SQL Server (e.g., `ecodanav2`).
2.  **Run Script**: Execute the consolidated SQL script **[sql2 (1).sql](<sql2%20(1).sql>)** in your SQL Server instance. This script contains both the schema (tables/constraints) and the initial data (roles, users, vehicles).

### 2. Environment Configuration

1.  Copy `env.example` to `.env`.
2.  Update the `.env` file with your specific credentials:
    - `DB_URL`: JDBC URL for SQL Server (e.g., `jdbc:sqlserver://localhost:1433;databaseName=ecodanav2;encrypt=true;trustServerCertificate=true`)
    - `DB_USERNAME`: Your SQL Server username (e.g., `sa`).
    - `DB_PASSWORD`: Your SQL Server password.
    - Provide other 3rd-party keys for Mail, Cloudinary, etc.

### 3. Run the Application

Execute the following command in the project root:

```bash
./mvnw spring-boot:run
```

The application will be accessible at `http://localhost:8080`.

## User Roles (Initial Data)

Role Email Password
Admin admin@ecodana.com password
Owner owner@ecodana.com password
Customer customer@ecodana.com password

_Note: Passwords in the database are hashed; the above are the plain-text passwords for the initial data._

---

Thank you for using EcoDana!
