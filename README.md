# MicMini — microfone do Android → PCM cru por TCP (baixa latência)

[![build](https://github.com/Apethor/android-micmini/actions/workflows/build.yml/badge.svg)](https://github.com/Apethor/android-micmini/actions/workflows/build.yml)
[![license: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![min SDK 23](https://img.shields.io/badge/minSdk-23%20(Android%206.0)-blue.svg)](#)

App Android mínimo que captura o microfone e serve **PCM cru por TCP**, com o **celular como
servidor**. Feito para o Nubia M2 (NX551J, **Android 6.0**, `minSdk 23`) alimentar um programa
próprio no **Orange Pi 5B / Batocera** (ARM64) — o consumidor só faz `connect(ip, 6000)` e lê bytes.

Motivação: WO Mic (50 ms, ótimo) só tem client **x86** no Linux; não roda no ARM64 do Batocera.
IP Webcam é HTTP (~500 ms). MicMini entrega **PCM cru por socket** (dezenas de ms) que qualquer
linguagem lê direto, sem instalar nada no Batocera.

## Formato do stream
- **PCM signed 16-bit little-endian, mono, 16000 Hz** (constantes em `MicService.java`).
- Porta TCP **6000**. Celular = servidor; grava só enquanto há cliente conectado.
- Trocar p/ 48 kHz: mudar `SAMPLE_RATE` em `MicService.java` (e no receptor).

## Baixar / Build
- **APK pronto:** ver **[Releases](https://github.com/Apethor/android-micmini/releases)**.
- **Do zero (sem Android SDK local):** cada push dispara o workflow `build-apk` (GitHub Actions) →
  baixe o artifact `micmini-apk`, ou rode manualmente em *Actions → build-apk → Run workflow*.

## Instalar e rodar no celular (headless, via adb)
```bash
adb install -r app-debug.apk
adb shell pm grant com.nubia.micmini android.permission.RECORD_AUDIO   # concede o mic sem UI
adb shell monkey -p com.nubia.micmini -c android.intent.category.LAUNCHER 1   # inicia o serviço
# opcional: conferir que está escutando
adb shell "cat /proc/net/tcp | grep 1770"    # 0x1770 = 6000
```
- **Foreground service:** segue rodando com a tela apagada.
- **Auto-start no boot:** o app tem um `BootReceiver` e sobe sozinho após reiniciar. Em ROMs que
  bloqueiam auto-start (ex.: Nubia), libere o app na whitelist: *Ajustes → Apps → ⋮ → Autostart
  management*. Diferencial vs WO Mic/IP Webcam grátis (nos quais isso é recurso Pago).

## Consumir (Orange Pi 5B / Batocera)
Só precisa alcançar o IP do celular na LAN. Teste rápido com ffmpeg (lê `tcp://` direto):
```bash
ffplay -f s16le -ar 16000 -ch_layout mono -i tcp://192.168.15.50:6000   # ouvir (ffplay usa -ch_layout)
ffmpeg -f s16le -ar 16000 -ac 1     -i tcp://192.168.15.50:6000 out.wav # gravar (ffmpeg usa -ac)
```
Do seu programa: ver [`receiver_example.py`](receiver_example.py) (Python puro, ~15 linhas). Em
C/qualquer linguagem é o mesmo: abrir socket TCP p/ `192.168.15.50:6000` e ler PCM 16-bit LE mono 16 kHz.

Autostart no Batocera: colocar seu consumidor em `/userdata/system/scripts/` (shebang + LF).

## ⚠️ Segurança / privacidade
O servidor **não tem autenticação** e escuta em **todas as interfaces** (`0.0.0.0:6000`). Enquanto o
serviço estiver rodando, **qualquer dispositivo na mesma rede pode conectar e ouvir o microfone**.
Use **apenas em rede local confiável** (o caso de uso é um appliance numa LAN isolada). Para reduzir
exposição: rode numa Wi-Fi isolada / com *client isolation*, ou pare o serviço quando não estiver
usando. *(Roadmap: opção de bind só em localhost + token de acesso.)*

## Licença
[MIT](LICENSE) © 2026 Guilherme Nicolino ([Apethor](https://github.com/Apethor)).
