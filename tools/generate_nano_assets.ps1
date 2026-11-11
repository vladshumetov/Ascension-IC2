param(
    [string]$Ic2Jar = "",
    [string]$ProjectRoot = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
if ([string]::IsNullOrWhiteSpace($Ic2Jar)) {
    $Ic2Jar = Join-Path $ProjectRoot "libs\ic2r-neoforge-1.2.11.jar"
}

$assets = Join-Path $ProjectRoot "src\main\resources\assets\ic2_tiers"
$data = Join-Path $ProjectRoot "src\main\resources\data\ic2_tiers"
$textureDir = Join-Path $assets "textures\block\nano"
$blockstateDir = Join-Path $assets "blockstates"
$blockModelDir = Join-Path $assets "models\block"
$itemModelDir = Join-Path $assets "models\item"
$lootDir = Join-Path $data "loot_table\blocks"
$recipeDir = Join-Path $data "recipe"
$tagBlockDir = Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block"
$tagMineableDir = Join-Path $tagBlockDir "mineable"

foreach ($dir in @($textureDir, $blockstateDir, $blockModelDir, $itemModelDir, $lootDir, $recipeDir, $tagMineableDir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

$machineRoot = "assets/ic2/textures/block/machine/processing/basic"
$generalRoot = "assets/ic2/textures/block/general"

$machines = @(
    @{
        Id = "nano_macerator"
        Base = "ic2:macerator"
        Front = "$machineRoot/macerator_front.png"
        Side = "$machineRoot/macerator_leftrightback.png"
        Top = "$machineRoot/macerator_top.png"
        Bottom = "$machineRoot/macerator_bottom.png"
    },
    @{
        Id = "nano_electric_furnace"
        Base = "ic2:electric_furnace"
        Front = "$machineRoot/electric_furnace_front.png"
        Side = "$machineRoot/electric_furnace_leftrightback.png"
        Top = "$machineRoot/electric_furnace_top.png"
        Bottom = "$machineRoot/electric_furnace_bottom.png"
    },
    @{
        Id = "nano_compressor"
        Base = "ic2:compressor"
        Front = "$machineRoot/compressor_front.png"
        Side = "$machineRoot/compressor_leftrightback.png"
        Top = "$machineRoot/compressor_top.png"
        Bottom = "$machineRoot/compressor_bottom.png"
    },
    @{
        Id = "nano_extractor"
        Base = "ic2:extractor"
        Front = "$machineRoot/extractor_front.png"
        Side = "$machineRoot/extractor_leftright.png"
        Top = "$machineRoot/extractor_top.png"
        Bottom = "$machineRoot/extractor_bottom.png"
    },
    @{
        Id = "nano_recycler"
        Base = "ic2:recycler"
        Front = "$machineRoot/recycler_front.png"
        Side = "$machineRoot/recycler_leftrightback.png"
        Top = "$machineRoot/recycler_top.png"
        Bottom = "$machineRoot/recycler_bottom.png"
    }
)

function Write-JsonFile([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 12
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $json, $utf8)
}

function C([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Px($bmp, [int]$x, [int]$y, [string]$hex) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height) {
        $bmp.SetPixel($x, $y, (C $hex))
    }
}

function Rect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [string]$hex) {
    for ($y = $y0; $y -le $y1; $y++) {
        for ($x = $x0; $x -le $x1; $x++) {
            Px $bmp $x $y $hex
        }
    }
}

function Border($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [string]$hex) {
    for ($x = $x0; $x -le $x1; $x++) {
        Px $bmp $x $y0 $hex
        Px $bmp $x $y1 $hex
    }
    for ($y = $y0; $y -le $y1; $y++) {
        Px $bmp $x0 $y $hex
        Px $bmp $x1 $y $hex
    }
}

function Save($bmp, [string]$path) {
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Convert-Texture($Zip, [string]$EntryName, [string]$OutPath) {
    $entry = $Zip.GetEntry($EntryName)
    if ($null -eq $entry) {
        throw "Missing IC2 texture entry: $EntryName"
    }

    $stream = $entry.Open()
    try {
        $src = [System.Drawing.Bitmap]::FromStream($stream)
    }
    finally {
        $stream.Dispose()
    }

    try {
        $dst = New-Object System.Drawing.Bitmap($src.Width, $src.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        for ($y = 0; $y -lt $src.Height; $y++) {
            for ($x = 0; $x -lt $src.Width; $x++) {
                $c = $src.GetPixel($x, $y)
                if ($c.A -eq 0) {
                    $dst.SetPixel($x, $y, $c)
                    continue
                }

                $lum = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
                $r = [Math]::Min(255, [Math]::Max(0, [int]($lum * 0.88 + 12)))
                $g = [Math]::Min(255, [Math]::Max(0, [int]($lum * 0.92 + 14)))
                $b = [Math]::Min(255, [Math]::Max(0, [int]($lum * 0.98 + 18)))
                $dst.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $r, $g, $b))
            }
        }
        $dst.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $dst.Dispose()
    }
    finally {
        $src.Dispose()
    }
}

$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $Ic2Jar))
try {
    foreach ($machine in $machines) {
        $id = $machine.Id
        $front = "${id}_front.png"
        $side = "${id}_side.png"
        $top = "${id}_top.png"
        $bottom = "${id}_bottom.png"

        Convert-Texture $zip $machine.Front (Join-Path $textureDir $front)
        Convert-Texture $zip $machine.Side (Join-Path $textureDir $side)
        Convert-Texture $zip $machine.Top (Join-Path $textureDir $top)
        Convert-Texture $zip $machine.Bottom (Join-Path $textureDir $bottom)

        Write-JsonFile (Join-Path $blockstateDir "$id.json") @{
            variants = @{
                "facing=north" = @{ model = "ic2_tiers:block/$id" }
                "facing=east" = @{ model = "ic2_tiers:block/$id"; y = 90 }
                "facing=south" = @{ model = "ic2_tiers:block/$id"; y = 180 }
                "facing=west" = @{ model = "ic2_tiers:block/$id"; y = 270 }
            }
        }

        Write-JsonFile (Join-Path $blockModelDir "$id.json") @{
            parent = "minecraft:block/cube"
            textures = @{
                particle = "ic2_tiers:block/nano/${id}_side"
                down = "ic2_tiers:block/nano/${id}_bottom"
                up = "ic2_tiers:block/nano/${id}_top"
                north = "ic2_tiers:block/nano/${id}_front"
                south = "ic2_tiers:block/nano/${id}_side"
                west = "ic2_tiers:block/nano/${id}_side"
                east = "ic2_tiers:block/nano/${id}_side"
            }
        }

        Write-JsonFile (Join-Path $itemModelDir "$id.json") @{
            parent = "ic2_tiers:block/$id"
        }

        Write-JsonFile (Join-Path $lootDir "$id.json") @{
            type = "minecraft:block"
            pools = @(
                @{
                    rolls = 1
                    bonus_rolls = 0
                    entries = @(
                        @{ type = "minecraft:item"; name = "ic2_tiers:${id}" }
                    )
                    conditions = @(
                        @{ condition = "minecraft:survives_explosion" }
                    )
                }
            )
            random_sequence = "ic2_tiers:blocks/$id"
        }

        Write-JsonFile (Join-Path $recipeDir "$id.json") @{
            type = "minecraft:crafting_shaped"
            category = "misc"
            pattern = @("IEI", "EME", "IEI")
            key = @{
                I = @{ item = "ic2:iridium" }
                E = @{ item = "ic2:advanced_circuit" }
                M = @{ item = $($machine.Base) }
            }
            result = @{
                id = "ic2_tiers:${id}"
                count = 1
            }
        }
    }
}
finally {
    $zip.Dispose()
}

$allIds = $machines | ForEach-Object { "ic2_tiers:$($_.Id)" }
Write-JsonFile (Join-Path $tagMineableDir "pickaxe.json") @{
    replace = $false
    values = $allIds
}
Write-JsonFile (Join-Path $tagBlockDir "needs_iron_tool.json") @{
    replace = $false
    values = $allIds
}

# ---------- GUI ----------
$guiDir = Join-Path $assets "textures\gui"
New-Item -ItemType Directory -Path $guiDir -Force | Out-Null

$guiW = 176
$guiH = 236
$gui = New-Object System.Drawing.Bitmap(256, 256, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$guiGraphics = [System.Drawing.Graphics]::FromImage($gui)
$guiGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

$gradient = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Rectangle(0, 0, $guiW, $guiH)),
    (C '#182230'), (C '#2C394A'), 90.0)
$guiGraphics.FillRectangle($gradient, 0, 0, $guiW, $guiH)
$gradient.Dispose()

Border $gui 0 0 ($guiW - 1) ($guiH - 1) '#05080C'
Border $gui 1 1 ($guiW - 2) ($guiH - 2) '#05080C'
Border $gui 2 2 ($guiW - 3) ($guiH - 3) '#C9A45C'
Border $gui 3 3 ($guiW - 4) ($guiH - 4) '#334255'
Border $gui 4 4 ($guiW - 5) ($guiH - 5) '#4A5C72'

foreach ($p in @(@(7, 7), @(($guiW - 8), 7), @(7, ($guiH - 8)), @(($guiW - 8), ($guiH - 8)))) {
    Rect $gui $p[0] $p[1] ($p[0] + 2) ($p[1] + 2) '#64E6F2'
}

# Player inventory panel
Rect $gui 4 132 ($guiW - 4) ($guiH - 8) '#1B2532'
Border $gui 4 132 ($guiW - 4) ($guiH - 8) '#334255'
Border $gui 5 133 ($guiW - 5) ($guiH - 9) '#243140'

# Machine panel
Rect $gui 8 16 168 120 '#141C26'
Border $gui 8 16 168 120 '#3A4B5F'
Rect $gui 9 17 167 17 '#1F2B3A'
Rect $gui 8 116 8 120 '#64E6F2'
Rect $gui 168 116 168 120 '#64E6F2'

# Title underline
Rect $gui 8 16 168 16 '#C9A45C'
Rect $gui 8 17 168 17 '#6E5A32'

function Draw-PlayerSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 1) ($y - 1) ($x + 16) ($y + 16) '#0D141C'
    Border $bmp ($x - 1) ($y - 1) ($x + 16) ($y + 16) '#263445'
    Rect $bmp ($x - 1) ($y - 1) ($x + 16) ($y - 1) '#3D4E62'
    Rect $bmp ($x - 1) ($y - 1) ($x - 1) ($y + 16) '#3D4E62'
    Rect $bmp ($x - 1) ($y + 16) ($x + 16) ($y + 16) '#05080C'
    Rect $bmp ($x + 16) ($y - 1) ($x + 16) ($y + 16) '#05080C'
}

function Draw-MachineSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 3) ($y - 3) ($x + 18) ($y + 18) '#0A0F15'
    Border $bmp ($x - 3) ($y - 3) ($x + 18) ($y + 18) '#C9A45C'
    Rect $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#0D141C'
    Border $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#263445'
    Rect $bmp ($x - 2) ($y - 2) ($x + 15) ($y - 2) '#4A5C72'
    Rect $bmp ($x - 2) ($y - 2) ($x - 2) ($y + 15) '#4A5C72'
    Rect $bmp ($x - 2) ($y + 17) ($x + 17) ($y + 17) '#05080C'
    Rect $bmp ($x + 17) ($y - 2) ($x + 17) ($y + 17) '#05080C'
    Rect $bmp ($x - 3) ($y - 3) ($x - 1) ($y - 3) '#64E6F2'
    Rect $bmp ($x - 3) ($y - 3) ($x - 3) ($y - 1) '#64E6F2'
}

foreach ($i in 0..2) {
    Draw-MachineSlot $gui (32 + $i * 48) 34
    Draw-MachineSlot $gui (32 + $i * 48) 84
}
foreach ($row in 0..2) {
    foreach ($col in 0..8) {
        Draw-PlayerSlot $gui (8 + $col * 18) (140 + $row * 18)
    }
}
foreach ($col in 0..8) {
    Draw-PlayerSlot $gui (8 + $col * 18) 198
}

foreach ($x in @(32, 80, 128)) {
    Rect $gui ($x + 4) 60 ($x + 11) 75 '#0A1018'
    Border $gui ($x + 4) 60 ($x + 11) 75 '#263445'
    Rect $gui ($x + 7) 62 ($x + 8) 73 '#1E4B57'
}

# Vertical progress overlay: 16x16 at (192, 0).
Rect $gui 198 0 201 10 '#4FE3F2'
Rect $gui 198 0 199 10 '#B9F6FF'
foreach ($y in 10..15) {
    $half = $y - 9
    Rect $gui (198 - $half) $y (201 + $half) $y '#4FE3F2'
}
Rect $gui 198 10 199 15 '#B9F6FF'

$guiGraphics.Dispose()
Save $gui (Join-Path $guiDir "nano_machine.png")

# Mod logo
$logo = New-Object System.Drawing.Bitmap(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($logo)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Rectangle(0, 0, 128, 128)),
    [System.Drawing.ColorTranslator]::FromHtml("#202A35"),
    [System.Drawing.ColorTranslator]::FromHtml("#4A5B6E"),
    45.0)
$graphics.FillRectangle($brush, 0, 0, 128, 128)
$brush.Dispose()
$pen = New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml("#9FB3C8"), 4)
$graphics.DrawRectangle($pen, 10, 10, 108, 108)
$pen.Dispose()
$font = New-Object System.Drawing.Font("Arial", 34, [System.Drawing.FontStyle]::Bold)
$small = New-Object System.Drawing.Font("Arial", 22, [System.Drawing.FontStyle]::Bold)
$textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#E8F1F8"))
$format = New-Object System.Drawing.StringFormat
$format.Alignment = [System.Drawing.StringAlignment]::Center
$graphics.DrawString("IC2", $font, $textBrush, (New-Object System.Drawing.RectangleF(0, 30, 128, 50)), $format)
$graphics.DrawString("TIERS", $small, $textBrush, (New-Object System.Drawing.RectangleF(0, 72, 128, 40)), $format)
$textBrush.Dispose()
$format.Dispose()
$font.Dispose()
$small.Dispose()
$graphics.Dispose()
$logo.Save((Join-Path $ProjectRoot "src\main\resources\logo.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$logo.Dispose()

Write-Output "Generated nano assets for $($machines.Count) machines."


