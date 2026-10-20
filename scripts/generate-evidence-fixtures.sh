#!/usr/bin/env sh
set -eu

output_dir="${1:-$(CDPATH= cd -- "$(dirname -- "$0")/../tests/fixtures/pilot/evidence" && pwd)/generated}"
mkdir -p "$output_dir"
printf 'SEC-UNL synthetic evidence fixture\nNo institutional data.\n' > "$output_dir/synthetic-evidence.txt"
python3 -c 'import pathlib,sys; p=pathlib.Path(sys.argv[1]); n=int(sys.argv[2]); b=bytes(i % 251 for i in range(65536)); f=p.open("wb"); q,r=divmod(n,len(b)); [f.write(b) for _ in range(q)]; f.write(b[:r]); f.close()' "$output_dir/exact-25-mib.bin" 26214400
python3 -c 'import pathlib,sys; p=pathlib.Path(sys.argv[1]); n=int(sys.argv[2]); b=bytes(i % 251 for i in range(65536)); f=p.open("wb"); q,r=divmod(n,len(b)); [f.write(b) for _ in range(q)]; f.write(b[:r]); f.close()' "$output_dir/over-25-mib.bin" 26214401
printf 'synthetic plain text intentionally declared as PDF' > "$output_dir/declared-pdf-mismatch.pdf"
sha256sum "$output_dir"/*
