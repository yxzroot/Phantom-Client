param([string]$BaseJar = '',[switch]$CheckOpenGL,[string]$AuditDirectory = '')
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
$stage = New-Item -ItemType Directory -Path (Join-Path $project ('build-config\' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff')))
$classes = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'classes')
$tests = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'tests')
$toolClasses = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'tools')
$empty = New-Item -ItemType Directory -Path (Join-Path $stage.FullName 'empty-source')
$deps = @(Get-ChildItem -LiteralPath $cache -Recurse -File -Filter '*.jar' | Where-Object { $_.Name -match '^(guava-18\.0|gson-2\.2\.4|lwjgl-2\.9|lwjgl_util-2\.9|commons-lang3-|log4j-api-|log4j-core-|netty-all-|authlib-|commons-io-|commons-codec-|commons-logging-|httpclient-|httpcore-|jopt-simple-)' } | ForEach-Object FullName)
$asm = @(Get-ChildItem -LiteralPath (Join-Path $workspace 'Raven-main\.gradle-user\wrapper\dists') -Recurse -File -Filter 'asm*-6.1.1.jar' | ForEach-Object FullName)
$cp = (@($forge, $BaseJar) + $deps) -join ';'
$sources = @('config/Config.java','config/ConfigManager.java','clickgui/apple/rebuild/ConfigTab.java','clickgui/apple/rebuild/GuiRoot.java') | ForEach-Object { Join-Path $project ('src/keystrokesmod/client/' + $_) }
& (Join-Path $jdk8 'javac.exe') -encoding UTF-8 -source 8 -target 8 -proc:none -sourcepath $empty.FullName -classpath $cp -d $classes.FullName $sources
if ($LASTEXITCODE -ne 0) { throw 'Config compilation failed' }
$testSources = @((Join-Path $PSScriptRoot 'ConfigProfilesTest.java'),(Join-Path $PSScriptRoot 'fixtures/config/keystrokesmod/client/main/Raven.java'))
foreach ($relative in @('clickgui/apple/rebuild/UiDraw.java','clickgui/apple/rebuild/SdfRenderer.java','clickgui/apple/rebuild/GuiFont.java','clickgui/apple/rebuild/GuiRenderContext.java','module/modules/render/aurora/AuroraGLState.java','utils/font/PhantomDiagnostics.java')) { $testSources += Join-Path $project ('src/keystrokesmod/client/' + $relative) }
& (Join-Path $jdk8 'javac.exe') -encoding UTF-8 -source 8 -target 8 -proc:none -sourcepath $empty.FullName -classpath ($classes.FullName + ';' + $cp) -d $tests.FullName $testSources
if ($LASTEXITCODE -ne 0) { throw 'Config tests compilation failed' }
& (Join-Path $jdk8 'java.exe') -ea -classpath ($tests.FullName + ';' + $classes.FullName + ';' + $cp) ConfigProfilesTest
if ($LASTEXITCODE -ne 0) { throw 'Config filesystem regression failed' }
if ($AuditDirectory) {
    & (Join-Path $jdk8 'java.exe') -ea -classpath ($tests.FullName + ';' + $classes.FullName + ';' + $cp) ConfigProfilesTest audit $AuditDirectory
    if ($LASTEXITCODE -ne 0) { throw 'Real profile read-only compatibility check failed' }
}
if ($CheckOpenGL) {
    $nativeJar = @(Get-ChildItem -LiteralPath $cache -Recurse -File -Filter 'lwjgl-platform-*-natives-windows.jar')[0]
    $nativeDir = Join-Path $stage.FullName 'natives'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($nativeJar.FullName,$nativeDir)
    & (Join-Path $jdk8 'java.exe') "-Djava.library.path=$nativeDir" -ea -classpath ($tests.FullName + ';' + $classes.FullName + ';' + $cp) ConfigProfilesTest gl (Join-Path $stage.FullName 'config-tab-preview.png')
    if ($LASTEXITCODE -ne 0) { throw 'Config tab UI regression failed' }
}
$toolCp = (@($cp) + $asm) -join ';'
& (Join-Path $jdk21 'javac.exe') -proc:none -classpath $toolCp -d $toolClasses.FullName (Join-Path $PSScriptRoot 'BuildLunarModuleJar.java') (Join-Path $PSScriptRoot 'PatchMappedClassesJar.java') (Join-Path $PSScriptRoot 'VerifyConfigPatch.java')
if ($LASTEXITCODE -ne 0) { throw 'Config packaging compilation failed' }
$output = Join-Path $stage.FullName 'Phantom-Client-1.8.9-ConfigFix.jar'
& (Join-Path $jdk21 'java.exe') -classpath ($toolClasses.FullName + ';' + $toolCp) PatchMappedClassesJar $BaseJar $classes.FullName $mappings $output
if ($LASTEXITCODE -ne 0) { throw 'Config packaging failed' }
& (Join-Path $jdk21 'java.exe') -classpath ($toolClasses.FullName + ';' + $toolCp) VerifyConfigPatch $BaseJar $output $forge $mappings
if ($LASTEXITCODE -ne 0) { throw 'Config patch validation failed' }
Get-FileHash -LiteralPath $output -Algorithm SHA256
