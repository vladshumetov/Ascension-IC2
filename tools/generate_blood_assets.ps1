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
$metal = C '#3A1218'
$metalLight = C '#6E2A2F'
$blood = C '#A01818'
$bloodLight = C '#E05A5A'

function New-MachineTexture([bool]$top) {
    $bmp = New-Bmp 16 16
    Rect $bmp 0 0 15 15 $dark
    Rect $bmp 1 1 14 14 $metal
    Rect $bmp 1 1 14 2 $metalLight
    Rect $bmp 1 14 14 14 (C '#100508')
    Rect $bmp 14 1 14 14 (C '#100508')
    foreach ($pos in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) { Px $bmp $pos[0] $pos[1] $metalLight }
    if ($top) {
        Rect $bmp 3 3 12 12 (C '#16080A')
        foreach ($y in 4, 7, 10) {
            Rect $bmp 4 $y 11 $y (C '#5A1010')
            Rect $bmp 4 ($y + 1) 11 ($y + 1) $blood
        }
        Px $bmp 7 5 $bloodLight
        Px $bmp 8 8 $bloodLight
    } else {
        Rect $bmp 4 4 11 11 (C '#16080A')
        Rect $bmp 5 7 10 7 $blood
        Rect $bmp 5 8 10 8 $bloodLight
        Rect $bmp 6 5 9 5 (C '#5A1010')
        Rect $bmp 6 10 9 10 (C '#5A1010')
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
$topColor = C '#2A0E12'
$bottomColor = C '#17080B'
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
Rect $gui 8 3 231 3 (C '#C9A3A3')

# Title banner.
Rect $gui 50 2 189 14 (C '#3A1218')
Rect $gui 50 2 189 2 (C '#7A2A2A')
Rect $gui 50 14 189 14 (C '#7A2A2A')
Rect $gui 50 2 50 14 (C '#7A2A2A')
Rect $gui 189 2 189 14 (C '#7A2A2A')
Rect $gui 51 3 188 3 (C '#E0B7B7')

function Draw-MachineSlot([int]$x, [int]$y) {
    Rect $gui ($x - 3) ($y - 3) ($x + 18) ($y + 18) (C '#140609')
    Rect $gui ($x - 3) ($y - 3) ($x + 18) ($y - 3) (C '#7A2A2A')
    Rect $gui ($x - 3) ($y + 18) ($x + 18) ($y + 18) (C '#7A2A2A')
    Rect $gui ($x - 3) ($y - 3) ($x - 3) ($y + 18) (C '#7A2A2A')
    Rect $gui ($x + 18) ($y - 3) ($x + 18) ($y + 18) (C '#7A2A2A')
    Rect $gui ($x - 2) ($y - 2) ($x + 17) ($y + 17) (C '#0D0406')
}
Draw-MachineSlot 112 28
foreach ($pos in @(@(8, 28), @(8, 62), @(216, 28), @(216, 62))) { Draw-MachineSlot $pos[0] $pos[1] }

# Two bars: blood (top) and energy (bottom).
foreach ($y in 96, 104) {
    Rect $gui 24 ($y - 1) 215 ($y + 6) (C '#140609')
    Rect $gui 24 ($y - 1) 215 ($y - 1) (C '#7A2A2A')
    Rect $gui 24 ($y + 6) 215 ($y + 6) (C '#7A2A2A')
    Rect $gui 24 ($y - 1) 24 ($y + 6) (C '#7A2A2A')
    Rect $gui 215 ($y - 1) 215 ($y + 6) (C '#7A2A2A')
    Rect $gui 25 $y 214 ($y + 4) (C '#1A090C')
}

# Player inventory.
Rect $gui 32 132 207 217 (C '#3A2A2A')
Rect $gui 32 132 207 132 (C '#7A2A2A')
Rect $gui 32 217 207 217 (C '#7A2A2A')
Rect $gui 32 132 32 217 (C '#7A2A2A')
Rect $gui 207 132 207 217 (C '#7A2A2A')
for ($row = 0; $row -lt 3; $row++) {
    for ($col = 0; $col -lt 9; $col++) {
        $x = 40 + $col * 18; $y = 140 + $row * 18
        Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y + 16) (C '#140609')
        Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y - 1) (C '#7A2A2A')
        Rect $gui ($x - 1) ($y + 16) ($x + 16) ($y + 16) (C '#0D0406')
        Rect $gui ($x - 1) ($y - 1) ($x - 1) ($y + 16) (C '#7A2A2A')
        Rect $gui ($x + 16) ($y - 1) ($x + 16) ($y + 16) (C '#0D0406')
    }
}
foreach ($col in 0..8) {
    $x = 40 + $col * 18; $y = 198
    Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y + 16) (C '#140609')
    Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y - 1) (C '#7A2A2A')
    Rect $gui ($x - 1) ($y + 16) ($x + 16) ($y + 16) (C '#0D0406')
    Rect $gui ($x - 1) ($y - 1) ($x - 1) ($y + 16) (C '#7A2A2A')
    Rect $gui ($x + 16) ($y - 1) ($x + 16) ($y + 16) (C '#0D0406')
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
