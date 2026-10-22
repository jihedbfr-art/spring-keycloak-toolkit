"""Check a local lab without writing access tokens to stdout or disk."""
import argparse
import base64
import json
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


def local_url(value):
    parsed = urlsplit(value)
    if parsed.scheme != "http" or parsed.hostname not in ("127.0.0.1", "localhost", "::1") or parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise argparse.ArgumentTypeError("use a loopback HTTP URL without credentials, query or fragment")
    return value.rstrip("/")


def http(url, headers=None, body=None):
    request = Request(url, headers=headers or {}, data=body)
    try:
        with urlopen(request, timeout=5) as response:
            return response.status, response.read()
    except HTTPError as error:
        return error.code, error.read()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--issuer", type=local_url, default="http://127.0.0.1:18080/realms/iam-lab")
    parser.add_argument("--api", type=local_url, default="http://127.0.0.1:18081")
    args = parser.parse_args()
    deadline = time.monotonic() + 90
    while True:
        try:
            ready = http(args.issuer + "/.well-known/openid-configuration")[0] == 200 and http(args.api + "/api/public")[0] == 200
        except (URLError, TimeoutError, OSError):
            ready = False
        if ready:
            break
        if time.monotonic() >= deadline:
            raise RuntimeError("local Keycloak and API were not ready within the retry window")
        time.sleep(0.5)

    tokens = {}
    for client in ("lab-reader", "lab-admin", "lab-no-role", "lab-other-audience"):
        credential = base64.b64encode((client + ":" + client + "-only-for-local-demo").encode()).decode()
        status, body = http(args.issuer + "/protocol/openid-connect/token", {"Authorization": "Basic " + credential, "Content-Type": "application/x-www-form-urlencoded"}, b"grant_type=client_credentials")
        if status != 200:
            raise RuntimeError(f"token issuance for {client}: HTTP {status}")
        tokens[client] = json.loads(body)["access_token"]
    cases = [("missing-token", "/api/reader", None, 401), ("reader", "/api/reader", "lab-reader", 200),
             ("reader-to-admin", "/api/admin", "lab-reader", 403), ("admin", "/api/admin", "lab-admin", 200),
             ("no-role", "/api/reader", "lab-no-role", 403), ("other-audience", "/api/reader", "lab-other-audience", 401)]
    for name, path, client, expected in cases:
        status, _ = http(args.api + path, {"Authorization": "Bearer " + tokens[client]} if client else {})
        if status != expected:
            raise RuntimeError(f"{name}: expected HTTP {expected}, got {status}")
        print(f"{name}: HTTP {status}")
    print("Six local HTTP checks passed; tokens were kept in memory only.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, URLError, OSError, ValueError, KeyError):
        print("Local lab check failed; inspect server readiness and the synthetic fixture configuration.", file=sys.stderr)
        sys.exit(1)
