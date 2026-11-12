param(
    [string]$ProjectRoot = ""
)

# Generates natural gas assets: fluid textures, bucket/cell icons, gas generator block model,
# loot/tags, lang keys and the underground spring worldgen data.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
$assets = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2"
$data = Join-Path $ProjectRoot "src\main\resources\data\ascension_ic2"
foreach ($dir in @("$assets\textures\block", "$assets\textures\item", "$assets\blockstates", "$assets\models\block", "$assets\models\item", "$data\loot_table\blocks", "$data\worldgen\configured_feature", "$data\worldgen\placed_feature", "$data\neoforge\biome_modifier", (Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block\mineable"))) {
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

$metalDark = C '#1B1F27'
$metal = C '#2C323D'
$metalLight = C '#4A5360'
$green = C '#9AE870'
$greenDark = C '#3E7A35'

function New-MachineTexture([bool]$top) {
    $bmp = New-Bmp 16 16
    Rect $bmp 0 0 15 15 $metalDark
    Rect $bmp 1 1 14 14 $metal
    Rect $bmp 1 1 14 2 $metalLight
    Rect $bmp 1 14 14 14 (C '#10131A')
    Rect $bmp 14 1 14 14 (C '#10131A')
    foreach ($pos in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) {
        Px $bmp $pos[0] $pos[1] $metalLight
    }
    if ($top) {
        Rect $bmp 3 3 12 12 (C '#151A20')
        foreach ($y in 4, 7, 10) {
            Rect $bmp 4 $y 11 $y $greenDark
            Rect $bmp 4 ($y + 1) 11 ($y + 1) $green
        }
        Px $bmp 7 5 (C '#D6FFC0')
        Px $bmp 8 8 (C '#D6FFC0')
    } else {
        Rect $bmp 4 4 11 11 (C '#151A20')
        Rect $bmp 5 7 10 7 $greenDark
        Rect $bmp 5 8 10 8 $green
        Rect $bmp 6 5 9 5 $greenDark
        Rect $bmp 6 10 9 10 $greenDark
    }
    return $bmp
}

$top = New-MachineTexture $true
$side = New-MachineTexture $false
$bottom = New-MachineTexture $false
$top.Save((Join-Path $assets "textures\block\gas_generator_top.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$side.Save((Join-Path $assets "textures\block\gas_generator_side.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bottom.Save((Join-Path $assets "textures\block\gas_generator_bottom.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$top.Dispose(); $side.Dispose(); $bottom.Dispose()

# Fluid textures: semi-transparent green swirls.
foreach ($kind in "still", "flowing") {
    $bmp = New-Bmp 16 16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $n = (($x * 5 + $y * 11 + ($(if ($kind -eq 'flowing') { $x * $y } else { 0 }))) % 9)
            $alpha = 150 + (($n * 7) % 60)
            $g = 150 + (($n * 9) % 80)
            Px $bmp $x $y ([System.Drawing.Color]::FromArgb($alpha, 90, $g, 70))
        }
    }
    $bmp.Save((Join-Path $assets "textures\block\natural_gas_$kind.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Bucket icon.
$bucket = New-Bmp 16 16
Rect $bucket 3 5 12 14 $metal
Rect $bucket 3 5 12 5 $metalLight
Rect $bucket 3 13 12 14 (C '#10131A')
Rect $bucket 4 6 11 8 (C '#7FCB5A')
Rect $bucket 4 6 11 6 (C '#B6F58E')
Rect $bucket 2 5 2 12 $metalLight
Rect $bucket 13 5 13 12 $metalLight
$bucket.Save((Join-Path $assets "textures\item\natural_gas_bucket.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bucket.Dispose()

# Cell icon.
$cell = New-Bmp 16 16
Rect $cell 5 1 10 14 $metal
Rect $cell 5 1 10 2 $metalLight
Rect $cell 5 13 10 14 (C '#10131A')
Rect $cell 4 3 11 4 $metalLight
Rect $cell 6 6 9 10 (C '#7FCB5A')
Rect $cell 6 6 9 7 (C '#B6F58E')
$cell.Save((Join-Path $assets "textures\item\gas_cell.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$cell.Dispose()

Write-JsonFile (Join-Path $assets "blockstates\gas_generator.json") @{
    variants = @{ "" = @{ model = "ascension_ic2:block/gas_generator" } }
}
Write-JsonFile (Join-Path $assets "models\block\gas_generator.json") @{
    parent = "minecraft:block/cube"
    textures = @{
        particle = "ascension_ic2:block/gas_generator_side"
        down = "ascension_ic2:block/gas_generator_bottom"
        up = "ascension_ic2:block/gas_generator_top"
        north = "ascension_ic2:block/gas_generator_side"
        south = "ascension_ic2:block/gas_generator_side"
        east = "ascension_ic2:block/gas_generator_side"
        west = "ascension_ic2:block/gas_generator_side"
    }
}
Write-JsonFile (Join-Path $assets "blockstates\natural_gas.json") @{
    variants = @{ "" = @{ model = "ascension_ic2:block/natural_gas" } }
}
Write-JsonFile (Join-Path $assets "models\block\natural_gas.json") @{
    textures = @{ particle = "ascension_ic2:block/natural_gas_still" }
}
Write-JsonFile (Join-Path $assets "models\item\gas_generator.json") @{
    parent = "ascension_ic2:block/gas_generator"
}
Write-JsonFile (Join-Path $assets "models\item\natural_gas_bucket.json") @{
    parent = "minecraft:item/generated"
    textures = @{ layer0 = "ascension_ic2:item/natural_gas_bucket" }
}
Write-JsonFile (Join-Path $assets "models\item\gas_cell.json") @{
    parent = "minecraft:item/generated"
    textures = @{ layer0 = "ascension_ic2:item/gas_cell" }
}
Write-JsonFile (Join-Path $data "loot_table\blocks\gas_generator.json") @{
    type = "minecraft:block"
    pools = @(
        @{
            rolls = 1
            bonus_rolls = 0
            entries = @(@{ type = "minecraft:item"; name = "ascension_ic2:gas_generator" })
            conditions = @(@{ condition = "minecraft:survives_explosion" })
        }
    )
    random_sequence = "ascension_ic2:blocks/gas_generator"
}

function Merge-Tag([string]$Path, [string[]]$Ids) {
    $values = @()
    if (Test-Path $Path) {
        $existing = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
        $values = @($existing.values)
    }
    foreach ($id in $Ids) { if ($values -notcontains $id) { $values += $id } }
    Write-JsonFile $Path @{ replace = $false; values = $values }
}
$tags = Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block"
Merge-Tag (Join-Path $tags "mineable\pickaxe.json") @("ascension_ic2:gas_generator")
Merge-Tag (Join-Path $tags "needs_iron_tool.json") @("ascension_ic2:gas_generator")

# Worldgen: spring of natural gas inside stone/deepslate.
Write-JsonFile (Join-Path $data "worldgen\configured_feature\natural_gas_spring.json") @{
    type = "minecraft:spring_feature"
    config = @{
        hole_count = 1
        requires_block_below = $true
        rock_count = 4
        state = @{ Name = "ascension_ic2:natural_gas" }
        valid_blocks = @(
            "minecraft:stone",
            "minecraft:granite",
            "minecraft:diorite",
            "minecraft:andesite",
            "minecraft:deepslate",
            "minecraft:tuff",
            "minecraft:calcite"
        )
    }
}
Write-JsonFile (Join-Path $data "worldgen\placed_feature\natural_gas_spring.json") @{
    feature = "ascension_ic2:natural_gas_spring"
    placement = @(
        @{ type = "minecraft:count"; count = 8 },
        @{ type = "minecraft:in_square" },
        @{
            type = "minecraft:height_range"
            height = @{
                type = "minecraft:uniform"
                min_inclusive = @{ absolute = -56 }
                max_inclusive = @{ absolute = 40 }
            }
        },
        @{ type = "minecraft:biome" }
    )
}
Write-JsonFile (Join-Path $data "neoforge\biome_modifier\add_natural_gas_spring.json") @{
    type = "neoforge:add_features"
    biomes = "#minecraft:is_overworld"
    features = @("ascension_ic2:natural_gas_spring")
    step = "underground_decoration"
}

Write-Output "Generated natural gas assets and worldgen."
