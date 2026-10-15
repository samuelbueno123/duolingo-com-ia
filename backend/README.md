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

Veja também o [guia de consultas `SELECT` com Java e PostgreSQL](GUIA_SELECT_POSTGRES_JAVA.md), com exemplos de parâmetros `?` para evitar SQL injection.

Para iniciar API e banco em containers:

```powershell
docker compose -f compose.yaml -f docker-compose.yml up --build
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
docker compose -f compose.yaml -f docker-compose.yml up -d --no-build
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
