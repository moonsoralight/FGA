$ErrorActionPreference = 'Stop'
$buildTools = Join-Path (Split-Path -Parent $PSScriptRoot) 'AndroidBuildEnv'
$javaRoot = Join-Path $buildTools 'jdk17/jdk-17.0.20+8'
$sdkRoot = Join-Path $buildTools 'sdk'
$gradleCommand = Join-Path $buildTools 'gradle-9.5.1/bin/gradle.bat'
foreach ($required in @((Join-Path $javaRoot 'bin/java.exe'), (Join-Path $sdkRoot 'platforms/android-35/android.jar'), $gradleCommand)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "缺少本地构建工具：$required；此脚本不会自动下载。"
    }
}
$env:JAVA_HOME = $javaRoot
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:ANDROID_USER_HOME = Join-Path $buildTools 'android-user-home'
$env:GRADLE_USER_HOME = Join-Path $buildTools 'gradle-cache'
$env:FGA_VERSION_CODE = '1'
$env:FGA_VERSION_NAME = '0.1.2'
Push-Location $PSScriptRoot
try {
    & $gradleCommand :app:assembleCi --offline --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "离线编译失败，退出码 $LASTEXITCODE；未自动下载依赖。" }
} finally { Pop-Location }
