# E-commerce Backend System

Backend system for an e-commerce platform designed for modularity and scalability.

## Technologies
* **Java (Spring Boot)**
* **Spring Data JPA**
* **Spring Security (JWT)**
* **MySQL**
* **Docker**
* *(Additional)* Cloudinary for Image Storage

## Key Features & Architecture
* **Stateless Authentication:** Implemented authentication and authorization using Spring Security with JWT (stateless), including role-based access control (Admin/User).
* **IDOR Prevention:** Designed secure APIs by extracting user identity directly from JWT instead of trusting client input, preventing Insecure Direct Object Reference (IDOR) vulnerabilities.
* **Core Business Flows:** Built comprehensive flows for product browsing, add-to-cart, and checkout, including real-time stock management and order creation.
* **Feature-based Modularity:** Applied layered architecture with feature-based packaging to significantly improve modularity and maintainability.
* **Optimized Database:** Designed and optimized the relational database schema using MySQL and Spring Data JPA.
* **Data Consistency:** Implemented transaction management (`@Transactional`) and rigorous order state validation to ensure data consistency and prevent invalid operations.
* **Consistent Deployment:** Containerized the application using Docker and Docker Compose.

## Documentation
Detailed architectural decisions, data flows, and security implementations can be found in the `/docs` directory:
1. [Database Design & Logical Foreign Keys](./docs/01-database-design.md)
2. [Spring Security & JWT Flow](./docs/02-spring-security-flow.md)
3. [Cart Flow & Transactional Setup](./docs/03-cart-flow-and-transactional.md)
4. [Order Processing & Concurrency handling](./docs/04-order-flow-and-transactional.md)

## Getting Started

### Prerequisites
* Java 21
* Maven
* Docker & Docker Compose
* A free [Cloudinary](https://cloudinary.com/) account for API credentials.

### Installation & Run

**1. Clone the repository:**
```bash
git clone <your-github-repo-url>
cd <your-repo-folder>
```
**2. Environment Variables:**
```bash
cp .env.example .env
```
**3. Run the system:**
```bash
docker-compose up --build -d
```

The Spring Boot backend will be available at http://localhost:8080 and the MySQL database on port 3306.

