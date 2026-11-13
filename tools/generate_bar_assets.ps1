param(
    [string]$ProjectRoot = ""
)

# Generates 190x5 GUI bar textures in the style of Next Blood/Next Botany:
# mana and blood with light streaks, energy with repeating dark/light bands.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
$out = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2\textures\gui\bars"
New-Item -ItemType Directory -Path $out -Force | Out-Null

$W = 190
$H = 5

function C([string]$hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function New-Bar([string]$name, [System.Drawing.Color]$base, [System.Drawing.Color]$light, [System.Drawing.Color]$glow, [System.Drawing.Color]$dark) {
    $bmp = New-Object System.Drawing.Bitmap($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $H; $y++) {
        for ($x = 0; $x -lt $W; $x++) {
            $c = $base
            $n = ($x * 7 + $y * 13) % 41
            if ($n -eq 0 -or $n -eq 1) { $c = $light }
            elseif ($n -eq 5) { $c = $glow }
            elseif ($n -eq 9) { $c = $dark }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    $bmp.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function New-BandBar([string]$name, [System.Drawing.Color]$a, [System.Drawing.Color]$b) {
    $bmp = New-Object System.Drawing.Bitmap($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $H; $y++) {
        for ($x = 0; $x -lt $W; $x++) {
            $bmp.SetPixel($x, $y, $(if ([int](($x / 2) % 2) -eq 0) { $a } else { $b }))
        }
    }
    $bmp.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Mana: #00A8E3 base, #5FD3E5 highlight, #FFDBDB glow, #35568A shadow.
New-Bar "mana" (C '#00A8E3') (C '#5FD3E5') (C '#FFDBDB') (C '#35568A')
# Blood: #971313 base, #AF2222 highlight, #FFDBDB glow, #831000 shadow.
New-Bar "blood" (C '#971313') (C '#AF2222') (C '#FFDBDB') (C '#831000')
# Energy: repeating #A22C2C / #982424 bands.
New-BandBar "energy" (C '#A22C2C') (C '#982424')

Write-Output "Generated GUI bar textures."
