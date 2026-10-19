"""Regenerate synthetic upload fixtures: python3 -m pip install 'pypdf[crypto]==6.10.0'."""
from pathlib import Path
from pypdf import PdfReader, PdfWriter
from pypdf.constants import UserAccessPermissions

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT.parent / "test.pdf"

# Reuse the existing test certificate, which already exceeds the UI's 10 KiB minimum.
for filename, password in (("password-required.pdf", "test-password"), ("copy-restricted.pdf", "")):
    writer = PdfWriter(clone_from=PdfReader(SOURCE))
    writer.encrypt(password, owner_password="test-owner-password", algorithm="RC4-40",
                   permissions_flag=UserAccessPermissions.PRINT)
    writer.write(ROOT / filename)

writer = PdfWriter()
writer.add_blank_page(width=595, height=842)
writer.write(ROOT / "too-small.pdf")

writer = PdfWriter()
# Padding keeps this zero-page PDF above the client-side minimum, reaching backend validation.
writer.add_metadata({"/Fixture": "no-pages " + "x" * (11 * 1024)})
writer.write(ROOT / "no-pages.pdf")

(ROOT / "empty.pdf").write_bytes(b"")
(ROOT / "unsupported.txt").write_text("Unsupported plain-text certificate fixture.\n" * 5)
(ROOT / "invalid-content.pdf").write_bytes(b"This is not a PDF.\n" + b"x" * (11 * 1024))
# A truncated PDF header, padded with a PDF comment to reach the backend through the real UI.
(ROOT / "truncated.pdf").write_bytes(SOURCE.read_bytes()[:20] + b"\n%" + b"x" * (11 * 1024))
