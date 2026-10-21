# UTF-8 JSON-lines protocol. Reads media sessions only; no browser/history/account access.
param([switch]$Once,[switch]$Diagnostics)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties,Windows.Media.Control,ContentType=WindowsRuntime]
$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType,Windows.Storage.Streams,ContentType=WindowsRuntime]
$null = [Windows.Storage.Streams.DataReader,Windows.Storage.Streams,ContentType=WindowsRuntime]
$awaiter = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetGenericArguments().Count -eq 1 -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1
function Await-Media($operation, [Type]$resultType) {
    $task = $awaiter.MakeGenericMethod($resultType).Invoke($null,@($operation))
    if (!$task.Wait(2500)) { throw 'Media operation timeout' }
    return $task.Result
}
function Close-MediaResource($resource) {
    if($null -eq $resource){return}
    # WinRT can return an RCW rather than an IDisposable projection. Cleanup must
    # NEVER throw out the track's successfully read title/artist/playback metadata.
    try {
        if($resource -is [IDisposable]) { ([IDisposable]$resource).Dispose() }
        elseif([Runtime.InteropServices.Marshal]::IsComObject($resource)) { $null=[Runtime.InteropServices.Marshal]::ReleaseComObject($resource) }
    } catch { }
}
$artKeys = @{}
$manager = $null
while ($true) {
    try {
        if ($null -eq $manager) { $manager = Await-Media ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]) }
        $sessions = @(); $artBudget=1048576; $errors=@()
        foreach ($session in $manager.GetSessions()) {
            if ($sessions.Count -ge 12) { break }
            try {
                $properties = Await-Media ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
                if ([string]::IsNullOrWhiteSpace($properties.Title)) { continue }
                $timeline = $session.GetTimelineProperties()
                $playback = $session.GetPlaybackInfo()
                if ($playback.PlaybackStatus.ToString() -eq 'Stopped' -or $playback.PlaybackStatus.ToString() -eq 'Closed') { continue }
                $source = [string]$session.SourceAppUserModelId
                $key = $source + '|' + $properties.Artist + '|' + $properties.Title
                $entry = @{session=($source+'#'+$session.GetHashCode());source=$source;title=[string]$properties.Title;artist=[string]$properties.Artist;album=[string]$properties.AlbumTitle;id=$key;duration=[Math]::Max(0,($timeline.EndTime-$timeline.StartTime).TotalSeconds);position=[Math]::Max(0,($timeline.Position-$timeline.StartTime).TotalSeconds);playing=($playback.PlaybackStatus.ToString() -eq 'Playing');rate=1.0}
                if ($null -ne $playback.PlaybackRate) { $entry.rate=[double]$playback.PlaybackRate }
                # GSMTC Position is anchored at LastUpdatedTime, not necessarily "now".
                if ($entry.playing) {
                    $elapsed=([DateTimeOffset]::UtcNow-[DateTimeOffset]$timeline.LastUpdatedTime).TotalSeconds
                    if ($elapsed -gt 0 -and $elapsed -lt 86400) { $entry.position += $elapsed*$entry.rate }
                    if ($entry.duration -gt 0) { $entry.position=[Math]::Min($entry.duration,$entry.position) }
                }
                if (!$artKeys.ContainsKey($key) -and $null -ne $properties.Thumbnail) {
                    $stream=$null; $reader=$null
                    try {
                        $stream=Await-Media ($properties.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                        if ($stream.Size -gt 0 -and $stream.Size -le $artBudget) {
                            $reader=[Windows.Storage.Streams.DataReader]::new($stream.GetInputStreamAt(0))
                            $null=Await-Media ($reader.LoadAsync([uint32]$stream.Size)) ([uint32])
                            $bytes=New-Object byte[] ([int]$stream.Size); $reader.ReadBytes($bytes)
                            $entry.artworkBase64=[Convert]::ToBase64String($bytes);$artKeys[$key]=$true;$artBudget-=$bytes.Length
                        }
                    } catch { if($Diagnostics){$errors += ('Artwork: '+$_.Exception.Message)} } finally { Close-MediaResource $reader;Close-MediaResource $stream }
                }
                $sessions += $entry
            } catch { if($Diagnostics){$errors += $_.Exception.Message} }
        }
        if($artKeys.Count -gt 64){$artKeys.Clear()}
        $response=@{sessions=$sessions};if($Diagnostics){$response.errors=$errors}
        [Console]::WriteLine(($response | ConvertTo-Json -Depth 4 -Compress))
    } catch {
        $manager=$null
        [Console]::WriteLine('{"sessions":[],"error":"Windows media sessions unavailable"}')
    }
    if($Once){break};Start-Sleep -Milliseconds 1000
}
