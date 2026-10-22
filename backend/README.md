# Backend Docker

## Requisito de Java

O projeto usa Java 25. Para compilar ou executar localmente, instale o JDK 25 e use o Maven Wrapper (`.\mvnw.cmd` no Windows ou `./mvnw` em Linux/macOS). O Dockerfile também usa Java 25 nas etapas de build e execução.

Este backend já está preparado para rodar em Docker e também para ser distribuído como um arquivo `.tar` da imagem.

## PostgreSQL para desenvolvimento local

O PostgreSQL roda em Docker e fica acessível em `localhost:5432`. Para iniciar apenas o banco:

```powershell
docker compose up -d postgres
```

Depois, inicie a API fora do Docker:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS, use `./mvnw spring-boot:run`.

## Acesso, segurança em produção e rotação de segredos

Crie o arquivo local `.env.local` (ignorado pelo Git, veja `.env.example`) e configure `JWT_SECRET` com uma chave aleatória de pelo menos 32 bytes. Defina `APP_ADMIN_EMAILS` com os emails Google verificados que terão acesso administrativo, separados por vírgula. O primeiro login Google desses emails cria ou associa a conta e concede o papel `ADMIN`. O arquivo `.env.local` é carregado ao iniciar pelo Maven e pelo Docker Compose. Não grave segredos no `.env` versionado nem no código do Flutter.

### Validação de Produção (`SPRING_PROFILES_ACTIVE=prod`)

Quando o perfil `prod` ou `production` está ativo (`application-prod.properties`), o `ProductionSecurityValidator` interrompe imediatamente a inicialização caso:
- `JWT_SECRET` esteja ausente, tenha menos de 32 caracteres ou seja igual ao valor padrão de desenvolvimento;
- `POSTGRES_PASSWORD` esteja ausente ou utilize `duolingo_dev_password` / `postgres`;
- `APP_ADMIN_EMAILS` não esteja explicitamente definido;
- `APP_ADMIN_INITIAL_PASSWORD` esteja ausente, tenha menos de 12 caracteres ou seja igual a `duadmin`.

### Procedimento de Rotação de Segredos (Ambientes Implantados)

Caso credenciais padrão ou segredos antigos tenham sido expostos em ambientes já implantados, execute a rotação na seguinte ordem:
1. **Chave JWT (`JWT_SECRET`)**:
   - Gere um novo segredo criptograficamente seguro (`openssl rand -base64 48`).
   - Atualize a variável `JWT_SECRET` no cofre/ambiente do servidor e reinicie a API. Todos os tokens de acesso e `refreshToken` emitidos anteriormente serão invalidados imediatamente, exigindo novo login seguro.
2. **Senha Administrativa (`admin@duolingo.com`)**:
   - Defina `APP_ADMIN_INITIAL_PASSWORD` com uma senha forte (>= 12 caracteres) e altere a senha da conta `admin@duolingo.com` via `PUT /api/auth/me` ou fluxo de redefinição de senha. O `CatalogDataInitializer` preserva hashes BCrypt existentes e nunca sobrescreve a senha em reinicializações.
3. **Banco de Dados (`POSTGRES_PASSWORD`)**:
   - No PostgreSQL, execute `ALTER USER duolingo WITH PASSWORD '<nova_senha_forte>';`.
   - Atualize `POSTGRES_PASSWORD` nas variáveis de ambiente da API e reinicie os containers.
4. **Provedores Externos (`GOOGLE_CLIENT_ID` e `AI_API_KEY`)**:
   - No Google Cloud Console / painel do provedor de IA, revogue as chaves antigas, gere novas credenciais e injete `AI_API_KEY` exclusivamente no backend (nunca no cliente Flutter).

O cadastro de professor continua público, mas a conta fica pendente (`approved: false`) até um administrador aprová-la. Para aprovar ou revogar o acesso, use `PATCH /api/teachers/{id}/approval` com `{"approved": true}` ou `{"approved": false}`. Professores aprovados podem editar apenas o próprio perfil; a exclusão e as demais rotas de gestão exigem `ADMIN`.

Fotos JPG e PNG enviadas pelo perfil são validadas (até 5 MB e 1024x1024), gravadas em `uploads/profile-pictures` fora do banco e servidas pela API. No primeiro login Google, a foto retornada pelo Google é importada para essa pasta quando a conta é criada ou vinculada. Logins seguintes preservam a foto escolhida pelo usuário. No Docker, o diretório fica no volume persistente `profile-pictures` montado na API; o PostgreSQL guarda somente o caminho da imagem.


Veja também o [guia de consultas `SELECT` com Java e PostgreSQL](GUIA_SELECT_POSTGRES_JAVA.md), com exemplos de parâmetros `?` para evitar SQL injection.

O arquivo `compose.yaml` contém a API e o PostgreSQL. Para iniciar os dois serviços:

```powershell
docker compose up --build
```

## Gerar o `.tar`

No diretório `backend/`, rode:

```powershell
.\package-image.ps1
```

Isso vai:

1. Construir a imagem `duolingo-ia-backend:latest`.
2. Gerar o arquivo `duolingo-ia-backend.tar`.

Se quiser trocar o nome:

```powershell
.\package-image.ps1 -ImageName "meu-backend:1.0" -TarFile "meu-backend.tar"
```

## Enviar para outra máquina

Copie o `.tar` para a outra máquina e rode:

```powershell
docker load -i duolingo-ia-backend.tar
docker compose up -d --no-build
```

O serviço PostgreSQL inicia pelo Compose e guarda os dados no volume `postgres-data`.

## Observação importante

O arquivo `.tar` contém apenas a imagem da API. O PostgreSQL roda em serviço separado e persiste os dados no volume `postgres-data`; faça backup desse volume para transferir o banco.

O arquivo H2 `data/duolingo.mv.db` foi mantido, mas os dados não foram migrados automaticamente para PostgreSQL.





# MER (Modelo Entidade Relacionamento)

## Tipos de Relacionamentos

| Entidade A | Cardinalidade | Entidade B |
| :--- | :---: | :--- |
| Institution | 1:N | Teacher |
| Institution | 1:N | School |
| School | 1:N | SchoolClass |
| SchoolClass | 1:N | Enrollment |
| Student | 1:N | Enrollment |
| Student | 1:N | StudentLanguage |
| SchoolClass | 1:N | ClassTeacherAssignment |
| Teacher | 1:N | ClassTeacherAssignment |
| Teacher | 1:N | TeacherDocument |
| User | 1:N | Student |
| User | 1:N | Teacher |
