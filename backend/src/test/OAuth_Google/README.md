# Teste manual: Login com Google

Este diretório contém uma página estática que obtém um **Google ID Token** no navegador e o envia para o backend em `POST /api/auth/google`.

## URLs locais obrigatórias

Inicie o backend na pasta `backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Ele ficará disponível em `http://localhost:8080`.

Depois sirva esta pasta com um servidor HTTP, por exemplo a extensão Live Server do VS Code. Use uma destas origens já liberadas no backend:

- `http://localhost:5500` (recomendado)
- `http://127.0.0.1:5500`
- `http://localhost:5501`
- `http://127.0.0.1:5501`

Não abra o `index.html` pelo endereço `file://`.

## Configuração no Google Cloud

No cliente OAuth 2.0 do tipo **Aplicativo da Web** cujo ID está em `config.js`, cadastre em **Authorized JavaScript origins** a origem exata usada para abrir esta página. Para a configuração recomendada, cadastre:

```
http://localhost:5500
```

Se também usar `127.0.0.1` ou a porta 5501, cadastre cada origem separadamente. Uma origem tem esquema, host e porta; não inclua caminho, barra final ou `http://localhost:8080` nessa lista.

Este fluxo usa Google Identity Services no navegador e retorna o token pelo callback, portanto não exige uma **Authorized redirect URI**. Só será necessária uma redirect URI se você trocar para o fluxo de redirecionamento/authorization-code.

## Verificação

Abra a URL do servidor estático, clique em **Sign in with Google** e conclua o login. A página deve mostrar nome, email, identificador Google e foto. O backend valida assinatura, expiração e `aud` do ID Token antes de responder.

## Segurança

O arquivo `client_secret_*.json` não é usado por este teste. Não publique esse arquivo: se a chave chegou a um repositório ou foi compartilhada, revogue/rotacione o segredo no Google Cloud Console.
