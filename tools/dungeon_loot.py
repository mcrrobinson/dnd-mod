#!/usr/bin/env python3
"""Generates the dungeon loot tables (docs/systems/dungeon-loot.md).

Writes into src/main/resources/data/dndclasses/loot_tables/:
  chests/dungeon/<theme>_<room>_t<tier>.json   room chests (encounter, secret, side_vault) per theme and tier
  chests/dungeon/vault_t<tier>.json            the Hoard Coffer's first roll per player
  chests/dungeon/vault_salvage.json            the Hoard Coffer's roll after a repopulation
  gameplay/dungeon_elite_t<tier>.json          the extra roll a Veteran drops

Chests carry their tier in the table name, so vanilla opening works and Lockpicking.dcFor can read
the DC from it. Run from the repo root: python3 tools/dungeon_loot.py
"""
import json
import os

OUT = os.path.join("src", "main", "resources", "data", "dndclasses", "loot_tables")
TIERS = (1, 2, 3, 4)
THEMES = ("crypt", "warren", "ruin")
# Vault magic item by tier (design 2.2): [(rarity, weight)]
VAULT_RARITY = {
    1: [("uncommon", 1)],
    2: [("uncommon", 70), ("rare", 30)],
    3: [("rare", 70), ("very_rare", 30)],
    4: [("very_rare", 90), ("legendary", 10)],
}
BOOK_LEVELS = {1: 20, 2: 25, 3: 30, 4: 30}


def count(lo, hi):
    if lo == hi:
        return {"function": "minecraft:set_count", "count": lo}
    return {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}


def item(name, weight, lo=1, hi=1, extra=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    funcs = []
    if hi > 1:
        funcs.append(count(lo, hi))
    if extra:
        funcs.extend(extra)
    if funcs:
        e["functions"] = funcs
    return e


def potion(pid, weight):
    return item("minecraft:potion", weight, extra=[{"function": "minecraft:set_potion", "id": pid}])


def book(tier, weight):
    return item("minecraft:book", weight, extra=[
        {"function": "minecraft:enchant_with_levels", "levels": BOOK_LEVELS[tier], "treasure": True}])


def ref(table, weight=1):
    return {"type": "minecraft:loot_table", "name": table, "weight": weight}


def empty(weight):
    return {"type": "minecraft:empty", "weight": weight}


def rolls(lo, hi):
    return lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}


def magic_pool(rarities, chance=None):
    pool = {"rolls": 1, "entries": [ref("dndclasses:magic/" + r, w) for r, w in rarities]}
    if chance is not None:
        pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
    return pool


def table(kind, pools):
    return {"type": kind, "pools": pools}


def scale(t, base):
    """Stack sizes grow with the tier: base at I, +50% per tier."""
    return max(1, round(base * (1 + 0.5 * (t - 1))))


# Theme flavour for room chests: [(item, weight, min, max)]
FLAVOUR = {
    "crypt": [("minecraft:bone", 12, 2, 6), ("minecraft:rotten_flesh", 8, 2, 5), ("minecraft:candle", 6, 1, 3),
              ("minecraft:soul_torch", 5, 2, 6), ("minecraft:name_tag", 2, 1, 1)],
    "warren": [("minecraft:leather", 10, 2, 5), ("minecraft:string", 8, 2, 6), ("minecraft:cooked_mutton", 8, 2, 5),
               ("minecraft:rabbit_hide", 6, 1, 4), ("minecraft:saddle", 2, 1, 1)],
    "ruin": [("minecraft:raw_iron", 10, 2, 6), ("minecraft:coal", 8, 3, 8), ("minecraft:lantern", 5, 1, 2),
             ("minecraft:iron_nugget", 8, 4, 12), ("minecraft:raw_gold", 4, 1, 4)],
}


def flavour(theme):
    return [item(n, w, lo, hi) for n, w, lo, hi in FLAVOUR[theme]]


def encounter(theme, t):
    """Supplies along the way: food, arrows, some coin."""
    entries = flavour(theme) + [
        item("minecraft:bread", 10, 1, 4),
        item("minecraft:arrow", 10, 4, scale(t, 10)),
        item("minecraft:torch", 8, 2, 8),
        item("minecraft:iron_ingot", 8, 1, scale(t, 3)),
        item("minecraft:gold_nugget", 8, 2, scale(t, 8)),
        item("minecraft:emerald", 5, 1, scale(t, 2)),
        potion("minecraft:healing", 4 + t),
    ]
    if t >= 2:
        entries.append(book(t, 2))
    if t >= 3:
        entries.append(item("minecraft:diamond", 2, 1, 2))
    return table("minecraft:chest", [{"rolls": rolls(3, 5), "entries": entries}])


def treasure_entries(t):
    """Coin, gems and tier enchantments: the core of the better chests and the vault."""
    entries = [
        item("minecraft:gold_ingot", 20, 2, scale(t, 5)),
        item("minecraft:emerald", 15, 2, scale(t, 4)),
        item("minecraft:diamond", 6 + 3 * t, 1, scale(t, 2)),
        book(t, 10),
        potion("minecraft:strong_healing" if t >= 3 else "minecraft:healing", 8),
        potion("minecraft:fire_resistance" if t >= 2 else "minecraft:regeneration", 4),
        item("minecraft:experience_bottle", 6, 2, scale(t, 4)),
        item("minecraft:golden_apple", 2 + t, 1, 1),
    ]
    if t >= 4:
        entries.append(item("minecraft:enchanted_golden_apple", 1))
        entries.append(item("minecraft:netherite_scrap", 2, 1, 2))
    return entries


def secret(theme, t):
    """A hidden room's chest: tier treasure, with a little theme flavour."""
    return table("minecraft:chest", [
        {"rolls": rolls(4, 6), "entries": treasure_entries(t) + flavour(theme)},
        # A small chance of a magic item of the vault's lowest rarity for the tier
        magic_pool(VAULT_RARITY[t][:1], 0.1 + 0.05 * t),
    ])


def side_vault(theme, t):
    """The class-gated side vault: one tier richer than the secret room, half the time a magic item a step up."""
    up = min(t + 1, 4)
    return table("minecraft:chest", [
        {"rolls": rolls(4, 6), "entries": treasure_entries(up) + flavour(theme)},
        magic_pool(VAULT_RARITY[up], 0.5),
    ])


def vault(t):
    """The Hoard Coffer's first roll per player: 4-6 treasure rolls and one guaranteed magic item."""
    return table("minecraft:chest", [
        {"rolls": rolls(4, 6), "entries": treasure_entries(t)},
        magic_pool(VAULT_RARITY[t]),
    ])


def tier_condition(lo, hi=4):
    return [{"condition": "dndclasses:dungeon_tier", "min": lo, "max": hi}]


def salvage():
    """A repeat clear's coffer roll: smaller, scaled by the dungeon's tier through dndclasses:dungeon_tier."""
    pools = [
        {"rolls": rolls(2, 3), "entries": [
            item("minecraft:gold_ingot", 20, 1, 4),
            item("minecraft:emerald", 15, 1, 3),
            item("minecraft:iron_ingot", 15, 2, 6),
            item("minecraft:arrow", 10, 4, 12),
            item("minecraft:experience_bottle", 8, 1, 3),
            potion("minecraft:healing", 8),
        ]},
        {"rolls": 1, "conditions": tier_condition(2), "entries": [
            item("minecraft:diamond", 3, 1, 2), book(2, 2), empty(5)]},
        {"rolls": 1, "conditions": tier_condition(3) + [{"condition": "minecraft:random_chance", "chance": 0.25}],
         "entries": [ref("dndclasses:magic/uncommon")]},
    ]
    return table("minecraft:chest", pools)


def elite(t):
    """One extra roll from a Veteran, on top of its normal drops."""
    entries = [
        item("minecraft:gold_nugget", 20, 3, scale(t, 8)),
        item("minecraft:emerald", 10, 1, scale(t, 2)),
        item("minecraft:arrow", 10, 3, 8),
        item("minecraft:iron_ingot", 10, 1, 3),
        potion("minecraft:healing", 6),
        item("minecraft:gold_ingot", 4 + 2 * t, 1, 2),
    ]
    if t >= 2:
        entries.append(item("minecraft:diamond", 2 * (t - 1), 1, 1))
    pools = [{"rolls": 1, "entries": entries}]
    if t >= 3:
        pools.append(magic_pool([("uncommon", 1)], 0.02 * (t - 2)))
    return table("minecraft:chest", pools)


def write(path, data):
    full = os.path.join(OUT, path + ".json")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for t in TIERS:
        for theme in THEMES:
            write(f"chests/dungeon/{theme}_encounter_t{t}", encounter(theme, t))
            write(f"chests/dungeon/{theme}_secret_t{t}", secret(theme, t))
            write(f"chests/dungeon/{theme}_side_vault_t{t}", side_vault(theme, t))
        write(f"chests/dungeon/vault_t{t}", vault(t))
        write(f"gameplay/dungeon_elite_t{t}", elite(t))
    write("chests/dungeon/vault_salvage", salvage())


if __name__ == "__main__":
    main()
