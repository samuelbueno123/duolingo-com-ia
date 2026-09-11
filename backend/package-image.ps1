param(
    [string]$ImageName = "duolingo-ia-backend:latest",
    [string]$TarFile = "duolingo-ia-backend.tar"
)

$ErrorActionPreference = "Stop"

Write-Host "Building image $ImageName..."
docker build -t $ImageName .

Write-Host "Saving image to $TarFile..."
docker save -o $TarFile $ImageName

Write-Host "Done. Share the file $TarFile with the other machine."