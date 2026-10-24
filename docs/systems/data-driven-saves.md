# Data-Driven Saves
Saving throws against status effects and damage types from any mob, vanilla included. They're set up in datapack JSON, so server owners can add, change or switch off saves without code. By default a cave spider's or witch's poison, wither from wither skeletons, the Wither and the Lich, the Warden's sonic boom and the Ender Dragon's breath cloud all allow a CON save. The roll shows on the [save lane](saving-throws.md).

## How it works
| Rule (file) | Against | Save | DC | On a success |
|-|-|-|-|-|
| `poison` | Poison from a hostile mob | CON | cave spider 11, witch 12, any other hostile mob 12 | Half the duration |
| `wither` | Wither from a hostile mob | CON | wither skeleton 13, Lich 15, Wither 15, any other hostile mob 12 | Half the duration |
| `sonic_boom` | `minecraft:sonic_boom` damage (Warden) | CON | 15 | Half damage |
| `dragon_breath` | `minecraft:indirect_magic` damage from the Ender Dragon (its breath cloud) | CON | 14 | Half damage |

- **Players only.** Pets, villagers and mobs don't make these saves. Creative and spectator players don't roll.
- **One roll per exposure.** A source gets one roll per rule for 40 ticks: a cave spider biting twice in a second is one save. Damage from a cloud or projectile is keyed by that entity, and the dragon breath rule's window is 600 ticks, so standing in one breath cloud is one save however many times it hurts you.
- **No double saves.** If you already made a save this tick (a trap's poison needle or a dragon's breath rolled its own), the generic rule doesn't roll again.
- **No spam.** There's no roll when the effect wouldn't change anything (you already have it as strong and as long).
- **Never rolls for:**
  - effects with no source, such as a potion you drink yourself or a trap's poison;
  - effects or damage from yourself or another player;
  - poison or wither from a non-hostile source (`hostile_source_only`);
  - effects you're immune to (`SaveRules.immune`, public so the Conditions follow-up can reuse it): Rogue (poison), Wizard (Freeze), the Alchemist's Iron Stomach (poison, wither, nausea), the Devotion Paladin's Purity of Spirit, magic items (Frostbrand, Eye of the Tyrant), potion immunities (Fighter, Artificer, Paladin) and anything the player can't get at all (Draconic Scale).
- A success halves the effect's duration (amplifier unchanged) or the damage. The save comes before armour and the Paladin's aura.
- The tags (`poison`, `wither`, `necrotic`, `thunder`, `breath`) feed advantage filters on the [character sheet](ability-scores.md), such as a Dwarf's advantage against poison.
- [Creature study](creature-study.md) lists the rules that name a creature in `dc_by_source`: studying a cave spider shows "Poison: CON save DC 11, half duration".

## Where to find it
Anywhere: it applies to vanilla and mod mobs alike.

## Commands
- `/dndclass forceroll <player> <n...>` rigs the next d20s for testing. With `forceroll @s 20`, a cave spider bite gives half the normal poison duration; check with `/data get entity @s active_effects`.
- `/reload` reloads the rules after a datapack changes.

## Configuration
Rules live in `data/<namespace>/saving_throws/*.json`. The defaults are in `data/dndclasses/saving_throws/`.

```json
{"effect": "minecraft:poison", "ability": "CON", "default_dc": 12,
 "dc_by_source": {"minecraft:cave_spider": 11, "minecraft:witch": 12},
 "on_success": "half_duration", "hostile_source_only": true,
 "label": "save.dndclasses.poison", "tags": ["poison"]}
```

| Field | Meaning |
|-|-|
| `effect` / `damage_type` | What the rule saves against (exactly one) |
| `ability` | `STR`, `DEX`, `CON`, `INT`, `WIS`, `CHA` (or the full name) |
| `dc` / `default_dc` | DC for sources not in `dc_by_source`. Leave it out to roll only against the listed sources |
| `dc_by_source` | Entity type id to DC. The source is the attacker: a projectile's or cloud's owner |
| `on_success` | `half_duration` (effects, the default for them), `half` (damage, the default for it) or `negate` (either) |
| `hostile_source_only` | Only roll when the source is a hostile mob |
| `label` | Translation key shown on the save lane (default: the effect's name, or "Damage") |
| `tags` | Advantage-filter keywords |
| `window` | Ticks one roll covers per source (default 40) |
| `condition` | Effect rules: a [condition](conditions.md) id the effect also gives for its final (possibly halved) length, from the same source. The default `poison.json` sets `"poisoned"` |
| `enabled` | `false` switches the rule off |

To switch a default off, add a datapack with a file of the same name (for example `data/dndclasses/saving_throws/poison.json`) containing `{"enabled": false}`. A file with errors is skipped and logged as `[SaveRules] Skipping saving throw ...`.

## Known limitations
- Rolls are checked per tick: if a scripted save and an unrelated rule-matching effect hit the same player in the same tick, the rule doesn't roll.
- Damage rules need a source entity for `dc_by_source`. Environmental damage with no attacker only matches a rule with a `dc`.
- The Rogue's poison immunity (`LivingEntityMixin`) hooks only the one-argument `addStatusEffect`, so mob poison (which uses the two-argument overload) may still land on Rogues. This rule skips the save for Rogues either way. Fixing the immunity itself is out of scope here.

## For developers
- `classes/SkillChecks/SaveRules.java`: the reload listener (`SimpleSynchronousResourceReloadListener`, registered from `DnDClasses`), parsing, `onEffect` and `onDamage`, and a `MobSaveInfo` provider for the Study card.
- `classes/mixin/SavingThrowMixin.java`:
  - `ModifyVariable` on `amount` at the head of `LivingEntity.damage`, with the source captured via MixinExtras `@Local`.
  - `Inject` at the head of `addStatusEffect(StatusEffectInstance, Entity)`. A halved effect is re-added through `SaveRules.reapply` under a thread-local guard, so it isn't saved against twice.
- `SavingThrow.savedThisTick(target)`: true when the target made any save (fresh or cached) this tick. `SaveRules` uses it to avoid double saves.
- `MobSaveInfo.registerProvider(Provider)`: reloadable entries, listed after the registered ones.
- Logs: `[SaveRules] Loaded N data-driven saving throws` on each reload, plus the usual `[D20]` roll lines.
