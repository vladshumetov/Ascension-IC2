param(
    [string]$ProjectRoot = "",
    [string]$Ic2Jar = ""
)

# Generates the tier machine casing blocks: nano and quantum.
# Both are recoloured copies of the IC2 advanced machine casing (ic2:block/resource/advanced_machine),
# each in its tier colour:
#   nano    -> steel blue
#   quantum -> teal #6AAE9D
# Nano casing is built from advanced machine casings; quantum casing from two nano casings.

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
$recipeDir = Join-Path $data "recipe"
$tagBlockDir = Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block"
$tagMineableDir = Join-Path $tagBlockDir "mineable"
foreach ($dir in @($blockTextureDir, $blockstateDir, $blockModelDir, $itemModelDir, $lootDir, $recipeDir, $tagMineableDir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

function Write-JsonFile([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}

# Keep the advanced machine casing shading, shift the hue to the tier colour.
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
            # The IC2 casing texture is bright; normalise so its main tone lands on the tier colour.
            $f = [Math]::Min(1.15, [Math]::Max(0.35, $lum / 210.0))
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
    $entry = $zip.GetEntry("assets/ic2/textures/block/resource/advanced_machine.png")
    if (-not $entry) {
        throw "advanced_machine.png not found in $Ic2Jar"
    }
    $stream = $entry.Open()
    $source = New-Object System.Drawing.Bitmap($stream)
}
finally {
    $zip.Dispose()
}

try {
    $nano = Convert-Tint $source 159 182 196
    $nano.Save((Join-Path $blockTextureDir "nano_casing.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $nano.Dispose()

    $quantum = Convert-Tint $source 106 174 157
    $quantum.Save((Join-Path $blockTextureDir "quantum_casing.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $quantum.Dispose()
}
finally {
    $source.Dispose()
    $stream.Dispose()
}

foreach ($id in @("nano_casing", "quantum_casing")) {
    Write-JsonFile (Join-Path $blockstateDir "$id.json") @{
        variants = @{
            "" = @{ model = "ascension_ic2:block/$id" }
        }
    }

    Write-JsonFile (Join-Path $blockModelDir "$id.json") @{
        parent = "minecraft:block/cube_all"
        textures = @{ all = "ascension_ic2:block/$id" }
    }

    Write-JsonFile (Join-Path $itemModelDir "$id.json") @{
        parent = "ascension_ic2:block/$id"
    }

    Write-JsonFile (Join-Path $lootDir "$id.json") @{
        type = "minecraft:block"
        pools = @(
            @{
                rolls = 1
                bonus_rolls = 0
                entries = @(
                    @{ type = "minecraft:item"; name = "ascension_ic2:$id" }
                )
                conditions = @(
                    @{ condition = "minecraft:survives_explosion" }
                )
            }
        )
        random_sequence = "ascension_ic2:blocks/$id"
    }
}

# Mirror-symmetric layouts: the casing sits in the centre, its ingredients on the axes.
Write-JsonFile (Join-Path $recipeDir "nano_casing.json") @{
    type = "minecraft:crafting_shaped"
    category = "misc"
    pattern = @(" A ", "CMC", " A ")
    key = @{
        A = @{ item = "ic2:advanced_machine" }
        C = @{ item = "ic2:carbon_plate" }
        M = @{ item = "ic2:alloy" }
    }
    result = @{ id = "ascension_ic2:nano_casing"; count = 1 }
}

Write-JsonFile (Join-Path $recipeDir "quantum_casing.json") @{
    type = "minecraft:crafting_shaped"
    category = "misc"
    pattern = @(" N ", "ICI", " N ")
    key = @{
        N = @{ item = "ascension_ic2:nano_casing" }
        I = @{ item = "ic2:iridium" }
        C = @{ item = "ascension_ic2:quantum_circuit" }
    }
    result = @{ id = "ascension_ic2:quantum_casing"; count = 1 }
}

# Tags include all machine blocks and both casings, so any generator run keeps the full list.
$allIds = @(
    "ascension_ic2:nano_macerator",
    "ascension_ic2:quantum_macerator",
    "ascension_ic2:nano_electric_furnace",
    "ascension_ic2:quantum_electric_furnace",
    "ascension_ic2:nano_compressor",
    "ascension_ic2:quantum_compressor",
    "ascension_ic2:nano_extractor",
    "ascension_ic2:quantum_extractor",
    "ascension_ic2:nano_recycler",
    "ascension_ic2:quantum_recycler",
    "ascension_ic2:nano_casing",
    "ascension_ic2:quantum_casing"
)
Write-JsonFile (Join-Path $tagMineableDir "pickaxe.json") @{ replace = $false; values = $allIds }
Write-JsonFile (Join-Path $tagBlockDir "needs_iron_tool.json") @{ replace = $false; values = $allIds }

Write-Output "Generated nano and quantum machine casing blocks from the advanced machine texture."
