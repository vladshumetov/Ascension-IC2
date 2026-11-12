param(
    [string]$ProjectRoot = ""
)

# Draws the solar panel GUI background in the approved dark-blue style.
# Working area: 264x180 (1.5x the original 176x120), canvas 512x512 for GuiGraphics.blit.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
$out = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2\textures\gui\solar_panel.png"
New-Item -ItemType Directory -Path (Split-Path -Parent $out) -Force | Out-Null

$W = 264
$H = 180
$CANVAS = 512

function C([string]$hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Px($bmp, [int]$x, [int]$y, [System.Drawing.Color]$c) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height) { $bmp.SetPixel($x, $y, $c) }
}
function Rect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [System.Drawing.Color]$c) {
    for ($y = $y0; $y -le $y1; $y++) { for ($x = $x0; $x -le $x1; $x++) { Px $bmp $x $y $c } }
}

$bmp = New-Object System.Drawing.Bitmap($CANVAS, $CANVAS, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$top = C '#171D39'
$bottom = C '#10162E'

for ($y = 0; $y -lt $H; $y++) {
    $t = $y / ($H - 1.0)
    $r = [int]($top.R + ($bottom.R - $top.R) * $t)
    $g = [int]($top.G + ($bottom.G - $top.G) * $t)
    $b = [int]($top.B + ($bottom.B - $top.B) * $t)
    Rect $bmp 0 $y ($W - 1) $y ([System.Drawing.Color]::FromArgb(255, $r, $g, $b))
}

# Outer frame and top highlight.
Rect $bmp 0 0 ($W - 1) 0 (C '#05080C')
Rect $bmp 0 ($H - 1) ($W - 1) ($H - 1) (C '#05080C')
Rect $bmp 0 0 0 ($H - 1) (C '#05080C')
Rect $bmp ($W - 1) 0 ($W - 1) ($H - 1) (C '#05080C')
Rect $bmp 12 4 251 4 (C '#B7E0EA')

# Title banner.
Rect $bmp 60 3 204 21 (C '#1F274F')
Rect $bmp 60 3 204 3 (C '#4D7C9A')
Rect $bmp 60 21 204 21 (C '#4D7C9A')
Rect $bmp 60 3 60 21 (C '#4D7C9A')
Rect $bmp 204 3 204 21 (C '#4D7C9A')
Rect $bmp 61 4 203 4 (C '#B7E0EA')

# Info panel.
Rect $bmp 15 33 249 119 (C '#141A32')
Rect $bmp 15 33 249 33 (C '#253C5E')
Rect $bmp 15 119 249 119 (C '#253C5E')
Rect $bmp 15 33 15 119 (C '#253C5E')
Rect $bmp 249 33 249 119 (C '#253C5E')

# Energy bar frame and well.
Rect $bmp 21 132 241 144 (C '#0F1325')
Rect $bmp 21 132 241 132 (C '#4D7C9A')
Rect $bmp 21 144 241 144 (C '#4D7C9A')
Rect $bmp 21 132 21 144 (C '#4D7C9A')
Rect $bmp 241 132 241 144 (C '#4D7C9A')
Rect $bmp 22 133 240 143 (C '#3D001C')
foreach ($k in 1..9) {
    $x = 22 + [int](219 * $k / 10)
    Rect $bmp $x 133 $x 143 (C '#63121A')
}

# Footer line and corner studs.
Rect $bmp 24 156 239 156 (C '#253C5E')
foreach ($pos in @(@(6, 6), @(257, 6), @(6, 173), @(257, 173))) {
    Px $bmp $pos[0] $pos[1] (C '#4D7C9A')
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "Wrote $out (${W}x${H} on ${CANVAS}x${CANVAS})"
