"""Verify existing Spring endpoints with synthetic data; save redacted evidence.

Run only against the isolated sqlite-test backend described in VERIFICACION.md.
This is an HTTP client, not a Django implementation or a browser UI E2E test.
"""
import datetime
import http.cookiejar
import json
from pathlib import Path
import time
import urllib.error
import urllib.request
import uuid

BASE = "http://127.0.0.1:18093/api/v1"
OUTPUT = Path(__file__).resolve().parents[1] / "evidencias" / "http-resultados.json"
records = []
jar = http.cookiejar.CookieJar()
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
csrf = None


def call(label, method, path, expected, payload=None, match=None, use_csrf=True):
    headers = {"Accept": "application/json, application/problem+json"}
    if payload is not None:
        headers["Content-Type"] = "application/json"
    if csrf and use_csrf and method not in ("GET", "HEAD"):
        headers["X-CSRF-TOKEN"] = csrf
    if match is not None:
        headers["If-Match"] = match
    request = urllib.request.Request(
        BASE + path,
        data=None if payload is None else json.dumps(payload).encode(),
        headers=headers,
        method=method,
    )
    started = time.monotonic()
    try:
        response = client.open(request, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    raw = response.read()
    body = json.loads(raw) if raw else None
    safe_body = dict(body) if isinstance(body, dict) else body
    if isinstance(safe_body, dict):
        for field in ("csrfToken", "password", "token"):
            if field in safe_body:
                safe_body[field] = "[REDACTED]"
    selected = {
        name: response.headers[name]
        for name in ("Content-Type", "Location", "ETag", "WWW-Authenticate")
        if name in response.headers
    }
    records.append({
        "case": label, "method": method, "path": path,
        "request_body": "[CREDENTIALS REDACTED]" if path == "/sessions" else payload,
        "if_match": match, "csrf_sent": "X-CSRF-TOKEN" in headers,
        "expected_status": expected, "actual_status": response.status,
        "headers": selected, "body": safe_body, "body_bytes": len(raw),
        "duration_ms": round((time.monotonic() - started) * 1000, 2),
        "status_passed": response.status == expected,
    })
    if response.status != expected:
        raise AssertionError(f"{label}: expected {expected}, got {response.status}")
    return body, response.headers


def run():
    global csrf
    call("HTTP-01 unauthenticated", "GET", "/careers", 401)
    login, _ = call("HTTP-02 login", "POST", "/sessions", 201,
                    {"username": "admin", "password": "Synthetic-Admin-2026"})
    csrf = login["csrfToken"]
    suffix = "APE3-" + uuid.uuid4().hex[:12].upper()
    unit, unit_headers = call("HTTP-03 create unit", "POST", "/academic-units", 201,
                              {"code": suffix, "name": "APE3 synthetic unit"})
    assert unit_headers["Location"].endswith(unit["id"])
    data = {"code": suffix, "name": "APE3 synthetic career", "academicUnitId": unit["id"],
            "validFrom": "2020-01-01", "validUntil": None}
    call("HTTP-04 CSRF rejected", "POST", "/careers", 403, data, use_csrf=False)
    call("HTTP-05 invalid input", "POST", "/careers", 400, {"code": ""})
    career, headers = call("HTTP-06 create career", "POST", "/careers", 201, data)
    assert headers["ETag"] == '"1"' and headers["Location"].endswith(career["id"])
    call("HTTP-07 duplicate", "POST", "/careers", 409,
         {**data, "code": " " + suffix.lower() + " "})
    path = "/careers/" + career["id"]
    _, headers = call("HTTP-08 read career", "GET", path, 200)
    call("HTTP-09 missing precondition", "PATCH", path, 428, data)
    changed, headers = call("HTTP-10 update", "PATCH", path, 200,
                            {**data, "name": "APE3 updated career"}, headers["ETag"])
    assert changed["revision"] == 2 and headers["ETag"] == '"2"'
    call("HTTP-11 stale revision", "PATCH", path, 412, data, '"1"')
    history, _ = call("HTTP-12 history", "GET", path + "/history", 200)
    assert history["totalElements"] == 2
    page, _ = call("HTTP-13 filtered list", "GET", "/careers?q=" + suffix, 200)
    assert page["totalElements"] == 1 and page["items"][0]["id"] == career["id"]
    call("HTTP-14 missing resource", "GET", "/careers/ffffffff-ffff-ffff-ffff-ffffffffffff", 404)
    disabled, _ = call("HTTP-15 state decision", "POST", path + "/state-decisions", 201,
                       {"status": "DISABLED", "reason": "APE3 synthetic state decision"}, '"2"')
    assert disabled["status"] == "DISABLED" and disabled["revision"] == 3
    version_id = "60000000-0000-0000-0000-000000000002"
    problem, _ = call("HTTP-16 legacy representation rejected", "GET", "/model-versions/" + version_id, 409)
    assert problem["code"] == "MODEL_VERSION_REQUIRES_V2"
    version, _ = call("HTTP-16B configured representation", "GET", "/model-versions/" + version_id + "/configured", 200)
    assert version["id"] == version_id and version["status"] == "PUBLISHED"
    process, process_headers = call("HTTP-17 prepare process", "POST", "/processes", 201, {
        "careerId": "11111111-1111-1111-1111-111111111111", "code": suffix,
        "name": "APE3 synthetic preparation", "periodStart": "2026-01-01",
        "periodEnd": "2026-12-31", "modelVersionId": version_id})
    assert process["state"] == "PREPARATION" and process["revision"] == 0
    assert process_headers["ETag"] == '"0"' and process_headers["Location"].endswith(process["id"])
    call("HTTP-18 read process", "GET", "/processes/" + process["id"], 200)
    processes, _ = call("HTTP-19 list processes", "GET", "/processes", 200)
    assert any(p["id"] == process["id"] for p in processes)
    call("HTTP-20 logout", "DELETE", "/sessions", 204)
    assert records[-1]["body_bytes"] == 0
    call("HTTP-21 revoked access", "GET", "/careers", 401)


if __name__ == "__main__":
    result = {"executed_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
              "base_url": BASE, "implementation": "existing Spring Boot backend",
              "database": "isolated SQLite auxiliary profile, not PostgreSQL acceptance",
              "ui_browser_test": False, "cases": records, "passed": False}
    try:
        run()
        result["passed"] = True
    except Exception as error:
        result["failure"] = f"{type(error).__name__}: {error}"
        raise
    finally:
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"HTTP verification: {len(records)} cases; passed={result['passed']}")
