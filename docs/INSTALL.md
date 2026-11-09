# Instalação

## Requisitos

- Android 8.0 (API 26) ou superior.
- ~3 MB de espaço livre.
- Nenhuma conta, nenhum login.

## Passo a passo

1. Abra <https://github.com/Ald3b4r4n/apolo-booster/releases/latest>.
2. Baixe `ApoloBooster-<versão>.apk`.
3. Opcional: confira a integridade com o `SHA256SUMS.txt` do mesmo release.

   ```powershell
   Get-FileHash .\ApoloBooster-0.1.0.apk -Algorithm SHA256
   ```

4. Abra o arquivo baixado. O Android vai pedir permissão para instalar de fontes desconhecidas
   para o app que abriu o APK (navegador ou gerenciador de arquivos). Autorize apenas para
   aquela ação.
5. Instale e abra.

## Atualização

- Baixe o APK novo e instale **por cima**. Não é preciso desinstalar.
- Todas as builds são assinadas com a mesma chave. Se o Android reclamar de assinatura
  diferente, desinstale a versão antiga uma única vez (os dados de boost são recriados).

## Permissões

| Permissão | Para quê |
|---|---|
| `MODIFY_AUDIO_SETTINGS` | aplicar efeitos de áudio |
| `FOREGROUND_SERVICE` + `..._MEDIA_PLAYBACK` | manter o boost vivo fora da tela |
| `POST_NOTIFICATIONS` (Android 13+) | mostrar a notificação do serviço |

O app **não** pede `INTERNET`, armazenamento, localização, microfone ou acessibilidade.

## Desinstalar

Desative o boost antes de desinstalar (a notificação tem a ação **Parar**). Se desinstalar com
o serviço ativo, o Android encerra o processo e os efeitos caem junto.

## Problemas comuns

**"App não instalado"**
Pode ser assinatura de uma versão anterior diferente. Desinstale a antiga e instale de novo.

**"Configuração bloqueada por política"**
Um perfil corporativo pode bloquear fontes desconhecidas. Instale em um perfil pessoal.

**Boost não muda nada**
Abra **Diagnóstico**: se `LoudnessEnhancer` estiver indisponível, o OEM bloqueou o efeito
global. Copie o relatório e abra uma issue.
