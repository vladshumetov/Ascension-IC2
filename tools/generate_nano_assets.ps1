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

$gui = New-Object System.Drawing.Bitmap(192, 160, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$guiGraphics = [System.Drawing.Graphics]::FromImage($gui)
$guiGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

Rect $gui 0 0 175 159 '#C6C6C6'
Border $gui 0 0 175 159 '#373737'
Border $gui 1 1 174 158 '#FFFFFF'

function Draw-GuiSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 1) ($y - 1) ($x + 16) ($y + 16) '#8B8B8B'
    Border $bmp ($x - 1) ($y - 1) ($x + 16) ($y + 16) '#373737'
    Rect $bmp ($x - 1) ($y - 1) ($x + 16) ($y - 1) '#FFFFFF'
    Rect $bmp ($x - 1) ($y - 1) ($x - 1) ($y + 16) '#FFFFFF'
    Rect $bmp ($x - 1) ($y + 16) ($x + 16) ($y + 16) '#555555'
    Rect $bmp ($x + 16) ($y - 1) ($x + 16) ($y + 16) '#555555'
}

foreach ($i in 0..2) {
    Draw-GuiSlot $gui (61 + $i * 18) 16
    Draw-GuiSlot $gui (61 + $i * 18) 54
}
foreach ($row in 0..2) {
    foreach ($col in 0..8) {
        Draw-GuiSlot $gui (8 + $col * 18) (82 + $row * 18)
    }
}
foreach ($col in 0..8) {
    Draw-GuiSlot $gui (8 + $col * 18) 140
}

$arrowPen = New-Object System.Drawing.Pen((C '#373737'), 1)
$arrowBrush = New-Object System.Drawing.SolidBrush((C '#8B8B8B'))
$progressBrush = New-Object System.Drawing.SolidBrush((C '#55D6E8'))
$highlightPen = New-Object System.Drawing.Pen((C '#E8FCFF'), 1)

foreach ($arrowX in @(61, 79, 97)) {
    Rect $gui ($arrowX + 6) 35 ($arrowX + 9) 43 '#8B8B8B'
    Border $gui ($arrowX + 6) 35 ($arrowX + 9) 43 '#373737'
    $head = @(
        (New-Object System.Drawing.Point(($arrowX + 2), 43)),
        (New-Object System.Drawing.Point(($arrowX + 13), 43)),
        (New-Object System.Drawing.Point(($arrowX + 8), 50))
    )
    $guiGraphics.FillPolygon($arrowBrush, $head)
    $guiGraphics.DrawPolygon($arrowPen, $head)
}

# Vertical progress overlay, 16x16 at (176, 0).
Rect $gui 182 0 185 8 '#55D6E8'
Rect $gui 182 0 182 8 '#E8FCFF'
$progressHead = @(
    (New-Object System.Drawing.Point(178, 8)),
    (New-Object System.Drawing.Point(189, 8)),
    (New-Object System.Drawing.Point(184, 15))
)
$guiGraphics.FillPolygon($progressBrush, $progressHead)
$guiGraphics.DrawLine($highlightPen, 184, 8, 184, 14)

$arrowPen.Dispose()
$arrowBrush.Dispose()
$progressBrush.Dispose()
$highlightPen.Dispose()
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


