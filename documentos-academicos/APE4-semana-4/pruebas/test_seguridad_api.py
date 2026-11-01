"""Proposed bounded tests; NOT executed or evidence of DRF implementation.

Run only in the OS-isolated synthetic environment described in PLAN_PRUEBAS.md.
The module skips by default and rejects remote URLs and redirects.
"""
import http.cookiejar
import json
import os
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, HTTPCookieProcessor, ProxyHandler, Request, build_opener
import uuid

import pytest

if os.environ.get("SDD_SECURITY_TESTS") != "1":
    pytest.skip("Security runtime checks not explicitly enabled in isolated fixture", allow_module_level=True)

BASE = os.environ.get("SDD_TEST_BASE_URL", "http://127.0.0.1:18094/api/v1").rstrip("/")
url = urlsplit(BASE)
if url.hostname not in ("127.0.0.1", "localhost", "::1") or url.scheme not in ("http", "https"):
    raise RuntimeError("Only an isolated loopback backend is permitted")
if url.username or url.password or url.query or url.fragment:
    raise RuntimeError("Credentials/query/fragment are not allowed in base URL")


class NoRedirects(HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, newurl):
        raise RuntimeError("Redirect rejected; remain inside isolated backend")


class Client:
    def __init__(self):
        self.opener = build_opener(ProxyHandler({}), HTTPCookieProcessor(http.cookiejar.CookieJar()), NoRedirects())
        self.csrf = None

    def call(self, method, path, payload=None, csrf=True):
        if not path.startswith("/") or path.startswith("//"):
            raise ValueError("Expected relative API resource")
        headers = {"Accept": "application/json, application/problem+json"}
        if payload is not None:
            headers["Content-Type"] = "application/json"
        if csrf and self.csrf:
            headers["X-CSRF-TOKEN"] = self.csrf
        request = Request(BASE + path, method=method, headers=headers,
                          data=None if payload is None else json.dumps(payload).encode())
        try:
            response = self.opener.open(request, timeout=10)
        except HTTPError as error:
            response = error
        with response:
            raw = response.read(262145)
            if len(raw) > 262144:
                raise RuntimeError("Response exceeds bounded fixture limit")
            return response.status, response.headers, json.loads(raw) if raw else None

    def login(self, prefix):
        user = os.environ.get(prefix + "_USERNAME")
        password = os.environ.get(prefix + "_PASSWORD")
        if not user or not password:
            pytest.skip("Synthetic principal variables are required; no credentials guessed")
        status, _, body = self.call("POST", "/sessions", {"username": user, "password": password})
        assert status == 201, "Synthetic principal login failed"
        self.csrf = body["csrfToken"]


@pytest.fixture
def admin():
    client = Client()
    client.login("SDD_ADMIN")
    try:
        yield client
    finally:
        client.call("DELETE", "/sessions")


def test_vt01_no_credentials_rejected():
    status, _, body = Client().call("GET", "/careers")
    assert status == 401
    assert body["code"] and body["correlationId"]


def test_vt01_challenge_header_regression():
    status, headers, _ = Client().call("GET", "/careers")
    assert status == 401
    assert headers.get("WWW-Authenticate"), "Resolve HTTP-AA-01 using the accepted authentication scheme"


def test_vt02_unknown_synthetic_identity_rejected():
    status, _, body = Client().call("POST", "/sessions", {
        "username": "missing-" + uuid.uuid4().hex[:12], "password": "Synthetic-Invalid-Only-2026"})
    assert status == 401
    assert body["code"] == "AUTHENTICATION_FAILED"


def test_vt06_missing_csrf_rejected(admin):
    status, _, _ = admin.call("POST", "/academic-units", {
        "code": "VT-" + uuid.uuid4().hex[:8], "name": "Synthetic rejected unit"}, csrf=False)
    assert status == 403


def test_vt07_required_fields_rejected(admin):
    status, _, _ = admin.call("POST", "/careers", {})
    assert status == 400


def test_vt04_foreign_fixture_hidden():
    foreign_id = os.environ.get("SDD_FOREIGN_CAREER_ID")
    if not foreign_id:
        pytest.skip("A foreign synthetic fixture UUID is required")
    uuid.UUID(foreign_id)
    reader = Client()
    reader.login("SDD_READER")
    try:
        status, _, _ = reader.call("GET", "/careers/" + foreign_id)
        assert status == 404, "Fixture reader must not hold catalog admin or scope to target career"
    finally:
        reader.call("DELETE", "/sessions")


def test_vt12_logout_has_no_body_and_revokes():
    client = Client()
    client.login("SDD_ADMIN")
    status, _, body = client.call("DELETE", "/sessions")
    assert status == 204 and body is None
    assert client.call("GET", "/careers")[0] == 401
