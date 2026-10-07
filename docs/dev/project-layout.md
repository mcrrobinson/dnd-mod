# Project layout
Where things live in the code.

## For developers
| Area | Location (`src/main/java/mattonfire/dnd/`) |
|-|-|
| Mod entry point, class switching, specials' packets | `classes/DnDClasses.java` |
| Class stats | `classes/SetClassAttributes.java`, `classes/DndCharacter.java` |
| Class passives | `classes/` (`Druid`, `Warlock`, `MonkHandler`, `PotionImmunity`) and `classes/Misc/` |
| Class specials | `classes/Misc/PowerUpEffect.java`, `classes/Effects/` |
| Mixins (most class rules, dragon hit detection, dev window) | `classes/mixin/` (listed in `dndclasses.mixins.json`) |
| Items, armor, blocks | `classes/Items/`, `classes/Items/lib/`, `classes/Blocks/`, `classes/Registry/` |
| Enchantments | `classes/Enchantments/`, `classes/TreeFeller.java`, `GridMiner.java`, `Invulnerability.java` |
| Client: HUD, keybinds, music, DevScript | `classes/Client/` |
| Mobs and bosses | `entity/`, `entity/boss/`, `entity/ai/goal/` |
| Models and renderers | `client/model/`, `client/renderer/` |
| Structures and spawns | `world/gen/` (`village/`, `fortress/`, `lair/`, `ModSpawns.java`) |
| Data (loot, tags, structures, advancements) | `src/main/resources/data/dndclasses/` |
| Assets (geo models, textures, sounds, lang) | `src/main/resources/assets/dndclasses/` |

- Minecraft sources aren't decompiled; see [CLAUDE.md](../../CLAUDE.md#reading-minecraft--library-code) for reading the jars.
- `build/` is tracked in git. Don't commit build output.
