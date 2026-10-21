param([string]$BaseJar = '',[switch]$CheckOpenGL)
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
$stage = New-Item -ItemType Directory -Path (Join-Path $project ('build-trail\' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')))
$classes = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'classes')
$tests = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'tests')
$toolClasses = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'tools')
$empty = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'empty-source')
$deps = @(Get-ChildItem -LiteralPath $cache -Recurse -File -Filter '*.jar' | Where-Object { $_.Name -match '^(guava-18\.0|gson-2\.2\.4|lwjgl-2\.9|lwjgl_util-2\.9|commons-lang3-|log4j-api-|log4j-core-|netty-all-|authlib-|commons-io-|commons-codec-|commons-logging-|httpclient-|httpcore-|jopt-simple-)' } | ForEach-Object FullName)
$asm = @(Get-ChildItem -LiteralPath (Join-Path $workspace 'Raven-main\.gradle-user\wrapper\dists') -Recurse -File -Filter 'asm*-6.1.1.jar' | ForEach-Object FullName)
$cp = (@($forge, $BaseJar) + $deps) -join ';'
$render = Join-Path $project 'src\keystrokesmod\client\module\modules\render'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $render 'trail') -Filter '*.java' | ForEach-Object FullName)
$sources += Join-Path $render 'Trail.java'
& (Join-Path $jdk8 'javac.exe') -source 8 -target 8 -proc:none -sourcepath $empty.FullName -classpath $cp -d $classes.FullName $sources
if ($LASTEXITCODE -ne 0) { throw 'Trail compilation failed' }
& (Join-Path $jdk8 'javac.exe') -source 8 -target 8 -proc:none -sourcepath $empty.FullName -classpath ($classes.FullName + ';' + $cp) -d $tests.FullName (Join-Path $PSScriptRoot 'TrailTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Trail tests compilation failed' }
& (Join-Path $jdk8 'java.exe') -ea -classpath ($tests.FullName + ';' + $classes.FullName + ';' + $cp) TrailTest
if ($LASTEXITCODE -ne 0) { throw 'Trail simulation/settings regression failed' }
if ($CheckOpenGL) {
    $nativeJar = @(Get-ChildItem -LiteralPath $cache -Recurse -File -Filter 'lwjgl-platform-*-natives-windows.jar')[0]
    $nativeDir = Join-Path $stage.FullName 'natives'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($nativeJar.FullName, $nativeDir)
    & (Join-Path $jdk8 'java.exe') "-Djava.library.path=$nativeDir" -ea -classpath ($tests.FullName + ';' + $classes.FullName + ';' + $cp) TrailTest gl (Join-Path $stage.FullName 'trail-preview.png')
    if ($LASTEXITCODE -ne 0) { throw 'Trail production GL regression failed' }
}
$toolCp = (@($cp) + $asm) -join ';'
& (Join-Path $jdk21 'javac.exe') -proc:none -classpath $toolCp -d $toolClasses.FullName (Join-Path $PSScriptRoot 'BuildLunarModuleJar.java') (Join-Path $PSScriptRoot 'PatchTrailJar.java')
if ($LASTEXITCODE -ne 0) { throw 'Trail packaging tool compilation failed' }
$output = Join-Path $stage.FullName 'Phantom-Client-1.8.9-Trail.jar'
& (Join-Path $jdk21 'java.exe') -classpath ($toolClasses.FullName + ';' + $toolCp) PatchTrailJar $BaseJar $classes.FullName $mappings $output $forge
if ($LASTEXITCODE -ne 0) { throw 'Trail JAR validation failed' }
Get-FileHash -LiteralPath $output -Algorithm SHA256
