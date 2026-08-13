# Frontend Flutter Web

Tela Flutter Web para autenticação com Google. O botão oficial do Google retorna um ID Token, que é validado pelo backend em `POST /api/auth/google`.

## Executar localmente

Em um terminal, inicie o backend:

```powershell
cd ..\backend
.\mvnw.cmd spring-boot:run
```

Em outro terminal, inicie o Flutter usando uma porta fixa:

```powershell
cd ..\frontend
flutter run -d chrome --web-hostname localhost --web-port 5500
```

O frontend abre em `http://localhost:5500` e chama o backend em `http://localhost:8080`.

Para apontar para outro backend, use `--dart-define`:

```powershell
flutter run -d chrome --web-hostname localhost --web-port 5500 --dart-define=BACKEND_URL=https://api.seudominio.com
```

## Google Cloud Console

No mesmo cliente OAuth 2.0 do tipo **Aplicativo da Web** cujo Client ID está em `web/index.html`, adicione em **Authorized JavaScript origins**:

```text
http://localhost
http://localhost:5500
```

Se executar em `127.0.0.1`, cadastre também `http://127.0.0.1:5500` e mantenha essa origem liberada em `backend/src/main/java/com/duolingo/ia/proj/config/CorsConfig.java`.

Este fluxo não requer *Authorized redirect URI*, pois usa o botão Google Identity Services com callback no navegador. Não use `file://`.
