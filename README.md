# Enchantment Forge

[🇩🇪 Deutsche Version](README.de.md)

Fabric mod for **Minecraft 1.21.11** (Fabric Loader 0.20.1+, Fabric API). Adds the **Enchantment Forge**: a block
that upgrades the enchantments of weapons, armor and tools beyond their vanilla maximum (up to level **XX** by
default). Every level costs the item's own material and only succeeds with a chance that drops with each level.

## Installation

Put `enchantforge-1.0.0.jar` and [Fabric API](https://modrinth.com/mod/fabric-api) into the `mods` folder – on the
server **and** on every client. [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds the in-game
settings screen.

## How to play

1. Craft the forge:

   ```
   Diamond    Enchanting Table  Diamond
   Obsidian   Anvil             Obsidian
   Obsidian   Obsidian          Obsidian
   ```

   (Creative: "Functional Blocks" tab, right after the enchanting table.)
2. Right-click the forge, put an enchanted item into the top left slot, the material below it and optionally
   **Luck Charms** into the bottom (purple) slot.
3. Click the enchantment you want in the list. The tooltip shows the chance, the material and experience cost and the
   maximum level.

Material and experience are used up on failure too.

### Luck Charms

They raise the success chance: every Luck Charm in the charm slot adds **+1 percentage point** by default, up to 64
per attempt. An attempt only uses as many as it needs to reach 100 % – the rest stays in the slot. They are used up on
success and on failure. A green ♣ in the list marks chances raised by charms; the tooltip shows base chance and bonus.

Recipe (makes 2):

```
      Bone
Bone  Iron Ingot  Bone
      Bone
```

### Materials

The material depends on the item – it is its vanilla repair material:

| Item | Material |
| --- | --- |
| Wooden tools, shield | any planks |
| Stone tools | cobblestone / blackstone / cobbled deepslate |
| Copper / iron / gold / diamond | copper, iron or gold ingot, diamond |
| Netherite | netherite ingot |
| Leather armor / chainmail | leather / iron ingot |
| Elytra / turtle shell / mace | phantom membrane / turtle scute / breeze rod |
| Bow, fishing rod | string (config) |
| Crossbow, shears, flint and steel | iron ingot (config) |
| Trident | prismarine shard (config) |
| anything else without a repair material | diamond (`fallbackMaterial`) |

The empty material slot shows the required material as a faint ghost item.

Not upgradeable by default: enchantments with only one level (Silk Touch, Mending, Infinity, Multishot, Flame,
Channeling, Aqua Affinity, …) and curses. Lure is capped at V, because above that fish stop biting in vanilla.

With the default settings, for example:

| Step | Chance | Diamonds | XP levels |
| --- | --- | --- | --- |
| V → VI | 49.6 % | 6 | 6 |
| IX → X | 25.9 % | 10 | 10 |
| XIV → XV | 11.5 % | 15 | 15 |
| XIX → XX | 5.1 % | 20 | 20 |

With 64 Luck Charms, XIX → XX rises from 5.1 % to 69.1 %.

## Settings

With Mod Menu: **Mods → Enchantment Forge → settings button**. Every option has a tooltip, a reset button and is
checked before saving; chances are shown in percent, lists and maps open in a text editor (one entry per line).
Changes apply immediately. Without Mod Menu, edit `config/enchantforge.json` (created on first start) and run
`/enchantforge reload` (operator level 2).

The settings belong to whoever runs the world: in singleplayer and LAN worlds your own settings count, on a dedicated
server the server's `config/enchantforge.json`.

| Key | Default | Meaning |
| --- | --- | --- |
| `maxLevel` | `20` | highest reachable level (1–255) |
| `maxLevelOverrides` | `{"minecraft:lure": 5}` | own limit per enchantment. An entry also makes single-level enchantments upgradeable (e.g. `"minecraft:multishot": 5`) |
| `excludeSingleLevelEnchantments` | `true` | exclude enchantments whose vanilla maximum is I |
| `excludeCurses` | `true` | exclude curses |
| `excludedEnchantments` | Silk Touch, Mending, Infinity | always excluded |
| `excludedItems` | `[]` | item ids the forge refuses |
| `allowEnchantedBooks` | `false` | allow enchanted books (material: lapis lazuli) |
| `baseChance` | `0.95` | chance for the step I → II |
| `chanceMultiplierPerLevel` | `0.85` | each further target level multiplies the chance by this |
| `minChance` | `0.02` | lower limit of the chance |
| `chancePerTargetLevel` | `{}` | fixed chances per target level, e.g. `{"20": 0.01}` |
| `failDowngradeChance` | `0.0` | chance to lose a level on failure |
| `charmChanceBonus` | `0.01` | extra chance per Luck Charm (0.01 = +1 percentage point) |
| `charmMaxPerAttempt` | `64` | most Luck Charms per attempt (0 turns them off) |
| `charmMaxChance` | `1.0` | Luck Charms never raise the chance above this |
| `materialCostBase` / `materialCostPerLevel` | `1` / `1` | material cost = ⌈(base + perLevel × currentLevel) × multiplier⌉, at most one stack |
| `materialCostMultipliers` | `{"minecraft:netherite_ingot": 0.25}` | cost factor per material |
| `xpCostBase` / `xpCostPerLevel` | `1` / `1` | XP level cost = base + perLevel × currentLevel |
| `creativeIgnoresCosts` | `true` | creative mode pays nothing and uses no Luck Charms (the chance still applies) |
| `materialOverrides` | bow → string … | material per item, tags too: `"#minecraft:planks"` |
| `fallbackMaterial` | `minecraft:diamond` | material for items without repair material and without override |

In the settings screen, chances are typed in percent (`95` = `0.95`), also in the map of fixed chances
(`20 = 1` = 1 % for level XX).

## Note

When the vanilla anvil combines two items with the **same** enchantment, it caps it at the vanilla maximum – a
Sharpness XX sword combined with a Sharpness book in the anvil drops back to V. Repairing with material in the anvil
or combining without a shared enchantment is fine.

## Languages

English and German (Deutsch). The game language decides; the settings screen is translated too.

## Development

Needs JDK 21 or newer.

```
gradlew build              # builds build/libs/enchantforge-1.0.0.jar and runs the translation check
gradlew runClient          # test client (with Mod Menu)
gradlew runClientGameTest  # end-to-end test in a real client, screenshots in build/run/clientGameTest/screenshots
python tools/gen_textures.py .  # regenerates the textures (block, GUI, Luck Charm) and the icon
```

The client game test (English, 1920×1080) opens the forge, inserts a sword, diamonds and Luck Charms, clicks
enchantment rows with the mouse and checks levels, costs, charm use and the returned items on the server. Then it
changes settings in the config screen behind Mod Menu's button: invalid input, a percentage, the map editor of fixed
chances and the "discard changes?" prompt.

## License

[MIT](LICENSE)
