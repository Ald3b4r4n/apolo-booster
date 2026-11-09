# Arquitetura

## Visão em uma frase

Uma UI Compose observa um controlador singleton que é o **dono único** dos `AudioEffect`s do
processo; um foreground service mantém esse processo vivo; o estado é espelhado em prefs para
sobreviver à morte do processo.

## Camadas

```
ui/            Compose + Material 3 (sem ViewModel: o controlador expõe StateFlow)
   │
audio/         BoostController, GainMath, EffectProbe, VolumeProbe, EqPresets
   │
service/       BoostService (foreground mediaPlayback)
   │
Android        AudioManager + android.media.audiofx (sessão 0)
```

### `audio/GainMath`

Matemática pura, sem Android, coberta por testes JVM:

- percentual → dB: `(percent - 100) / 10` (100→0 dB, 300→+20 dB);
- dB → milibels (`×100`);
- clamp de dispositivo;
- conversão de −12..+12 dB para `Short` no range nativo do equalizador;
- fração de volume segura contra `max == 0`.

### `audio/EffectProbe`

Cria e **libera imediatamente** cada efeito para descobrir o que o aparelho aceita:
`LoudnessEnhancer(0)`, `Equalizer(0, 0)` e `BassBoost(0, 0)`. Também lê `numberOfBands`,
`bandLevelRange` e as frequências centrais. `DynamicsProcessing` é detectado apenas por
descritor (`AudioEffect.queryEffects`), sem instanciar.

Consequência de projeto: nenhum caminho do app assume que um efeito existe. Tudo é
`runCatching` + fallback visível no Diagnóstico.

### `audio/BoostController`

Singleton inicializado em `ApoloApplication`. Responsabilidades:

- `StateFlow<BoostState>` como única fonte de verdade da UI;
- aplicar/liberar efeitos;
- persistir estado em `SharedPreferences` (síncrono e determinístico — não há corrida com
  o ciclo de vida do serviço);
- gerar o relatório de diagnóstico;
- sondar o turbo de hardware.

Decisão: **não** usar DataStore. O motor precisa ler o estado de forma síncrona no
`onStartCommand` do serviço; um fluxo assíncrono criaria janela para aplicar ganho errado.

### `audio/VolumeProbe`

Tenta `setStreamVolume(STREAM_MUSIC, max + 1, 0)` e lê de volta. A maioria dos aparelhos
recusa ou faz clamp silencioso; quando aceita, o ganho é de hardware. Ao desligar o turbo,
o volume anterior é restaurado.

### `service/BoostService`

- `foregroundServiceType="mediaPlayback"`, notificação `IMPORTANCE_LOW` sem som;
- ações: alternar e parar;
- observa o `StateFlow` e se encerra quando o boost é desligado;
- `startForeground` antes de qualquer trabalho, com fallback para `stopSelf()` se o sistema
  recusar (Android 12+).

### `ui`

- `ApoloApp`: shell responsivo — `NavigationBar` em telas compactas, `NavigationRail` a partir
  de 720 dp.
- `HomeScreen`: dial em `Canvas` (arco de 270°, ticks, glow em duas camadas), slider, presets,
  volume do sistema, turbo e aviso de segurança a partir de 200%.
- `EqualizerScreen`: bandas reais do aparelho; presets interpolados por posição.
- `DiagnosticsScreen`: mesmos dados do relatório de texto, com botão de copiar.
- Tema: `darkColorScheme` próprio + tipografia Outfit/Manrope como fontes variáveis.

## Fluxo de ativação

1. Usuário toca em **Ativar boost**.
2. Se Android 13+, o app pede `POST_NOTIFICATIONS` (a recusa não impede o boost).
3. `BoostService.start()` → `startForegroundService`.
4. `BoostController.setEnabled(true)` → cria efeitos, aplica ganho/EQ/bass.
5. Serviço entra em foreground e publica a notificação.
6. Ao desligar: efeitos são liberados e o serviço para.

## Limitações conhecidas

| Limitação | Por quê |
|---|---|
| Efeito global pode não pegar todos os apps | depende do OEM e do mix; sessão 0 é o caminho suportado |
| Não existe limiter próprio no caminho global | exigiria inserir DSP no mix, o que só `DynamicsProcessing` (quando suportado) permite |
| Turbo costuma ser recusado | a maioria dos aparelhos não expõe passos acima do máximo |
| Distorção em ganho alto | ganho digital em material já masterizado em 0 dBFS estoura; o framework comprime |
| Atualização depende da mesma keystore | regra do Android para atualizar por cima |

## Testes

- JVM: `GainMathTest`, `EqPresetsTest` (curva, clamps, ranges).
- Sem testes instrumentados e sem emulador por decisão de projeto: a validação de áudio é
  humana, no aparelho, com o relatório de Diagnóstico como evidência.
