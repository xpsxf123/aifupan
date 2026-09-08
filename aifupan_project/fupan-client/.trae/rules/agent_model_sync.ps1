param(
  [ValidateSet('root_to_rules', 'rules_to_root')]
  [string]$Direction = 'root_to_rules'
)

$ErrorActionPreference = 'Stop'

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$sourceRoot = Join-Path $projectRoot 'agent'
$rulesRoot = Join-Path $projectRoot '.trae\rules'

$map = @(
  @{ Source = 'AGENT.md';      Target = 'frontend_agent__AGENT.md' },
  @{ Source = 'INDEX.md';      Target = 'frontend_agent__INDEX.md' },
  @{ Source = 'MEMORY.md';     Target = 'frontend_agent__MEMORY.md' },
  @{ Source = 'POLICIES.md';   Target = 'frontend_agent__POLICIES.md' },
  @{ Source = 'TOOLS.md';      Target = 'frontend_agent__TOOLS.md' },
  @{ Source = 'CHANGE_LOG.md'; Target = 'frontend_agent__CHANGE_LOG.md' },
  @{ Source = 'DREAMS.md';     Target = 'frontend_agent__DREAMS.md' }
)

function Copy-File([string]$from, [string]$to) {
  $fromPath = Resolve-Path $from
  $toDir = Split-Path $to -Parent
  if (!(Test-Path $toDir)) {
    New-Item -ItemType Directory -Path $toDir | Out-Null
  }
  Copy-Item -Path $fromPath -Destination $to -Force
}

if ($Direction -eq 'root_to_rules') {
  foreach ($item in $map) {
    Copy-File (Join-Path $sourceRoot $item.Source) (Join-Path $rulesRoot $item.Target)
  }
  exit 0
}

if ($Direction -eq 'rules_to_root') {
  foreach ($item in $map) {
    Copy-File (Join-Path $rulesRoot $item.Target) (Join-Path $sourceRoot $item.Source)
  }
  exit 0
}

