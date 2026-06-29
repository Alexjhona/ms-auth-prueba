param(
    [string]$WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path,
    [switch]$FailOnWarn,
    [switch]$SkipAvailabilityCheck,
    [switch]$DryRun
)

$runner = Join-Path $WorkspaceRoot "security\owasp-zap\run-zap-scan.ps1"
& $runner -Target "ms-auth-service" -FailOnWarn:$FailOnWarn -SkipAvailabilityCheck:$SkipAvailabilityCheck -DryRun:$DryRun
exit $LASTEXITCODE
