param(
    [string]$ProjectRoot = "",
    [string]$Ic2Jar = ""
)

# Generates the tier machine bases (frames): nano and quantum.
# Textures are recoloured and downscaled IC2 machine casings.

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

$nanoSrc = Read-Texture "assets/ic2/textures/block/general/machine_top.png"
try {
    $nano = Convert-TintIcon $nanoSrc.Bitmap 159 182 196
    $nano.Save((Join-Path $textureDir "nano_base.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $nano.Dispose()
}
finally {
    $nanoSrc.Bitmap.Dispose()
    $nanoSrc.Stream.Dispose()
}

$quantumSrc = Read-Texture "assets/ic2/textures/block/resource/advanced_machine.png"
try {
    $quantum = Convert-TintIcon $quantumSrc.Bitmap 106 174 157
    $quantum.Save((Join-Path $textureDir "quantum_base.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $quantum.Dispose()
}
finally {
    $quantumSrc.Bitmap.Dispose()
    $quantumSrc.Stream.Dispose()
}

foreach ($id in @("nano_base", "quantum_base")) {
    Write-JsonFile (Join-Path $itemModelDir "$id.json") @{
        parent = "minecraft:item/generated"
        textures = @{ layer0 = "ascension_ic2:item/$id" }
    }
}

Write-JsonFile (Join-Path $recipeDir "nano_base.json") @{
    type = "minecraft:crafting_shapeless"
    category = "misc"
    ingredients = @(
        @{ item = "ic2:advanced_machine" }
        @{ item = "ic2:carbon_plate" }
        @{ item = "ic2:carbon_plate" }
        @{ item = "ic2:carbon_plate" }
        @{ item = "ic2:carbon_plate" }
        @{ item = "ic2:alloy" }
        @{ item = "ic2:alloy" }
        @{ item = "ic2:alloy" }
        @{ item = "ic2:alloy" }
    )
    result = @{ id = "ascension_ic2:nano_base"; count = 1 }
}

Write-JsonFile (Join-Path $recipeDir "quantum_base.json") @{
    type = "minecraft:crafting_shapeless"
    category = "misc"
    ingredients = @(
        @{ item = "ic2:advanced_machine" }
        @{ item = "ic2:iridium" }
        @{ item = "ic2:iridium" }
        @{ item = "ic2:iridium" }
        @{ item = "ic2:iridium" }
        @{ item = "ic2:alloy" }
        @{ item = "ic2:alloy" }
        @{ item = "ic2:alloy" }
        @{ item = "ic2:alloy" }
    )
    result = @{ id = "ascension_ic2:quantum_base"; count = 1 }
}

Write-Output "Generated nano and quantum machine bases."
