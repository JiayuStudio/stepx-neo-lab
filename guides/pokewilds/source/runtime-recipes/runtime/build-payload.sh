#!/bin/sh
set -eu
runtime_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec python3 "$runtime_dir/payload_tool.py" build "$@"
