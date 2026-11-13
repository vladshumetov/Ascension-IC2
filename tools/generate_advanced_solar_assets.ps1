param(
    [string]$ProjectRoot = ""
)

# Generates the four ASP-style panels in the visual style of next_ic:
# 16x16 top/bottom, 16x192 animated side (12 frames), per-tier solar-cell palettes.

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
$assets = Join-Path $ProjectRoot "src\main\resources\assets\ascension_ic2"
$data = Join-Path $ProjectRoot "src\main\resources\data\ascension_ic2"
foreach ($dir in @("$assets\textures\block", "$assets\blockstates", "$assets\models\block", "$assets\models\item", "$data\loot_table\blocks", (Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block\mineable"))) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

function Write-JsonFile([string]$Path, $Value) {
    $json = $Value | ConvertTo-Json -Depth 20
    [System.IO.File]::WriteAllText($Path, $json, (New-Object System.Text.UTF8Encoding($false)))
}
function C([string]$hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Px($bmp, [int]$x, [int]$y, [System.Drawing.Color]$c) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $bmp.Width -and $y -lt $bmp.Height) { $bmp.SetPixel($x, $y, $c) }
}
function Rect($bmp, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [System.Drawing.Color]$c) {
    for ($y = $y0; $y -le $y1; $y++) { for ($x = $x0; $x -le $x1; $x++) { Px $bmp $x $y $c } }
}
function Scale([System.Drawing.Color]$c, [double]$f) {
    return [System.Drawing.Color]::FromArgb(
        255,
        [Math]::Min(255, [Math]::Max(0, [int]($c.R * $f))),
        [Math]::Min(255, [Math]::Max(0, [int]($c.G * $f))),
        [Math]::Min(255, [Math]::Max(0, [int]($c.B * $f))))
}

function New-Top($p) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $frame = C $p.Frame
    $frameLight = C $p.FrameLight
    $frameDark = C $p.FrameDark
    $cellLight = C $p.CellLight
    $cellDark = C $p.CellDark
    $bus = C $p.Bus

    # Metal frame with a bevel.
    Rect $bmp 0 0 15 15 $frameDark
    Rect $bmp 1 1 14 14 $frame
    Rect $bmp 1 1 14 1 $frameLight
    Rect $bmp 1 1 1 14 $frameLight
    Rect $bmp 1 14 14 14 (Scale $frameDark 0.65)
    Rect $bmp 14 1 14 14 (Scale $frameDark 0.7)
    # Corner studs.
    foreach ($pos in @(@(0, 0), @(15, 0), @(0, 15), @(15, 15))) {
        Px $bmp $pos[0] $pos[1] $frameLight
    }

    # 3x3 solar cells, 4x4 each with 1px metal gaps.
    for ($gy = 0; $gy -lt 3; $gy++) {
        for ($gx = 0; $gx -lt 3; $gx++) {
            $x0 = 1 + $gx * 5
            $y0 = 1 + $gy * 5
            for ($y = 0; $y -lt 4; $y++) {
                for ($x = 0; $x -lt 4; $x++) {
                    $t = ($x + $y) / 6.0
                    $color = [System.Drawing.Color]::FromArgb(
                        255,
                        [int]($cellLight.R + ($cellDark.R - $cellLight.R) * $t),
                        [int]($cellLight.G + ($cellDark.G - $cellLight.G) * $t),
                        [int]($cellLight.B + ($cellDark.B - $cellLight.B) * $t))
                    if (($x + $y) % 3 -eq 0) { $color = Scale $color 1.1 }
                    Px $bmp ($x0 + $x) ($y0 + $y) $color
                }
            }
            Px $bmp $x0 $y0 (Scale $cellLight 1.35)
            for ($y = 0; $y -lt 4; $y++) { Px $bmp ($x0 + 2) ($y0 + $y) $bus }
        }
    }
    return $bmp
}

function New-Bottom($p) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $frameDark = C $p.FrameDark
    $frame = C $p.Frame
    $frameLight = C $p.FrameLight
    Rect $bmp 0 0 15 15 (Scale $frameDark 0.8)
    Rect $bmp 1 1 14 14 $frame
    foreach ($y in 4, 7, 10) {
        Rect $bmp 3 $y 12 $y (Scale $frameDark 0.6)
        Rect $bmp 3 ($y + 1) 12 ($y + 1) $frameLight
    }
    foreach ($pos in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) {
        Px $bmp $pos[0] $pos[1] $frameLight
    }
    return $bmp
}

function New-Side($p) {
    # 12 animated frames, 16x192.
    $bmp = New-Object System.Drawing.Bitmap(16, 192, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $frame = C $p.Frame
    $frameLight = C $p.FrameLight
    $frameDark = C $p.FrameDark
    $accent = C $p.CellLight
    $glow = C $p.Bus

    for ($f = 0; $f -lt 12; $f++) {
        $base = $f * 16
        Rect $bmp 0 $base 15 ($base + 15) $frameDark
        Rect $bmp 1 ($base + 1) 14 ($base + 14) $frame
        Rect $bmp 1 ($base + 1) 14 ($base + 1) $frameLight
        Rect $bmp 1 ($base + 14) 14 ($base + 14) (Scale $frameDark 0.6)
        # Central energy window.
        Rect $bmp 4 ($base + 4) 11 ($base + 11) (Scale $frameDark 0.8)
        $glowY = 5 + (($f * 3) % 6)
        for ($y = 4; $y -le 11; $y++) {
            $distance = [Math]::Abs($y - $glowY)
            $color = if ($distance -eq 0) { $glow } elseif ($distance -eq 1) { $accent } else { Scale $accent 0.45 }
            Rect $bmp 6 ($base + $y) 9 ($base + $y) $color
        }
        # Corner bolts.
        foreach ($pos in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) {
            Px $bmp $pos[0] ($base + $pos[1]) $frameLight
        }
    }
    return $bmp
}

$panels = @(
    @{ Id = "advanced_solar_panel"; Frame = "#464569"; FrameLight = "#A1A6C2"; FrameDark = "#15121B"; CellLight = "#4187D4"; CellDark = "#240132"; Bus = "#D3D4E2" },
    @{ Id = "hybrid_solar_panel"; Frame = "#253F3B"; FrameLight = "#A3A7BE"; FrameDark = "#121B15"; CellLight = "#41C067"; CellDark = "#062921"; Bus = "#C0FF85" },
    @{ Id = "ultimate_hybrid_solar_panel"; Frame = "#151090"; FrameLight = "#A3A7BE"; FrameDark = "#12171B"; CellLight = "#7838ED"; CellDark = "#071538"; Bus = "#FFA5EF" },
    @{ Id = "quantum_solar_panel"; Frame = "#003F69"; FrameLight = "#A3A7BE"; FrameDark = "#12171B"; CellLight = "#43CFA3"; CellDark = "#062029"; Bus = "#C2FFBF" }
)

foreach ($panel in $panels) {
    $id = $panel.Id
    $top = New-Top $panel
    $side = New-Side $panel
    $bottom = New-Bottom $panel
    $top.Save((Join-Path $assets "textures\block\${id}_top.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $side.Save((Join-Path $assets "textures\block\${id}_side.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bottom.Save((Join-Path $assets "textures\block\${id}_bottom.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $top.Dispose(); $side.Dispose(); $bottom.Dispose()

    Write-JsonFile (Join-Path $assets "textures\block\${id}_side.png.mcmeta") @{
        animation = @{ interpolate = $true; frametime = 2 }
    }

    Write-JsonFile (Join-Path $assets "blockstates\$id.json") @{
        variants = @{ "" = @{ model = "ascension_ic2:block/$id" } }
    }
    Write-JsonFile (Join-Path $assets "models\block\$id.json") @{
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
    Write-JsonFile (Join-Path $assets "models\item\$id.json") @{ parent = "ascension_ic2:block/$id" }
    Write-JsonFile (Join-Path $data "loot_table\blocks\$id.json") @{
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
    if (Test-Path $Path) { $values = @((Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json).values) }
    foreach ($id in $Ids) { if ($values -notcontains $id) { $values += $id } }
    Write-JsonFile $Path @{ replace = $false; values = $values }
}
$ids = $panels | ForEach-Object { "ascension_ic2:$($_.Id)" }
Merge-Tag (Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block\mineable\pickaxe.json") $ids
Merge-Tag (Join-Path $ProjectRoot "src\main\resources\data\minecraft\tags\block\needs_iron_tool.json") $ids

Write-Output "Generated 16x16 + animated solar panel textures in next_ic style."
