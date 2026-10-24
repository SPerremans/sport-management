param([switch]$Refresh)
$ErrorActionPreference = 'Stop'
$snapshotRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$snapshotBuilder = Join-Path $PSScriptRoot 'build-competition-snapshot.py'
$snapshotQueue = Join-Path $snapshotRoot 'deployment/tmp/competition-fetch-queue.json'
$snapshotSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()

# Public browsing with necessary cookies only; no sign-in, personal contacts, or API secrets.
Invoke-WebRequest -Uri 'https://www.toernooi.nl/sport/league?id=7c182df5-153d-443a-86db-c21a50e46372' -WebSession $snapshotSession | Out-Null
Invoke-WebRequest -Uri 'https://www.toernooi.nl/cookiewall/Save' -Method Post -WebSession $snapshotSession -Body @{
    ReturnUrl='/sport/league?id=7c182df5-153d-443a-86db-c21a50e46372'
    SettingsOpen='true'
    CookiePurposes='1'
} | Out-Null

foreach ($snapshotPhase in @('initial','groups','results')) {
    python -X utf8 $snapshotBuilder --plan $snapshotPhase
    if ($LASTEXITCODE -ne 0) { throw 'Could not plan public snapshot requests.' }
    $snapshotItems = Get-Content -LiteralPath $snapshotQueue -Raw | ConvertFrom-Json
    $snapshotIndex = 0
    foreach ($snapshotItem in $snapshotItems) {
        if ($Refresh -or -not (Test-Path -LiteralPath $snapshotItem.path)) {
            $snapshotResponse = Invoke-WebRequest -Uri $snapshotItem.url -WebSession $snapshotSession
            $snapshotResponse.Content | Set-Content -LiteralPath $snapshotItem.path -Encoding utf8
        }
        $snapshotIndex++
        if (($snapshotIndex % 100) -eq 0) { Write-Output "$snapshotPhase $snapshotIndex/$($snapshotItems.Count)" }
    }
}
# Update CAPTURED and bump PublicCompetitionSamples.FEATURE before publishing a newer capture.
python -X utf8 $snapshotBuilder
if ($LASTEXITCODE -ne 0) { throw 'Public snapshot validation failed.' }
