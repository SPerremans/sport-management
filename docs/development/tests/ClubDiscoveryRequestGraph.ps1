param(
    [string]$MicroflowsPath = (Join-Path $PSScriptRoot '../../../deployment/model/microflows.json')
)

$ErrorActionPreference = 'Stop'
$metadata = Get-Content -LiteralPath $MicroflowsPath -Raw | ConvertFrom-Json
$checked = 0
foreach ($workflow in @('OCH_ClubDiscovery_Refresh', 'DS_ClubDiscovery_Results', 'DS_ClubDiscovery_Choices')) {
    $name = 'MyFirstModule.' + $workflow
    $property = $metadata.PSObject.Properties[$name]
    if ($null -eq $property) { throw "Build/export is missing $name." }
    foreach ($kind in @('Cities', 'Sports', 'Ages', 'Levels')) {
        $association = 'MyFirstModule.ClubDiscoverySearch_' + $kind
        $matches = @($property.Value.a | Where-Object {
            $_[0] -eq $association -and $_[1] -eq 'MyFirstModule.ClubDiscoverySearch'
        })
        if ($matches.Count -eq 0) {
            throw "$name omits $association from its client request graph; current selections may not reach the server."
        }
        $checked++
    }
}
Write-Output "PASS: $checked club discovery selection request-graph checks (export contract only)."
