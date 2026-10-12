# Lich
A late-game undead caster boss that haunts stronghold libraries. It keeps its distance, casts wither and frost spells and raises the dead. Its soul is kept in a **phylactery**: while that stands, killing the Lich only sends it back to reform, so smash the phylactery first. Necromancers are its sworn rivals.

![A Lich with its phylactery beside it](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/lich-phylactery.png)

## How it works
**Stats**: 300 health, 10 armour, 4 toughness, 80% knockback resistance. It is undead (Smite works, healing potions hurt it), immune to wither, poison and freezing, and never despawns. It stays 6-14 blocks from its target, backing off when you get closer and closing in when you're further away or out of sight.

**Spells** (each has a short wind-up with particles and an animation):

| Spell | Effect | Cooldown |
|-|-|-|
| Wither Bolt | A wither skull from its staff. The blast hurts but never breaks blocks. Bolts aren't saved, so one in flight when the chunk unloads is gone rather than coming back as a vanilla skull | 2.5 s |
| Ray of Frost | A beam (up to 20 blocks, needs line of sight): 7 damage, Slowness III for 5 s, powder-snow freeze | 4.5 s |
| Frost Nova | When anyone is within 4 blocks: 8 damage, Slowness IV for 6 s, freezing and a strong shove to everyone within 5 blocks. **CON save** DC 15 (16 below 30%): half damage (4) and Slowness IV for 3 s; the shove still lands | 7 s |
| Raise Dead | A wave of undead around it: zombies, husks and skeletons | 20 s |

The Lich, its phylactery and its minions never hurt each other. Its minions crumble when it dies. Like an evoker's vexes, they drop no loot, equipment or XP and give no class XP or bounty progress (tag `dndclasses.boss_minion`), so a Lich can't be kept alive as a wither skeleton skull farm.

**Dread Presence**: as it passes 60% and 30% health, and when it reforms, a wave of dread rolls out (a chat line, a ring of souls and the elder guardian's curse). Every player within 24 blocks (not creative or spectator) makes a **WIS save**, DC 15 (16 below 30%), or is **Frightened** for 8 s: your attacks deal half damage. Necromancers, its rivals, have advantage on the save. It's one save per player per wave, even when one big hit passes both thresholds at once.

**Saving throws** show on the [save lane](../systems/saving-throws.md) beside the crosshair, and studying the Lich (V) lists them. The Wither Bolt's wither allows a **CON save** DC 15 for half the duration, from the [data-driven](../systems/data-driven-saves.md) `wither` rule.

**Phases** (boss bar visible within 48 blocks):
- **Start** (purple bar): waves of 2, at most 4 minions at once.
- **Below 60%** (blue bar): Dread Presence, and raises a wave immediately. Waves of 3 every 15 s (at most 6 minions), which can include strays and wither skeletons.
- **Below 30%** (red bar): Dread Presence (DC 16), its saves become DC 16, 25% faster, 40% shorter cooldowns, waves of 4 (at most 8 minions), three bolts at a time (the middle one a blue charged skull), an immediate nova, and a 40% chance to teleport 6-10 blocks away when hit in melee (at most once every 3 s).

**Phylactery** (60 health, 8 armour, can't be pushed, immune to status effects):
- On its first tick, the Lich places its phylactery 4-7 blocks away, or at a set spot in strongholds.
- While the phylactery stands, the Lich heals 1 health a second and a thread of soul fire links them.
- If the Lich dies with the phylactery intact, its soul flees into it: no loot, XP or advancement. The phylactery spins faster and the Lich **reforms 30 s later** next to it at full health, unleashes its Dread Presence and attacks the nearest player within 24 blocks. This repeats without limit.
- Hitting the phylactery makes the Lich target you. At most once every 30 s it shouts, and it teleports back if it's more than 8 blocks away. A Lich lured more than 28 blocks from its phylactery teleports home.
- Smash the phylactery and the Lich's next death is final.
- Smash the phylactery while the Lich is reforming and the Lich is destroyed: the phylactery drops the Lich's loot and XP and grants Lichbane.

**Necromancer rivalry**:
- The Lich targets Necromancers in sight before other players and taunts them by name.
- Its raised dead ignore the truce undead normally keep with Necromancers.
- Necromancers deal **+50% damage** to the Lich and its phylactery.
- A Necromancer who lands the killing blow gets a special parting line.

**Dialogue**: the Lich speaks in chat (purple, to players within 48 blocks) on greeting, phase changes, fleeing, reforming, defending or losing its phylactery, and dying.

**Music**: the Lich theme (120 bpm, D minor) loops for everyone who can see the boss bar.

**Rewards**:
- **Lich** (`entities/lich`): always 4-10 bones and 3-8 bottles o' enchanting. When killed by a player, also 150 XP, 3-6 diamonds, 2 treasure-enchanted books (level 30), 50% totem of undying, 35% wither skeleton skull and 25% 1-2 netherite scrap (+ Looting), plus the **Lichbane** advancement (challenge, +150 XP). Also, when killed by a player, 25% a [Mace of Disruption](../items/magic-items.md#mace-of-disruption) and 20% a [Phylactery Locket](../items/magic-items.md#phylactery-locket) (unidentified; added to `entities/lich` by `BossItemDrops`), one Very Rare (80%) or Legendary (20%) magic item and 10% a Tome of Clear Thought (unidentified; added by `MagicLoot`, see [Magic loot](../systems/magic-loot.md)). The Lich only drops this table on its final death, from the smashed phylactery.
- **Phylactery** (`entities/phylactery`): 2-5 amethyst shards, plus a 25% chance of an echo shard when a player smashes it.

## Where to find it
- **Strongholds**: every stronghold library. A library generated on Peaceful gets an invisible marker instead, which raises the Lich the first time it's loaded on a higher difficulty. The Lich stands in the open row at the back, facing the entrance; its phylactery goes in the open middle row. Find one with `/locate structure minecraft:stronghold`. A stronghold can have up to two libraries, so it can have two Liches.
- **The library's Desecrated Altar**: libraries generated since the [obstacle](../systems/obstacles.md) placements also have a Desecrated Altar (Hard, DC 15) in the open middle row, three blocks from the phylactery spot. A Cleric consecrates it with Religion: for 10 minutes spawners within 12 blocks stop and, while it stays consecrated, every wave of dead the Lich raises within 24 blocks of it rises at **half health**. Then the altar desecrates itself again.
- **Crypt dungeons**: the boss of every [Crypt](../structures/dungeons.md#the-crypt), called up when the party walks into the boss room, with its health scaled to the party. Its phylactery sits on the altar of the reliquary off the boss room, and it keeps to the boss room instead (it blinks back when more than about 33 blocks from the dais, which still reaches the reliquary). See [Dungeon encounters](../systems/dungeon-encounters.md#boss).
- **Lich Spawn Egg**: in the mod's creative tab.

## Commands
- `/summon dndclasses:lich`: a Lich that places its own phylactery.
- `/summon dndclasses:lich ~ ~ ~ {NoPhylactery:1b}`: a Lich with no phylactery, which dies for good.
- `/data merge entity @e[type=dndclasses:phylactery,limit=1] {ReformTicks:20}`: speeds up a reform (testing).

## Configuration
No config options. `doMobLoot` controls loot as usual. On Peaceful, Liches and phylacteries despawn, and libraries generated then get their Lich later (see above).

## Known limitations
- If the Lich dies while its phylactery's chunk is unloaded, the death is final. The home radius and teleport-home make this unlikely.
- Models and textures are generated placeholder art.
- The library spawn spots come from vanilla's library layout.

## For developers
- `entity/LichEntity.java`: spells, the caster movement goal, phases (`BossFight`), the phylactery link, rivalry, dialogue, and `registerEvents()` (no friendly fire, via `ALLOW_DAMAGE`).
- `entity/BossSaves.java`: the Frost Nova and Dread Presence `MobSaveInfo` entries, `BOSS_DC` / `LAST_PHASE_DC`, `DREAD_RADIUS` (24) and `DREAD_FRIGHTENED_TICKS` (160). `LichEntity.castFrostNova` rolls one save per nova (exposure tag `nova`); `LichEntity.dreadPresence()` runs from both phase callbacks and from `PhylacteryEntity.reformLich`, at most once per tick, with exposure tag `dread`, the `fear` tag and Necromancer advantage. `isLastPhase()` is phase 2 (below 30%).
- `entity/PhylacteryEntity.java`: the reform timer, defending, and final-death rewards. Uses `BossFight.grantAdvancement`.
- `classes/mixin/StrongholdLibraryMixin.java`: library placement. `ActiveTargetGoalMixin`: the minion exemption from the Necromancer truce (`LichEntity.defiesNecromancers`).
- Assets: `tools/lich_models.py` generates `geo/entity/{lich,phylactery}.geo.json` and the textures and glow masks. Animations are in `animations/entity/`. The music is `loop_lich_fight` in `tools/music-gen/music_gen.py` (sound event `music.lich_fight`).
- Data: `loot_tables/entities/{lich,phylactery}.json`, `advancements/lichbane.json`.
- Devscript: `devscripts/lich.txt` covers phylactery spawning, flee and reform, final death, and a survival fight with spells and minions. `devscripts/boss-saves.txt` rigs the Frost Nova and Dread Presence saves with `/dndclass forceroll`.
