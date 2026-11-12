param(
    [string]$ProjectRoot = "",
    [string]$Ic2Jar = ""
)

# Generates the four ASP-style solar panels: advanced, hybrid, ultimate hybrid, quantum.
# Textures are tinted copies of the IC2 solar generator top; no recipes.

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
$textureDir = Join-Path $assets "textures\block"
$blockstateDir = Join-Path $assets "blockstates"
$blockModelDir = Join-Path $assets "models\block"
$itemModelDir = Join-Path $assets "models\item"
$lootDir = Join-Path $data "loot_table\blocks"
$tagBlockDir = Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block"
$tagMineableDir = Join-Path $tagBlockDir "mineable"
foreach ($dir in @($textureDir, $blockstateDir, $blockModelDir, $itemModelDir, $lootDir, $tagMineableDir)) {
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
            $f = [Math]::Min(1.2, [Math]::Max(0.35, $lum / 210.0))
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
    $entry = $zip.GetEntry("assets/ic2/textures/block/generator/electric/solar_generator_top.png")
    if (-not $entry) {
        throw "solar_generator_top.png not found in $Ic2Jar"
    }
    $stream = $entry.Open()
    $source = New-Object System.Drawing.Bitmap($stream)
}
finally {
    $zip.Dispose()
}

$panels = @(
    @{ Id = "advanced_solar_panel"; R = 74; G = 144; B = 217 },
    @{ Id = "hybrid_solar_panel"; R = 88; G = 179; B = 104 },
    @{ Id = "ultimate_hybrid_solar_panel"; R = 224; G = 138; B = 46 },
    @{ Id = "quantum_solar_panel"; R = 139; G = 92; B = 246 }
)

try {
    foreach ($panel in $panels) {
        $tex = Convert-Tint $source $panel.R $panel.G $panel.B
        $tex.Save((Join-Path $textureDir "$($panel.Id).png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $tex.Dispose()
    }
}
finally {
    $source.Dispose()
    $stream.Dispose()
}

foreach ($panel in $panels) {
    $id = $panel.Id
    Write-JsonFile (Join-Path $blockstateDir "$id.json") @{
        variants = @{ "" = @{ model = "ascension_ic2:block/$id" } }
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
                entries = @(@{ type = "minecraft:item"; name = "ascension_ic2:$id" })
                conditions = @(@{ condition = "minecraft:survives_explosion" })
            }
        )
        random_sequence = "ascension_ic2:blocks/$id"
    }
}

# Merge into the existing block tags without dropping machine/cable entries.
function Merge-Tag([string]$Path, [string[]]$Ids) {
    $values = @()
    if (Test-Path $Path) {
        $existing = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
        $values = @($existing.values)
    }
    foreach ($id in $Ids) {
        if ($values -notcontains $id) { $values += $id }
    }
    Write-JsonFile $Path @{ replace = $false; values = $values }
}

$ids = $panels | ForEach-Object { "ascension_ic2:$($_.Id)" }
Merge-Tag (Join-Path $tagMineableDir "pickaxe.json") $ids
Merge-Tag (Join-Path $tagBlockDir "needs_iron_tool.json") $ids

Write-Output "Generated advanced, hybrid, ultimate hybrid and quantum solar panels."
