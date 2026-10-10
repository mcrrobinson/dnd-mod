# Magic Items
The named wondrous and utility magic items: a wand of darts, a bag bigger on the inside, a decanter that never runs dry, a rod that hangs in the air, protective rings and amulets, a Bard's master lute and a tome that lets you choose your subclass again. They use the [magic item system](../systems/magic-items.md): rarity, identification, attunement and anti-magic all apply.

## How it works
Every item below works only while its magic is awake: it's identified, you're not in a Beholder's anti-magic cone, and, if it needs attunement, you're attuned to it (and of the right class). Otherwise using it puts the reason on the action bar ("Its magic sleeps. Identify it first.", "Attune to it at an Attunement Table to wake its magic."). Wondrous items, rings and amulets work from anywhere in your inventory: carried is worn.

| # | Item | Tier | Attunement | Effect |
|-|-|-|-|-|
| 1 | Wand of Magic Missiles | Uncommon wand | no | Right-click: 3 darts at the nearest hostile you face (within 24 blocks and 30 degrees of your aim, in sight), 2 magic damage each (6 in one hit; magic ignores armor). 7 charges |
| 2 | Bag of Holding | Uncommon wondrous item | no | Right-click: a private 27-slot inventory stored on the bag |
| 3 | Decanter of Endless Water | Uncommon wondrous item | no | A water bucket that never empties. Sneak + right-click: a geyser |
| 4 | Immovable Rod | Uncommon wondrous item | no | Right-click: a solid rod fixed in place where you look, for 60 s |
| 5 | Cloak of Protection | Uncommon wondrous item | yes | +1 armor, +1 armor toughness |
| 6 | Periapt of Wound Closure | Uncommon wondrous item | yes | You stabilise as soon as you're Downed; natural regeneration twice as fast |
| 7 | Ring of Protection | Rare ring | yes | +2 armor, +1 to every saving throw |
| 8 | Amulet of Health | Rare wondrous item | yes | +6 max health (3 hearts), and your Constitution is at least 19 |
| 9 | Doss Lute | Rare wondrous item | yes, Bard | A Lute whose song gives Regeneration II to players within 24 blocks; Animal Friends companions get +4 max health |
| 19 | Tome of Clear Thought | Very Rare consumable | no | Read near an Attunement Table: clears your current class's subclass and refunds its points |

### Wand of Magic Missiles
- Right-click fires three glowing darts that curve onto the nearest hostile mob (any monster, or any mob targeting you) within 24 blocks and 30 degrees of where you look, that you can see. Together they deal 6 magic damage, credited to you. No target: "No hostile creature in sight." and no charge is spent.
- 7 charges, shown in the tooltip ("Charges: 5/7") and as a purple bar under the item once it's below full. 0.5 s between shots.
- It regains 1d6+1 charges at each dawn (the in-game day changes while it's in your inventory) and on a [long rest](../systems/rests.md).
- Spending the last charge rolls a d20. On a 1 the wand crumbles to dust; otherwise "Last charge spent. The wand holds together (rolled *n*)." At 0 charges: "The wand is spent."
- Creative mode doesn't spend charges.

### Bag of Holding
- Right-click opens a 9x3 chest screen named after the bag. The contents are saved in the bag itself (NBT `BagItems`), so they go wherever the bag goes: dropped, in a chest, through a relog.
- A Bag of Holding can't go into a Bag ("That would tear a hole in space."), or into a shulker box or bundle.
- The bag you opened is locked in its slot while it's open, so it can't be moved, thrown or swapped.

### Decanter of Endless Water
- Right-click: places water exactly like a water bucket (waterlogs blocks, evaporates in the Nether), but the decanter stays full.
- Sneak + right-click: a geyser. Mobs and players within 6 blocks in front of you (about 60 degrees either side) are shoved about 4 blocks away (less with knockback resistance, none at full), burning ones are put out, and fire and lit campfires along the jet are doused. 30 s cooldown, kept on the item: "The geyser is gathering water (*n* s)."

### Immovable Rod
- Right-click: the rod fixes itself against the face of the block you aim at within 4 blocks, or, aiming at nothing, in mid-air 2.5 blocks along your view. Looking down (60 degrees or more) at nothing within reach, it fixes itself right under your feet and lifts you onto it if you've started to fall: you can stand on it in mid-air.
- It's a solid, unbreakable iron bar (6/16 of a block wide, full height) that pistons can't move. It only goes into a free spot (air, water, grass) where you may build, not inside an [obstacle's](../systems/obstacles.md) protected area, and not where an entity stands: "The rod won't hold there."
- Right-click the item again, or right-click the rod itself (anyone can), and it lets go. Otherwise it returns after 60 s. The item never leaves your hand; its tooltip shows where the rod is fixed.

### Periapt of Wound Closure
- When you're [Downed](../systems/death-saves.md), you stabilise straight away ("Your Periapt of Wound Closure seals your wounds: you're stable.") and stand up after the usual 30 s.
- Natural regeneration: with food 18 or more, you heal an extra 1 HP every 4 s (for the usual 6 exhaustion), doubling the vanilla slow heal.

### Ring of Protection and Amulet of Health
- The Ring adds +2 armor and +1 to every saving throw on your [character sheet](../systems/ability-scores.md).
- The Amulet adds +6 max health and sets your Constitution to at least 19 (CON saves, Hit Dice).

### Doss Lute
- In the hands of the Bard attuned to it, the song gives **Regeneration II** for 30 s to every player within **24** blocks (a Lute gives Regeneration I within 16), and it shares the instruments' 10 s cooldown.
- While a Bard carries an attuned Doss Lute, the instrument slot key (G) plays the Doss Lute instead of the plain lute, and animals they gather with Animal Friends get +4 max health (a permanent modifier on the companion).
- For anyone else, or before attunement, it plays like a plain [Lute](bard-instruments.md).

### Tome of Clear Thought
- Read it (right-click) within 4 blocks of an [Attunement Table](../systems/class-progression.md) (or at the table you last opened). It clears the subclass of your current class and refunds every node only that subclass could have (and the capstone), with their ranks. Chat says how many points came back.
- It's used up only if there was a subclass to clear ("You have no subclass to forget.").

## How to get it
- From the D&D Classes creative tab, or `/dndmagic give <player> dndclasses:<item>` (add `unidentified` to test identification).
- Loot placement (goblin camps, dwarven fortresses, Mimics, bounties, vanilla structures) comes with the loot tables update.

Item ids: `wand_of_magic_missiles`, `bag_of_holding`, `decanter_of_endless_water`, `immovable_rod`, `cloak_of_protection`, `periapt_of_wound_closure`, `ring_of_protection`, `amulet_of_health`, `doss_lute`, `tome_of_clear_thought`.

## Commands
- `/dndmagic give|identify|attune|bonds ...`: see [Magic items](../systems/magic-items.md).
- Wand charges: `/data get entity @s SelectedItem.tag.dndclasses_magic.charges`; set them with `/item modify` or a give with NBT, e.g. `/give @s dndclasses:wand_of_magic_missiles{dndclasses_magic:{charges:1}}`.
- `/dndclass forceroll <player> 1` rigs the wand's crumble roll.

## Configuration
- The Wand's long-rest recharge needs the `dndRests` gamerule; the dawn recharge always works.
- The Periapt's extra heal follows the `naturalRegeneration` gamerule.

## Known limitations
- The wand's three darts land as one 6-damage hit (Minecraft's damage cooldown would swallow three quick 2-damage hits).
- The Periapt doubles the slow natural heal only, not the fast saturation heal.
- If you release the Immovable Rod and fix it again on the same spot within 60 s, the first placement's timer can take the new rod away early.
- The Cloak of Protection's +1 to saving throws is a separate update.
- The textures are 16x16 placeholders (`tools/wondrous_item_textures.py`).

## For developers
- `magic/items/`: one class per active item (`WandOfMagicMissilesItem`, `BagOfHoldingItem` with its `BagScreenHandler`, `DecanterOfEndlessWaterItem`, `ImmovableRodItem` + `ImmovableRodBlock`, `DossLuteItem`, `TomeOfClearThoughtItem`), `MagicItemUse.ready` (the shared "is the magic awake" check with its message) and `WondrousItems` (the tiers, the Ring/Amulet/Periapt effects, the long-rest and Downed hooks and the `magic/wondrous_items` ability contributor).
- Registration: `ModItems`, `ModBlocks.IMMOVABLE_ROD`, `ModItemGroup`; called from `Magic.register`.
- Charges live in `MagicData` (`charges`, with `MagicData.charges(stack, full)` for fresh items); the wand's dawn day is `dndclasses_magic.dawnDay`, the Decanter's cooldown `geyserReadyAt`, the Rod's placement `dndclasses_magic.rod {Pos, Dim, State}`.
- The Rod's 60 s return uses `ScheduledBlockRestore.schedule`.
- `InstrumentItem.playAsBard(user, amplifier, radius)` is the stronger song; `BardInstrumentSlot` and `BardSkills.animalFriends` call into `DossLuteItem`.
- Textures: `python3 tools/wondrous_item_textures.py`.
