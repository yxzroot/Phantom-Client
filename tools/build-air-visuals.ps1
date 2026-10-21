param(
    [ValidateSet('Foundation', 'Hud')][string]$Area = 'Hud',
    [string]$BaseJar = '',
    [switch]$CheckOpenGL
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$workspace = Split-Path -Parent $project
if (!$BaseJar) { $BaseJar = Join-Path $project 'dist\Phantom-Client-1.8.9.jar' }
$BaseJar = (Resolve-Path -LiteralPath $BaseJar).Path
$cache = Join-Path $workspace 'Raven-main\.gradle-user\caches'
$jdk8 = Join-Path $workspace 'Raven-main\jdk8u504-b01\bin'
$jdk21 = Join-Path $workspace 'krypton-analysis\tools\jdk21\jdk-21.0.12.1+1\bin'
$forge = Join-Path $cache 'minecraft\net\minecraftforge\forge\1.8.9-11.15.1.2318-1.8.9\stable\20\forgeBin-1.8.9-11.15.1.2318-1.8.9.jar'
$mappings = Join-Path $cache 'minecraft\de\oceanlabs\mcp\mcp_stable\20\srgs\mcp-srg.srg'
$stage = New-Item -ItemType Directory -Path (Join-Path $project ('build-air-visuals\' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '-' + $Area))
$classes = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'classes')
$toolClasses = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'tools')
$empty = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'empty-source')
$deps = @(Get-ChildItem -LiteralPath $cache -Recurse -File -Filter '*.jar' | Where-Object {
    $_.Name -match '^(guava-18\.0|gson-2\.2\.4|lwjgl-2\.9|lwjgl_util-2\.9)'
} | ForEach-Object FullName)
$asm = @(Get-ChildItem -LiteralPath (Join-Path $workspace 'Raven-main\.gradle-user\wrapper\dists') -Recurse -File -Filter 'asm*-6.1.1.jar' | ForEach-Object FullName)
$cp = (@($forge, $BaseJar) + $deps) -join ';'
$render = Join-Path $project 'src\keystrokesmod\client\module\modules\render'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $render 'airvisuals') -Filter '*.java' | ForEach-Object FullName)
if ($Area -eq 'Hud') {
    $sources += Join-Path $render 'AirHudRenderer.java'
    $sources += Join-Path $render 'PhantomHUD.java'
    $sources += Join-Path $render 'SwordGlow.java'
    $sources += @(Get-ChildItem -LiteralPath (Join-Path $render 'swordglow') -Filter '*.java' | ForEach-Object FullName)
    $sources += Join-Path $project 'src\keystrokesmod\client\mixin\mixins\MixinSwordGlowHand.java'
    $sources += Join-Path $project 'src\keystrokesmod\client\mixin\mixins\MixinSwordGlowItem.java'
}
$sources += Join-Path $PSScriptRoot 'AirVisualsTest.java'
& (Join-Path $jdk8 'javac.exe') -source 8 -target 8 -proc:none -sourcepath $empty.FullName -classpath $cp -d $classes.FullName $sources
if ($LASTEXITCODE -ne 0) { throw "$Area compilation failed" }
& (Join-Path $jdk8 'java.exe') -ea -classpath ($classes.FullName + ';' + $cp) AirVisualsTest
if ($LASTEXITCODE -ne 0) { throw 'Visual timing/palette checks failed' }
if ($CheckOpenGL) {
    $nativeJar = @(Get-ChildItem -LiteralPath $cache -Recurse -File -Filter 'lwjgl-platform-*-natives-windows.jar')[0]
    $nativeDir = Join-Path $stage.FullName 'natives'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($nativeJar.FullName, $nativeDir)
    & (Join-Path $jdk8 'java.exe') "-Djava.library.path=$nativeDir" -ea -classpath ($classes.FullName + ';' + $cp) AirVisualsTest gl
    if ($LASTEXITCODE -ne 0) { throw 'GL state isolation checks failed' }
}
if ($Area -eq 'Foundation') { Write-Output "PASS: theme palette and render helpers compile ($($stage.FullName))"; return }
$toolCp = (@($cp) + $asm) -join ';'
& (Join-Path $jdk21 'javac.exe') -proc:none -classpath $toolCp -d $toolClasses.FullName (Join-Path $PSScriptRoot 'BuildLunarModuleJar.java') (Join-Path $PSScriptRoot 'PatchAirVisualsJar.java')
if ($LASTEXITCODE -ne 0) { throw 'Visual patch tool compilation failed' }
$output = Join-Path $stage.FullName 'Phantom-Client-1.8.9-AirVisuals.jar'
& (Join-Path $jdk21 'java.exe') -classpath ($toolClasses.FullName + ';' + $toolCp) PatchAirVisualsJar $BaseJar $classes.FullName $mappings $project $output
if ($LASTEXITCODE -ne 0) { throw 'Visual JAR verification failed' }
Get-FileHash -LiteralPath $output -Algorithm SHA256
