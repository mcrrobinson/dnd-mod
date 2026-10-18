# Crypt building blocks
The Crypt's own building blocks: ossuary walls, carved reliefs, burial niches, sarcophagi that can hide an ambush, grave candles and grave moss, and the Lich's sigil floor and phylactery pedestal. [Crypt dungeons](../structures/dungeons.md#the-crypt) are built from them, and they're all in the **DnD Building Blocks** creative tab.

## How it works
| Block | Looks like | Notes |
|-|-|-|
| **Ossuary Bricks** (and stairs, slab, wall) | grey stone bricks with skulls and long bones set into the mortar | a [block family](../dev/building-blocks.md): stone bricks' hardness, pickaxe |
| **Funerary Relief** | a carved stone panel with a shrouded figure, hands folded | faces the player who places it; chiselled stone bricks' hardness, pickaxe |
| **Burial Niche Wall** | a wall block with a recess holding a wrapped body | faces the player who places it; a full block to walk into, stone bricks' hardness, pickaxe |
| **Sarcophagus** | a stone coffin two blocks long, its lid carved with a knight's effigy | placed like a bed: the foot where you click, the head beyond it. Breaking either half breaks both and drops one. Polished deepslate's hardness, pickaxe |
| **Grave Candle Cluster** | a puddle of white wax with five burning stubs | always lit, light level 6, with flames and smoke; needs a solid block under it and breaks instantly, like a candle |
| **Lich Sigil Floor** | dark stone with a ring of glowing green necrotic runes | light level 4; the runes pulse slowly. Polished deepslate's hardness, pickaxe |
| **Grave Moss** and **Grave Moss Carpet** | pale, sickly green moss with grey threads | moss block and moss carpet settings, hoe |
| **Phylactery Pedestal** | a black stone pedestal with silver bands and a glowing green rune on top | light level 5; its top is a full block high. Polished blackstone's hardness, pickaxe |

- **Sarcophagus ambush**: a sarcophagus a Crypt places is *occupied* 35% of the time. Using an occupied one slides the lid (a grinding sound) and a **zombie** climbs out on top of it, after whoever opened it (not a creative or spectator player). The sarcophagus is empty from then on. A player the dead take for one of their own (a Necromancer and their party, as for [Crypt Sleepers](../systems/obstacles.md)) sees "The dead within sleep on, untroubled by you." and the sarcophagus stays occupied. A sarcophagus you place yourself is always empty.
- **The Lich's pedestal**: in a Crypt, the phylactery appears on the reliquary's pedestal. A Lich raised anywhere else (a spawn egg, `/dm`, a stronghold library) sets a Phylactery Pedestal in the block where its phylactery would stand and lifts the phylactery onto it, if that block is air, there's room above and `mobGriefing` is on. The pedestal stays after the phylactery is smashed.

### In the Crypt
| Where | What |
|-|-|
| Room and corridor walls and ceilings | 30% Ossuary Bricks and 5% Grave Moss among the stone bricks (the rest: 8% mossy, 12% cracked, stone bricks) |
| Floors | 6% Grave Moss among the polished andesite |
| Corridors | Burial Niche Walls in the side walls at head height, every third block (centred along it), half of the spots |
| Antechamber and boss room | sarcophagi (Sarcophagus blocks) along the walls, with candles or a grave candle cluster at one end half the time |
| Encounter rooms | Funerary Reliefs in the walls (4 blocks either side of each wall's middle, 40% each), grave moss patches |
| Boss room | a Funerary Relief in the middle of each wall, over the gap between its sarcophagi; Lich Sigil Floor on top of the dais; grave candle clusters on the dais corners; grave moss patches |
| Reliquary | the Phylactery Pedestal in the middle (the phylactery stands on it), a ring of Lich Sigil Floor round it, grave candle clusters on the diagonals, Funerary Reliefs either side of each wall's middle (75% each), grave moss patches |

Crypts generated before this change keep their old blocks.

## Where to find it
- Mine them out of a Crypt: every block drops itself (a pickaxe for the stone ones, a hoe is fastest for the moss).
- The **DnD Building Blocks** creative tab.
- Recipes:
  - Ossuary Bricks: 8 stone bricks round a bone make 8. Its stairs, slab and wall craft and stonecut like stone bricks' (see [Building blocks](../dev/building-blocks.md)).
  - Funerary Relief, Burial Niche Wall: stonecut 1 Ossuary Bricks into 1.
  - Sarcophagus: 5 polished deepslate round a bone block (`#B#` over `###`) make 1.
  - Grave Candle Cluster: 3 candles (shapeless).
  - Grave Moss: a moss block and bone meal (shapeless). Grave Moss Carpet: 2 grave moss side by side make 3.
  - The Lich Sigil Floor and the Phylactery Pedestal have no recipe.

## For developers
- `classes/Registry/CryptBlocks.java`: the one-off blocks, their settings and registration (items go in the DnD Building Blocks tab after the families). Ossuary Bricks is `ModBlockFamilies.OSSUARY_BRICKS`.
- `classes/Blocks/SarcophagusBlock.java` (the two halves, `OCCUPIED` and the ambush), `CarvedFacingBlock.java` (relief and niche), `GraveCandleClusterBlock.java`, `PhylacteryPedestalBlock.java`.
- Placement: `world/gen/dungeon/CryptDressing.java` (`sarcophagus`, `candleCluster`, `mossPatches`, `wallReliefs`, `corridorNiches`, `sigilRing`; `OCCUPIED_CHANCE`), the Crypt palette in `DungeonTheme.Palette`, and the room pieces. Reliefs and niches only replace plain crypt wall (`CryptDressing.isPlainWall`), so they never cut into a doorway, mural or seal.
- `LichEntity.raisePedestal`: the pedestal under a new phylactery.
- Resources: `tools/crypt_blocks.py` (blockstates, models, loot, recipes, tags, lang for the one-off blocks), `tools/block_families.py` (Ossuary Bricks), `tools/crypt_block_textures.py` (the textures; `lich_sigil_floor.png` is a 4-frame animation).
- Devscripts: `devscripts/crypt-blocks.txt` (each block on a platform, drops, recipes, the ambush, the Lich's pedestal) and `devscripts/crypt-blocks-survey.txt` (in-room shots of a placed Crypt).
