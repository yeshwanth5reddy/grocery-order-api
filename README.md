# Grocery Order Management API

A production-ready RESTful API for managing grocery orders, built with Java 21 and Spring Boot 3.

## Tech Stack

- **Java 21** + **Spring Boot 3.3.5**
- **PostgreSQL** — Relational data (Products, Customers, Orders)
- **Spring Security + JWT** — Stateless authentication with role-based access
- **Spring Data JPA** — Repository pattern with derived queries
- **Bean Validation** — Request validation with custom error responses
- **Swagger/OpenAPI 3** — Interactive API documentation with JWT support
- **JUnit 5 + Mockito** — Unit tests (services) + MockMvc integration tests (controllers)
- **Docker** — Multi-stage Dockerfile + docker-compose
- **GitHub Actions** — CI pipeline with automated build & test
- **Lombok** — Reduces boilerplate code

## Architecture

```
                          ┌──────────────────┐
                          │  JWT Auth Filter  │
                          └────────┬─────────┘
                                   │
┌─────────────┐     ┌──────────────┐     ┌──────────────┐     ┌────────────┐
│  Controller  │────>│   Service    │────>│  Repository  │────>│ PostgreSQL │
│  (REST API)  │<────│ (Business    │<────│  (JPA)       │<────│            │
│              │     │   Logic)     │     │              │     │            │
└─────────────┘     └──────────────┘     └──────────────┘     └────────────┘
       │                   │
       │                   ├── Stock validation & deduction
       │                   ├── Order state machine
       │                   └── Price snapshot at purchase
       │
  DTO Request/Response separation
```

## Authentication

The API uses **JWT (JSON Web Tokens)** for stateless authentication.

### Register
```json
POST /api/auth/register
{
    "username": "yeshwanth",
    "password": "password123"
}
```
Returns a JWT token + user details.

### Login
```json
POST /api/auth/login
{
    "username": "yeshwanth",
    "password": "password123"
}
```
Returns a JWT token valid for 24 hours.

### Using the Token
Add the token to the `Authorization` header:
```
Authorization: Bearer <your-jwt-token>
```

### Access Control

| Endpoint | Access |
|----------|--------|
| `POST /api/auth/**` | Public |
| `GET /api/products/**`, `GET /api/customers/**` | Public |
| `POST`, `PUT`, `PATCH` on all resources | Authenticated (USER or ADMIN) |
| `DELETE` on all resources | ADMIN only |
| Swagger UI, Actuator | Public |

## API Endpoints

### Authentication `/api/auth`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Products `/api/products`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/products` | Add a new product |
| GET | `/api/products` | Get all products |
| GET | `/api/products/{id}` | Get product by ID |
| GET | `/api/products/category/{category}` | Get products by category |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product (ADMIN) |

### Customers `/api/customers`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/customers` | Register a new customer |
| GET | `/api/customers` | Get all customers |
| GET | `/api/customers/{id}` | Get customer by ID |
| PUT | `/api/customers/{id}` | Update customer details |
| DELETE | `/api/customers/{id}` | Delete a customer (ADMIN) |

### Orders `/api/orders`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Place a new order |
| GET | `/api/orders/{id}` | Get order by ID |
| GET | `/api/orders/customer/{customerId}` | Get orders by customer (paginated) |
| PATCH | `/api/orders/{id}/status` | Update order status |
| DELETE | `/api/orders/{id}` | Cancel an order (ADMIN) |

## Key Features

### JWT Authentication
- Stateless authentication with Bearer tokens
- BCrypt password hashing
- Role-based access control (USER, ADMIN)
- Token expiry (24 hours)

### Order State Machine
Orders follow a strict status flow with validated transitions:

```
PENDING ──> CONFIRMED ──> OUT_FOR_DELIVERY ──> DELIVERED
   │             │
   └─── CANCELLED ◄──┘
```

### Stock Management
- **On order placement**: Stock is validated and deducted atomically
- **On cancellation**: Stock is automatically restored
- **Insufficient stock**: Returns `400 Bad Request` with details

### Price Snapshot
Product prices are captured at the time of purchase (`priceAtPurchase`), so subsequent price changes don't affect existing orders.

### Global Exception Handling
All exceptions are handled centrally via `@RestControllerAdvice`:

| Exception | HTTP Status | When |
|-----------|-------------|------|
| `ResourceNotFoundException` | 404 | Entity not found |
| `DuplicateResourceException` | 409 | Duplicate email/name |
| `InsufficientStockException` | 400 | Not enough stock |
| `IllegalStateException` | 400 | Invalid status transition |
| `BadCredentialsException` | 401 | Invalid login credentials |
| `MethodArgumentNotValidException` | 400 | Validation failure |

## Setup & Run

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL database (or use Docker)

### Option 1: Run Locally

1. **Clone the repository**
   ```bash
   git clone https://github.com/yeshwanth5reddy/grocery-order-api.git
   cd grocery-order-api
   ```

2. **Configure the database**
   ```bash
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   ```
   Update `application.yml` with your PostgreSQL connection details and JWT secret.

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```
   The API starts on `http://localhost:9090`

### Option 2: Run with Docker

```bash
docker-compose up --build
```
This starts PostgreSQL and the API together. No configuration needed.

### Swagger UI
Visit `http://localhost:9090/swagger-ui.html` for interactive API docs.
Click the **Authorize** button and paste your JWT token to test protected endpoints.

### Run Tests
```bash
./mvnw test
```
Tests use H2 in-memory database — no external DB required.

## Testing

| Layer | Type | Count | Framework |
|-------|------|-------|-----------|
| Service | Unit tests | 21 | JUnit 5 + Mockito |
| Controller | MockMvc integration tests | 27 | @WebMvcTest + MockMvc |
| Application | Context load test | 1 | @SpringBootTest |

## Project Structure

```
src/main/java/com/groceryorder/
├── config/              # Security + OpenAPI configuration
├── controller/          # REST controllers (with Swagger annotations)
├── dto/
│   ├── request/         # Request DTOs with validation
│   └── response/        # Response DTOs with builders
├── enums/               # OrderStatus, PaymentStatus, Role
├── exception/           # Custom exceptions + GlobalExceptionHandler
├── model/entity/        # JPA entities (Product, Customer, Order, OrderItem, AppUser)
├── repository/          # Spring Data JPA repositories
├── security/            # JWT service, filter, UserDetailsService
└── service/             # Business logic layer

src/test/java/com/groceryorder/
├── controller/          # MockMvc controller tests
└── service/             # Unit tests with Mockito
```

## Sample Requests

### Register and Get Token
```json
POST /api/auth/register
{
    "username": "yeshwanth",
    "password": "password123"
}
// Response: { "token": "eyJhbG...", "username": "yeshwanth", "role": "ROLE_USER" }
```

### Create a Product (requires auth)
```
POST /api/products
Authorization: Bearer <token>

{
    "name": "Organic Milk",
    "description": "Fresh organic whole milk 1L",
    "price": 2.49,
    "quantity": 50,
    "category": "Dairy"
}
```

### Place an Order (requires auth)
```
POST /api/orders
Authorization: Bearer <token>

{
    "customerId": 1,
    "items": [
        { "productId": 1, "quantity": 3 },
        { "productId": 2, "quantity": 1 }
    ],
    "deliveryAddress": "123 Amsterdam St, Netherlands"
}
```

### Update Order Status (requires auth)
```
PATCH /api/orders/1/status
Authorization: Bearer <token>

{
    "newStatus": "CONFIRMED"
}
```
