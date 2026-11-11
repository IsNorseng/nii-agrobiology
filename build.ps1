param(
    [string] $Sdk = $env:ANDROID_HOME,
    [string] $Jdk = $env:JAVA_HOME,
    [string] $Keystore,
    [string] $KeyAlias = "agrobiology"
)

$ErrorActionPreference = "Stop"
if (!$Sdk) { $Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
if (!$Jdk) { throw "Set JAVA_HOME or provide -Jdk." }
if ($Keystore) {
    $Keystore = (Resolve-Path -LiteralPath $Keystore).Path
    if (!$env:AGROBIOLOGY_KEYSTORE_PASSWORD) {
        throw "Set AGROBIOLOGY_KEYSTORE_PASSWORD to sign the APK."
    }
}

$tools = Join-Path $Sdk "build-tools\36.1.0"
$androidJar = Join-Path $Sdk "platforms\android-36.1\android.jar"
$build = (New-Item -ItemType Directory -Force "$PSScriptRoot\build").FullName
$classes = (New-Item -ItemType Directory -Force "$build\classes").FullName
$dex = (New-Item -ItemType Directory -Force "$build\dex").FullName

function Invoke-Step([string] $Tool, [string[]] $Arguments) {
    & $Tool @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Tool failed with exit code $LASTEXITCODE" }
}

$sources = (Get-ChildItem -LiteralPath "$PSScriptRoot\src" -Recurse -Filter *.java).FullName
Invoke-Step "$Jdk\bin\javac.exe" (@("--release", "8", "-encoding", "UTF-8", "-classpath", $androidJar, "-d", $classes) + $sources)
Invoke-Step "$Jdk\bin\jar.exe" @("cf", "$build\classes.jar", "-C", $classes, ".")
Invoke-Step "$Jdk\bin\java.exe" @("-cp", "$tools\lib\d8.jar", "com.android.tools.r8.D8", "--release", "--lib", $androidJar, "--min-api", "23", "--output", $dex, "$build\classes.jar")
Invoke-Step "$tools\aapt2.exe" @("compile", "--dir", "$PSScriptRoot\res", "-o", "$build\resources.zip")
Invoke-Step "$tools\aapt2.exe" @("link", "-I", $androidJar, "--manifest", "$PSScriptRoot\AndroidManifest.xml", "-o", "$build\unpacked.apk", "$build\resources.zip")
Invoke-Step "$Jdk\bin\jar.exe" @("uf", "$build\unpacked.apk", "-C", $dex, "classes.dex")
$unsigned = "$build\agrobiology-unsigned.apk"
Invoke-Step "$tools\zipalign.exe" @("-f", "4", "$build\unpacked.apk", $unsigned)

if ($Keystore) {
    $signed = "$build\agrobiology.apk"
    Invoke-Step "$Jdk\bin\java.exe" @("-jar", "$tools\lib\apksigner.jar", "sign", "--ks", $Keystore, "--ks-pass", "env:AGROBIOLOGY_KEYSTORE_PASSWORD", "--ks-key-alias", $KeyAlias, "--v4-signing-enabled", "false", "--out", $signed, $unsigned)
    Invoke-Step "$Jdk\bin\java.exe" @("-jar", "$tools\lib\apksigner.jar", "verify", $signed)
    Invoke-Step "$tools\zipalign.exe" @("-c", "4", $signed)
    Write-Output $signed
} else {
    Write-Output $unsigned
}
