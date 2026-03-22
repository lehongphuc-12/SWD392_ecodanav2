# 🚲 EcoDana Project - SQL Server Edition

## 📝 Introduction
**EcoDana** is a premium web application built with **Spring Boot**, designed to seamlessly connect vehicle owners and renters. With a strong focus on eco-friendly transportation, the platform provides an efficient solution for vehicle management, real-time booking, and secure rentals.

---

## ✨ Key Features
- 👤 **User Management:** Streamlined registration, secure login, and comprehensive profile control.
- 🚗 **Vehicle Management:** Tools for owners to add and manage high-quality rental fleets.
- 📅 **Smart Booking:** Intuitive search and reservation system for all vehicle types.
- 💳 **Secure Payments:** Integrated with **VNPAY** and **PayOS** for reliable transaction handling.
- 🔔 **Instant Notifications:** Automated email updates for all critical booking events.
- 🖼️ **Media Hosting:** High-performance image management powered by **Cloudinary**.
- 🤖 **AI Integration:** Enhanced features leveraging **Cloudflare AI** capabilities.

---

## 🛠️ Technologies Used
| Category | Stack |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.5.x, Data JPA, Security (OAuth2) |
| **Frontend** | Thymeleaf, Vanilla CSS |
| **Database** | **Microsoft SQL Server 2022+** |
| **Services** | Cloudinary, Cloudflare AI, VNPAY, PayOS, OCR Space |

---

## 🚀 Setup Instructions

### 1️⃣ Database Setup (SQL Server)
1. **Create Database**: Initialize a new database in SQL Server named `ecodanav2`.
2. **Execute Script**: Run the consolidated script **[sql2 (1).sql](sql2%20(1).sql)**. 
   > This file contains the complete schema and essential initial data (Roles, Admin/Owner accounts, and sample Vehicles).

### 2️⃣ Environment Configuration
1. Rename `env.example` to `.env`.
2. Configure your environment variables:
   - `DB_URL`: `jdbc:sqlserver://YOUR_HOST;databaseName=ecodanav2;encrypt=true;trustServerCertificate=true`
   - `DB_USERNAME`: Database user (e.g., `sa`)
   - `DB_PASSWORD`: Database password
   - *Fill in additional keys for Cloudinary, Mail, and Payment Gateways as needed.*

### 3️⃣ Launch the Application
Start the development server from the project root:
```bash
./mvnw spring-boot:run
```
Once started, the application is available at: [http://localhost:8080](http://localhost:8080)

---

## 🔑 User Roles (Initial Data)

| Role | Email | Password |
| :--- | :--- | :--- |
| **Admin** | `admin@ecodana.com` | `password` |
| **Owner** | `owner@ecodana.com` | `password` |
| **Customer** | `customer@ecodana.com` | `password` |

> ℹ️ **Note:** Initial passwords in the database are hashed. Use the plain-text passwords above for your first login.

---

**Thank you for choosing EcoDana!**
