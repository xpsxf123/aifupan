if (-not (Get-Command Set-MpPreference -ErrorAction SilentlyContinue)) {
    Write-Warning "Set-MpPreference not found, skipping Defender configuration."
    exit 0
}

$settings = @(
    @{ Name = "DisableRealtimeMonitoring"; Param = @{ DisableRealtimeMonitoring = $true } },
    @{ Name = "DisableBehaviorMonitoring"; Param = @{ DisableBehaviorMonitoring = $true } },
    @{ Name = "DisableIOAVProtection";     Param = @{ DisableIOAVProtection     = $true } }
)

foreach ($s in $settings) {
    try {
        Set-MpPreference @($s.Param) -ErrorAction Stop
        Write-Host "$($s.Name) disabled."
    }
    catch {
        Write-Warning "$($s.Name) failed (Defender unavailable or protected): $($_.Exception.Message)"
    }
}
