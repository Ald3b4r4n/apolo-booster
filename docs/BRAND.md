# Marca

## Nome

**Apolo Booster** — referência ao programa Apollo: potência controlada, instrumentação e
um toque de exploração. O app mede, mostra e só então amplifica.

## Paleta

| Token | Hex | Uso |
|---|---|---|
| Navy | `#070B1A` | fundo global |
| Surface Deep | `#101A33` | superfícies e cards |
| Surface Raised | `#17223D` | cards elevados e trilhas |
| Gold | `#FFB020` | acento primário, dial, valores positivos |
| Ember | `#FF6B35` | acento secundário, estado ativo, gradiente |
| Text Primary | `#F5F7FB` | texto principal |
| Text Muted | `#9AA6BF` | texto secundário |
| Success | `#3DDC97` | status ativo, efeito disponível |
| Caution | `#FFC24B` | aviso de segurança |
| Danger | `#FF5C5C` | erro |
| Outline | `#2A3554` | bordas e trilhas |

Gradiente do dial: `Ember → Gold → Gold → Ember` em `sweepGradient`.

## Tipografia

- **Outfit** (OFL) — display, números do dial, títulos.
- **Manrope** (OFL) — corpo de texto, labels, diagnóstico.

Ambas entram como **fontes variáveis** (`res/font/outfit_variable.ttf`,
`res/font/manrope_variable.ttf`) e são instanciadas com `FontVariation.weight(...)` para
400/500/600/700. Isso mantém o APK pequeno e a escala de peso correta.

## Ícone

- Estrutura: **adaptive icon** (`mipmap-anydpi-v26/ic_launcher.xml`) com `background`,
  `foreground` e `monochrome`.
- Marca: monograma **A** desenhado com dois traços grossos (perna esquerda, perna direita e
  travessa em Ember), sobre um **anel orbital** inclinado a −18°.
- Safe zone: conteúdo dentro de 72×72 do viewport de 108×108.
- Fundo: navy sólido com dois halos concêntricos a 6% e 10% de alfa.
- `monochrome`: mesma geometria em branco puro, para ícones temáticos do Android 13+.

### Arquivos

| Arquivo | Papel |
|---|---|
| `docs/assets/brand/icon-1024.png` | arte conceitual aprovada (referência) |
| `docs/assets/brand/icon-512.png` | divulgação e lojas |
| `docs/assets/brand/banner.png` | topo do README (2:1) |
| `docs/assets/screens/ui-concept.png` | direção visual das telas |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | implementação vetorial |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | camada temática |

## Regras

- Não aplicar o ícone sobre fundo claro nem adicionar texto dentro dele.
- O gradiente do "A" vai do dourado no topo para o ember na base; não inverter.
- O anel orbital fica **atrás** do monograma na versão final vetorial.
- Em peças de marketing, usar o banner e a tagline como texto real, nunca rasterizada.

## Voz

Direta e técnica, sem promessa milagrosa. "Amplitude além do padrão" é a assinatura; o app
explica o que é ganho digital e o que o hardware não faz.
