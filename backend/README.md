# Backend Docker

Este backend já está preparado para rodar em Docker e também para ser distribuído como um arquivo `.tar` da imagem.

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
docker run -d --name duolingo-api -p 8080:8080 -v duolingo-data:/app/data duolingo-ia-backend:latest
```

Se quiser usar `docker compose` depois do `docker load`, o jeito mais simples é rodar `docker compose up -d --no-build` depois de configurar o `docker-compose.yml` para usar a imagem carregada.

## Observação importante

O `.tar` carrega a imagem da aplicação, mas não inclui automaticamente os dados do volume `duolingo-data`. Se você quiser distribuir também o banco H2 já preenchido, o ideal é copiar a pasta de dados junto ou fazer um backup separado do volume.