#!/usr/bin/env python3
"""Download pinned upstream builds, verify SHA-256 before extracting. No secret material."""
import hashlib, pathlib, urllib.request, tarfile, sys
ROOT = pathlib.Path(__file__).resolve().parents[1] / '.native-deps'
ARTIFACTS = {
 'android-arm64-v8a': 'facb332bfcc70e7923f0cf1f309148b8abd83c85f1a71152f01feb9fee2f1b8d',
 'android-x86_64': 'e62e2701376a21f5564f0354a075605cc68d3ace58a38d250e4ec9d36b9be6ae',
 'linux-x86_64': '658feeabd4a6c3c1da9de9e947de34e11d6779bb4c9d1cdbdefd70d1ba5c1863',
}
ROOT.mkdir(exist_ok=True)
for target in sys.argv[1:] or ARTIFACTS:
    expected = ARTIFACTS[target]
    archive = ROOT / f'{target}.tar.gz'
    if not archive.exists():
        url = f'https://github.com/TokTok/c-toxcore/releases/download/v0.2.23/toxcore-v0.2.23-{target}.tar.gz'
        with urllib.request.urlopen(url, timeout=120) as response: archive.write_bytes(response.read())
    if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
        raise SystemExit(f'Checksum mismatch: {target}; remove archive and investigate')
    with tarfile.open(archive) as tf:
        tf.extractall(ROOT, filter='data')
    print(f'Verified c-toxcore 0.2.23 {target}')
