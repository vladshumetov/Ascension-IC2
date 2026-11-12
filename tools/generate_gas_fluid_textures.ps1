param(
    [string]$ProjectRoot = ""
)

# Recolours the animated vanilla water textures into light-blue natural gas, keeping mcmeta frames.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
$outDir = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2\textures\block"
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$clientJar = Get-ChildItem "$env:USERPROFILE\.gradle\caches\neoformruntime\artifacts" -Filter 'minecraft_1.21.1_client.jar' | Select-Object -First 1
if (-not $clientJar) { throw "minecraft client jar not found" }

function Recolor([string]$entryName, [string]$outName) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($clientJar.FullName)
    try {
        $entry = $zip.GetEntry($entryName)
        if (-not $entry) { throw "$entryName not found" }
        $stream = $entry.Open()
        $src = New-Object System.Drawing.Bitmap($stream)
    }
    finally {
        $zip.Dispose()
    }

    try {
        $dst = New-Object System.Drawing.Bitmap($src.Width, $src.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        for ($y = 0; $y -lt $src.Height; $y++) {
            for ($x = 0; $x -lt $src.Width; $x++) {
                $c = $src.GetPixel($x, $y)
                if ($c.A -eq 0) {
                    $dst.SetPixel($x, $y, $c)
                    continue
                }
                $lum = (0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B) / 255.0
                $f = 0.45 + 0.7 * $lum
                $r = [int](30 * $f)
                $g = [int](220 * $f)
                $b = [int](210 * $f)
                $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, [Math]::Min(255, $r), [Math]::Min(255, $g), [Math]::Min(255, $b)))
            }
        }
        $dst.Save((Join-Path $outDir $outName), [System.Drawing.Imaging.ImageFormat]::Png)
        $dst.Dispose()
    }
    finally {
        $src.Dispose()
        $stream.Dispose()
    }
}

Recolor "assets/minecraft/textures/block/water_still.png" "natural_gas_still.png"
Recolor "assets/minecraft/textures/block/water_flow.png" "natural_gas_flowing.png"

# Copy animation metadata (it only contains frame timing, no texture paths).
$zip = [System.IO.Compression.ZipFile]::OpenRead($clientJar.FullName)
try {
    foreach ($pair in @(
        @("assets/minecraft/textures/block/water_still.png.mcmeta", "natural_gas_still.png.mcmeta"),
        @("assets/minecraft/textures/block/water_flow.png.mcmeta", "natural_gas_flowing.png.mcmeta"))) {
        $entry = $zip.GetEntry($pair[0])
        if (-not $entry) { throw "$($pair[0]) not found" }
        $reader = New-Object System.IO.StreamReader($entry.Open())
        $meta = $reader.ReadToEnd()
        $reader.Dispose()
        [System.IO.File]::WriteAllText((Join-Path $outDir $pair[1]), $meta, (New-Object System.Text.UTF8Encoding($false)))
    }
}
finally {
    $zip.Dispose()
}

Write-Output "Recoloured animated water textures for natural gas."
