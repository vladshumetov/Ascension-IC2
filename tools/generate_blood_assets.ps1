param(
    [string]$ProjectRoot = ""
)

# Blood generator assets: dark crimson GUI in the approved layout, block textures, models, loot.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
$assets = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2"
$data = Join-Path $ProjectRoot "src\main\resources\data\ascension_ic2"
foreach ($dir in @("$assets\textures\block", "$assets\textures\gui", "$assets\blockstates", "$assets\models\block", "$assets\models\item", "$data\loot_table\blocks", (Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block\mineable"))) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

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
function New-Bmp([int]$w, [int]$h) { return New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }

$dark = C '#1B0A0D'
$metal = C '#563258'
$metalLight = C '#6E2A2F'
$blood = C '#A01818'
$bloodLight = C '#E05A5A'

function New-MachineTexture([bool]$top) {
    $bmp = New-Bmp 16 16
    $frame = C '#0F1013'
    $metal = C '#36373F'
    $metalMid = C '#56545D'
    $metalLight = C '#756D7B'
    $metalHigh = C '#B2A8B4'
    $red = C '#E70010'
    $redDark = C '#A80025'
    $orange = C '#FF7027'
    $deep = C '#660026'

    Rect $bmp 0 0 15 15 $frame
    Rect $bmp 1 1 14 14 $metal
    # Bevel.
    Rect $bmp 1 1 14 2 $metalLight
    Rect $bmp 1 1 2 14 $metalMid
    Rect $bmp 1 14 14 14 (C '#080F0D')
    Rect $bmp 14 1 14 14 (C '#080F0D')
    # Corner plates with bolts.
    foreach ($pos in @(@(1, 1), @(12, 1), @(1, 12), @(12, 12))) {
        Rect $bmp $pos[0] $pos[1] ($pos[0] + 2) ($pos[1] + 2) $metalMid
        Px $bmp $pos[0] $pos[1] $metalHigh
        Px $bmp ($pos[0] + 2) ($pos[1] + 2) $frame
    }
    # Recessed inner panel.
    Rect $bmp 4 4 11 11 (C '#252529')
    Rect $bmp 4 4 11 4 $frame
    Rect $bmp 4 11 11 11 $frame
    Rect $bmp 4 4 4 11 $frame
    Rect $bmp 11 4 11 11 $frame
    if ($top) {
        # Central cross core with orange/red heat accents.
        Rect $bmp 7 4 8 11 $redDark
        Rect $bmp 4 7 11 8 $redDark
        Rect $bmp 7 6 8 9 $red
        Rect $bmp 6 7 9 8 $red
        Px $bmp 7 7 $orange
        Px $bmp 8 8 $orange
        Px $bmp 7 8 $metalHigh
        # Vent slots in the corners.
        foreach ($pos in @(@(5, 5), @(10, 5), @(5, 10), @(10, 10))) {
            Px $bmp $pos[0] $pos[1] $metalLight
        }
        Rect $bmp 5 5 6 6 $deep
        Rect $bmp 9 5 10 6 $deep
        Rect $bmp 5 9 6 10 $deep
        Rect $bmp 9 9 10 10 $deep
    } else {
        # Side: twin red rails and central vent.
        Rect $bmp 5 5 6 10 $redDark
        Rect $bmp 9 5 10 10 $redDark
        Rect $bmp 7 5 8 10 $metalMid
        Rect $bmp 7 7 8 8 $orange
        Rect $bmp 5 12 10 12 $deep
        Px $bmp 5 12 $red
        Px $bmp 10 12 $red
    }
    return $bmp
}
$top = New-MachineTexture $true
$side = New-MachineTexture $false
$bottom = New-MachineTexture $false
$top.Save((Join-Path $assets "textures\block\blood_generator_top.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$side.Save((Join-Path $assets "textures\block\blood_generator_side.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bottom.Save((Join-Path $assets "textures\block\blood_generator_bottom.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$top.Dispose(); $side.Dispose(); $bottom.Dispose()

# GUI: 240x230 working area on a 256x256 canvas.
$gui = New-Bmp 256 256
$topColor = C '#261831'
$bottomColor = C '#2E1F3B'
for ($y = 0; $y -lt 230; $y++) {
    $t = $y / 229.0
    $r = [int]($topColor.R + ($bottomColor.R - $topColor.R) * $t)
    $g = [int]($topColor.G + ($bottomColor.G - $topColor.G) * $t)
    $b = [int]($topColor.B + ($bottomColor.B - $topColor.B) * $t)
    Rect $gui 0 $y 239 $y ([System.Drawing.Color]::FromArgb(255, $r, $g, $b))
}
Rect $gui 0 0 239 0 (C '#05080C')
Rect $gui 0 229 239 229 (C '#05080C')
Rect $gui 0 0 0 229 (C '#05080C')
Rect $gui 239 0 239 229 (C '#05080C')
Rect $gui 8 3 231 3 (C '#B2A8B4')

# Title banner.
Rect $gui 50 2 189 14 (C '#563258')
Rect $gui 50 2 189 2 (C '#8B8B8B')
Rect $gui 50 14 189 14 (C '#8B8B8B')
Rect $gui 50 2 50 14 (C '#8B8B8B')
Rect $gui 189 2 189 14 (C '#8B8B8B')
Rect $gui 51 3 188 3 (C '#FFE698')

function Draw-MachineSlot([int]$x, [int]$y) {
    Rect $gui ($x - 3) ($y - 3) ($x + 18) ($y + 18) (C '#252529')
    Rect $gui ($x - 3) ($y - 3) ($x + 18) ($y - 3) (C '#8B8B8B')
    Rect $gui ($x - 3) ($y + 18) ($x + 18) ($y + 18) (C '#8B8B8B')
    Rect $gui ($x - 3) ($y - 3) ($x - 3) ($y + 18) (C '#8B8B8B')
    Rect $gui ($x + 18) ($y - 3) ($x + 18) ($y + 18) (C '#8B8B8B')
    Rect $gui ($x - 2) ($y - 2) ($x + 17) ($y + 17) (C '#373737')
}
Draw-MachineSlot 112 28
foreach ($pos in @(@(8, 28), @(8, 62), @(216, 28), @(216, 62))) { Draw-MachineSlot $pos[0] $pos[1] }

# Two bars: blood (top) and energy (bottom).
foreach ($y in 96, 104) {
    Rect $gui 24 ($y - 1) 215 ($y + 6) (C '#252529')
    Rect $gui 24 ($y - 1) 215 ($y - 1) (C '#8B8B8B')
    Rect $gui 24 ($y + 6) 215 ($y + 6) (C '#8B8B8B')
    Rect $gui 24 ($y - 1) 24 ($y + 6) (C '#8B8B8B')
    Rect $gui 215 ($y - 1) 215 ($y + 6) (C '#8B8B8B')
    Rect $gui 25 $y 214 ($y + 4) (C '#1A090C')
}

# Player inventory.
Rect $gui 32 132 207 217 (C '#C6C6C6')
Rect $gui 32 132 207 132 (C '#8B8B8B')
Rect $gui 32 217 207 217 (C '#8B8B8B')
Rect $gui 32 132 32 217 (C '#8B8B8B')
Rect $gui 207 132 207 217 (C '#8B8B8B')
for ($row = 0; $row -lt 3; $row++) {
    for ($col = 0; $col -lt 9; $col++) {
        $x = 40 + $col * 18; $y = 140 + $row * 18
        Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y + 16) (C '#252529')
        Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y - 1) (C '#8B8B8B')
        Rect $gui ($x - 1) ($y + 16) ($x + 16) ($y + 16) (C '#373737')
        Rect $gui ($x - 1) ($y - 1) ($x - 1) ($y + 16) (C '#8B8B8B')
        Rect $gui ($x + 16) ($y - 1) ($x + 16) ($y + 16) (C '#373737')
    }
}
foreach ($col in 0..8) {
    $x = 40 + $col * 18; $y = 198
    Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y + 16) (C '#252529')
    Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y - 1) (C '#8B8B8B')
    Rect $gui ($x - 1) ($y + 16) ($x + 16) ($y + 16) (C '#373737')
    Rect $gui ($x - 1) ($y - 1) ($x - 1) ($y + 16) (C '#8B8B8B')
    Rect $gui ($x + 16) ($y - 1) ($x + 16) ($y + 16) (C '#373737')
}
$gui.Save((Join-Path $assets "textures\gui\blood_generator.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$gui.Dispose()

Write-JsonFile (Join-Path $assets "blockstates\blood_generator.json") @{
    variants = @{ "" = @{ model = "ascension_ic2:block/blood_generator" } }
}
Write-JsonFile (Join-Path $assets "models\block\blood_generator.json") @{
    parent = "minecraft:block/cube"
    textures = @{
        particle = "ascension_ic2:block/blood_generator_side"
        down = "ascension_ic2:block/blood_generator_bottom"
        up = "ascension_ic2:block/blood_generator_top"
        north = "ascension_ic2:block/blood_generator_side"
        south = "ascension_ic2:block/blood_generator_side"
        east = "ascension_ic2:block/blood_generator_side"
        west = "ascension_ic2:block/blood_generator_side"
    }
}
Write-JsonFile (Join-Path $assets "models\item\blood_generator.json") @{ parent = "ascension_ic2:block/blood_generator" }
Write-JsonFile (Join-Path $data "loot_table\blocks\blood_generator.json") @{
    type = "minecraft:block"
    pools = @(
        @{
            rolls = 1
            bonus_rolls = 0
            entries = @(@{ type = "minecraft:item"; name = "ascension_ic2:blood_generator" })
            conditions = @(@{ condition = "minecraft:survives_explosion" })
        }
    )
    random_sequence = "ascension_ic2:blocks/blood_generator"
}

function Merge-Tag([string]$Path, [string[]]$Ids) {
    $values = @()
    if (Test-Path $Path) { $values = @((Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json).values) }
    foreach ($id in $Ids) { if ($values -notcontains $id) { $values += $id } }
    Write-JsonFile $Path @{ replace = $false; values = $values }
}
$tags = Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block"
Merge-Tag (Join-Path $tags "mineable\pickaxe.json") @("ascension_ic2:blood_generator")
Merge-Tag (Join-Path $tags "needs_iron_tool.json") @("ascension_ic2:blood_generator")

Write-Output "Generated blood generator assets."
