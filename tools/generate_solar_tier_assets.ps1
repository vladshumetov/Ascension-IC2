param(
    [string]$ProjectRoot = ""
)

# Generates unique textures for the 13 generic solar panels (solar_panel_1..13) in the next_ic style:
# 16x16 top/bottom, 16x192 animated side, a distinct palette per tier.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
$assets = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2"
$blockTextureDir = Join-Path $assets "textures\block"
$blockModelDir = Join-Path $assets "models\block"
New-Item -ItemType Directory -Path $blockTextureDir -Force | Out-Null
New-Item -ItemType Directory -Path $blockModelDir -Force | Out-Null

function Write-JsonFile([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 20
    [System.IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}
function C([string]$hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Px($bmp, [int]$x, [int]$y, [System.Drawing.Color]$c) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height) { $bmp.SetPixel($x, $y, $c) }
}
function Rect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [System.Drawing.Color]$c) {
    for ($y = $y0; $y -le $y1; $y++) { for ($x = $x0; $x -le $x1; $x++) { Px $bmp $x $y $c } }
}
function Scale([System.Drawing.Color]$c, [double]$f) {
    return [System.Drawing.Color]::FromArgb(
        255,
        [Math]::Min(255, [Math]::Max(0, [int]($c.R * $f))),
        [Math]::Min(255, [Math]::Max(0, [int]($c.G * $f))),
        [Math]::Min(255, [Math]::Max(0, [int]($c.B * $f))))
}

$frame = C '#464569'
$frameLight = C '#A3A7BE'
$frameDark = C '#12171B'
$bus = C '#D3D4E2'

function New-Top([System.Drawing.Color]$cell) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $cellDark = Scale $cell 0.35
    Rect $bmp 0 0 15 15 $frameDark
    Rect $bmp 1 1 14 14 $frame
    Rect $bmp 1 1 14 1 $frameLight
    Rect $bmp 1 1 1 14 $frameLight
    Rect $bmp 1 14 14 14 (Scale $frameDark 0.65)
    Rect $bmp 14 1 14 14 (Scale $frameDark 0.7)
    foreach ($pos in @(@(0, 0), @(15, 0), @(0, 15), @(15, 15))) { Px $bmp $pos[0] $pos[1] $frameLight }
    for ($gy = 0; $gy -lt 3; $gy++) {
        for ($gx = 0; $gx -lt 3; $gx++) {
            $x0 = 1 + $gx * 5
            $y0 = 1 + $gy * 5
            for ($y = 0; $y -lt 4; $y++) {
                for ($x = 0; $x -lt 4; $x++) {
                    $t = ($x + $y) / 6.0
                    $color = [System.Drawing.Color]::FromArgb(
                        255,
                        [int]($cell.R + ($cellDark.R - $cell.R) * $t),
                        [int]($cell.G + ($cellDark.G - $cell.G) * $t),
                        [int]($cell.B + ($cellDark.B - $cell.B) * $t))
                    if (($x + $y) % 3 -eq 0) { $color = Scale $color 1.1 }
                    Px $bmp ($x0 + $x) ($y0 + $y) $color
                }
            }
            Px $bmp $x0 $y0 (Scale $cell 1.35)
            for ($y = 0; $y -lt 4; $y++) { Px $bmp ($x0 + 2) ($y0 + $y) $bus }
        }
    }
    return $bmp
}

function New-Bottom() {
    $bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Rect $bmp 0 0 15 15 (Scale $frameDark 0.8)
    Rect $bmp 1 1 14 14 $frame
    foreach ($y in 4, 7, 10) {
        Rect $bmp 3 $y 12 $y (Scale $frameDark 0.6)
        Rect $bmp 3 ($y + 1) 12 ($y + 1) $frameLight
    }
    foreach ($pos in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) { Px $bmp $pos[0] $pos[1] $frameLight }
    return $bmp
}

function New-Side([System.Drawing.Color]$cell) {
    $bmp = New-Object System.Drawing.Bitmap(16, 192, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($f = 0; $f -lt 12; $f++) {
        $base = $f * 16
        Rect $bmp 0 $base 15 ($base + 15) $frameDark
        Rect $bmp 1 ($base + 1) 14 ($base + 14) $frame
        Rect $bmp 1 ($base + 1) 14 ($base + 1) $frameLight
        Rect $bmp 1 ($base + 14) 14 ($base + 14) (Scale $frameDark 0.6)
        Rect $bmp 4 ($base + 4) 11 ($base + 11) (Scale $frameDark 0.8)
        $glowY = 5 + (($f * 3) % 6)
        for ($y = 4; $y -le 11; $y++) {
            $distance = [Math]::Abs($y - $glowY)
            $color = if ($distance -eq 0) { $bus } elseif ($distance -eq 1) { $cell } else { Scale $cell 0.45 }
            Rect $bmp 6 ($base + $y) 9 ($base + $y) $color
        }
        foreach ($pos in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) { Px $bmp $pos[0] ($base + $pos[1]) $frameLight }
    }
    return $bmp
}

$colors = @(
    '#4187D4', '#41C067', '#7838ED', '#43CFA3', '#E57523', '#D34B4B', '#C0FF85',
    '#FFA5EF', '#209C9C', '#F1E453', '#6A6D91', '#CE5AFF', '#C2FFBF'
)

for ($tier = 1; $tier -le 13; $tier++) {
    $id = "solar_panel_$tier"
    $cell = C $colors[$tier - 1]
    $top = New-Top $cell
    $side = New-Side $cell
    $bottom = New-Bottom
    $top.Save((Join-Path $blockTextureDir "${id}_top.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $side.Save((Join-Path $blockTextureDir "${id}_side.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bottom.Save((Join-Path $blockTextureDir "${id}_bottom.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $top.Dispose(); $side.Dispose(); $bottom.Dispose()

    Write-JsonFile (Join-Path $blockTextureDir "${id}_side.png.mcmeta") @{
        animation = @{ interpolate = $true; frametime = 2 }
    }
    Write-JsonFile (Join-Path $blockModelDir "$id.json") @{
        parent = "minecraft:block/cube"
        textures = @{
            particle = "ascension_ic2:block/${id}_side"
            down = "ascension_ic2:block/${id}_bottom"
            up = "ascension_ic2:block/${id}_top"
            north = "ascension_ic2:block/${id}_side"
            south = "ascension_ic2:block/${id}_side"
            east = "ascension_ic2:block/${id}_side"
            west = "ascension_ic2:block/${id}_side"
        }
    }
}

Write-Output "Generated unique textures for 13 solar panel tiers."
