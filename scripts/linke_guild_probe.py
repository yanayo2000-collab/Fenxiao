#!/usr/bin/env python3
import argparse
import base64
import hashlib
import hmac
import json
import os
import sys
import time
import urllib.parse
import urllib.request

API_BASE = "https://api.linke.ai"
DEFAULT_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 en-US/US"


def build_query(params: dict[str, str], timestamp_ms: int) -> str:
    filtered = []
    for key, value in params.items():
        if value is None or value == "":
            continue
        encoded = urllib.parse.quote(str(value), safe="")
        filtered.append(f"{key}={encoded}")
    query = "?" + "&".join(filtered) if filtered else ""
    return f"{query}&{timestamp_ms}" if query else f"&{timestamp_ms}"


def sign(path: str, params: dict[str, str], timestamp_ms: int, oauth_secret: str) -> str:
    payload = f"{path}{build_query(params, timestamp_ms)}".encode("utf-8")
    digest = hmac.new(oauth_secret.encode("utf-8"), payload, hashlib.sha1).digest()
    return base64.b64encode(digest).decode("utf-8")


def request_json(path: str, method: str, oauth_token: str, oauth_secret: str, params=None, data=None):
    params = params or {}
    timestamp_ms = int(time.time() * 1000)
    signature = sign(path, params if method.upper() == "GET" else {}, timestamp_ms, oauth_secret)
    url = f"{API_BASE}{path}"
    if method.upper() == "GET" and params:
        url += "?" + urllib.parse.urlencode({k: v for k, v in params.items() if v is not None and v != ""})
    body = None
    headers = {
        "X-Auth-Token": oauth_token,
        "X-Auth-Timestamp": str(timestamp_ms),
        "X-Auth-Signature": signature,
        "X-App-Language": "en",
        "Country": "US",
        "User-Agent": DEFAULT_USER_AGENT,
        "Accept": "application/json, text/plain, */*",
    }
    if method.upper() != "GET" and data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method.upper())
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(raw)
        except json.JSONDecodeError:
            return e.code, {"raw": raw}


def classify_linky_account(oauth_token: str, oauth_secret: str, linky_account: str) -> tuple[int, dict]:
    status, search_payload = request_json(
        "/api/guild/search_anchors",
        "GET",
        oauth_token,
        oauth_secret,
        params={"id": linky_account, "page": "1", "page_size": "10"},
    )
    search_payload = search_payload or {}
    if search_payload.get("error", {}).get("message") == "please re-login":
        return status, {"classification": "UNAVAILABLE", "reason": "please re-login", "raw": search_payload}
    items = search_payload.get("items") or []
    if items:
        first = items[0]
        return status, {
            "classification": "MATCHED_OURS",
            "guild_id": first.get("guild_id"),
            "guild_name": first.get("guild_name"),
            "item": first,
            "raw": search_payload,
        }

    info_status, info_payload = request_json(
        "/api/guild/anchor_info",
        "GET",
        oauth_token,
        oauth_secret,
        params={"id": linky_account},
    )
    info_payload = info_payload or {}
    if info_payload.get("error", {}).get("message") == "please re-login":
        return info_status, {"classification": "UNAVAILABLE", "reason": "please re-login", "raw": info_payload}
    guild_id = info_payload.get("guild_id")
    guild_name = info_payload.get("guild_name")
    if guild_id or guild_name:
        return info_status, {
            "classification": "JOINED_OTHER_GUILD",
            "guild_id": guild_id,
            "guild_name": guild_name,
            "raw": info_payload,
        }
    return info_status, {
        "classification": "NOT_JOINED",
        "reason": "not found in current guild scope; outside guild status unconfirmed",
        "raw": {
            "search": search_payload,
            "anchor_info": info_payload,
        },
    }


def main():
    parser = argparse.ArgumentParser(description="Probe guild.linke.ai API without browser automation.")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("user-info", help="Call /api/guild/user_info")

    p_search = sub.add_parser("search-anchors", help="Call /api/guild/search_anchors")
    p_search.add_argument("--id", dest="anchor_id", default="", help="Streamer/anchor ID")
    p_search.add_argument("--page", default="1")
    p_search.add_argument("--page-size", default="10")

    p_anchor = sub.add_parser("anchor-info", help="Call /api/guild/anchor_info")
    p_anchor.add_argument("--id", dest="anchor_id", required=True, help="Streamer user_id")

    p_classify = sub.add_parser("classify-linky", help="Best-effort classify a Linky account against current guild")
    p_classify.add_argument("--id", dest="anchor_id", required=True, help="Linky / anchor ID")

    args = parser.parse_args()
    oauth_token = os.environ.get("LINKE_OAUTH_TOKEN", "").strip()
    oauth_secret = os.environ.get("LINKE_OAUTH_TOKEN_SECRET", "").strip()
    if not oauth_token or not oauth_secret:
        print(json.dumps({
            "error": "missing_credentials",
            "message": "Set LINKE_OAUTH_TOKEN and LINKE_OAUTH_TOKEN_SECRET first."
        }, ensure_ascii=False, indent=2))
        sys.exit(2)

    if args.command == "user-info":
        status, payload = request_json("/api/guild/user_info", "GET", oauth_token, oauth_secret)
    elif args.command == "search-anchors":
        status, payload = request_json(
            "/api/guild/search_anchors",
            "GET",
            oauth_token,
            oauth_secret,
            params={"id": args.anchor_id, "page": args.page, "page_size": args.page_size},
        )
    elif args.command == "anchor-info":
        status, payload = request_json(
            "/api/guild/anchor_info",
            "GET",
            oauth_token,
            oauth_secret,
            params={"id": args.anchor_id},
        )
    elif args.command == "classify-linky":
        status, payload = classify_linky_account(oauth_token, oauth_secret, args.anchor_id)
    else:
        raise RuntimeError(f"unsupported command: {args.command}")

    print(json.dumps({"http_status": status, "payload": payload}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
