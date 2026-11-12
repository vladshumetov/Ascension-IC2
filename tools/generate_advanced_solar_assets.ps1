param(
    [string]$ProjectRoot = ""
)

# Generates the four ASP-style solar panels with hand-drawn 16x16 textures:
# top (cell grid), side (ridged casing) and bottom (vents). No IC2 textures are reused.

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

function Write-JsonFile([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 12
    [System.IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}

function C([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Scale([System.Drawing.Color]$c, [double]$f) {
    $r = [Math]::Min(255, [Math]::Max(0, [int]($c.R * $f)))
    $g = [Math]::Min(255, [Math]::Max(0, [int]($c.G * $f)))
    $b = [Math]::Min(255, [Math]::Max(0, [int]($c.B * $f)))
    return [System.Drawing.Color]::FromArgb(255, $r, $g, $b)
}

function Blend([System.Drawing.Color]$a, [System.Drawing.Color]$b, [double]$t) {
    return [System.Drawing.Color]::FromArgb(
        255,
        [int]($a.R + ($b.R - $a.R) * $t),
        [int]($a.G + ($b.G - $a.G) * $t),
        [int]($a.B + ($b.B - $a.B) * $t))
}

function Px($bmp, [int]$x, [int]$y, [System.Drawing.Color]$c) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt 16 -and $y -lt 16) {
        $bmp.SetPixel($x, $y, $c)
    }
}

function Rect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [System.Drawing.Color]$c) {
    for ($y = $y0; $y -le $y1; $y++) {
        for ($x = $x0; $x -le $x1; $x++) {
            Px $bmp $x $y $c
        }
    }
}

function New-Top($p) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $frameDark = C $p.FrameDark
    $frameLight = C $p.FrameLight
    $cellLight = C $p.CellLight
    $cellDark = C $p.CellDark
    $accent = C $p.Accent
    $busbar = C $p.Busbar

    # Frame: dark outer ring, light inner edge, corner studs.
    Rect $bmp 0 0 15 15 $frameDark
    Rect $bmp 1 1 14 14 $frameLight
    Rect $bmp 1 1 14 1 $accent
    Px $bmp 0 0 $frameLight
    Px $bmp 15 0 $frameLight
    Px $bmp 0 15 $frameLight
    Px $bmp 15 15 $frameLight

    # 3x3 cells, 4x4 each, separated by 1px gaps.
    for ($gy = 0; $gy -lt 3; $gy++) {
        for ($gx = 0; $gx -lt 3; $gx++) {
            $x0 = 1 + $gx * 5
            $y0 = 1 + $gy * 5
            for ($y = 0; $y -lt 4; $y++) {
                for ($x = 0; $x -lt 4; $x++) {
                    $t = ($x + $y) / 6.0
                    $color = Blend $cellLight $cellDark $t
                    # Deterministic micro-shading keeps it from looking flat.
                    $n = (($x0 + $x) * 7 + ($y0 + $y) * 13) % 5
                    if ($n -eq 0) { $color = Scale $color 0.92 }
                    if ($n -eq 3) { $color = Scale $color 1.06 }
                    Px $bmp ($x0 + $x) ($y0 + $y) $color
                }
            }
            # Glossy corner and busbar.
            Px $bmp $x0 $y0 (Scale $cellLight 1.35)
            Px $bmp ($x0 + 1) $y0 (Scale $cellLight 1.18)
            for ($y = 0; $y -lt 4; $y++) {
                Px $bmp ($x0 + 2) ($y0 + $y) $busbar
            }
            Px $bmp ($x0 + 3) ($y0 + 3) (Scale $cellDark 0.72)
        }
    }
    return $bmp
}

function New-Side($p) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $frameDark = C $p.FrameDark
    $frameLight = C $p.FrameLight
    $accent = C $p.Accent
    Rect $bmp 0 0 15 15 $frameDark
    for ($y = 1; $y -le 14; $y++) {
        $t = ($y - 1) / 13.0
        $base = Blend (Scale $frameLight 1.05) (Scale $frameDark 0.85) $t
        Rect $bmp 1 $y 14 $y $base
    }
    # Horizontal ridges.
    foreach ($y in 3, 7, 11) {
        Rect $bmp 1 $y 14 $y (Scale $frameDark 0.68)
        Rect $bmp 1 ($y + 1) 14 ($y + 1) (Scale $frameLight 1.12)
    }
    # Accent stripe and rivets.
    Rect $bmp 1 13 14 13 $accent
    Px $bmp 2 13 (Scale $accent 1.3)
    Px $bmp 13 13 (Scale $accent 0.8)
    Px $bmp 1 1 (Scale $frameLight 1.25)
    Px $bmp 14 1 (Scale $frameLight 1.25)
    return $bmp
}

function New-Bottom($p) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $frameDark = C $p.FrameDark
    $frameLight = C $p.FrameLight
    $accent = C $p.Accent
    Rect $bmp 0 0 15 15 (Scale $frameDark 0.8)
    Rect $bmp 1 1 14 14 (Scale $frameDark 1.05)
    # Vent slits.
    foreach ($y in 4, 7, 10) {
        Rect $bmp 3 $y 12 $y (Scale $frameDark 0.55)
        Rect $bmp 3 ($y + 1) 12 ($y + 1) (Scale $frameLight 0.9)
    }
    # Corner screws.
    foreach ($pos in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) {
        Px $bmp $pos[0] $pos[1] (Scale $frameLight 1.15)
    }
    Px $bmp 7 15 $accent
    Px $bmp 8 15 $accent
    return $bmp
}

$panels = @(
    @{ Id = "advanced_solar_panel"; FrameDark = "#1E3A5F"; FrameLight = "#5B87B5"; CellLight = "#5AA8F0"; CellDark = "#1F4E8C"; Accent = "#7FD4FF"; Busbar = "#BFE8FF" },
    @{ Id = "hybrid_solar_panel"; FrameDark = "#2A4A3A"; FrameLight = "#6DA985"; CellLight = "#64D69A"; CellDark = "#1E6B4E"; Accent = "#C9F26B"; Busbar = "#DDFFB0" },
    @{ Id = "ultimate_hybrid_solar_panel"; FrameDark = "#4A2410"; FrameLight = "#C08A45"; CellLight = "#FF9A4D"; CellDark = "#8C2A12"; Accent = "#FFD36B"; Busbar = "#FFE7B0" },
    @{ Id = "quantum_solar_panel"; FrameDark = "#241A3F"; FrameLight = "#7A64B8"; CellLight = "#9C7CFF"; CellDark = "#221246"; Accent = "#66E0FF"; Busbar = "#C9B8FF" }
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

Write-Output "Generated hand-drawn textures for four solar panels."
