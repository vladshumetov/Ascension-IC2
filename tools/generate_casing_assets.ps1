param(
    [string]$ProjectRoot = ""
)

# Generates the tier machine casing blocks: nano and quantum.
# The blocks reuse the IC2 casing textures directly:
#   nano    -> ic2:block/resource/machine
#   quantum -> ic2:block/resource/advanced_machine
# Nano casing is built from advanced machine casings; quantum casing from two nano casings.

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

$assets = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2"
$data = Join-Path $ProjectRoot "src\main\resources\data\ascension_ic2"
$blockstateDir = Join-Path $assets "blockstates"
$blockModelDir = Join-Path $assets "models\block"
$itemModelDir = Join-Path $assets "models\item"
$lootDir = Join-Path $data "loot_table\blocks"
$recipeDir = Join-Path $data "recipe"
$tagBlockDir = Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block"
$tagMineableDir = Join-Path $tagBlockDir "mineable"
foreach ($dir in @($blockstateDir, $blockModelDir, $itemModelDir, $lootDir, $recipeDir, $tagMineableDir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

function Write-JsonFile([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}

$casings = @(
    @{ Id = "nano_casing"; Texture = "ic2:block/resource/machine" },
    @{ Id = "quantum_casing"; Texture = "ic2:block/resource/advanced_machine" }
)

foreach ($casing in $casings) {
    $id = $casing.Id
    Write-JsonFile (Join-Path $blockstateDir "$id.json") @{
        variants = @{
            "" = @{ model = "ascension_ic2:block/$id" }
        }
    }

    Write-JsonFile (Join-Path $blockModelDir "$id.json") @{
        parent = "minecraft:block/cube_all"
        textures = @{ all = $casing.Texture }
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

Write-Output "Generated nano and quantum machine casing blocks using IC2 textures."
