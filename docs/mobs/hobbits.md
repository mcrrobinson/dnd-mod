# Hobbits
Small, peaceful halflings that live in [hobbit villages](../structures/hobbit-villages.md).

## How it works
- 14 HP, 0.32 speed. Six looks, and each hobbit gets a random Shire name (e.g. "Bilbo Baggins").
- Wanders up to 24 blocks around its home by day and keeps within 3 blocks of it at night. Keeps away from monsters.
- Always snacking (bread, apples, cookies, pie, stew...).
- **Gifts:** right-click one with an empty hand and it shares some of its food, once every 5 minutes.
- Drops 0-2 bread, 0-1 apple and 0-3 cookies (+Looting).

## Where to find it / How to get it
- Only inside hobbit villages, which slowly top up their population (at most 16 hobbits within 48 blocks). There's also a spawn egg.

## For developers
- `entity/HobbitEntity`, `client/renderer/HobbitRenderer`. Loot: `loot_tables/entities/hobbit.json`.
- Devscripts: `hobbit-locate.txt`, `hobbit-village-tour.txt`.
