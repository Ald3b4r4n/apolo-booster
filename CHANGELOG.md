# Changelog

Todas as versões publicadas deste projeto seguem [SemVer](https://semver.org/lang/pt-BR/).
Cada release do GitHub carrega o APK assinado e o `SHA256SUMS.txt`.

## [0.4.1] — 2026-11-09

### Corrigido

- A aba Equalizador ganhou um controle para ligar o módulo e mostra se o Android habilitou o efeito.

## [0.4.0] — 2026-11-09

### Corrigido

- Ajustar o ganho, o equalizador ou os graves respeita o estado de cada módulo.
- Efeitos com ganho zero ou equalização plana ficam desativados.
- O diagnóstico identifica o ganho alvo e as saídas disponíveis.

## [0.3.0] — 2026-11-09

### Adicionado

- **Módulos independentes**: Loudness, Equalizador e Graves agora têm switches próprios.
  Default: só Loudness ligado; EQ e BassBoost desligados.
- **Comparação A/B**: botão que desliga todos os efeitos por 5 segundos e religa, para ouvir
  a diferença real sem placebo.

### Corrigido

- Equalizador e BassBoost não são mais habilitados sem uso. Em aparelhos com DSP do fabricante
  (Xiaomi MiSound, Dolby, Dirac), habilitar esses efeitos insere atenuação de headroom e o
  resultado final ficava **mais baixo** que o original.

## [0.2.0] — 2026-11-09

### Adicionado

- **Telemetria de aplicação real** no Diagnóstico: criado, com controle, habilitado, ganho alvo,
  status do `setTargetGain`, efeitos ativos, música ativa e saída atual.
- **Teste de tom** com o mesmo `LoudnessEnhancer`, anexado à sessão de um player interno —
  prova auditiva de que o processamento funciona (ou não) no aparelho.
- Botão **Reconectar efeitos** e reconexão automática quando a música começa a tocar.
- Opção **Volume do sistema no máximo** ao ativar (ligada por padrão).

### Nota técnica

Anexar efeitos à sessão 0 (mix global) está depreciado no Android moderno. Em aparelhos onde o
OEM remove o efeito da cadeia, o objeto é criado mas não processa nada. Esta versão torna isso
visível em vez de silencioso.

## [0.1.1] — 2026-11-09

### Corrigido

- O foreground service reaplica o boost quando o processo é recriado e se encerra quando o
  estado está desligado.
- O turbo de hardware não é mais restaurado como "ligado" sem ter sido aplicado de fato.
- `DynamicsProcessing` só é consultado em API 28+ (guard de lint).
- Locale explícito na formatação das frequências do equalizador.

### Alterado

- Presets e rótulos do boost agora vêm de recursos de string (PT-BR centralizado).
- Removida a dependência não usada de DataStore e recursos órfãos.

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
