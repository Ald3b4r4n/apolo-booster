# Changelog

Todas as versões publicadas deste projeto seguem [SemVer](https://semver.org/lang/pt-BR/).
Cada release do GitHub carrega o APK assinado e o `SHA256SUMS.txt`.

## [0.1.0] — 2026-11-09

Primeira versão pública, focada no caminho crítico de áudio.

### Adicionado

- Motor `BoostController` com `LoudnessEnhancer` na sessão global (mix de saída).
- Equalizador com as bandas e a faixa de ganho reais do aparelho + 5 presets.
- `BassBoost` com intensidade de 0 a 100%.
- Dial de boost 100–300% (0 a +20 dB) com presets Difusor, Fone, Bluetooth e Máx.
- Turbo de hardware experimental: sonda passos de volume acima do máximo do sistema.
- Foreground service `mediaPlayback` com notificação, toggle e ação de parar.
- Tela de Diagnóstico copiável (efeitos, bandas, volume, resultado do turbo).
- Tema escuro espacial com fontes Outfit + Manrope, ícone adaptativo vetorial e camada monochrome.
- Layout responsivo: bottom bar no celular, navigation rail em telas largas.
- Testes JVM de `GainMath` e `EqPresets`.
- CI de build e pipeline de release assinado por tag.
