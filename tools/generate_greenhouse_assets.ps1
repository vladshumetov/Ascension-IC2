param(
    [string]$ProjectRoot = ""
)

# Botania greenhouse assets: green GUI in the approved layout, block textures, models, loot.

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

$dark = C '#08160D'
$metal = C '#1E3A2A'
$metalLight = C '#4F8163'
$green = C '#2E8B57'
$greenLight = C '#9AE870'

function New-MachineTexture([bool]$top) {
    $bmp = New-Bmp 16 16
    Rect $bmp 0 0 15 15 $dark
    Rect $bmp 1 1 14 14 $metal
    Rect $bmp 1 1 14 2 $metalLight
    Rect $bmp 1 14 14 14 (C '#050C08')
    Rect $bmp 14 1 14 14 (C '#050C08')
    foreach ($pos in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) { Px $bmp $pos[0] $pos[1] $metalLight }
    if ($top) {
        Rect $bmp 3 3 12 12 (C '#0B1C11')
        foreach ($y in 4, 7, 10) {
            Rect $bmp 4 $y 11 $y $green
            Rect $bmp 4 ($y + 1) 11 ($y + 1) $greenLight
        }
        Px $bmp 7 5 $greenLight
        Px $bmp 8 8 $greenLight
    } else {
        Rect $bmp 4 4 11 11 (C '#0B1C11')
        Rect $bmp 5 7 10 7 $green
        Rect $bmp 5 8 10 8 $greenLight
        Rect $bmp 6 5 9 5 $green
        Rect $bmp 6 10 9 10 $green
    }
    return $bmp
}

$top = New-MachineTexture $true
$side = New-MachineTexture $false
$bottom = New-MachineTexture $false
$top.Save((Join-Path $assets "textures\block\botania_greenhouse_top.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$side.Save((Join-Path $assets "textures\block\botania_greenhouse_side.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bottom.Save((Join-Path $assets "textures\block\botania_greenhouse_bottom.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$top.Dispose(); $side.Dispose(); $bottom.Dispose()

$gui = New-Bmp 256 256
$topColor = C '#0E2A18'
$bottomColor = C '#07130B'
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
Rect $gui 8 3 231 3 (C '#B7E0EA')
Rect $gui 50 2 189 14 (C '#1F3F2B')
Rect $gui 50 2 189 2 (C '#4F8163')
Rect $gui 50 14 189 14 (C '#4F8163')
Rect $gui 50 2 50 14 (C '#4F8163')
Rect $gui 189 2 189 14 (C '#4F8163')
Rect $gui 51 3 188 3 (C '#B7E0EA')

function Draw-Slot([int]$x, [int]$y) {
    Rect $gui ($x - 3) ($y - 3) ($x + 18) ($y + 18) (C '#0A1A10')
    Rect $gui ($x - 3) ($y - 3) ($x + 18) ($y - 3) (C '#4F8163')
    Rect $gui ($x - 3) ($y + 18) ($x + 18) ($y + 18) (C '#4F8163')
    Rect $gui ($x - 3) ($y - 3) ($x - 3) ($y + 18) (C '#4F8163')
    Rect $gui ($x + 18) ($y - 3) ($x + 18) ($y + 18) (C '#4F8163')
    Rect $gui ($x - 2) ($y - 2) ($x + 17) ($y + 17) (C '#07130B')
}
foreach ($x in 86, 112, 138) { Draw-Slot $x 28 }
foreach ($x in 86, 112, 138) { Draw-Slot $x 58 }
foreach ($pos in @(@(8, 28), @(8, 62), @(216, 28), @(216, 62))) { Draw-Slot $pos[0] $pos[1] }

foreach ($y in 96, 104) {
    Rect $gui 24 ($y - 1) 215 ($y + 6) (C '#0A1A10')
    Rect $gui 24 ($y - 1) 215 ($y - 1) (C '#4F8163')
    Rect $gui 24 ($y + 6) 215 ($y + 6) (C '#4F8163')
    Rect $gui 24 ($y - 1) 24 ($y + 6) (C '#4F8163')
    Rect $gui 215 ($y - 1) 215 ($y + 6) (C '#4F8163')
    Rect $gui 25 $y 214 ($y + 4) (C '#07130B')
}

Rect $gui 32 132 207 217 (C '#2A3A2E')
Rect $gui 32 132 207 132 (C '#4F8163')
Rect $gui 32 217 207 217 (C '#4F8163')
Rect $gui 32 132 32 217 (C '#4F8163')
Rect $gui 207 132 207 217 (C '#4F8163')
for ($row = 0; $row -lt 3; $row++) {
    for ($col = 0; $col -lt 9; $col++) {
        $x = 40 + $col * 18; $y = 140 + $row * 18
        Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y + 16) (C '#0A1A10')
        Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y - 1) (C '#4F8163')
        Rect $gui ($x - 1) ($y + 16) ($x + 16) ($y + 16) (C '#07130B')
        Rect $gui ($x - 1) ($y - 1) ($x - 1) ($y + 16) (C '#4F8163')
        Rect $gui ($x + 16) ($y - 1) ($x + 16) ($y + 16) (C '#07130B')
    }
}
foreach ($col in 0..8) {
    $x = 40 + $col * 18; $y = 198
    Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y + 16) (C '#0A1A10')
    Rect $gui ($x - 1) ($y - 1) ($x + 16) ($y - 1) (C '#4F8163')
    Rect $gui ($x - 1) ($y + 16) ($x + 16) ($y + 16) (C '#07130B')
    Rect $gui ($x - 1) ($y - 1) ($x - 1) ($y + 16) (C '#4F8163')
    Rect $gui ($x + 16) ($y - 1) ($x + 16) ($y + 16) (C '#07130B')
}
$gui.Save((Join-Path $assets "textures\gui\botania_greenhouse.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$gui.Dispose()

Write-JsonFile (Join-Path $assets "blockstates\botania_greenhouse.json") @{
    variants = @{ "" = @{ model = "ascension_ic2:block/botania_greenhouse" } }
}
Write-JsonFile (Join-Path $assets "models\block\botania_greenhouse.json") @{
    parent = "minecraft:block/cube"
    textures = @{
        particle = "ascension_ic2:block/botania_greenhouse_side"
        down = "ascension_ic2:block/botania_greenhouse_bottom"
        up = "ascension_ic2:block/botania_greenhouse_top"
        north = "ascension_ic2:block/botania_greenhouse_side"
        south = "ascension_ic2:block/botania_greenhouse_side"
        east = "ascension_ic2:block/botania_greenhouse_side"
        west = "ascension_ic2:block/botania_greenhouse_side"
    }
}
Write-JsonFile (Join-Path $assets "models\item\botania_greenhouse.json") @{ parent = "ascension_ic2:block/botania_greenhouse" }
Write-JsonFile (Join-Path $data "loot_table\blocks\botania_greenhouse.json") @{
    type = "minecraft:block"
    pools = @(
        @{
            rolls = 1
            bonus_rolls = 0
            entries = @(@{ type = "minecraft:item"; name = "ascension_ic2:botania_greenhouse" })
            conditions = @(@{ condition = "minecraft:survives_explosion" })
        }
    )
    random_sequence = "ascension_ic2:blocks/botania_greenhouse"
}

function Merge-Tag([string]$Path, [string[]]$Ids) {
    $values = @()
    if (Test-Path $Path) { $values = @((Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json).values) }
    foreach ($id in $Ids) { if ($values -notcontains $id) { $values += $id } }
    Write-JsonFile $Path @{ replace = $false; values = $values }
}
$tags = Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block"
Merge-Tag (Join-Path $tags "mineable\pickaxe.json") @("ascension_ic2:botania_greenhouse")
Merge-Tag (Join-Path $tags "needs_iron_tool.json") @("ascension_ic2:botania_greenhouse")

Write-Output "Generated Botania greenhouse assets."
