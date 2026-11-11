param(
    [string]$ProjectRoot = "",
    [string]$Ic2Jar = ""
)

# Generates the tier machine casing blocks: nano and quantum.
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

function Convert-TintIcon([System.Drawing.Bitmap]$Src, [int]$BaseR, [int]$BaseG, [int]$BaseB) {
    $small = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($small)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.DrawImage($Src, 0, 0, 16, 16)
    $graphics.Dispose()

    $dst = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $c = $small.GetPixel($x, $y)
            if ($c.A -eq 0) {
                $dst.SetPixel($x, $y, $c)
                continue
            }
            $lum = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
            $f = [Math]::Min(1.5, [Math]::Max(0.28, $lum / 128.0))
            $r = [Math]::Min(255, [Math]::Max(0, [int]($BaseR * $f)))
            $g = [Math]::Min(255, [Math]::Max(0, [int]($BaseG * $f)))
            $b = [Math]::Min(255, [Math]::Max(0, [int]($BaseB * $f)))
            $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $r, $g, $b))
        }
    }
    $small.Dispose()
    return $dst
}

function Read-Texture([string]$EntryPath) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Ic2Jar)
    try {
        $entry = $zip.GetEntry($EntryPath)
        if (-not $entry) {
            throw "$EntryPath not found in $Ic2Jar"
        }
        $stream = $entry.Open()
        return @{ Bitmap = (New-Object System.Drawing.Bitmap($stream)); Stream = $stream }
    }
    finally {
        $zip.Dispose()
    }
}

$textures = @(
    @{ Id = "nano_casing"; Entry = "assets/ic2/textures/block/general/machine_top.png"; R = 159; G = 182; B = 196 },
    @{ Id = "quantum_casing"; Entry = "assets/ic2/textures/block/resource/advanced_machine.png"; R = 106; G = 174; B = 157 }
)

foreach ($texture in $textures) {
    $source = Read-Texture $texture.Entry
    try {
        $icon = Convert-TintIcon $source.Bitmap $texture.R $texture.G $texture.B
        $icon.Save((Join-Path $blockTextureDir "$($texture.Id).png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $icon.Dispose()
    }
    finally {
        $source.Bitmap.Dispose()
        $source.Stream.Dispose()
    }
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

Write-Output "Generated nano and quantum machine casing blocks."
