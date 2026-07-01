#!/usr/bin/env python3
"""
Reference MicMini client (runs anywhere with Python — e.g. a Raspberry/Orange Pi).
Reads raw PCM from the phone over TCP and hands each chunk to YOUR processing.

Stream format: PCM signed 16-bit LITTLE-ENDIAN, mono, 16000 Hz (no header).

Usage:
    python3 receiver_example.py <PHONE_IP> [PORT]
    # pipe straight into a player:
    python3 receiver_example.py 192.168.1.50 | ffplay -f s16le -ar 16000 -ch_layout mono -i -
"""
import socket
import sys

DEFAULT_PORT = 6000
RATE = 16000   # Hz — must match MicService.SAMPLE_RATE


def main():
    host = sys.argv[1] if len(sys.argv) > 1 else "<PHONE_IP>"
    port = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_PORT
    if host == "<PHONE_IP>":
        sys.exit("usage: receiver_example.py <PHONE_IP> [PORT]")

    s = socket.create_connection((host, port))
    s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    print(f"connected to {host}:{port}  ({RATE} Hz mono s16le)", file=sys.stderr)
    try:
        while True:
            data = s.recv(4096)          # up to 4096 bytes of PCM (16-bit LE mono 16 kHz)
            if not data:
                break
            # >>> YOUR PROCESSING HERE <<<
            # e.g. into NumPy:  import numpy as np; x = np.frombuffer(data, dtype='<i2')
            # or just forward it downstream:
            sys.stdout.buffer.write(data)
            sys.stdout.buffer.flush()
    except KeyboardInterrupt:
        pass
    finally:
        s.close()


if __name__ == "__main__":
    main()
