# Multipart mobs
How big GeckoLib mobs (the dragons) get extra hittable parts beyond their small entity hitbox, like the vanilla ender dragon.

![A frozen Wyvern with hitboxes on (F3+B): each green box is one of its parts](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/multipart-wyvern-hitboxes.png)

## How it works
- A Wyvern model is about 12 blocks wide but its hitbox is 1.5. Its wings, neck, head, tail and legs are separate `DragonPart` entities that pass damage to the owner.
- **`MultipartDragon`**: the interface a mob implements (`getParts()`, `getPartLayout()`).
- **`DragonPartLayout`**: lists the model's bone groups. `part(name, bones...)` adds one part; `pair(name, leftBones...)` adds a left part and its mirrored `_right` twin. The constructor takes the geo file name and how far the root bone drops in the ground and flight animations.
- **`DragonRenderer`**: on the client, it fits each part to its bones' cubes as they're animated, at most twice per tick (it reuses its matrix and vectors).
- **Server (and unrendered client dragons):** the server runs no GeckoLib animations, so `DragonPartLayout` poses the parts itself.
  - With `.animations(names...)` (Wyvern, Ember Wyvern, Lightning Chaser, Frost Drake) it reads those animations from the `.animation.json` (`DragonAnimations`) and evaluates them like GeckoLib 4.2 does: linear keyframes, pre/post values, rotations in radians with x and y flipped, and Molang (`query.anim_time`, `math.sin/cos/max`...) at the animation time. It uses its own small Molang evaluator, because GeckoLib's `MolangParser` is a global the render thread writes to.
  - **Which animation:** the server picks idle / walk / fly.idle / fly.straight (`DragonFlightAnimation`) and syncs it in tracked data. The client plays the same one.
  - **When in the animation:** both sides use the world clock. The client's body controller is a `DragonAnimationController`, whose animation time is (world time + partial tick) mod length, and the server uses world time mod length, so the wing beat lines up. Measured with the dev overlay on a hovering dragon, the part centres are 0.05-0.12 blocks apart on average (worst wing finger 0.2-0.5 blocks, from the partial tick and the renderer's half-tick refit).
  - Without animations (River Pikehorn, Beholder) parts use the rest pose, shifted by the root bone drop.
- **Posing:** `DragonPartLayout` reads each cube's origin, size and inflate, and applies each bone's animated offset, rotation (rest + animated) and scale about its pivot (parents first), then the cube's own rotation, in GeckoLib's `prepMatrixForBone` order. Each pose is computed once per animation time and shared by every dragon of that model. Each tick a part keeps its shapes if the pose and facing are unchanged, shifts them if the dragon only moved, and rebuilds them otherwise.
- **Hit detection:** `WorldMixin` adds parts to `getOtherEntities` (skipping dragons whose `DragonPartLayout.getReach()` can't touch the query box), `ServerWorldMixin` resolves part ids from attack packets, and `ProjectileUtilMixin` tests the small shape a ray actually hits instead of the part's mostly-empty box. A dragon's own projectiles skip its parts.

![With `serverhitboxes on`, the server's part shapes (red) are drawn next to the client's (green)](https://raw.githubusercontent.com/mcrrobinson/dnd-mod/pr-screenshots/docs-screenshots/multipart-wyvern-server-hitboxes.png)

## Adding one
1. Implement `MultipartDragon` and build a static `DragonPartLayout` (see `WyvernEntity.PART_LAYOUT`).
2. In the constructor, call `parts = LAYOUT.createParts(this)` and `setId(DragonPartLayout.reserveIds(parts))`. Override `setId` to call `DragonPartLayout.assignIds`.
3. Call `LAYOUT.update(this, parts, flying)` every tick.
4. Render it with a `DragonRenderer` (or a subclass).
5. Check it with `devscripts/dragon-hitboxes.txt` (`hitboxes on` draws the part shapes as green boxes). `devscripts/dragon-hitboxes-server.txt` adds the server's shapes in red and logs the gap (`[ServerPartDebug]`); `dragon-hitboxes-hold.txt` (with `-PdevHidden=false`) leaves two flapping dragons up for you to fly round.
6. To have server parts follow the animations, add `.animations(...)` to the layout and drive the body animation with a `DragonAnimationController` (see `DragonFlightAnimation`).

## Known limitations
- A moving dragon's server shapes are where the server has it. The client draws it up to ~3 ticks behind (vanilla entity interpolation) and its body yaw can trail during turns. Measured on wandering dragons, that's 0.4-0.8 blocks on average, more in sharp turns. Vanilla mobs have the same lag.
- The fire-breath head animation (a second controller) and any other overlays aren't followed on the server; only the body animation is.
- GeckoLib 4.2 adds an extra identity matrix in `GeoBone.getLocalSpaceMatrix()` / `getWorldSpaceMatrix()`; `DragonRenderer` subtracts it.

## For developers
- `entity/MultipartDragon`, `DragonPart`, `DragonPartLayout`, `DragonAnimations`, `DragonAnimationController`, `DragonFlightAnimation`, `DragonPartTracker`, `client/renderer/ServerPartDebug`, `client/renderer/DragonRenderer`, `mixin/WorldMixin`, `ServerWorldMixin`, `ProjectileUtilMixin`.
