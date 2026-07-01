<!-- Descreva o que muda e por quê. -->

## O que muda

## Por quê

## Como testei
- [ ] `gradle lintDebug` / `gradle assembleDebug` passam
- [ ] Instalei o APK e o stream em `tcp://<PHONE_IP>:6000` funciona (`ffplay -f s16le -ar 16000 -ch_layout mono -i tcp://<PHONE_IP>:6000`)
- [ ] Sobrevive a desconectar/reconectar cliente

## Checklist
- [ ] Sem segredos/IPs pessoais no diff
- [ ] Docs atualizadas se o comportamento mudou
