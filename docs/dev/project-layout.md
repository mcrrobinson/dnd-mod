# Project layout
Where things live in the code.

## For developers
| Area | Location (`src/main/java/mattonfire/dnd/`) |
|-|-|
| Mod entry point (registration order) | `classes/DnDClasses.java` |
| Class rules and mana regen (item bans, Necromancer wither, Blood Hunter fire, Lunge), the power-up key, packet ids | `classes/ClassEvents.java`, `classes/PowerUpRequests.java`, `classes/ClassPackets.java`, `classes/ManaManager.java` |
| Class stats and picking | `classes/ClassStats.java`, `classes/ClassLifecycle.java`, `classes/DndCharacter.java`, `classes/CharacterCreation.java`, `classes/ClassInfo.java` (reads `data/dndclasses/class_info.json`) |
| Class passives | `classes/` (`Druid`, `Warlock`, `MonkHandler`, `PotionImmunity`) and `classes/Misc/` |
| Class specials | `classes/Misc/PowerUpEffect.java`, `classes/Effects/` |
| Class XP, skill trees, ranks, subclasses, bestiary, Attunement Table | `classes/Progression/` (`Progression`, `ClassProgress`, `ClassTrees`, `Ranks`, `Subclass`, and one `Classes/<Class>Skills.java` per class; `SkillHelpers` has the shared targeting, summons, timed area effects (`startArea`), thrown-potion watching (`onLanding`) and per-player `Cooldowns`), `classes/Blocks/AttunementTableBlock.java` |
| Ability scores, proficiency, skills, saves | `classes/Abilities/` (`AbilityScores`, `CharacterSheet`, `ClassSaveFeatures`) |
| d20 rolls: checks, saves, attack rolls, lockpicking, stealth, perception, study, traps | `classes/SkillChecks/` (`D20`, `SavingThrow`, `AttackRolls`, `Lockpicking`, `Stealth`, `Perception`, `Study`, `TrapDisarm`) |
| Rests, charges, Hit Dice, party camps | `classes/Rest/` |
| Downed and death saves | `classes/Downed/` |
| Class-gated obstacles | `classes/Obstacles/` (`types/` has one class per obstacle) |
| Races, racial traits, Breath Weapon | `classes/Race/`; racial homes in `world/gen/RacialHomes.java` and `world/gen/HomeBonuses.java` |
| Parties and party roles | `classes/Party/`, `classes/PartyRole.java` |
| Commands | `classes/Commands/` (`/dndclass`, `/dndrace`, `/dndhome`, `/dndmagic`, `/dndobstacle`, `/dungeon`, `/goblinraid`); `/party` is `classes/Party/PartyCommand.java`, `/rep` `faction/RepCommand.java`, `/quest` `quest/QuestCommand.java`, `/dm` `dm/DmCommand.java` |
| Mixins (most class rules, dragon hit detection, dev window) | `classes/mixin/` (listed in `dndclasses.mixins.json`) |
| Items, armor, blocks | `classes/Items/`, `classes/Items/lib/`, `classes/Blocks/`, `classes/Registry/` |
| Magic items: rarity, attunement, identification, curses, loot | `magic/` (`items/` has the item classes) |
| Enchantments | `classes/Enchantments/`, `classes/TreeFeller.java`, `GridMiner.java`, `Invulnerability.java`, `Featherfall.java` |
| Music: Bard instrument slot, dungeon themes | `classes/Music/`, client side in `classes/Client/Music/` |
| Client: HUD, keybinds, screens, DevScript | `classes/Client/` (`Hud/`, `Keybinds/`, `DevScript.java`) |
| Mobs and bosses | `entity/`, `entity/boss/`, `entity/ai/goal/` |
| Goblin raids | `entity/raid/` |
| Factions and reputation | `faction/` |
| Quests and NPC dialogue | `quest/`, `quest/dialogue/` |
| Dungeons: rooms, encounters, gates, puzzles, loot | `dungeon/`; generation in `world/gen/dungeon/` |
| Dungeon Master tools | `dm/` (`encounter/` has the data-driven encounters) |
| Hobbit Tavern (bounty board, ale, rooms) | `tavern/` |
| Models, renderers, particles | `client/model/`, `client/renderer/`, `particle/` |
| Structures and spawns | `world/gen/` (`village/`, `fortress/`, `lair/`, `camp/`, `beholder/`, `dungeon/`, `enclave/`, `temple/`, `stronghold/`, `sanctum/`, `burrow/`, `ModSpawns.java`); shared piece code in `world/gen/ModPiece.java` (a piece's seed, `generate` and the `Builder` basics: `set`, `fill`, `get`, `propUp`, `container`, `chest`, `mimic`, `frame`, `npc`, ...), `FacingPiece.java` (turned the vanilla way), `OrientedPiece.java` (turned round a centre), `PieceBlocks.java` (block states, loot ids, the `UPDATE` flags) and `TerrainColumns.java` (cached height, water and biome samples for site checks); the `/dndhome` surveys share `classes/Commands/HomeCommandUtil.java` |
| Data (loot, tags, structures, advancements, quests, dialogue, factions, saves, encounters) | `src/main/resources/data/dndclasses/` |
| Assets (geo models, animations, textures, sounds, lang) | `src/main/resources/assets/dndclasses/` |

- Minecraft sources aren't decompiled; see [CLAUDE.md](../../CLAUDE.md#reading-minecraft--library-code) for reading the jars.
- `build/` is ignored by git (`.gitignore`), like the IDE files `.classpath` and `.project`.

## Dependencies
What each `build.gradle` dependency is for. Jars in `libs/` resolve through the `flatDir` repository, since their Maven repos are offline.

| Dependency | Used for | How it's declared |
|-|-|-|
| Fabric API | events, networking, registries | `modImplementation` |
| GeckoLib 4.2 | animated mobs, class armor, the player armor model | `modImplementation`; players install it (`depends` in `fabric.mod.json`) |
| LibGui (`libs/LibGui-7.1.1+1.19.4.jar`) | GUI screens | `modImplementation include` (bundled); its deps Jankson-Fabric, libninepatch and Jankson are `modLocalRuntime`/`localRuntime` because the dev runtime doesn't load nested jars |
| reach-entity-attributes 2.3.2 | the race attack-reach modifier (`Race/RaceStats`) | `modImplementation include` (bundled) |
| Identity (`libs/identity-2.7.0-1.19.4-fabric-dev-build-2.jar`) | Druid Wild Shape, Blood Hunter possession (`Druid`, `Misc/BloodHunterIdentityCompat`) | `modImplementation`; optional for players (`recommends`) |
| Architectury, omega-config (`libs/omega-config-base-1.3.0+1.19.2.jar`) | nothing in this mod; Identity needs them, so the dev client loads them | `modLocalRuntime` (not bundled, not on the compile classpath) |

## Tools
`tools/` holds the scripts that generate assets. Edit a script and rerun it rather than hand-editing what it writes. Each one's docstring says how to run it.

| Script | Generates |
|-|-|
| `tools/boss_item_textures.py` | the boss and monster magic item textures |
| `tools/breath_icons.py` | the Dragonborn Breath Weapon cooldown icons |
| `tools/craft_obstacle_textures.py` | the craft and element obstacle textures (Artificer, Alchemist, Warlock) |
| `tools/fire_curtain_textures.py` | the Fire Curtain's connected soul-fire sheets (`--preview <dir>` for mock-ups) |
| `tools/d20_texture.py` | `textures/gui/d20.png`, the skill-check HUD die |
| `tools/death_save_textures.py` | the death save pips on the Downed HUD |
| `tools/divine_obstacle_textures.py` | the divine and death obstacle textures (Cleric, Paladin, Necromancer, Blood Hunter) |
| `tools/dragonborn_skins.py`, `elf_skins.py`, `tiefling_skins.py` | the Dragonborn, elf and Tiefling NPC skins |
| `tools/dungeon_loot.py` | the dungeon loot tables |
| `tools/ember_wyvern_texture.py` | the Ember Wyvern textures, recoloured from the green wyvern |
| `tools/frost_drake_texture.py` | the Frost Drake textures, recoloured from the Lightning Chaser |
| `tools/lich_models.py` | the Lich and Phylactery models and textures |
| `tools/locked_door_texture.py` | the Locked Door obstacle texture |
| `tools/magic_tier_tables.py` | the magic item tier loot tables |
| `tools/nature_song_obstacle_textures.py` | the nature and song obstacle textures (Druid, Bard) |
| `tools/obstacle_textures.py` | the Arcane Seal textures |
| `tools/orc_stronghold_textures.py` | the Orc Stronghold textures |
| `tools/owlbear_model.py` | the Owlbear model and texture |
| `tools/physical_obstacle_textures.py` | the physical obstacle textures (Barbarian, Fighter, Monk) |
| `tools/racial_gear_textures.py` | the racial armor and weapon textures |
| `tools/rune_textures.py` | the six rune block faces of the puzzle room |
| `tools/speech_bubble_texture.py` | the `/dm say` speech bubble particle |
| `tools/tavern_room_textures.py` | the Room Key texture and the Well Rested icon |
| `tools/wondrous_item_textures.py` | the wondrous and utility magic item textures |
| `tools/pngio.py` | shared PNG reader/writer for the scripts above (no PIL needed) |
| `tools/music-gen/` | class stings, dice sounds, music loops and Bard songs (see its README) |
