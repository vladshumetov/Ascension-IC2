param(
    [string]$ProjectRoot = "",
    [string]$Ic2Jar = ""
)

# Generates the high-tier energy blocks: cables for tiers 6..14 and solar panels for tiers 1..14.
# No recipes on purpose. Cables use one recoloured glass fibre texture; all solar panels share
# the IC2 solar generator top texture.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
if ([string]::IsNullOrWhiteSpace($Ic2Jar)) {
    $Ic2Jar = Join-Path $ProjectRoot "libs\ic2r-neoforge-1.2.11.jar"
}

$assets = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2"
$data = Join-Path $ProjectRoot "src\main\resources\data\ascension_ic2"
$blockTextureDir = Join-Path $assets "textures\block"
$blockstateDir = Join-Path $assets "blockstates"
$blockModelDir = Join-Path $assets "models\block"
$itemModelDir = Join-Path $assets "models\item"
$lootDir = Join-Path $data "loot_table\blocks"
$tagBlockDir = Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block"
$tagMineableDir = Join-Path $tagBlockDir "mineable"
foreach ($dir in @($blockTextureDir, $blockstateDir, $blockModelDir, $itemModelDir, $lootDir, $tagMineableDir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

function Write-JsonFile([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}

function Convert-Tint([System.Drawing.Bitmap]$Src, [int]$BaseR, [int]$BaseG, [int]$BaseB) {
    $dst = New-Object System.Drawing.Bitmap($Src.Width, $Src.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $Src.Height; $y++) {
        for ($x = 0; $x -lt $Src.Width; $x++) {
            $c = $Src.GetPixel($x, $y)
            if ($c.A -eq 0) {
                $dst.SetPixel($x, $y, $c)
                continue
            }
            $lum = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
            $f = [Math]::Min(1.2, [Math]::Max(0.35, $lum / 200.0))
            $r = [Math]::Min(255, [Math]::Max(0, [int]($BaseR * $f)))
            $g = [Math]::Min(255, [Math]::Max(0, [int]($BaseG * $f)))
            $b = [Math]::Min(255, [Math]::Max(0, [int]($BaseB * $f)))
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $r, $g, $b))
        }
    }
    return $dst
}

$zip = [System.IO.Compression.ZipFile]::OpenRead($Ic2Jar)
try {
    $entry = $zip.GetEntry("assets/ic2/textures/block/wiring/cable/glass_cable_0_white.png")
    if (-not $entry) {
        $entry = $zip.GetEntry("assets/ic2/textures/block/wiring/cable/glass_cable_0_light_blue.png")
    }
    if (-not $entry) {
        throw "glass fibre cable texture not found in $Ic2Jar"
    }
    $stream = $entry.Open()
    $source = New-Object System.Drawing.Bitmap($stream)
}
finally {
    $zip.Dispose()
}

try {
    # Bright cyan fibre for the addon energy cables.
    $cable = Convert-Tint $source 102 224 255
    $cable.Save((Join-Path $blockTextureDir "energy_cable.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $cable.Dispose()
}
finally {
    $source.Dispose()
    $stream.Dispose()
}

function Write-SimpleBlock([string]$Id, [string]$Texture) {
    Write-JsonFile (Join-Path $blockstateDir "$Id.json") @{
        variants = @{ "" = @{ model = "ascension_ic2:block/$Id" } }
    }
    Write-JsonFile (Join-Path $blockModelDir "$Id.json") @{
        parent = "minecraft:block/cube_all"
        textures = @{ all = $Texture }
    }
    Write-JsonFile (Join-Path $itemModelDir "$Id.json") @{
        parent = "ascension_ic2:block/$Id"
    }
    Write-BlockLoot $Id
}

function Write-BlockLoot([string]$Id) {
    Write-JsonFile (Join-Path $lootDir "$Id.json") @{
        type = "minecraft:block"
        pools = @(
            @{
                rolls = 1
                bonus_rolls = 0
                entries = @(@{ type = "minecraft:item"; name = "ascension_ic2:$Id" })
                conditions = @(@{ condition = "minecraft:survives_explosion" })
            }
        )
        random_sequence = "ascension_ic2:blocks/$Id"
    }
}

function Write-CableBlock([string]$Id) {
    Write-JsonFile (Join-Path $blockstateDir "$Id.json") @{
        variants = @{ "" = @{ model = "ascension_ic2:block/energy_cable" } }
    }
    Write-JsonFile (Join-Path $itemModelDir "$Id.json") @{
        parent = "ascension_ic2:block/energy_cable"
    }
    Write-BlockLoot $Id
}

# Shared cable model: a small core with arms on all six sides, so it reads as a wire, not a cube.
$faceSet = @{
    down = @{ texture = "#all" }
    up = @{ texture = "#all" }
    north = @{ texture = "#all" }
    south = @{ texture = "#all" }
    east = @{ texture = "#all" }
    west = @{ texture = "#all" }
}
Write-JsonFile (Join-Path $blockModelDir "energy_cable.json") @{
    parent = "minecraft:block/block"
    textures = @{
        particle = "ascension_ic2:block/energy_cable"
        all = "ascension_ic2:block/energy_cable"
    }
    elements = @(
        @{ from = @(5, 5, 5); to = @(11, 11, 11); faces = $faceSet },
        @{ from = @(6, 6, 0); to = @(10, 10, 5); faces = $faceSet },
        @{ from = @(6, 6, 11); to = @(10, 10, 16); faces = $faceSet },
        @{ from = @(0, 6, 6); to = @(5, 10, 10); faces = $faceSet },
        @{ from = @(11, 6, 6); to = @(16, 10, 10); faces = $faceSet },
        @{ from = @(6, 0, 6); to = @(10, 5, 10); faces = $faceSet },
        @{ from = @(6, 11, 6); to = @(10, 16, 10); faces = $faceSet }
    )
}

$allIds = @(
    "ascension_ic2:nano_macerator", "ascension_ic2:quantum_macerator",
    "ascension_ic2:nano_electric_furnace", "ascension_ic2:quantum_electric_furnace",
    "ascension_ic2:nano_compressor", "ascension_ic2:quantum_compressor",
    "ascension_ic2:nano_extractor", "ascension_ic2:quantum_extractor",
    "ascension_ic2:nano_recycler", "ascension_ic2:quantum_recycler",
    "ascension_ic2:nano_casing", "ascension_ic2:quantum_casing"
)

for ($tier = 6; $tier -le 14; $tier++) {
    $id = "energy_cable_$tier"
    Write-CableBlock $id
    $allIds += "ascension_ic2:$id"
}

for ($tier = 1; $tier -le 14; $tier++) {
    $id = "solar_panel_$tier"
    Write-SimpleBlock $id "ic2:block/generator/electric/solar_generator_top"
    $allIds += "ascension_ic2:$id"
}

Write-JsonFile (Join-Path $tagMineableDir "pickaxe.json") @{ replace = $false; values = $allIds }
Write-JsonFile (Join-Path $tagBlockDir "needs_iron_tool.json") @{ replace = $false; values = $allIds }

Write-Output "Generated 9 cables and 14 solar panels."
