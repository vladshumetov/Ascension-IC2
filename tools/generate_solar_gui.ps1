param(
    [string]$ProjectRoot = ""
)

# Draws the solar panel GUI background in the approved dark-blue machine style.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
$out = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2\textures\gui\solar_panel.png"
New-Item -ItemType Directory -Path (Split-Path -Parent $out) -Force | Out-Null

function C([string]$hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Px($bmp, [int]$x, [int]$y, [System.Drawing.Color]$c) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height) { $bmp.SetPixel($x, $y, $c) }
}
function Rect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [System.Drawing.Color]$c) {
    for ($y = $y0; $y -le $y1; $y++) { for ($x = $x0; $x -le $x1; $x++) { Px $bmp $x $y $c } }
}

$bmp = New-Object System.Drawing.Bitmap(176, 120, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$top = C '#171D39'
$bottom = C '#10162E'

for ($y = 0; $y -lt 120; $y++) {
    $t = $y / 119.0
    $r = [int]($top.R + ($bottom.R - $top.R) * $t)
    $g = [int]($top.G + ($bottom.G - $top.G) * $t)
    $b = [int]($top.B + ($bottom.B - $top.B) * $t)
    Rect $bmp 0 $y 175 $y ([System.Drawing.Color]::FromArgb(255, $r, $g, $b))
}

# Outer frame and top highlight.
Rect $bmp 0 0 175 0 (C '#05080C')
Rect $bmp 0 119 175 119 (C '#05080C')
Rect $bmp 0 0 0 119 (C '#05080C')
Rect $bmp 175 0 175 119 (C '#05080C')
Rect $bmp 8 3 167 3 (C '#B7E0EA')

# Title banner.
Rect $bmp 40 2 135 14 (C '#1F274F')
Rect $bmp 40 2 135 2 (C '#4D7C9A')
Rect $bmp 40 14 135 14 (C '#4D7C9A')
Rect $bmp 40 2 40 14 (C '#4D7C9A')
Rect $bmp 135 2 135 14 (C '#4D7C9A')
Rect $bmp 41 3 134 3 (C '#B7E0EA')

# Info panel.
Rect $bmp 10 22 165 79 (C '#141A32')
Rect $bmp 10 22 165 22 (C '#253C5E')
Rect $bmp 10 79 165 79 (C '#253C5E')
Rect $bmp 10 22 10 79 (C '#253C5E')
Rect $bmp 165 22 165 79 (C '#253C5E')

# Energy bar frame and well.
Rect $bmp 14 88 161 96 (C '#0F1325')
Rect $bmp 14 88 161 88 (C '#4D7C9A')
Rect $bmp 14 96 161 96 (C '#4D7C9A')
Rect $bmp 14 88 14 96 (C '#4D7C9A')
Rect $bmp 161 88 161 96 (C '#4D7C9A')
Rect $bmp 15 89 160 95 (C '#3D001C')
foreach ($k in 1..9) {
    $x = 15 + [int](145 * $k / 10)
    Rect $bmp $x 89 $x 95 (C '#63121A')
}

# Footer line and corner studs.
Rect $bmp 16 104 159 104 (C '#253C5E')
foreach ($pos in @(@(4, 4), @(171, 4), @(4, 115), @(171, 115))) {
    Px $bmp $pos[0] $pos[1] (C '#4D7C9A')
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Wrote $out"
