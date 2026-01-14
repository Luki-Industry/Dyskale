# Script pour installer l'API Hytale en local
# Ce script copie HytaleServer.jar et l'installe dans le repo Maven local

param(
    [string]$ServerJarPath = ""
)

Write-Host "Installation de l'API Hytale..." -ForegroundColor Cyan

# Si aucun chemin n'est fourni, chercher dans les emplacements standards
if ([string]::IsNullOrEmpty($ServerJarPath)) {
    $possiblePaths = @(
        "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar",
        ".\HytaleServer.jar",
        "..\HytaleServer.jar"
    )
    
    foreach ($path in $possiblePaths) {
        if (Test-Path $path) {
            $ServerJarPath = $path
            break
        }
    }
}

# Demander le chemin si toujours pas trouvé
if ([string]::IsNullOrEmpty($ServerJarPath) -or -not (Test-Path $ServerJarPath)) {
    Write-Host "`nHytaleServer.jar non trouvé automatiquement." -ForegroundColor Yellow
    Write-Host "`nOptions:" -ForegroundColor Cyan
    Write-Host "1. Copiez HytaleServer.jar de votre serveur dans ce dossier"
    Write-Host "2. Puis relancez: .\setup-hytale-api.ps1"
    Write-Host "3. Ou spécifiez le chemin: .\setup-hytale-api.ps1 -ServerJarPath 'C:\chemin\vers\HytaleServer.jar'"
    exit 1
}

Write-Host "Serveur trouvé: $ServerJarPath" -ForegroundColor Green

# Installer dans le repo Maven local
Write-Host "Installation dans le repository Maven local..." -ForegroundColor Cyan

$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"

& ".\mvnw.cmd" install:install-file "-Dfile=$ServerJarPath" "-DgroupId=com.hypixel.hytale" "-DartifactId=Server" "-Dversion=1.0.0" "-Dpackaging=jar" "-DgeneratePom=true"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nAPI Hytale installée avec succès!" -ForegroundColor Green
    Write-Host "Vous pouvez maintenant compiler le projet avec:" -ForegroundColor Cyan
    Write-Host "  $env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'; .\mvnw.cmd clean package" -ForegroundColor Yellow
} else {
    Write-Host "`nErreur lors de l'installation" -ForegroundColor Red
    exit 1
}
