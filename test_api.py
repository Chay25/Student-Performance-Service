from pathlib import Path
from fastapi.testclient import TestClient
from app import main

def test_predict_and_health(tmp_path, monkeypatch):
    monkeypatch.setattr(main, "MODEL_PATH", tmp_path / "model.json")
    with TestClient(main.app) as client:
        assert client.get("/health").json()["status"] == "UP"
        response = client.post("/predict", json={"studentId": "s-1", "attendanceRate": 90,
            "studyHoursPerWeek": 12, "previousScore": 76, "assignmentsCompleted": 20})
        assert response.status_code == 200
        data = response.json()
        assert 0 <= data["predictedScore"] <= 100
        assert len(data["topFactors"]) == 3
        assert (tmp_path / "model.json").exists()

def test_validation(tmp_path, monkeypatch):
    monkeypatch.setattr(main, "MODEL_PATH", tmp_path / "model.json")
    with TestClient(main.app) as client:
        response = client.post("/predict", json={"studentId": "s-1", "attendanceRate": 200,
            "studyHoursPerWeek": 12, "previousScore": 76, "assignmentsCompleted": 20})
        assert response.status_code == 422
