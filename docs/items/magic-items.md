# Magic Items
The named magic items. The wondrous and utility ones: a wand of darts, a bag bigger on the inside, a decanter that never runs dry, a rod that hangs in the air, protective rings and amulets, a Bard's master lute and a tome that lets you choose your subclass again. The boss and monster drops: a goblin warhorn, a fire elemental's heart, storm boots, a gliding cloak, an owlbear belt, a mace that unmakes undead, a frost sword, a locket that cheats death, a thief's cursed ring, a beholder's eye, a gorgon's stone eye, and from the Underdark a drow matron's scourge and the drow's own cloak. They use the [magic item system](../systems/magic-items.md): rarity, identification, attunement and anti-magic all apply.

## How it works
Every item below works only while its magic is awake: it's identified, you're not in a Beholder's anti-magic cone, and, if it needs attunement, you're attuned to it (and of the right class). Otherwise using it puts the reason on the action bar ("Its magic sleeps. Identify it first.", "Attune to it at an Attunement Table to wake its magic."). Wondrous items, rings and amulets work from anywhere in your inventory: carried is worn.

| # | Item | Tier | Attunement | Effect |
|-|-|-|-|-|
| 1 | Wand of Magic Missiles | Uncommon wand | no | Right-click: 3 darts at the nearest hostile you face (within 24 blocks and 30 degrees of your aim, in sight), 2 magic damage each (6 in one hit; magic ignores armor). 7 charges |
| 2 | Bag of Holding | Uncommon wondrous item | no | Right-click: a private 27-slot inventory stored on the bag |
| 3 | Decanter of Endless Water | Uncommon wondrous item | no | A water bucket that never empties. Sneak + right-click: a geyser |
| 4 | Immovable Rod | Uncommon wondrous item | no | Right-click: a solid rod fixed in place where you look, for 60 s |
| 5 | Cloak of Protection | Uncommon wondrous item | yes | +1 armor, +1 armor toughness, +1 to every saving throw |
| 6 | Periapt of Wound Closure | Uncommon wondrous item | yes | You stabilise as soon as you're Downed; natural regeneration twice as fast |
| 7 | Ring of Protection | Rare ring | yes | +2 armor, +1 to every saving throw |
| 8 | Amulet of Health | Rare wondrous item | yes | +6 max health (3 hearts), and your Constitution is at least 19 |
| 9 | Doss Lute | Rare wondrous item | yes, Bard | A Lute whose song gives Regeneration II to players within 24 blocks; Animal Friends companions get +4 max health |
| 10 | Warlord's Warhorn | Rare wondrous item | no | Right-click: you and your party within 16 blocks get Strength I and Speed I for 15 s; goblins within 16 blocks are [Frightened](../systems/conditions.md) of you for 6 s. 120 s cooldown |
| 11 | Heart of the Furnace | Rare wondrous item (amulet) | yes | Fire Resistance; your melee hits set the target alight for 3 s. Water and rain deal 1 damage every 2 s |
| 12 | Storm-Chaser Boots | Rare armor (boots) | yes | Diamond boots. Worn: Speed I, Speed II while it rains or storms; lightning can't hurt you |
| 13 | Wyvern-Scale Cloak | Rare wondrous item (cloak) | yes | Half fall damage; sneak in mid-air to glide |
| 14 | Belt of Owlbear Might | Rare wondrous item (belt) | yes | +2 attack damage, +0.3 knockback resistance, Strength at least 21 |
| 15 | Mace of Disruption | Rare weapon | yes, Cleric or Paladin | 8 damage, 1.4 speed. +8 damage to undead; an undead at 25 HP or less that it hits saves (DC 15, d20 + 2) or crumbles to ash |
| 16 | Frostbrand | Very Rare weapon (sword) | yes | 9 damage, 1.6 speed. Hits give Slowness II for 3 s, 15% chance to Freeze for 2 s; in your hand, you can't be frozen and the Frost Drake's breath can't touch you |
| 17 | Phylactery Locket | Very Rare wondrous item (amulet) | yes, Wizard, Necromancer or Warlock | A killing blow leaves you on 2 HP with Resistance V for 3 s and blinks you about 8 blocks away. Recharges on a long rest or after 20 min |
| 18 | The Burglar's Ring | Very Rare ring | yes, always cursed (The Ring's Call) | Sneak still for a second: invisible until you attack or stop sneaking; mobs lose you beyond 4 blocks |
| 19 | Tome of Clear Thought | Very Rare consumable | no | Read near an Attunement Table: clears your current class's subclass and refunds its points |
| 20 | Eye of the Tyrant | Legendary wondrous item | yes | Right-click a creature within 32 blocks: a random eye ray (Paralysis, Telekinesis or Disintegration). 3 charges. You're immune to the Beholder's anti-magic cone |
| 21 | Gorgon's Gaze | Rare wondrous item | yes | Once per dawn, right-click a mob within 12 blocks (not a boss): CON save DC 14 or it's Petrified for good, a statue |
| 22 | Matron's Scourge | Very Rare weapon (whip) | yes | 7 damage, 1.6 speed, +1 reach. Hits carry Drow Poison (CON 14); once every 30 s a hit frightens the target (WIS 14, 4 s) |
| 23 | Piwafwi (Cloak of the Drow) | Rare armor (chestplate slot) | yes | 2 armor, +2 Stealth. Sneaking where the light is 4 or less, mobs more than 6 blocks away can't see you |
| 24 | Red Dragon Scale Mail | Very Rare armor (chestplate) | yes | 8 armor, 3 toughness. Half damage from fire, advantage on saves against dragons; crouch still for 3 s to sense the nearest Ancient lair (once a day) |

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

### Warlord's Warhorn
- Right-click to sound it (a raid horn, heard far away). You and every [party](../systems/party.md) member within 16 blocks get **Strength I** and **Speed I** for 15 s. Every goblin (Goblin Warriors, raid goblins, a Goblin Warlord) within 16 blocks gets **Frightened** for 6 s: it deals half damage, stops chasing you and runs from you (`ConditionFleeGoal`).
- The action bar says how many were rallied and frightened. 120 s cooldown, kept on the item (`hornReadyAt`), shown as the usual cooldown sweep; using it again before then says "Your lungs need a rest (N s)" on the action bar. No attunement, but it has to be identified.

### Heart of the Furnace
- Anywhere in your inventory, attuned: **Fire Resistance** (no particles, refreshed every second), and each melee hit sets the target alight for 3 s.
- The drawback isn't a curse, so it ends when you unattune or drop it: while you're in water or rain you take 1 damage every 2 s, like a Warlock, but never below 2 HP (1 heart). Warlocks don't take it twice.

### Storm-Chaser Boots
- Diamond boots (3 armor, 2 toughness, diamond durability, look like diamond boots when worn). Worn and attuned: **Speed I**, or **Speed II** while it's raining or thundering in the world. A stronger Speed from a potion isn't overwritten.
- Immune to lightning: vanilla bolts (thunderstorms, channeling tridents) and the [Lightning Chaser's](../mobs/dragons.md) storm do nothing, and its storm doesn't roll your save.

### Wyvern-Scale Cloak
- Attuned, anywhere in your inventory: fall damage is halved.
- Sneak while falling (not flying, gliding with an elytra or swimming) to glide: you get Slow Falling for as long as you hold sneak.

### Belt of Owlbear Might
- Attuned: +2 attack damage and +0.3 knockback resistance, and your [Strength](../systems/ability-scores.md) is at least 21 (STR checks and saves).
- A Druid in Owlbear form keeps the bonuses: the form is still the same player with the same inventory.

### Mace of Disruption
- A mace with 8 attack damage and 1.4 attack speed (built on the sword, so sword enchantments apply). It glints once identified ("glows in your hand").
- Attuned by a Cleric or Paladin, in the main hand: **+8 damage** to undead (zombies, skeletons, wither skeletons, the Lich, phantoms...).
- When it hits an undead that has **25 HP or less**, the undead rolls a save: d20 + 2 against DC 15, shown to you on the roll HUD ("Disruption save"). On a failure it crumbles to ash (the hit kills it, with ash and light particles); on a success it just takes the damage.

### Frostbrand
- A sword with 9 attack damage and 1.6 attack speed. Attuned, in your main hand: each melee hit gives **Slowness II for 3 s**, and 15% of hits also **Freeze** the target for 2 s (it's held in place and tinted blue).
- While you hold it you can't be frozen (the Frost Drake's breath, a Potion of Freezing, a Staff of Ice), and the [Frost Drake's](../mobs/dragons.md) breath doesn't touch you at all.

### Phylactery Locket
- Attuned by a Wizard, Necromancer or Warlock, anywhere in your inventory. When damage would kill you, it takes the blow instead: you're left on **2 HP**, put out, with **Resistance V** for 3 s, and you **blink about 8 blocks away** (a random safe spot, like a chorus fruit). Chat: "Your Phylactery Locket shatters the killing blow and pulls your soul away!"
- It comes before the [Downed](../systems/death-saves.md) rules, so it saves you even with nobody near. A totem of undying in your hand goes first. `/kill`, the void and other damage that ignores death saves can't be stopped.
- Then it's spent (tooltip: "Spent: recharging") until your next [long rest](../systems/rests.md), or 20 minutes (one in-game day) of world time.

### The Burglar's Ring
- Always cursed with **The Ring's Call**, so whoever holds it for 3 s, or attunes to it, is bound to it. It takes an attunement slot and only [Remove Curse](../systems/magic-items.md#remove-curse) (DC 16 for a Very Rare item) breaks the bond.
- Sneak and stand still for about a second: you turn **invisible** ("You fade from sight.") until you stop sneaking or attack (melee, arrows, spells). Mobs chasing you from more than 4 blocks away lose you, checked 4 times a second.
- The Ring's Call: -2 max health for each in-game day you've been bound (counted from world time of day, so `/time add 24000` counts), up to -10. At night, hostile mobs within 48 blocks come for you: the ones that see you target you, the rest walk towards you.

### Eye of the Tyrant
- Attuned, right-click a creature you're looking at within 32 blocks (not through walls). It fires one of three rays at random, drawn as a coloured beam:
  - **Paralysis** (blue): [Paralyzed](../systems/conditions.md) for 3 s (can't move or act, STR/DEX saves fail, close hits crit; a boss shakes it off after 1 s).
  - **Telekinesis** (purple): Levitation II for 3 s.
  - **Disintegration** (green): 12 magic damage (ignores armor), credited to you.
- 3 charges (tooltip and a green bar), 1 s between rays. One charge comes back at each dawn and on a long rest. Creative mode doesn't spend charges.
- While it's active (identified, attuned, anywhere in your inventory) the Beholder's anti-magic cone can't touch you, so your class specials and other magic items keep working.

### Gorgon's Gaze
- Attuned, right-click a mob you're looking at within 12 blocks (not through walls, not a player or a boss). It makes a **CON save** against DC 14 (mobs add +1). On a failure it's **[Petrified](../systems/conditions.md) indefinitely**: a statue that doesn't move, act or make a sound, and only a pickaxe breaks it (for its normal loot). On a success nothing happens; the charge is spent either way.
- 1 charge (tooltip and a green bar), back at each dawn and on a long rest. Creative mode doesn't spend it. A boss shrugs it off without spending it.

### Matron's Scourge
- The [Matron Mother](../bosses/matron-mother.md)'s whip of barbed chains, held in the main hand: 7 attack damage and 1.6 attack speed (a sword's), and **1 block more reach**.
- Every melee hit poisons: the target makes a **CON save DC 14** against [Drow Poison](../mobs/drow.md#drow-poison) (Poisoned and slowed; three failures in 10 s and it falls asleep). Drow, undead, constructs and anything else that can't be poisoned shrug it off.
- At most once every 30 s, a hit also makes the target save (**WIS DC 14**, mobs add +1) or be **[Frightened](../systems/conditions.md)** of you for 4 s. Bosses and the drow can't be frightened.

### Piwafwi
- The Cloak of the Drow, worn in the chestplate slot (2 armor; it takes the place of a chestplate). It's drawn as a near-black hooded cloak with a violet lining and a silver clasp.
- **+2 Stealth** on your [character sheet](../systems/ability-scores.md) while it's active (worn and attuned).
- **Unseen in the dark**: while you sneak where the light (block light, or what's left of the sky light) is **4 or less**, mobs **more than 6 blocks away** can't target you, and any that were chasing you lose you at once. Within 6 blocks, or in the light, you're seen as usual (passive Stealth still shortens how far they notice you).

### Red Dragon Scale Mail
- The [Ancient Red Dragon](../bosses/ancient-red-dragon.md)'s hide, worn as a chestplate: **8 armor, 3 toughness, +0.1 knockback resistance**, drawn as overlapping crimson scales with an ember-orange belly. Repaired with Ancient Red Scales.
- **Half damage from fire**: anything fire-typed (flames, lava, fire breath, magma vents).
- **Advantage on saves against dragons**: every dragon's breath and storm, and every save the Ancient Red Dragon and its lair call for (shown on the [character sheet](../systems/ability-scores.md) as "saves against dragons").
- **Dragon sense**: crouch without moving for 3 s and, once a day, the action bar points to the nearest Ancient dragon lair within 3000 blocks ("A pull to the north east, about 1200 blocks."). Until the Ancient lairs generate, it says no lair is near.

## Where to find it
- From the D&D Classes creative tab, or `/dndmagic give <player> dndclasses:<item>` (add `unidentified` to test identification).
- The boss and monster items drop, unidentified, when a player kills their monster (Looting adds the bonus shown):

| Item | Dropped by | Chance |
|-|-|-|
| Warlord's Warhorn | [Goblin Warlord](../bosses/goblin-warlord.md) | 35% |
| Heart of the Furnace | [Magmamuncher Alpha](../bosses/magmamuncher-alpha.md) | 20% (+5% per Looting level) |
| Storm-Chaser Boots | [Lightning Chaser](../mobs/dragons.md) | 12% (+3%) |
| Wyvern-Scale Cloak | [Wyvern, Ember Wyvern](../mobs/dragons.md) | 8% |
| Belt of Owlbear Might | [Owlbear](../mobs/owlbear.md) | 8% (+2%) |
| Mace of Disruption | [Lich](../bosses/lich.md) (final death) | 25% |
| Frostbrand | [Frost Drake](../mobs/dragons.md) | 10% (+5%) |
| Phylactery Locket | [Lich](../bosses/lich.md) (final death) | 20% |
| Eye of the Tyrant | [Beholder](../bosses/beholder.md) | 12% (+3%) |
| Gorgon's Gaze | [Medusa](../bosses/medusa.md) | 25% (+5%) |
| Matron's Scourge | [Matron Mother](../bosses/matron-mother.md) | 100% |
| Red Dragon Scale Mail | [Ancient Red Dragon](../bosses/ancient-red-dragon.md) | 100% |

- Every item also turns up in its rarity's loot table (Cloak of Protection and Ring of Protection only come from there), which mod structures, bosses, Mimics, bounties, raids, dungeons and vanilla structures roll on. See [Magic loot](../systems/magic-loot.md) for the rates.
- The named sources (per chest or per kill; chest and bounty items need no player kill, monsters do):

| Item | Where | Chance |
|-|-|-|
| Wand of Magic Missiles | [goblin camp](../structures/goblin-camps.md) chest; vanilla dungeon and mineshaft chests; minor bounty spoils (identified) | 3%; 2%; 5% |
| Bag of Holding | [Mimic](../mobs/mimic.md) (player kill); [dwarven](../structures/dwarven-fortresses.md) treasury | 10%; 3% |
| Decanter of Endless Water | [River Pikehorn](../mobs/dragons.md) (player kill); [hobbit village](../structures/hobbit-villages.md) harvest chests | 5%; 2% |
| Immovable Rod | dwarven forge; [beholder](../bosses/beholder.md) lair chest | 3%; 4% |
| Periapt of Wound Closure | dwarven barracks; major bounty spoils (identified) | 3%; 3% |
| Amulet of Health | dwarven treasury; vanilla stronghold library | 3%; 4% |
| Doss Lute | dwarven mead hall; major bounty spoils (identified) | 3%; 3% |
| Storm-Chaser Boots | [dragon lair](../structures/dragon-lairs.md) hoard | 6% |
| Frostbrand | [frost lair](../structures/frost-lairs.md) hoard | 8% |
| Mace of Disruption | vanilla stronghold library | 3% |
| The Burglar's Ring | goblin camp chest ("stolen from a hobbit"); hobbit village [goblin raid](../systems/goblin-raids.md) reward | 1%; 2% |
| Tome of Clear Thought | [Lich](../bosses/lich.md) (final death), Beholder (player kill); vanilla stronghold library | 10% each; 2% |
| Eye of the Tyrant | beholder lair chest | 2% |
| Piwafwi | [Drow Outpost](../structures/drow-outposts.md) shrine reliquary; [Drow Spire](../structures/drow-spire.md) treasure loft | 3%; 15% |

Item ids: `wand_of_magic_missiles`, `bag_of_holding`, `decanter_of_endless_water`, `immovable_rod`, `cloak_of_protection`, `periapt_of_wound_closure`, `ring_of_protection`, `amulet_of_health`, `doss_lute`, `tome_of_clear_thought`, `warlords_warhorn`, `heart_of_the_furnace`, `storm_chaser_boots`, `wyvern_scale_cloak`, `belt_of_owlbear_might`, `mace_of_disruption`, `frostbrand`, `phylactery_locket`, `burglars_ring`, `eye_of_the_tyrant`, `gorgons_gaze`, `matrons_scourge`, `piwafwi`, `red_dragon_scale_mail`.

## Commands
- `/dndmagic give|identify|attune|bonds ...`: see [Magic items](../systems/magic-items.md).
- Wand charges: `/data get entity @s SelectedItem.tag.dndclasses_magic.charges`; set them with `/item modify` or a give with NBT, e.g. `/give @s dndclasses:wand_of_magic_missiles{dndclasses_magic:{charges:1}}`.
- `/dndclass forceroll <player> 1` rigs the wand's crumble roll.
- Eye charges: the same `charges` key, e.g. `/give @s dndclasses:eye_of_the_tyrant{dndclasses_magic:{charges:1}}`. The Locket's recharge time is `dndclasses_magic.locketReadyAt` (world time); remove it to recharge.
- Drops: `/loot give @s kill @e[type=dndclasses:beholder,limit=1,sort=nearest]` rolls a table as a player kill.

## Configuration
- The Wand's long-rest recharge needs the `dndRests` gamerule; the dawn recharge always works.
- The Periapt's extra heal follows the `naturalRegeneration` gamerule.

## Known limitations
- The wand's three darts land as one 6-damage hit (Minecraft's damage cooldown would swallow three quick 2-damage hits).
- The Periapt doubles the slow natural heal only, not the fast saturation heal.
- If you release the Immovable Rod and fix it again on the same spot within 60 s, the first placement's timer can take the new rod away early.
- The textures are 16x16 placeholders (`tools/wondrous_item_textures.py`, `tools/boss_item_textures.py`). The Storm-Chaser Boots look like diamond boots when worn.
- The Burglar's Ring makes mobs that use the old target system forget you; brain-driven mobs (piglins, wardens) and the invisibility rules of vanilla still apply as usual.
- The Ring's day count follows the world's time of day, so `/time set` backwards doesn't give health back until the curse is broken.
- The Mace's "glow" is the enchantment glint, not real light.

## For developers
- `magic/items/`: one class per active item (`WandOfMagicMissilesItem`, `BagOfHoldingItem` with its `BagScreenHandler`, `DecanterOfEndlessWaterItem`, `ImmovableRodItem` + `ImmovableRodBlock`, `DossLuteItem`, `TomeOfClearThoughtItem`), `MagicItemUse.ready` (the shared "is the magic awake" check with its message) and `WondrousItems` (the tiers, the Ring/Amulet/Periapt effects, the long-rest and Downed hooks and the `magic/wondrous_items` ability contributor).
- Registration: `ModItems`, `ModBlocks.IMMOVABLE_ROD`, `ModItemGroup`; called from `Magic.register`.
- Charges live in `MagicData` (`charges`, with `MagicData.charges(stack, full)` for fresh items); the wand's dawn day is `dndclasses_magic.dawnDay`, the Decanter's cooldown `geyserReadyAt`, the Rod's placement `dndclasses_magic.rod {Pos, Dim, State}`.
- The Rod's 60 s return uses `ScheduledBlockRestore.schedule`.
- `InstrumentItem.playAsBard(user, amplifier, radius)` is the stronger song; `BardInstrumentSlot` and `BardSkills.animalFriends` call into `DossLuteItem`.
- `magic/items/BossItems`: the boss items' tiers and `MagicEffect`s (Heart, Boots, Cloak, Belt, Mace, Frostbrand, Ring), the Locket's `ALLOW_DEATH` listener (its own phase, ordered before the Downed rules), lightning immunity (`ALLOW_DAMAGE`), the Belt's `magic/boss_items` ability contributor, long-rest recharges, the Cloak glide and Ring invisibility server tick. `BossItems.immune` is called from `DragonSaves.isUnaffected` (lightning storm, frost breath); `BossItems.blocksEffect` from `mixin/MagicItemEffectImmunityMixin` (Freeze, anti-magic).
- `WarlordsWarhornItem`, `EyeOfTheTyrantItem`, `PhylacteryLocketItem`, `BurglarsRingItem` (puts the curse on every copy), `NamedWeaponItem` (Mace, Frostbrand), `DescribedItem` (passive items, with `DescribedItem.Armor` for the boots).
- `BossItemDrops`: one `LootTableEvents.MODIFY` pool per mob table (`dndclasses:entities/<mob>`), with `MagicItemLootFunction.unidentified()`. The Lich's table isn't edited, only extended here.
- `Curse.RINGS_CALL`: outside the random pool; the day clock is `dndRingsCallSince` in the player's persistent data, the health modifier `RINGS_CALL_UUID`.
- Goblins: `Conditions/ai/ConditionFleeGoal`, added to every path mob on load.
- Textures: `python3 tools/wondrous_item_textures.py` and `python3 tools/boss_item_textures.py`.
