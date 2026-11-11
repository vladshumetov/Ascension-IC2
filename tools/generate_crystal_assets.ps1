param(
    [string]$ProjectRoot = "",
    [string]$Ic2Jar = ""
)

# Generates the tier crystals: quantum and singular.
# Icons are recoloured copies of the IC2 lapotron crystal.

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
$textureDir = Join-Path $assets "textures\item"
$itemModelDir = Join-Path $assets "models\item"
$recipeDir = Join-Path $data "recipe"
foreach ($dir in @($textureDir, $itemModelDir, $recipeDir)) {
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
    $entry = $zip.GetEntry("assets/ic2/textures/item/battery/lapotron_crystal_0.png")
    if (-not $entry) {
        throw "lapotron_crystal_0.png not found in $Ic2Jar"
    }
    $stream = $entry.Open()
    $source = New-Object System.Drawing.Bitmap($stream)
}
finally {
    $zip.Dispose()
}

try {
    # quantum: tier teal #6AAE9D; singular: violet.
    $quantum = Convert-Tint $source 106 174 157
    $quantum.Save((Join-Path $textureDir "quantum_crystal.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $quantum.Dispose()

    $singular = Convert-Tint $source 139 92 246
    $singular.Save((Join-Path $textureDir "singular_crystal.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $singular.Dispose()
}
finally {
    $source.Dispose()
    $stream.Dispose()
}

foreach ($id in @("quantum_crystal", "singular_crystal")) {
    Write-JsonFile (Join-Path $itemModelDir "$id.json") @{
        parent = "minecraft:item/generated"
        textures = @{ layer0 = "ascension_ic2:item/$id" }
    }
}

Write-JsonFile (Join-Path $recipeDir "quantum_crystal.json") @{
    type = "minecraft:crafting_shaped"
    category = "misc"
    pattern = @("LIL", "IQI", "LIL")
    key = @{
        L = @{ item = "ic2:lapotron_crystal" }
        I = @{ item = "ic2:iridium" }
        Q = @{ item = "ascension_ic2:quantum_circuit" }
    }
    result = @{ id = "ascension_ic2:quantum_crystal"; count = 1 }
}

Write-JsonFile (Join-Path $recipeDir "singular_crystal.json") @{
    type = "minecraft:crafting_shaped"
    category = "misc"
    pattern = @("QIQ", "ISI", "QIQ")
    key = @{
        Q = @{ item = "ascension_ic2:quantum_crystal" }
        I = @{ item = "ic2:iridium" }
        S = @{ item = "ascension_ic2:quantum_circuit" }
    }
    result = @{ id = "ascension_ic2:singular_crystal"; count = 1 }
}

Write-Output "Generated quantum and singular crystals."
