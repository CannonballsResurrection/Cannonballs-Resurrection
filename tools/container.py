#!/usr/bin/env python3
"""In-process WLD3 container decode wrapper around pywttools/wtextract.py.

decode_file(path) -> bytes   (decoded inner blob: geom / jpeg / etc.)
"""
import os
import sys

_PYWT = '/tmp/cb/WTExtractor/pywttools'
if _PYWT not in sys.path:
    sys.path.insert(0, _PYWT)

import wtextract  # noqa: E402

# wtextract.py only imports `os` inside its __main__ block; when we import it
# as a module the cab-extraction path references a missing `os`. Inject it.
wtextract.os = os


def decode_file(path):
    with open(path, 'rb') as f:
        dec = wtextract.WTDecoder(f)
        data = dec.decode()
    return bytes(data)


if __name__ == '__main__':
    for p in sys.argv[1:]:
        d = decode_file(p)
        sys.stderr.write('%s -> %d bytes, head=%s\n'
                         % (p, len(d), d[:8].hex()))
