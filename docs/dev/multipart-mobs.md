# Multipart mobs
How big GeckoLib mobs (the dragons) get extra hittable parts beyond their small entity hitbox, like the vanilla ender dragon.

## How it works
- A Wyvern model is about 12 blocks wide but its hitbox is 1.5. Its wings, neck, head, tail and legs are separate `DragonPart` entities that pass damage to the owner.
- **`MultipartDragon`**: the interface a mob implements (`getParts()`, `getPartLayout()`).
- **`DragonPartLayout`**: lists the model's bone groups. `part(name, bones...)` adds one part; `pair(name, leftBones...)` adds a left part and its mirrored `_right` twin. The constructor takes the geo file name and how far the root bone drops in the ground and flight animations.
- **`DragonRenderer`**: on the client, it fits each part to its bones' cubes as they're animated every frame. The server has no animations, so it uses the rest pose from the `.geo.json`, shifted by the root bone drop.
- **Hit detection:** `WorldMixin` adds parts to `getOtherEntities`, `ServerWorldMixin` resolves part ids from attack packets, and `ProjectileUtilMixin` tests the small shape a ray actually hits instead of the part's mostly-empty box. A dragon's own projectiles skip its parts.

## Adding one
1. Implement `MultipartDragon` and build a static `DragonPartLayout` (see `WyvernEntity.PART_LAYOUT`).
2. In the constructor, call `parts = LAYOUT.createParts(this)` and `setId(DragonPartLayout.reserveIds(parts))`. Override `setId` to call `DragonPartLayout.assignIds`.
3. Call `LAYOUT.update(this, parts, flying)` every tick.
4. Render it with a `DragonRenderer` (or a subclass).
5. Check it with `devscripts/dragon-hitboxes.txt` (`hitboxes on` draws the part shapes as green boxes).

## Known limitations
- GeckoLib 4.2 adds an extra identity matrix in `GeoBone.getLocalSpaceMatrix()` / `getWorldSpaceMatrix()`; `DragonRenderer` subtracts it.

## For developers
- `entity/MultipartDragon`, `DragonPart`, `DragonPartLayout`, `DragonPartTracker`, `client/renderer/DragonRenderer`, `mixin/WorldMixin`, `ServerWorldMixin`, `ProjectileUtilMixin`.
