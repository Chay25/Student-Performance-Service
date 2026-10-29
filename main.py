"""DEMO ONLY: synthetic training data, not a validated educational prediction system."""
import os
from pathlib import Path
from contextlib import asynccontextmanager

import numpy as np
import shap
import xgboost as xgb
from fastapi import FastAPI
from pydantic import BaseModel, Field

FEATURES = ["attendanceRate", "studyHoursPerWeek", "previousScore", "assignmentsCompleted"]
MODEL_PATH = Path(os.getenv("MODEL_PATH", "/app/artifacts/xgboost.json"))
model = None
explainer = None

class StudentFeatures(BaseModel):
    attendanceRate: float = Field(ge=0, le=100)
    studyHoursPerWeek: float = Field(ge=0, le=80)
    previousScore: float = Field(ge=0, le=100)
    assignmentsCompleted: int = Field(ge=0, le=30)

class PredictRequest(StudentFeatures):
    studentId: str = Field(min_length=1, max_length=80)

class Factor(BaseModel):
    feature: str
    contribution: float

class PredictResponse(BaseModel):
    predictedScore: float
    topFactors: list[Factor]
    modelVersion: str = "synthetic-demo-v1"

def ensure_model():
    global model, explainer
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    model = xgb.XGBRegressor(n_estimators=70, max_depth=3, learning_rate=0.08,
                             subsample=0.9, random_state=42, n_jobs=1)
    if MODEL_PATH.exists():
        model.load_model(str(MODEL_PATH))
    else:
        rng = np.random.default_rng(42)
        rows = np.column_stack((rng.uniform(40, 100, 1500),
                                rng.uniform(0, 35, 1500),
                                rng.uniform(25, 100, 1500),
                                rng.integers(0, 31, 1500)))
        target = np.clip(0.2 * rows[:, 0] + 0.6 * rows[:, 1] +
                         0.55 * rows[:, 2] + 0.4 * rows[:, 3] - 10 +
                         rng.normal(0, 5, 1500), 0, 100)
        model.fit(rows, target)
        model.save_model(str(MODEL_PATH))
    explainer = shap.TreeExplainer(model)

@asynccontextmanager
async def lifespan(app: FastAPI):
    ensure_model()
    yield

app = FastAPI(title="Student Performance ML Demo", version="1.0.0", lifespan=lifespan)

@app.get("/health")
def health():
    return {"status": "UP" if model is not None else "DOWN", "modelVersion": "synthetic-demo-v1"}

@app.post("/predict", response_model=PredictResponse)
def predict(payload: PredictRequest):
    data = np.array([[getattr(payload, field) for field in FEATURES]], dtype=float)
    score = float(np.clip(model.predict(data)[0], 0, 100))
    values = explainer.shap_values(data)[0]
    factors = sorted((Factor(feature=field, contribution=round(float(val), 4))
                      for field, val in zip(FEATURES, values)),
                     key=lambda factor: abs(factor.contribution), reverse=True)
    return PredictResponse(predictedScore=round(score, 2), topFactors=factors[:3])
