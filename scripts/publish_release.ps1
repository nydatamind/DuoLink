param (
    [string]$Version = "1.0.0",
    [string]$ApkPath = "DuoLink.apk",
    [string]$Repo = "nydatamind/DuoLink"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " DuoLink - Official GitHub Release Automation" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Verify APK exists
if (-not (Test-Path $ApkPath)) {
    Write-Error "Production APK not found at $ApkPath. Run .\gradlew.bat assembleRelease first."
    exit 1
}

# 2. Compute SHA-256 Checksum
Write-Host "[1/4] Calculating cryptographic SHA-256 checksum..." -ForegroundColor Yellow
$hashInfo = Get-FileHash -Path $ApkPath -Algorithm SHA256
$sha256 = $hashInfo.Hash.ToLower()
$fileSizeMb = [math]::Round((Get-Item $ApkPath).Length / 1MB, 2)

Write-Host "  -> File Size: $fileSizeMb MB" -ForegroundColor Green
Write-Host "  -> SHA-256:   $sha256" -ForegroundColor Green

# 3. Generate Release Notes
$tag = "v" + $Version
$releaseTitle = "DuoLink $tag - Private Communication Release"

$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine("## DuoLink $tag - Official Production Release")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("A private, secure communication application exclusively for TWO authorized people.")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("### What's New")
[void]$sb.AppendLine("- **Telegram / iOS-Style Emoji Keyboard**: Integrated bundled Google Noto Color Emoji font (androidx.emoji2:emoji2-bundled) for uniform high-fidelity emoji rendering across all Android versions without broken glyphs. Features category tabs, dynamic search, recents, haptic feedback, and dedicated backspace.")
[void]$sb.AppendLine("- **In-App Auto-Update System**: In-app checker against GitHub Releases with real-time download progress, cryptographic SHA-256 checksum validation, signature verification against the installed app, and automatic package installer trigger.")
[void]$sb.AppendLine("- **Dynamic 3D Owl Mascot & Adaptive Launcher Icon**: Live eye states (Alert, Sleepy, Resting) reflecting partner activity level.")
[void]$sb.AppendLine("- **Google OAuth Login with Zero Identity Leaks**: Gmail address and Google profile are never transmitted to peer or third parties. Independent custom nicknames per device.")
[void]$sb.AppendLine("- **Hardware Silent Vibration Alerts**: Ultra-stealthy non-visual alert system.")
[void]$sb.AppendLine("- **Strict Peer Deletion**: Telegram-style 'Delete for Both' with complete local and remote wipe.")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("---")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("### Release Integrity & Verification")
[void]$sb.AppendLine("Every release APK is verified cryptographically before installation:")
[void]$sb.AppendLine("- **File Name**: ``DuoLink.apk``")
[void]$sb.AppendLine("- **Asset Size**: ``$fileSizeMb MB``")
[void]$sb.AppendLine("- **SHA-256**: ``$sha256``")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("### Android Compatibility")
[void]$sb.AppendLine("- **Minimum Android**: Android 8.0 (API Level 26)")
[void]$sb.AppendLine("- **Target Android**: Android 14+ (API Level 34)")
[void]$sb.AppendLine("- **Architecture**: Universal (arm64-v8a, armeabi-v7a, x86_64)")
[void]$sb.AppendLine("- **Package**: ``com.duolink.app``")
[void]$sb.AppendLine("- **Signing Keystore**: Persistent release key (persists database, keys, and chat across updates)")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("---")
[void]$sb.AppendLine("*Note: Due to Android security, verify 'Install Unknown Apps' permission for DuoLink when updating via the in-app updater.*")

$notesFile = "scripts/release_notes_$tag.md"
[System.IO.File]::WriteAllText($notesFile, $sb.ToString(), [System.Text.Encoding]::UTF8)
Write-Host "[2/4] Generated Release Notes at $notesFile" -ForegroundColor Yellow

# 4. Check for GitHub CLI (gh)
Write-Host "[3/4] Checking GitHub CLI availability..." -ForegroundColor Yellow
$ghAvailable = (Get-Command gh -ErrorAction SilentlyContinue) -ne $null

if ($ghAvailable) {
    Write-Host "GitHub CLI detected. Creating official GitHub Release on $Repo..." -ForegroundColor Cyan
    try {
        & gh release create $tag $ApkPath --repo $Repo --title $releaseTitle --notes-file $notesFile
        Write-Host "SUCCESS: Official release $tag created successfully at https://github.com/$Repo/releases/tag/$tag" -ForegroundColor Green
    } catch {
        Write-Warning "Could not automatically create release via gh CLI: $_"
        Write-Host "Please publish manually using instructions below." -ForegroundColor Yellow
    }
} else {
    Write-Host "[INFO] GitHub CLI (gh) not found in PATH." -ForegroundColor Gray
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " MANUAL GITHUB RELEASE INSTRUCTIONS" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "1. Open your GitHub Repository:" -ForegroundColor White
    Write-Host "   https://github.com/$Repo/releases/new" -ForegroundColor Cyan
    Write-Host "2. Set Tag version to: $tag" -ForegroundColor White
    Write-Host "3. Set Release title to: $releaseTitle" -ForegroundColor White
    Write-Host "4. Paste the generated notes from: $notesFile" -ForegroundColor White
    Write-Host "5. Upload the signed binary: $ApkPath" -ForegroundColor White
    Write-Host "6. Click 'Publish release'." -ForegroundColor White
    Write-Host "==========================================================" -ForegroundColor Green
}

Write-Host "[4/4] Release packaging complete!" -ForegroundColor Green
