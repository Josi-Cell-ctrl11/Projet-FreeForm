#!/usr/bin/env python3
"""Consume the private Actions secret without printing or committing key material."""
import base64
import json
import os
from pathlib import Path


def prepare():
    raw = os.environ.get("FLOATX_SIGNING_BUNDLE", "")
    if not raw:
        raise SystemExit("Secret FLOATX_SIGNING_BUNDLE manquant. Ajouter la sauvegarde privée aux secrets Actions.")
    data = json.loads(raw)
    for name in ("keystore_b64", "store_password", "key_alias", "key_password"):
        value = data.get(name)
        if not isinstance(value, str) or not value or "\n" in value or "\r" in value:
            raise SystemExit("Bundle de signature invalide : " + name)
    root = Path(os.environ["RUNNER_TEMP"]) / "floatx-signing"
    root.mkdir(mode=0o700, exist_ok=True)
    key = root / "stable.p12"
    key.write_bytes(base64.b64decode(data["keystore_b64"], validate=True))
    key.chmod(0o600)
    values = {"FLOATX_KEYSTORE_PATH": str(key), "FLOATX_STORE_PASSWORD": data["store_password"],
              "FLOATX_KEY_ALIAS": data["key_alias"], "FLOATX_KEY_PASSWORD": data["key_password"]}
    with open(os.environ["GITHUB_ENV"], "a") as output:
        for name, value in values.items():
            escaped = value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
            print("::add-mask::" + escaped)
            output.write(name + "=" + value + "\n")


if __name__ == "__main__":
    prepare()
