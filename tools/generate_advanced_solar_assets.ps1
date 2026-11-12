param(
    [string]$ProjectRoot = ""
)

# Generates the four ASP-style solar panels with 32x32 hand-drawn pixel-art textures:
# beveled frame, 2x2 cell grid with busbars and reflections, ridged sides, vented bottom.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
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

$SIZE = 32
function Write-JsonFile([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}
function C([string]$hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Scale([System.Drawing.Color]$c, [double]$f) {
    return [System.Drawing.Color]::FromArgb(
        255,
        [Math]::Min(255, [Math]::Max(0, [int]($c.R * $f))),
        [Math]::Min(255, [Math]::Max(0, [int]($c.G * $f))),
        [Math]::Min(255, [Math]::Max(0, [int]($c.B * $f))))
}
function Blend([System.Drawing.Color]$a, [System.Drawing.Color]$b, [double]$t) {
    return [System.Drawing.Color]::FromArgb(
        255,
        [int]($a.R + ($b.R - $a.R) * $t),
        [int]($a.G + ($b.G - $a.G) * $t),
        [int]($a.B + ($b.B - $a.B) * $t))
}
function Px($bmp, [int]$x, [int]$y, [System.Drawing.Color]$c) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $SIZE -and $y -lt $SIZE) { $bmp.SetPixel($x, $y, $c) }
}
function Rect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [System.Drawing.Color]$c) {
    for ($y = $y0; $y -le $y1; $y++) { for ($x = $x0; $x -le $x1; $x++) { Px $bmp $x $y $c } }
}

function New-Top($p) {
    $bmp = New-Object System.Drawing.Bitmap($SIZE, $SIZE, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $dark = C $p.FrameDark
    $light = C $p.FrameLight
    $cellLight = C $p.CellLight
    $cellDark = C $p.CellDark
    $accent = C $p.Accent
    $bus = C $p.Busbar

    # Beveled metal frame: outer dark, top/left highlight, bottom/right shadow.
    Rect $bmp 0 0 ($SIZE - 1) ($SIZE - 1) $dark
    Rect $bmp 1 1 ($SIZE - 2) 2 (Scale $light 1.2)
    Rect $bmp 1 1 2 ($SIZE - 2) (Scale $light 1.12)
    Rect $bmp 1 ($SIZE - 2) ($SIZE - 2) ($SIZE - 2) (Scale $dark 0.7)
    Rect $bmp ($SIZE - 2) 1 ($SIZE - 2) ($SIZE - 2) (Scale $dark 0.75)
    Rect $bmp 2 2 ($SIZE - 3) ($SIZE - 3) (Scale $dark 1.35)

    # Solar cells: 2x2 blocks, 12x12 cells with a 2px gap.
    for ($gy = 0; $gy -lt 2; $gy++) {
        for ($gx = 0; $gx -lt 2; $gx++) {
            $x0 = 3 + $gx * 14
            $y0 = 3 + $gy * 14
            for ($y = 0; $y -lt 12; $y++) {
                for ($x = 0; $x -lt 12; $x++) {
                    $t = ($x + $y) / 22.0
                    $color = Blend $cellLight $cellDark $t
                    $n = (($x0 + $x) * 7 + ($y0 + $y) * 13) % 9
                    if ($n -eq 0) { $color = Scale $color 0.9 }
                    elseif ($n -eq 4) { $color = Scale $color 1.08 }
                    # Diagonal reflection band across the cell.
                    if (($x + $y) -ge 5 -and ($x + $y) -le 8) { $color = Blend $color (Scale $cellLight 1.35) 0.45 }
                    Px $bmp ($x0 + $x) ($y0 + $y) $color
                }
            }
            # Busbars every 4 px.
            for ($k = 3; $k -lt 12; $k += 4) {
                for ($y = 0; $y -lt 12; $y++) { Px $bmp ($x0 + $k) ($y0 + $y) (Scale $bus 0.85) }
                for ($x = 0; $x -lt 12; $x++) { Px $bmp ($x0 + $x) ($y0 + $k) (Scale $bus 0.72) }
            }
            # Glossy corner + shadow.
            Rect $bmp $x0 $y0 ($x0 + 2) $y0 (Scale $cellLight 1.5)
            Rect $bmp $x0 $y0 $x0 ($y0 + 2) (Scale $cellLight 1.35)
            Px $bmp ($x0 + 11) ($y0 + 11) (Scale $cellDark 0.6)
            Rect $bmp ($x0 + 9) ($y0 + 11) ($x0 + 11) ($y0 + 11) (Scale $cellDark 0.75)
        }
    }

    # Frame studs and accent notch.
    foreach ($pos in @(@(1, 1), @(30, 1), @(1, 30), @(30, 30))) {
        Px $bmp $pos[0] $pos[1] (Scale $light 1.45)
        Px $bmp ($pos[0] + 1) ($pos[1] + 1) (Scale $dark 0.6)
    }
    Rect $bmp 14 0 17 0 $accent
    Rect $bmp 15 1 16 1 (Scale $accent 1.2)
    return $bmp
}

function New-Side($p) {
    $bmp = New-Object System.Drawing.Bitmap($SIZE, $SIZE, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $dark = C $p.FrameDark
    $light = C $p.FrameLight
    $accent = C $p.Accent
    Rect $bmp 0 0 ($SIZE - 1) ($SIZE - 1) (Scale $dark 0.85)
    for ($y = 1; $y -le ($SIZE - 2); $y++) {
        $t = ($y - 1) / 29.0
        $base = Blend (Scale $light 1.05) (Scale $dark 0.8) $t
        Rect $bmp 1 $y ($SIZE - 2) $y $base
    }
    Rect $bmp 1 1 ($SIZE - 2) 2 (Scale $light 1.25)
    Rect $bmp 1 ($SIZE - 2) ($SIZE - 2) ($SIZE - 2) (Scale $dark 0.6)
    # Ridges and bolts.
    foreach ($y in 7, 15, 23) {
        Rect $bmp 1 $y ($SIZE - 2) $y (Scale $dark 0.62)
        Rect $bmp 1 ($y + 1) ($SIZE - 2) ($y + 1) (Scale $light 1.18)
    }
    foreach ($pos in @(@(2, 2), @(29, 2), @(2, 29), @(29, 29))) {
        Px $bmp $pos[0] $pos[1] (Scale $light 1.4)
        Px $bmp ($pos[0] + 1) ($pos[1] + 1) (Scale $dark 0.55)
    }
    # Accent stripe near the bottom.
    Rect $bmp 1 26 ($SIZE - 2) 27 $accent
    Rect $bmp 1 26 ($SIZE - 2) 26 (Scale $accent 1.35)
    return $bmp
}

function New-Bottom($p) {
    $bmp = New-Object System.Drawing.Bitmap($SIZE, $SIZE, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $dark = C $p.FrameDark
    $light = C $p.FrameLight
    $accent = C $p.Accent
    Rect $bmp 0 0 ($SIZE - 1) ($SIZE - 1) (Scale $dark 0.72)
    Rect $bmp 1 1 ($SIZE - 2) ($SIZE - 2) (Scale $dark 1.1)
    # Louvered vents.
    foreach ($y in 6, 12, 18, 24) {
        Rect $bmp 4 $y 27 $y (Scale $dark 0.5)
        Rect $bmp 4 ($y + 1) 27 ($y + 1) (Scale $light 0.85)
        Rect $bmp 4 ($y + 2) 27 ($y + 2) (Scale $dark 0.65)
    }
    foreach ($pos in @(@(2, 2), @(29, 2), @(2, 29), @(29, 29))) {
        Px $bmp $pos[0] $pos[1] (Scale $light 1.2)
        Px $bmp ($pos[0] + 1) $pos[1] (Scale $dark 0.5)
        Px $bmp $pos[0] ($pos[1] + 1) (Scale $dark 0.5)
    }
    Rect $bmp 14 30 17 30 $accent
    return $bmp
}

$panels = @(
    @{ Id = "advanced_solar_panel"; FrameDark = "#1E3A5F"; FrameLight = "#5B87B5"; CellLight = "#5AA8F0"; CellDark = "#173F73"; Accent = "#7FD4FF"; Busbar = "#BFE8FF" },
    @{ Id = "hybrid_solar_panel"; FrameDark = "#2A4A3A"; FrameLight = "#6DA985"; CellLight = "#64D69A"; CellDark = "#17543D"; Accent = "#C9F26B"; Busbar = "#DDFFB0" },
    @{ Id = "ultimate_hybrid_solar_panel"; FrameDark = "#4A2410"; FrameLight = "#C08A45"; CellLight = "#FF9A4D"; CellDark = "#72200C"; Accent = "#FFD36B"; Busbar = "#FFE7B0" },
    @{ Id = "quantum_solar_panel"; FrameDark = "#241A3F"; FrameLight = "#7A64B8"; CellLight = "#9C7CFF"; CellDark = "#1B0E38"; Accent = "#66E0FF"; Busbar = "#C9B8FF" }
)

foreach ($panel in $panels) {
    $id = $panel.Id
    $top = New-Top $panel
    $side = New-Side $panel
    $bottom = New-Bottom $panel
    $top.Save((Join-Path $textureDir "${id}_top.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $side.Save((Join-Path $textureDir "${id}_side.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bottom.Save((Join-Path $textureDir "${id}_bottom.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $top.Dispose()
    $side.Dispose()
    $bottom.Dispose()

    Write-JsonFile (Join-Path $blockstateDir "$id.json") @{
        variants = @{ "" = @{ model = "ascension_ic2:block/$id" } }
    }
    Write-JsonFile (Join-Path $blockModelDir "$id.json") @{
        parent = "minecraft:block/cube"
        textures = @{
            particle = "ascension_ic2:block/${id}_side"
            down = "ascension_ic2:block/${id}_bottom"
            up = "ascension_ic2:block/${id}_top"
            north = "ascension_ic2:block/${id}_side"
            south = "ascension_ic2:block/${id}_side"
            east = "ascension_ic2:block/${id}_side"
            west = "ascension_ic2:block/${id}_side"
        }
    }
    Write-JsonFile (Join-Path $itemModelDir "$id.json") @{ parent = "ascension_ic2:block/$id" }
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

function Merge-Tag([string]$Path, [string[]]$Ids) {
    $values = @()
    if (Test-Path $Path) {
        $existing = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
        $values = @($existing.values)
    }
    foreach ($id in $Ids) { if ($values -notcontains $id) { $values += $id } }
    Write-JsonFile $Path @{ replace = $false; values = $values }
}
$ids = $panels | ForEach-Object { "ascension_ic2:$($_.Id)" }
Merge-Tag (Join-Path $tagMineableDir "pickaxe.json") $ids
Merge-Tag (Join-Path $tagBlockDir "needs_iron_tool.json") $ids

Write-Output "Generated 32x32 textures for four solar panels."
