# <img src="logo.png" width="24" height="24" alt="Graduation logo"> Student Performance Prediction Service
**Java 17 + Spring Boot + Python FastAPI + XGBoost + SHAP + PostgreSQL + Docker Compose**

A portfolio-ready demonstration of a Java REST API calling a Python ML service, storing prediction history, and returning explainability factors. **The training set is entirely synthetic and the resulting scores are not valid real-world student assessments.**

## Architecture

```text
Client (curl / Swagger UI)
      |
      v
Spring Boot API (port 8080, validation + X-API-Key auth)
      |                            |
      v                            v
FastAPI (XGBoost + SHAP)        PostgreSQL (prediction history)
```

## Quick start

Requires Docker Desktop or Docker Engine + Docker Compose.

```bash
cp .env.example .env
# IMPORTANT: Set a unique API_KEY and DB_PASSWORD in .env
# For local demo only; never commit .env
docker compose up --build
```

On initial startup the ML service trains a small XGBoost regressor on **reproducible synthetic examples** (seed 42) and saves the model in a Docker volume; subsequent starts reload it. First startup may take longer.

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/api/health

Click **Authorize** in Swagger and supply the API key from `.env`.

## REST API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/predictions` | Validate, predict, store, return `201` |
| `GET` | `/api/predictions/{id}` | Get one prediction (`404` if absent) |
| `GET` | `/api/predictions?studentId=s-1` | History for student ID |
| `GET` | `/api/health` | Java/model status (`503` if model down) |

### Request

```bash
curl -X POST http://localhost:8080/api/predictions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: YOUR_API_KEY_FROM_DOT_ENV" \
  -d '{"studentId":"s-1","attendanceRate":90,"studyHoursPerWeek":12,"previousScore":76,"assignmentsCompleted":20}'
```

### Example response (illustrative; actual numeric output will vary)

```json
{
  "id": 1,
  "studentId": "s-1",
  "predictedScore": 78.1,
  "topFactors": [
    {"feature": "previousScore", "contribution": 3.4},
    {"feature": "attendanceRate", "contribution": 2.1},
    {"feature": "studyHoursPerWeek", "contribution": -0.7}
  ],
  "modelVersion": "synthetic-demo-v1",
  "createdAt": "2026-10-29T12:00:00Z"
}
```

SHAP contributions are relative to the model's baseline score; they are **not causal explanations**.

### Retrieve and history

```bash
curl -H "X-API-Key: YOUR_API_KEY_FROM_DOT_ENV" http://localhost:8080/api/predictions/1
curl -H "X-API-Key: YOUR_API_KEY_FROM_DOT_ENV" 'http://localhost:8080/api/predictions?studentId=s-1'
```

Input constraints: `studentId` nonblank (max 80 chars); attendance and previous score 0–100; study hours 0–80; completed assignments 0–30. Missing/invalid input returns `400` from Java; missing/invalid API key returns `401`; failed Python upstream returns `502`.

## Tests

Java (requires JDK 17+ and Maven):
```bash
cd java-api
mvn test
```

Python (requires Python 3.11 and packages in requirements.txt):
```bash
cd model-service
pip install -r requirements.txt
python -m pytest -q
```

GitHub Actions runs both test suites on pushes and pull requests (`.github/workflows/ci.yml`).

## Repo layout

```text
student-performance-service/
├── java-api/            # Spring Boot REST API, security, JPA, tests
├── model-service/       # FastAPI, synthetic XGBoost, SHAP, tests
├── docker-compose.yml   # Java + Python + PostgreSQL
├── .env.example         # Non-secret sample configuration
├── .github/workflows/ci.yml
├── docs/architecture.md
└── README.md
```

## Limitations and responsible use

- **Synthetic training data only**. Predicted results have no demonstrated accuracy on real students and should never inform grades, admission, or educational decisions.
- No real student records are bundled. Use dummy IDs; avoid entering real personal/student data.
- SHAP shows model attribution rather than cause/effect.
- API-key auth is demo-level; for production use OAuth2/OIDC, TLS, access controls, rate limits, audit logs, secret management, and privacy safeguards.
- JPA `ddl-auto: update` is demonstration-only; use Flyway/Liquibase migrations for real deployments.
- No production metrics, tracing, model drift monitoring, calibration, or fairness evaluation.
- No end-to-end integration tests against a real database; included tests focus on Python model API and Java controller/service behavior.

## Upload to GitHub

1. Extract the ZIP file.
2. Create an **empty** GitHub repository called `student-performance-service` (do not create a second README).
3. In a terminal inside the extracted project folder:

```bash
git init
git add .
git commit -m "Initial student performance prediction service"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/student-performance-service.git
git push -u origin main
```

Do not commit `.env`, passwords, database files, or real student data.
