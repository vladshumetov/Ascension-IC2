param(
    [string]$ProjectRoot = "",
    [string]$OutDir = "",
    # Comma-separated lane counts, e.g. "5,7". A string is used so that -File invocations
    # pass it intact instead of joining the values into one number.
    [string]$Lanes = "5,7"
)

# Generates the machine GUI textures for the Quantum (5 lanes) and Singular (7 lanes) tiers.
# The layout and palette are the same as the approved nano GUI (examples/nano/nano_machine_approved.png).
# Only the number of input/output lanes changes; the lanes stay centred on the body (centre x = 120).
# Running with -Lanes 3 reproduces the approved nano texture and is used as a self-check.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $OutDir = Join-Path $ProjectRoot "src\main\resources\assets\ic2_tiers\textures\gui"
}
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

$names = @{
    3 = "nano_machine"
    5 = "quantum_machine"
    7 = "singular_machine"
}

function C([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Px($bmp, [int]$x, [int]$y, [string]$hex) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height) {
        $bmp.SetPixel($x, $y, (C $hex))
    }
}

function Rect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [string]$hex) {
    for ($y = $y0; $y -le $y1; $y++) {
        for ($x = $x0; $x -le $x1; $x++) {
     Px $bmp $x $y $hex
        }
    }
}

function Border($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [string]$hex) {
    for ($x = $x0; $x -le $x1; $x++) {
        Px $bmp $x $y0 $hex
        Px $bmp $x $y1 $hex
    }
    for ($y = $y0; $y -le $y1; $y++) {
        Px $bmp $x0 $y $hex
        Px $bmp $x1 $y $hex
    }
}

function Draw-PlayerSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 1) ($y - 1) ($x + 16) ($y + 16) '#8B8B8B'
    Border $bmp ($x - 1) ($y - 1) ($x + 16) ($y + 16) '#373737'
    Rect $bmp ($x - 1) ($y - 1) ($x + 16) ($y - 1) '#FFFFFF'
    Rect $bmp ($x - 1) ($y - 1) ($x - 1) ($y + 16) '#FFFFFF'
    Rect $bmp ($x - 1) ($y + 16) ($x + 16) ($y + 16) '#555555'
    Rect $bmp ($x + 16) ($y - 1) ($x + 16) ($y + 16) '#555555'
}

function Draw-MachineSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 3) ($y - 3) ($x + 18) ($y + 18) '#0F1325'
    Border $bmp ($x - 3) ($y - 3) ($x + 18) ($y + 18) '#4D7C9A'
    Rect $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#09203F'
    Border $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#253C5E'
    Rect $bmp ($x - 2) ($y - 2) ($x + 15) ($y - 2) '#3F6C89'
    Rect $bmp ($x - 2) ($y - 2) ($x - 2) ($y + 15) '#3F6C89'
    Rect $bmp ($x - 2) ($y + 17) ($x + 17) ($y + 17) '#041833'
    Rect $bmp ($x + 17) ($y - 2) ($x + 17) ($y + 17) '#041833'
}

function Draw-UpgradeSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#09203F'
    Border $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#4D7C9A'
}

# Slot x for a row of $count machine slots with a 26 px pitch, centred on the body (slot centre = x + 8).
function Get-LaneX([int]$count, [int]$lane) {
    return 112 - ($count - 1) * 13 + $lane * 26
}

function New-MachineGui([int]$count, [string]$outPath) {
    $gui = New-Object System.Drawing.Bitmap(256, 256, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    # Body: one flat rectangle x 16..224, y 4..127. Outline on the edge, navy gradient inside.
    for ($y = 4; $y -le 127; $y++) {
        for ($x = 16; $x -le 224; $x++) {
     if ($x -eq 16 -or $x -eq 224 -or $y -eq 4 -or $y -eq 127) {
         $gui.SetPixel($x, $y, (C '#05080C'))
  continue
     }
     $t2 = [Math]::Min(1.0, [Math]::Max(0.0, ($y - 4) / 124.0))
     $r = [int](27 + (16 - 27) * $t2)
     $g = [int](36 + (22 - 36) * $t2)
            $bl = [int](69 + (46 - 69) * $t2)
     $gui.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $bl))
        }
    }

    # Top highlight, joint line between body and inventory, title banner.
    Rect $gui 22 7 218 7 '#B7E0EA'
    Rect $gui 16 128 224 128 '#4D7C9A'
    Rect $gui 16 129 224 129 '#B7E0EA'
    Rect $gui 50 2 189 14 '#1F274F'
    Border $gui 50 2 189 14 '#4D7C9A'
    Rect $gui 51 3 188 3 '#B7E0EA'

    # Upgrade sockets: two per side, same positions as the approved texture.
    foreach ($pos in @(@(8, 28), @(8, 62), @(216, 28), @(216, 62))) {
 Draw-UpgradeSlot $gui $pos[0] $pos[1]
    }

    # Machine inputs (y 28) and outputs (y 76), one per lane.
    for ($i = 0; $i -lt $count; $i++) {
 $x = Get-LaneX $count $i
        Draw-MachineSlot $gui $x 28
 Draw-MachineSlot $gui $x 76
    }

    # Energy bar under the outputs (frame 24..215 x 100..106, well 25..214 x 101..105).
    Rect $gui 24 100 215 106 '#0F1325'
    Border $gui 24 100 215 106 '#4D7C9A'
    Rect $gui 25 101 214 105 '#3D001C'
    foreach ($k in 1..9) {
        $tx = 25 + [int](190 * $k / 10)
        Rect $gui $tx 101 $tx 105 '#63121A'
    }

    # Vanilla-style player inventory panel joined under the body.
    Rect $gui 32 132 207 217 '#C6C6C6'
    Border $gui 32 132 207 217 '#373737'
    Rect $gui 33 133 206 133 '#FFFFFF'
    Rect $gui 33 133 33 216 '#FFFFFF'
    Rect $gui 33 217 207 217 '#555555'
    Rect $gui 207 133 207 217 '#555555'

    foreach ($row in 0..2) {
 foreach ($col in 0..8) {
     Draw-PlayerSlot $gui (40 + $col * 18) (140 + $row * 18)
        }
    }
    foreach ($col in 0..8) {
        Draw-PlayerSlot $gui (40 + $col * 18) 198
    }

    $gui.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $gui.Dispose()
}

$laneCounts = $Lanes -split "," | ForEach-Object { [int]$_.Trim() }
foreach ($count in $laneCounts) {
    if (-not $names.ContainsKey($count)) {
 throw "Unsupported lane count: $count (expected 3, 5 or 7)"
    }
    $file = Join-Path $OutDir ($names[$count] + ".png")
    New-MachineGui $count $file
    Write-Output "Generated $file ($count lanes, first slot x=$(Get-LaneX $count 0), last slot x=$(Get-LaneX $count ($count - 1)))"
}
