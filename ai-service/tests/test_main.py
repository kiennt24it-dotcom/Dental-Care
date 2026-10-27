from fastapi.testclient import TestClient
from app.main import app, FALLBACK
import httpx
import json

client = TestClient(app)


def test_health(monkeypatch):
    monkeypatch.delenv("LLM_API_KEY", raising=False)
    assert client.get("/health").json() == {
        "status": "ok",
        "providerConfigured": False,
        "model": None,
    }


def test_invalid_symptom_is_rejected():
    response = client.post("/analyze", json={"symptoms": "x"})
    assert response.status_code == 422


def test_provider_missing_uses_fallback(monkeypatch):
    monkeypatch.delenv("LLM_API_KEY", raising=False)
    response = client.post("/analyze", json={"symptoms": "Răng ê buốt khi uống nước lạnh"})
    assert response.status_code == 200
    assert response.json()["possible_concern"] == "Ê buốt hoặc nhạy cảm răng"
    assert response.json()["suggested_service"] == "Khám răng tổng quát"
    assert "không thay thế chẩn đoán" in response.json()["note"]


def test_urgent_symptoms_are_referred_for_immediate_care(monkeypatch):
    monkeypatch.setenv("LLM_API_KEY", "test-key")

    async def should_not_call_provider(_):
        raise AssertionError("Emergency symptoms must bypass the LLM")

    monkeypatch.setattr("app.main.generate_with_llm", should_not_call_provider)
    response = client.post("/analyze", json={"symptoms": "Tôi bị sưng mặt nặng và khó thở"})

    assert response.status_code == 200
    assert response.json()["suggested_service"] == "Đánh giá cấp cứu ngay"
    assert "cấp cứu ngay" in response.json()["explanation"]
    assert "không thay thế chẩn đoán" in response.json()["note"]


def test_provider_failure_uses_endpoint_fallback(monkeypatch):
    monkeypatch.setenv("LLM_API_KEY", "test-key")

    async def fail(_):
        raise RuntimeError("provider unavailable")

    monkeypatch.setattr("app.main.generate_with_llm", fail)
    response = client.post("/analyze", json={"symptoms": "Đau răng kéo dài từ hôm qua"})
    assert response.status_code == 200
    assert response.json()["suggested_service"] == FALLBACK["suggested_service"]


def test_invalid_provider_json_uses_safe_fallback(monkeypatch):
    monkeypatch.setenv("LLM_API_KEY", "test-key")

    class FakeResponse:
        status_code = 200
        headers = {}

        def raise_for_status(self):
            return None

        def json(self):
            return {
                "choices": [
                    {
                        "message": {
                            "content": json.dumps(
                                {
                                    "possible_concern": "",
                                    "suggested_service": "Khám răng",
                                    "explanation": " ",
                                }
                            )
                        }
                    }
                ]
            }

    class FakeClient:
        def __init__(self, timeout):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_):
            return None

        async def post(self, *_args, **_kwargs):
            return FakeResponse()

    monkeypatch.setattr("app.main.httpx.AsyncClient", FakeClient)
    response = client.post("/analyze", json={"symptoms": "Đau răng kéo dài từ hôm qua"})

    assert response.status_code == 200
    assert response.json()["possible_concern"] == "Đau răng cần được nha sĩ đánh giá"


def test_provider_request_uses_configured_gemini_key(monkeypatch):
    monkeypatch.setenv("LLM_API_KEY", "test-key")
    monkeypatch.delenv("LLM_API_URL", raising=False)
    monkeypatch.delenv("LLM_MODEL", raising=False)
    captured = {}

    class FakeResponse:
        status_code = 200
        headers = {}

        def raise_for_status(self):
            return None

        def json(self):
            return {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": json.dumps(
                                        {
                                            "possible_concern": "Ê buốt răng",
                                            "suggested_service": "Khám răng tổng quát",
                                            "explanation": "Có thể liên quan đến răng nhạy cảm.",
                                        }
                                    )
                                }
                            ]
                        }
                    }
                ]
            }

    class FakeClient:
        def __init__(self, timeout):
            captured["timeout"] = timeout

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_):
            return None

        async def post(self, endpoint, headers, json):
            captured["endpoint"] = endpoint
            captured["headers"] = headers
            captured["payload"] = json
            return FakeResponse()

    monkeypatch.setattr("app.main.httpx.AsyncClient", FakeClient)
    response = client.post("/analyze", json={"symptoms": "Răng ê buốt khi uống nước lạnh"})

    assert response.status_code == 200
    assert captured["headers"]["x-goog-api-key"] == "test-key"
    assert captured["endpoint"] == "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent"
    assert captured["payload"]["generationConfig"]["maxOutputTokens"] == 1024
    assert captured["payload"]["generationConfig"]["responseMimeType"] == "application/json"
    assert "không bịa dịch vụ cụ thể" in captured["payload"]["systemInstruction"]["parts"][0]["text"]
    assert response.json()["possible_concern"] == "Ê buốt răng"


def test_transient_provider_overload_is_retried(monkeypatch):
    monkeypatch.setenv("LLM_API_KEY", "test-key")
    requests = {"count": 0}

    class FakeResponse:
        headers = {}

        def __init__(self, status_code):
            self.status_code = status_code

        def raise_for_status(self):
            if self.status_code >= 400:
                raise httpx.HTTPStatusError(
                    "temporary provider failure",
                    request=httpx.Request("POST", "https://example.test"),
                    response=httpx.Response(self.status_code),
                )

        def json(self):
            return {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": json.dumps(
                                        {
                                            "possible_concern": "Ê buốt răng",
                                            "suggested_service": "Khám răng tổng quát",
                                            "explanation": "Nha sĩ cần kiểm tra trực tiếp.",
                                        }
                                    )
                                }
                            ]
                        }
                    }
                ]
            }

    class FakeClient:
        def __init__(self, timeout):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *_):
            return None

        async def post(self, *_args, **_kwargs):
            requests["count"] += 1
            return FakeResponse(503 if requests["count"] == 1 else 200)

    async def no_wait(_):
        return None

    monkeypatch.setattr("app.main.httpx.AsyncClient", FakeClient)
    monkeypatch.setattr("app.main.asyncio.sleep", no_wait)
    response = client.post("/analyze", json={"symptoms": "Răng ê buốt khi uống nước lạnh"})

    assert response.status_code == 200
    assert requests["count"] == 2
    assert response.json()["possible_concern"] == "Ê buốt răng"


def test_configured_health_does_not_expose_api_key(monkeypatch):
    monkeypatch.setenv("LLM_API_KEY", "secret-test-key")
    monkeypatch.setenv("LLM_MODEL", "gemini-3.1-flash-lite")

    response = client.get("/health")

    assert response.json() == {
        "status": "ok",
        "providerConfigured": True,
        "model": "gemini-3.1-flash-lite",
    }
    assert "secret-test-key" not in response.text
