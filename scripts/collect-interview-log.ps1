<#
면접 수동 테스트 하네스(interview-harness) 세션 종료 후, logs/app*.log 에서 특정 sessionId 관련
로그 줄만 걸러 logs/interview-{sessionId}.log 로 뽑아낸다. local 프로파일로 앱을 띄운 상태에서만 의미가 있다
(logback-spring.xml 의 local 전용 FILE appender가 logs/app.log 를 남긴다).

사용법: ./scripts/collect-interview-log.ps1 -SessionId 42
#>
param(
    [Parameter(Mandatory = $true)]
    [int]$SessionId,

    [string]$LogDir = "logs"
)

$files = Get-ChildItem -Path $LogDir -Filter "app*.log" -File -ErrorAction SilentlyContinue | Sort-Object Name
if (-not $files) {
    Write-Error "로그 파일을 찾을 수 없어요: $LogDir/app*.log (local 프로파일로 앱을 띄웠는지 확인해주세요)"
    exit 1
}

$pattern = "sessionId=$SessionId(?!\d)"
$matches = $files | Get-Content -Encoding utf8 | Select-String -Pattern $pattern

if (-not $matches) {
    Write-Warning "sessionId=$SessionId 로 매칭되는 로그가 없어요."
    exit 0
}

$outFile = Join-Path $LogDir "interview-$SessionId.log"
$matches | ForEach-Object { $_.Line } | Set-Content -Encoding utf8 -Path $outFile

Write-Host "저장됨: $outFile ($($matches.Count)줄)"
