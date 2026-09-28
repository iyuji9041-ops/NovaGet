#!/usr/bin/env python3
import sys
import os
import ssl
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

def get_file_size(url, ctx):
    req = urllib.request.Request(url, method='HEAD')
    with urllib.request.urlopen(req, context=ctx) as resp:
        return int(resp.headers.get('Content-Length', 0))

def download_chunk(url, start, end, part_path, chunk_idx, ctx):
    req = urllib.request.Request(url, headers={'Range': f'bytes={start}-{end}'})
    with urllib.request.urlopen(req, context=ctx) as resp:
        with open(part_path, 'wb') as f:
            while True:
                buf = resp.read(65536)
                if not buf:
                    break
                f.write(buf)
    print(f"Chunk {chunk_idx} finished", flush=True)

def parallel_download(url, out_path, num_chunks=8):
    ctx = ssl._create_unverified_context()
    total_size = get_file_size(url, ctx)
    print(f"Downloading {out_path} ({total_size / (1024*1024):.2f} MB) in {num_chunks} parallel chunks...", flush=True)
    if total_size <= 0:
        raise ValueError("Invalid total size")

    chunk_size = total_size // num_chunks
    futures = []
    part_files = []
    t0 = time.time()
    with ThreadPoolExecutor(max_workers=num_chunks) as executor:
        for i in range(num_chunks):
            start = i * chunk_size
            end = total_size - 1 if i == num_chunks - 1 else (i + 1) * chunk_size - 1
            part_path = f"{out_path}.part{i}"
            part_files.append(part_path)
            futures.append(executor.submit(download_chunk, url, start, end, part_path, i, ctx))

        for f in futures:
            f.result()

    elapsed = time.time() - t0
    speed = (total_size / (1024*1024)) / elapsed if elapsed > 0 else 0
    print(f"All chunks downloaded in {elapsed:.1f}s ({speed:.2f} MB/s). Merging...", flush=True)

    tmp_out = f"{out_path}.tmp"
    with open(tmp_out, 'wb') as outfile:
        for part_path in part_files:
            with open(part_path, 'rb') as infile:
                while True:
                    buf = infile.read(1048576)
                    if not buf:
                        break
                    outfile.write(buf)
            os.remove(part_path)

    os.rename(tmp_out, out_path)
    print(f"Successfully saved {out_path} ({os.path.getsize(out_path)} bytes)", flush=True)

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: fast_download.py <url> <out_path> [num_chunks]")
        sys.exit(1)
    url = sys.argv[1]
    out_path = sys.argv[2]
    chunks = int(sys.argv[3]) if len(sys.argv) > 3 else 8
    parallel_download(url, out_path, chunks)
