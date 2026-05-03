#Requires -Version 5.1
# UTF-8: bump app versionCode (+1) and versionName (patch +1), commit, push branch + tag v* -> GitHub Actions.
# Uses max(gradle, latest semver tag on origin after fetch) so local gradle can lag behind GitHub Releases.
param(
    [string] $Tag = "",
    [int] $VersionCode = 0,
    [switch] $SkipGit,
    [switch] $LocalOnly,
    [switch] $PullRebase
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$gradleRel = "app/build.gradle.kts"
$gradlePath = Join-Path $root $gradleRel

Set-Location $root

if (-not (Test-Path $gradlePath)) {
    Write-Error "Missing $gradlePath. Run from repo (scripts folder)."
    exit 1
}

if ($PullRebase -and $SkipGit) {
    Write-Error "-PullRebase cannot be used with -SkipGit."
    exit 1
}

function Read-Trim([string] $prompt) {
    $s = Read-Host $prompt
    if ($null -eq $s) { return "" }
    return $s.Trim()
}

function Bump-VersionPatch([string] $semVer) {
    $s = $semVer.Trim()
    if ($s -match '^(\d+)\.(\d+)\.(\d+)') {
        $maj = [int]$Matches[1]
        $min = [int]$Matches[2]
        $pat = [int]$Matches[3]
        return "$maj.$min.$($pat + 1)"
    }
    return $null
}

function Parse-SemVerTriple([string] $semVer) {
    $s = $semVer.Trim()
    if ($s -notmatch '^(\d+)\.(\d+)\.(\d+)$') { return $null }
    return @{ Maj = [int]$Matches[1]; Min = [int]$Matches[2]; Pat = [int]$Matches[3] }
}

# -1 if a < b, 0 if equal, 1 if a > b; -2 if either invalid
function Compare-SemVer([string] $a, [string] $b) {
    $pa = Parse-SemVerTriple $a
    $pb = Parse-SemVerTriple $b
    if ($null -eq $pa -or $null -eq $pb) { return -2 }
    if ($pa.Maj -ne $pb.Maj) { return [Math]::Sign($pa.Maj - $pb.Maj) }
    if ($pa.Min -ne $pb.Min) { return [Math]::Sign($pa.Min - $pb.Min) }
    return [Math]::Sign($pa.Pat - $pb.Pat)
}

function SemVer-Max([string] $a, [string] $b) {
    $c = Compare-SemVer $a $b
    if ($c -eq -2) {
        if ($null -ne (Parse-SemVerTriple $a)) { return $a }
        if ($null -ne (Parse-SemVerTriple $b)) { return $b }
        return $a
    }
    if ($c -ge 0) { return $a }
    return $b
}

function Get-LatestSemVerTagRef {
    $tags = @(git tag -l 2>$null | Where-Object { $_ -match '^v\d+\.\d+\.\d+$' })
    if ($tags.Count -eq 0) { return $null }
    $best = $null
    foreach ($t in $tags) {
        if ($t -notmatch '^v(\d+)\.(\d+)\.(\d+)$') { continue }
        $tv = "$($Matches[1]).$($Matches[2]).$($Matches[3])"
        if ($null -eq $best) { $best = $t; continue }
        if ($best -match '^v(\d+)\.(\d+)\.(\d+)$') {
            $bv = "$($Matches[1]).$($Matches[2]).$($Matches[3])"
            if ((Compare-SemVer $tv $bv) -gt 0) { $best = $t }
        }
    }
    return $best
}

function Get-VersionCodeFromTagRef([string] $tagWithV, [string] $gradleRelPath) {
    git rev-parse -q --verify "refs/tags/$tagWithV" 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { return $null }
    $blob = git show "${tagWithV}:${gradleRelPath}" 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    if ($blob -notmatch 'versionCode\s*=\s*(\d+)') { return $null }
    return [int]$Matches[1]
}

# git push/progress writes to stderr; with $ErrorActionPreference Stop, 2>&1 | Out-Host can throw NativeCommandError.
function Invoke-GitForHost {
    param([Parameter(Mandatory = $true)][string[]] $Arguments)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    $out = & git @Arguments 2>&1
    $code = $LASTEXITCODE
    $ErrorActionPreference = $prev
    foreach ($line in $out) {
        if ($null -eq $line) { continue }
        Write-Host $line
    }
    return $code
}

$raw = [System.IO.File]::ReadAllText($gradlePath, [System.Text.UTF8Encoding]::new($false))
if ($raw -notmatch 'versionCode\s*=\s*(\d+)') {
    Write-Error "Could not find versionCode = ... in $gradleRel"
    exit 1
}
$currentCode = [int]$Matches[1]

if ($raw -notmatch 'versionName\s*=\s*"([^"]*)"') {
    Write-Error "Could not find versionName = ... in $gradleRel"
    exit 1
}
$currentName = $Matches[1].Trim()

$branch = $null
if (-not $SkipGit) {
    $branch = git rev-parse --abbrev-ref HEAD 2>$null
    if (-not $branch) {
        Write-Error "Not a git repository?"
        exit 1
    }
}

if ($PullRebase) {
    Write-Host "git pull --rebase origin $branch ..." -ForegroundColor Cyan
    $pullExit = Invoke-GitForHost @("pull", "--rebase", "origin", $branch)
    if ($pullExit -ne 0) {
        Write-Error "git pull --rebase failed. Resolve conflicts, then re-run (with or without -PullRebase)."
        exit 1
    }
    $raw = [System.IO.File]::ReadAllText($gradlePath, [System.Text.UTF8Encoding]::new($false))
    if ($raw -notmatch 'versionCode\s*=\s*(\d+)') {
        Write-Error "Could not find versionCode = ... in $gradleRel after pull."
        exit 1
    }
    $currentCode = [int]$Matches[1]
    if ($raw -notmatch 'versionName\s*=\s*"([^"]*)"') {
        Write-Error "Could not find versionName = ... in $gradleRel after pull."
        exit 1
    }
    $currentName = $Matches[1].Trim()
}

$latestTagRef = $null
$remoteSemVer = $null
if (-not $LocalOnly) {
    Write-Host "git fetch origin (tags + branch tips)..." -ForegroundColor Cyan
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    git fetch origin 2>$null | Out-Null
    $ErrorActionPreference = $prevEap
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "git fetch origin failed. Using local tags + gradle for baseline."
    }
    if (-not $SkipGit -and -not $PullRebase -and $branch) {
        $prevEap2 = $ErrorActionPreference
        $ErrorActionPreference = "SilentlyContinue"
        $behind = git rev-list --count "HEAD..origin/$branch" 2>$null
        $ErrorActionPreference = $prevEap2
        if ($behind -match '^\d+$' -and [int]$behind -gt 0) {
            Write-Warning "origin/$branch is $behind commit(s) ahead of you; push will likely fail. Run once: .\scripts\publish-release.ps1 -PullRebase   (or: git pull --rebase origin $branch)"
        }
    }
    $latestTagRef = Get-LatestSemVerTagRef
    if ($latestTagRef -match '^v(\d+)\.(\d+)\.(\d+)$') {
        $remoteSemVer = "$($Matches[1]).$($Matches[2]).$($Matches[3])"
    }
}

if ($remoteSemVer -and (Compare-SemVer $currentName $remoteSemVer) -gt 0) {
    Write-Warning "gradle versionName $currentName is ahead of latest tag $latestTagRef ($remoteSemVer); next patch follows gradle, not the tag alone."
}

$baseSemVer = $currentName
if ($remoteSemVer) {
    $baseSemVer = SemVer-Max $currentName $remoteSemVer
    if ((Compare-SemVer $remoteSemVer $currentName) -gt 0) {
        Write-Host "Baseline: latest tag $latestTagRef ($remoteSemVer) ahead of gradle $currentName -> next patch from $baseSemVer." -ForegroundColor Cyan
    }
}

# Tag: optional. Empty -> v<baseSemVer patch+1> (base = max(gradle, latest vM.m.p tag))
$bumped = $null
if ([string]::IsNullOrWhiteSpace($Tag)) {
    $bumped = Bump-VersionPatch $baseSemVer
    if ($bumped) {
        $Tag = "v$bumped"
    } else {
        $Tag = Read-Trim "versionName is not M.m.p. Enter tag (v1.0.6 or 1.0.6):"
    }
}

$Tag = $Tag.Trim()
if ($Tag -match '^V\d') {
    $Tag = "v" + $Tag.Substring(1)
}
if ($Tag -match '^\d') {
    $Tag = "v" + $Tag
}
if ($Tag -notmatch '^v\d') {
    Write-Error "Invalid tag. Use e.g. v1.0.6 or 1.0.6 (Git tag will be v...)."
    exit 1
}

$versionName = $Tag.Substring(1)

$maxCode = $currentCode
if ($latestTagRef) {
    $tc = Get-VersionCodeFromTagRef $latestTagRef $gradleRel
    if ($null -ne $tc) { $maxCode = [Math]::Max($maxCode, $tc) }
}
$tcTag = Get-VersionCodeFromTagRef $Tag $gradleRel
if ($null -ne $tcTag) { $maxCode = [Math]::Max($maxCode, $tcTag) }

$newCode = if ($VersionCode -gt 0) { $VersionCode } else { $maxCode + 1 }
if ($newCode -le $currentCode) {
    Write-Error "New versionCode ($newCode) must be greater than current ($currentCode)."
    exit 1
}

if ($null -ne $bumped -and $bumped) {
    Write-Host "Auto: gradle versionCode=$currentCode versionName=`"$currentName`" -> tag $Tag versionCode=$newCode (versionName=`"$versionName`")" -ForegroundColor Cyan
}

$raw2 = [regex]::Replace(
    $raw,
    '(?m)^(\s*)versionCode\s*=\s*\d+',
    { param($m) $m.Groups[1].Value + "versionCode = $newCode" }
)
$vn = $versionName
$raw2 = [regex]::Replace(
    $raw2,
    '(?m)^(\s*)versionName\s*=\s*"[^"]*"',
    { param($m) $m.Groups[1].Value + 'versionName = "' + $vn + '"' }
)
if ($raw2 -eq $raw) {
    Write-Error "No changes applied to $gradleRel (regex mismatch?)."
    exit 1
}

[System.IO.File]::WriteAllText($gradlePath, $raw2, [System.Text.UTF8Encoding]::new($false))
Write-Host "Written: versionCode=$newCode versionName=`"$versionName`" (git tag $Tag)" -ForegroundColor Green

if ($SkipGit) {
    Write-Host "SkipGit: no commit/push/tag." -ForegroundColor Yellow
    exit 0
}

$dirty = @(git status --porcelain 2>$null)
if ($dirty.Count -gt 0) {
    $other = $dirty | Where-Object { $_ -notmatch 'app/build\.gradle\.kts' }
    if ($other.Count -gt 0) {
        Write-Warning "Other uncommitted files:"
        $other | ForEach-Object { Write-Host "  $_" }
        $c = Read-Trim "Continue anyway? (y/n)"
        if ($c -ne "y" -and $c -ne "Y") { exit 1 }
    }
}

git add -- $gradleRel
git commit -m "chore(release): $Tag (versionCode $newCode)"
if ($LASTEXITCODE -ne 0) {
    Write-Error "git commit failed (nothing to commit?)."
    exit 1
}

$pushBranchExit = Invoke-GitForHost @("push", "origin", $branch)
if ($pushBranchExit -ne 0) {
    Write-Host ""
    Write-Host "Branch push failed (often: remote has commits you do not have)." -ForegroundColor Yellow
    Write-Host "Fix:  git pull --rebase origin $branch" -ForegroundColor Yellow
    Write-Host "      resolve conflicts, git push origin $branch" -ForegroundColor Yellow
    Write-Host "After pull/rebase, push the branch, then create tag if needed." -ForegroundColor Yellow
    Write-Host "Undo last commit if you want to retry:  git reset --soft HEAD~1" -ForegroundColor Yellow
    exit 1
}

git rev-parse -q --verify "refs/tags/$Tag" 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Error "Tag $Tag already exists locally. Remove: git tag -d $Tag"
    exit 1
}
git tag $Tag
if ($LASTEXITCODE -ne 0) {
    Write-Error "git tag $Tag failed."
    exit 1
}
$pushTagExit = Invoke-GitForHost @("push", "origin", $Tag)
if ($pushTagExit -ne 0) {
    Write-Error "git push origin $Tag failed. Branch is already pushed; fix tag push manually (git push origin $Tag)."
    exit 1
}

Write-Host ""
Write-Host "Done. Open GitHub Actions workflow Release AAB + APK for tag $Tag" -ForegroundColor Cyan
