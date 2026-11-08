#!/usr/bin/env python3
"""Every stable build must retain the committed public signing identity."""
import re
import subprocess
import sys
from pathlib import Path


def verify(tool, apk, fingerprint):
    result = subprocess.run([tool, "verify", "--verbose", "--print-certs", apk],
                            check=True, text=True, capture_output=True)
    expected = Path(fingerprint).read_text().strip().lower()
    actual = re.findall(r"Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+)", result.stdout)
    if actual != [expected]:
        raise SystemExit("Le certificat ne correspond pas à la signature permanente FloatX.")
    print("Signature APK vérifiée : " + expected)


if __name__ == "__main__":
    verify(*sys.argv[1:])
