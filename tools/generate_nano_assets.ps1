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

$assets = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2"
$data = Join-Path $ProjectRoot "src\main\resources\data\ascension_ic2"
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
                "facing=north" = @{ model = "ascension_ic2:block/$id" }
                "facing=east" = @{ model = "ascension_ic2:block/$id"; y = 90 }
                "facing=south" = @{ model = "ascension_ic2:block/$id"; y = 180 }
                "facing=west" = @{ model = "ascension_ic2:block/$id"; y = 270 }
            }
        }

        Write-JsonFile (Join-Path $blockModelDir "$id.json") @{
            parent = "minecraft:block/cube"
            textures = @{
                particle = "ascension_ic2:block/nano/${id}_side"
                down = "ascension_ic2:block/nano/${id}_bottom"
                up = "ascension_ic2:block/nano/${id}_top"
                north = "ascension_ic2:block/nano/${id}_front"
                south = "ascension_ic2:block/nano/${id}_side"
                west = "ascension_ic2:block/nano/${id}_side"
                east = "ascension_ic2:block/nano/${id}_side"
            }
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
                        @{ type = "minecraft:item"; name = "ascension_ic2:${id}" }
                    )
                    conditions = @(
                        @{ condition = "minecraft:survives_explosion" }
                    )
                }
            )
            random_sequence = "ascension_ic2:blocks/$id"
        }

        Write-JsonFile (Join-Path $recipeDir "$id.json") @{
            type = "minecraft:crafting_shaped"
            category = "misc"
            pattern = @("ICI", "CMC", "ICI")
            key = @{
                I = @{ item = "ic2:iridium" }
                C = @{ item = "ascension_ic2:nano_circuit" }
                M = @{ item = $($machine.Base) }
            }
            result = @{
                id = "ascension_ic2:${id}"
                count = 1
            }
        }
    }
}
finally {
    $zip.Dispose()
}

$allIds = @()
foreach ($m in $machines) {
    $base = $m.Id -replace '^nano_', ''
    $allIds += "ascension_ic2:$($m.Id)"
    $allIds += "ascension_ic2:quantum_$base"
}
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

$guiW = 240
$guiH = 230
$gui = New-Object System.Drawing.Bitmap(256, 256, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$guiGraphics = [System.Drawing.Graphics]::FromImage($gui)
$guiGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

# One flat rectangle: upgrade slots sit on its side edges, no wings.
$shapes = ,@(16, 4, 224, 127)
$inside = New-Object 'bool[,]' 256, 256
foreach ($s in $shapes) {
    for ($y = $s[1]; $y -le $s[3]; $y++) {
        for ($x = $s[0]; $x -le $s[2]; $x++) {
     $inside[$x, $y] = $true
        }
    }
}

# Distance from the outside edge of the silhouette (0 = outline pixel).
$dist = New-Object 'int[,]' 256, 256
$queue = New-Object 'System.Collections.Generic.Queue[int]'
$dirs = @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))
for ($y = 0; $y -lt 256; $y++) {
    for ($x = 0; $x -lt 256; $x++) {
        $dist[$x, $y] = -1
    }
}
for ($y = 0; $y -lt 256; $y++) {
    for ($x = 0; $x -lt 256; $x++) {
        if ($inside[$x, $y]) {
     $edge = $false
     foreach ($d in $dirs) {
  $nx = $x + $d[0]; $ny = $y + $d[1]
         if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge 256 -or $ny -ge 256 -or -not $inside[$nx, $ny]) {
      $edge = $true
  }
     }
     if ($edge) {
         $dist[$x, $y] = 0
         $queue.Enqueue($y * 256 + $x)
     }
        }
    }
}
while ($queue.Count -gt 0) {
    $idx = $queue.Dequeue()
    $x = $idx % 256
    $y = [int][math]::Floor($idx / 256)
    foreach ($d in $dirs) {
        $nx = $x + $d[0]; $ny = $y + $d[1]
 if ($nx -ge 0 -and $ny -ge 0 -and $nx -lt 256 -and $ny -lt 256 -and $inside[$nx, $ny] -and $dist[$nx, $ny] -eq -1) {
     $dist[$nx, $ny] = $dist[$x, $y] + 1
     $queue.Enqueue($ny * 256 + $nx)
        }
    }
}

# Paint the body: dark outline on the silhouette edge, cyan rim, inner dark-blue rim, navy gradient fill.
for ($y = 0; $y -lt 256; $y++) {
    for ($x = 0; $x -lt 256; $x++) {
        if (-not $inside[$x, $y]) { continue }
        $edge = $false
 foreach ($dd in $dirs) {
     $nx = $x + $dd[0]
     $ny = $y + $dd[1]
     if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge 256 -or $ny -ge 256 -or -not $inside[$nx, $ny]) { $edge = $true }
        }
        if ($edge) {
     $gui.SetPixel($x, $y, (C '#05080C'))
     continue
        }
        $t2 = [Math]::Min(1.0, [Math]::Max(0.0, ($y - 4) / 124.0))
 $r = [int](27 + (16 - 27) * $t2)
 $g = [int](36 + (22 - 36) * $t2)
 $b = [int](69 + (46 - 69) * $t2)
 $gui.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
    }
}
# Top highlight along the body (light line just inside the rim).
Rect $gui 22 7 218 7 '#B7E0EA'

# Joint line between machine body and inventory.
Rect $gui 16 128 224 128 '#4D7C9A'
Rect $gui 16 129 224 129 '#B7E0EA'

# Title banner on the top edge of the body.
Rect $gui 50 2 189 14 '#1F274F'
Border $gui 50 2 189 14 '#4D7C9A'
Rect $gui 51 3 188 3 '#B7E0EA'

function Draw-PlayerSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 1) ($y - 1) ($x + 16) ($y + 16) '#8B8B8B'
    Border $bmp ($x - 1) ($y - 1) ($x + 16) ($y + 16) '#373737'
    Rect $bmp ($x - 1) ($y - 1) ($x + 16) ($y - 1) '#FFFFFF'
    Rect $bmp ($x - 1) ($y - 1) ($x - 1) ($y + 16) '#FFFFFF'
    Rect $bmp ($x - 1) ($y + 16) ($x + 16) ($y + 16) '#555555'
    Rect $bmp ($x + 16) ($y - 1) ($x + 16) ($y + 16) '#555555'
}

function Draw-MachineSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 3) ($y - 3) ($x + 18) ($y + 18) '#0F1325'
    Border $bmp ($x - 3) ($y - 3) ($x + 18) ($y + 18) '#4D7C9A'
    Rect $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#09203F'
    Border $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#253C5E'
    Rect $bmp ($x - 2) ($y - 2) ($x + 15) ($y - 2) '#3F6C89'
    Rect $bmp ($x - 2) ($y - 2) ($x - 2) ($y + 15) '#3F6C89'
    Rect $bmp ($x - 2) ($y + 17) ($x + 17) ($y + 17) '#041833'
    Rect $bmp ($x + 17) ($y - 2) ($x + 17) ($y + 17) '#041833'
}

# Upgrade socket: only the slot itself, no grouping rectangle around it.
function Draw-UpgradeSlot($bmp, [int]$x, [int]$y) {
    Rect $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#09203F'
    Border $bmp ($x - 2) ($y - 2) ($x + 17) ($y + 17) '#4D7C9A'
}

# Upgrade slots: two per side, sitting directly in the protruding wings.
foreach ($pos in @(@(8, 28), @(8, 62), @(216, 28), @(216, 62))) {
    Draw-UpgradeSlot $gui $pos[0] $pos[1]
}

# Machine inputs (top) and outputs (bottom).
foreach ($i in 0..2) {
    Draw-MachineSlot $gui (86 + $i * 26) 28
    Draw-MachineSlot $gui (86 + $i * 26) 76
}



# Horizontal energy bar under the output slots (frame 24..215 x 100..106, well 25..214 x 101..105).
Rect $gui 24 100 215 106 '#0F1325'
Border $gui 24 100 215 106 '#4D7C9A'
Rect $gui 25 101 214 105 '#3D001C'
foreach ($k in 1..9) {
    $tx = 25 + [int](190 * $k / 10)
    Rect $gui $tx 101 $tx 105 '#63121A'
}



# Vanilla player inventory panel, joined under the body.
Rect $gui 32 132 207 217 '#C6C6C6'
Border $gui 32 132 207 217 '#373737'
Rect $gui 33 133 206 133 '#FFFFFF'
Rect $gui 33 133 33 216 '#FFFFFF'
Rect $gui 33 217 207 217 '#555555'
Rect $gui 207 133 207 217 '#555555'

foreach ($row in 0..2) {
    foreach ($col in 0..8) {
        Draw-PlayerSlot $gui (40 + $col * 18) (140 + $row * 18)
    }
}
foreach ($col in 0..8) {
    Draw-PlayerSlot $gui (40 + $col * 18) 198
}

$guiGraphics.Dispose()
Save $gui (Join-Path $guiDir "nano_machine.png")

# ---------- Per-machine progress arrows (taken from examples/nano) ----------
# Each machine keeps the arrow of its own reference example. Sprite layout: 32 x H,
#   left  16 px (u 0..15)  = unlit track: arrow silhouette in dark #253C5E;
#   right 16 px (u 16..31) = lit arrow in the example's own colours.
# The arrow is cropped from lane 0 of the example (window x 80..104, y 46..80) and centred
# on a 16 px slot canvas, so it lines up with the machine input slot at x = 86 + lane * 26.
$progressDir = Join-Path $guiDir "progress"
New-Item -ItemType Directory -Path $progressDir -Force | Out-Null
$exampleDir = Join-Path $ProjectRoot "examples\nano"
$progressSources = @(
    @{ Id = "nano_macerator"; File = "nano_macerator.png" },
    @{ Id = "nano_electric_furnace"; File = "nano_furnace.png" },
    @{ Id = "nano_compressor"; File = "nano_compressor.png" },
    @{ Id = "nano_extractor"; File = "nano_extractor.png" },
    @{ Id = "nano_recycler"; File = "nano_recycler.png" }
)
$arrowBg = (C '#171D39').ToArgb()
$canvasW = 16
foreach ($src in $progressSources) {
    $ex = [System.Drawing.Bitmap]::FromFile((Join-Path $exampleDir $src.File))
    try {
        $minx = 9999; $miny = 9999; $maxx = -1; $maxy = -1
        for ($y = 46; $y -le 80; $y++) {
     for ($x = 80; $x -le 104; $x++) {
  $c = $ex.GetPixel($x, $y)
         if ($c.A -gt 0 -and $c.ToArgb() -ne $arrowBg) {
      if ($x -lt $minx) { $minx = $x }
      if ($x -gt $maxx) { $maxx = $x }
      if ($y -lt $miny) { $miny = $y }
      if ($y -gt $maxy) { $maxy = $y }
         }
     }
        }
        if ($maxx -lt 0) { throw "No progress arrow found in $($src.File)" }
        $w = $maxx - $minx + 1
        $h = $maxy - $miny + 1
        if ($w -gt $canvasW) { throw "Progress arrow in $($src.File) is wider than $canvasW px ($w)" }
        $offX = [int][Math]::Floor(($canvasW - $w) / 2)

 $sprite = New-Object System.Drawing.Bitmap(($canvasW * 2), $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
 $trackColor = [System.Drawing.Color]::FromArgb(255, 0x25, 0x3C, 0x5E)
        for ($j = 0; $j -lt $h; $j++) {
            for ($i = 0; $i -lt $w; $i++) {
         $c = $ex.GetPixel($minx + $i, $miny + $j)
  if ($c.A -eq 0 -or $c.ToArgb() -eq $arrowBg) { continue }
  $sprite.SetPixel($offX + $i, $j, $trackColor)
  $sprite.SetPixel($canvasW + $offX + $i, $j, $c)
     }
        }
        # Arrow shapes are shared by both tiers, so the sprite is named after the machine type.
        $spriteName = $src.Id -replace '^nano_', ''
        Save $sprite (Join-Path $progressDir "$spriteName.png")
        Write-Output "progress ${spriteName}: arrow ${w}x${h} at $minx,$miny in $($src.File)"
    }
    finally {
        $ex.Dispose()
    }
}

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


