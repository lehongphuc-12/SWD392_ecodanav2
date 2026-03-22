# EcoDana Project

## Introduction

EcoDana is a web application built with Spring Boot, designed to connect vehicle owners and renters with a focus on eco-friendly transportation solutions. This project provides a platform for efficient vehicle management, booking, and rental.

## Key Features

*   **User Management:** Registration, login, and user profile management.
*   **Vehicle Management:** Allows vehicle owners to add, update, and manage rental vehicle information.
*   **Booking:** Users can search, view details, and book vehicles.
*   **Payment:** Secure payment integration to handle transactions.
*   **Notifications:** Sends email notifications for booking confirmations and other updates.
*   **Image Upload:** Uses Cloudinary for managing vehicle images.

## Technologies Used

*   **Backend:**
    *   Java 21
    *   Spring Boot 3
    *   Spring Web
    *   Spring Data JPA
    *   Spring Security
    *   Spring Mail
*   **Frontend:**
    *   Thymeleaf
*   **Database:**
    *   MySQL
*   **Image Storage:**
    *   Cloudinary
*   **Build Tool:**
    *   Maven

## Project Structure

The project follows the standard Maven structure:

*   `src/main/java`: Java source code
*   `src/main/resources`: Resource files, including `application.properties` and Thymeleaf templates
*   `pom.xml`: Maven project configuration file

## Participants

This project was developed by a dedicated team of developers.

## User Roles

| Role     | Email                 | Password |
|----------|-----------------------|----------|
| Admin    | admin@ecodana.com     | password |
| Owner    | owner@ecodana.com     | password |
| Customer | customer@ecodana.com  | password |

## Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/datlee27/EcoDanav2
    ```
2.  **Configure the database:**
    *   Create a MySQL database.
    *   Update the `spring.datasource` properties in `application.properties`.
3.  **Run the application:**
    ```bash
    ./mvnw spring-boot:run
    ```

Thank you for considering our project!
