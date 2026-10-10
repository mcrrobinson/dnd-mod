#!/usr/bin/env python3
"""Generates the magic item tier tables (docs/systems/magic-loot.md).

Writes src/main/resources/data/dndclasses/loot_tables/magic/<tier>.json for uncommon, rare, very_rare and
legendary. Each table rolls one item: +N vanilla gear (the bulk, so named items stay special) or one of the
tier's named magic items. One `dndclasses:magic_item` function sets the tier (gear gets the tier's +N), leaves
it unidentified and rolls the tier's curse chance. Where the tables are used is in magic/MagicLoot.java
(and the dungeon tables from tools/dungeon_loot.py). Run from the repo root: python3 tools/magic_tier_tables.py
"""
import json
import os

OUT = os.path.join("src", "main", "resources", "data", "dndclasses", "loot_tables", "magic")

IRON = ["iron_sword", "iron_axe", "iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots"]
DIAMOND = ["diamond_sword", "diamond_axe", "diamond_helmet", "diamond_chestplate", "diamond_leggings",
           "diamond_boots"]
NETHERITE = ["netherite_sword", "netherite_axe", "netherite_helmet", "netherite_chestplate",
             "netherite_leggings", "netherite_boots"]


def gear(names, weapon_weight, armor_weight):
    return [(n, weapon_weight if n.endswith(("sword", "axe")) else armor_weight) for n in names]


# (item, weight). Gear ids are minecraft:, named ids dndclasses:.
TABLES = {
    "uncommon": {
        "gear": gear(IRON, 8, 5) + gear(DIAMOND, 3, 2) + [("bow", 6), ("crossbow", 4), ("trident", 2)],
        # Cloak of Protection is the Uncommon pool's own item; the rest also have their own sources
        "named": [("cloak_of_protection", 6), ("wand_of_magic_missiles", 2), ("immovable_rod", 1),
                  ("bag_of_holding", 1), ("decanter_of_endless_water", 1), ("periapt_of_wound_closure", 1)],
    },
    "rare": {
        "gear": gear(IRON, 4, 3) + gear(DIAMOND, 6, 4) + [("bow", 6), ("crossbow", 4), ("trident", 2)],
        # Ring of Protection is the Rare pool's own item; the staffs replace the old chests/staff_of_* tables
        "named": [("ring_of_protection", 6), ("staff_of_ice", 2), ("staff_of_fire", 2),
                  ("staff_of_lightning", 2), ("amulet_of_health", 1), ("doss_lute", 1),
                  ("warlords_warhorn", 1), ("heart_of_the_furnace", 1), ("storm_chaser_boots", 1),
                  ("wyvern_scale_cloak", 1), ("belt_of_owlbear_might", 1), ("mace_of_disruption", 1)],
    },
    "very_rare": {
        "gear": gear(DIAMOND, 6, 4) + [("bow", 6), ("crossbow", 4), ("trident", 2)],
        "named": [("frostbrand", 2), ("tome_of_clear_thought", 2), ("phylactery_locket", 1),
                  ("burglars_ring", 1)],
    },
    "legendary": {
        "gear": gear(NETHERITE, 6, 4) + [("bow", 4), ("crossbow", 3), ("trident", 3)],
        "named": [("eye_of_the_tyrant", 6)],
    },
}


def table(tier, spec):
    entries = [{"type": "minecraft:item", "name": "minecraft:" + n, "weight": w} for n, w in spec["gear"]]
    entries += [{"type": "minecraft:item", "name": "dndclasses:" + n, "weight": w} for n, w in spec["named"]]
    return {
        "type": "minecraft:chest",
        "pools": [{
            "rolls": 1,
            "entries": entries,
            "functions": [{"function": "dndclasses:magic_item", "tier": tier}],
        }],
    }


def main():
    os.makedirs(OUT, exist_ok=True)
    for tier, spec in TABLES.items():
        total = sum(w for _, w in spec["gear"] + spec["named"])
        named = sum(w for _, w in spec["named"])
        with open(os.path.join(OUT, tier + ".json"), "w") as f:
            json.dump(table(tier, spec), f, indent=2)
            f.write("\n")
        print(f"{tier}: {len(spec['gear']) + len(spec['named'])} entries, named {named}/{total}"
              f" = {100 * named / total:.0f}%")


if __name__ == "__main__":
    main()
