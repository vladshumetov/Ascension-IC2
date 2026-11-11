# Ascension IC2

[Русская версия](README.ru.md)

Add-on for **IndustrialCraft 2: Refactored** (Minecraft 1.21.1, NeoForge) that adds new tiers of
classic IC2 machines.

## Tiers

| Tier | Inputs | Outputs | Items per operation | Energy tier |
|------|--------|---------|---------------------|-------------|
| Nano | 3 | 3 | 4 | 6 |
| Quantum | 5 | 5 | 12 | 8 |
| Singular | 7 | 7 | planned | planned |

Every input lane works independently and has its own progress arrow and output slot. The number
of items processed per operation is configurable in `config/ascension_ic2-common.toml`
(`nano.itemsPerOperation`, `quantum.itemsPerOperation`).

## Energy tiers

EU/t follows the IC2 rule `32 * 4^(tier - 1)`. The addon defines 14 levels:

| Tier | EU/t | Tier | EU/t |
|------|------|------|------|
| 1 (LV) | 32 | 8 | 524 288 |
| 2 (MV) | 128 | 9 | 2 097 152 |
| 3 (HV) | 512 | 10 | 8 388 608 |
| 4 (EV) | 2 048 | 11 | 33 554 432 |
| 5 (IV) | 8 192 | 12 | 134 217 728 |
| 6 | 32 768 | 13 | 536 870 912 |
| 7 | 131 072 | 14 | 2 147 483 648 |

Tiers 1–5 match IC2. Tiers 6–14 are the addon ladder; the addon machines use their own energy
block, which accepts all 14 tiers. IC2 cables and the standard addon energy block stop at 8192 EU/t,
so transport above tier 5 needs the addon cables.

The addon currently ships these energy blocks (creative only, recipes are not set up yet):

- **Energy Cable (Tier 6–14)** — one cyan glass-fibre cable per tier, conducts that tier's voltage.
- **Solar Panel (Tier 1–14)** — generates a full tier packet per tick in daylight with sky access;
  all panels share one texture.

Feeding a machine with a voltage above its energy tier destroys it.

## Machines

Nano and Quantum tiers:

- Macerator
- Electric Furnace
- Compressor
- Extractor
- Recycler

The machines use the standard IC2 recipes, so they stay compatible with existing packs and
recipe managers.

## Casings and circuits

Each tier has its own casing and its own circuit:

- **Nano Casing** — four advanced machine casings, carbon plates and advanced alloy.
- **Quantum Casing** — four nano casings, iridium and a quantum crystal.
- **Nano Circuit** — carbon plates, advanced alloy and an advanced circuit.
- **Quantum Circuit** — four lapotron crystals, iridium and a nano circuit.
- **Quantum Crystal** — four lapotron crystals, iridium and a quantum circuit.
- **Singular Crystal** — four quantum crystals, iridium and a quantum circuit; reserved for the
  singular tier.

Machine recipes mirror the tier progression: the casing sits in the centre, two machines of the
previous tier above and below it, two circuits on the sides, and the four corners are filled with
the tier material (advanced alloy for nano, iridium for quantum).

- Nano Macerator = Nano Casing + 2 Macerators + 2 Nano Circuits.
- Quantum Macerator = Quantum Casing + 2 Nano Macerators + 2 Quantum Circuits.

## Upgrades

The four upgrade slots accept IC2 upgrades:

- **Overclocker** — faster processing, higher EU demand.
- **Transformer** — raises the machine energy tier.
- **Ejector / Pulling** (including advanced variants) — automatic item transfer to and from
  adjacent inventories.

Energy Storage upgrades are not supported yet because the add-on energy block keeps a fixed
buffer.

## Tooltips and GUI

- Machine items show energy tier, input EU/t, buffer, lane count and items per operation.
- Each tier has its own GUI texture with a matching slot grid; progress arrows follow the
  machine type.

## Planned

- Singular tier (7 inputs and 7 outputs).
- 10 tiers of solar panels.
- 10 tiers of solar panels themed around Botania, with matching resources and textures.
- 10 tiers of solar panels themed around Blood Magic, with matching resources and textures.
- Botania: a machine that turns flowers and resources into mana using EU.
- Blood Magic: a machine that reproduces alchemy slot recipes (single slot, with upgrades).
- Blood Magic: a blood generator that produces blood from EU.
- Upgrade effects for the remaining IC2 upgrade types.

## Building

Requires JDK 21 and `libs/ic2r-neoforge-1.2.11.jar` (not stored in the repository).

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat build
.\gradlew.bat runClient
```

The built jar is `build/libs/ascension_ic2-neoforge-1.0.0.jar`.

## License

AGPL-3.0-or-later. Textures derived from IC2 are attributed in [docs/ATTRIBUTION.md](docs/ATTRIBUTION.md).
