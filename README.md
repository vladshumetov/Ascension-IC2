# Ascension IC2

[Русская версия](README.ru.md)

Add-on for **IndustrialCraft 2: Refactored** (Minecraft 1.21.1, NeoForge) that adds new tiers of
classic IC2 machines.

## Tiers

| Tier | Inputs | Outputs | Items per operation | Energy tier |
|------|--------|---------|---------------------|-------------|
| Nano | 3 | 3 | 4 | 5 |
| Quantum | 5 | 5 | 12 | 5 |
| Singular | 7 | 7 | planned | planned |

Every input lane works independently and has its own progress arrow and output slot. The number
of items processed per operation is configurable in `config/ascension_ic2-common.toml`
(`nano.itemsPerOperation`, `quantum.itemsPerOperation`).

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

- **Nano Casing** — two advanced machine casings, carbon plates and advanced alloy.
- **Quantum Casing** — two nano casings, iridium and a quantum circuit.
- **Nano Circuit** — carbon plates, gold and an advanced circuit.
- **Quantum Circuit** — advanced alloy, lapotron crystals, iridium and a nano circuit.

Machine recipes mirror the tier progression: the casing sits in the centre, two machines of the
previous tier above and below it, and two circuits on the sides.

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
