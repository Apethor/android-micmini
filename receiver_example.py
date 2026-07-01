#!/usr/bin/env python3
"""
Receptor de referencia do MicMini (roda no Orange Pi 5B / Batocera — so precisa de Python).
Le PCM cru do celular via TCP e entrega ao SEU processamento.

Formato do stream: PCM signed 16-bit LITTLE-ENDIAN, mono, 16000 Hz.
"""
import socket

HOST = "192.168.15.50"   # IP fixo do celular
PORT = 6000
RATE = 16000             # Hz  (bate com MicService.SAMPLE_RATE)

def main():
    s = socket.create_connection((HOST, PORT))
    s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    print(f"conectado a {HOST}:{PORT}  ({RATE} Hz mono s16le)")
    try:
        while True:
            data = s.recv(3200)          # ~100 ms de audio (16000*2*0.1)
            if not data:
                break
            # >>> SEU PROCESSAMENTO AQUI <<<
            # data = bytes PCM 16-bit LE mono 16kHz. Ex.: converter p/ numpy:
            #   import numpy as np; x = np.frombuffer(data, dtype='<i2')
            print(f"\rrecebidos {len(data)} bytes", end="", flush=True)
    finally:
        s.close()

if __name__ == "__main__":
    main()
