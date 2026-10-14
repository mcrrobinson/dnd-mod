package mattonfire.dnd.classes;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;

/**
 * Server and shared event handlers for the class rules, registered in this order from
 * {@link DnDClasses#onInitialize}: the Necromancer's wither on melee, the brewing-stand death hint, mana
 * regeneration, the Blood Hunter's burning sword, the item-use bans (bows for Fighters, drinking potions
 * for Paladins, Fighters and Artificers) and the Lunge enchantment.
 */
public final class ClassEvents {
        private ClassEvents() {
        }

        public static void register() {
                AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
                        if (player instanceof PlayerEntityExt) {
                                PlayerEntityExt playerEntityExt = (PlayerEntityExt) player;
                                DndCharacter dndCharacter = playerEntityExt.getDndClass();
                                if (dndCharacter == DndCharacter.NECROMANCER) {
                                        // Apply wither to the enemy
                                        if (entity instanceof LivingEntity) {
                                                LivingEntity livingEntity = (LivingEntity) entity;
                                                livingEntity.addStatusEffect(new StatusEffectInstance(
                                                                StatusEffects.WITHER,
                                                                5,
                                                                0));
                                        }
                                }
                        }
                        return ActionResult.PASS;
                });

                ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
                        if (damageSource.getTypeRegistryEntry().getKey()
                                        .orElse(null) == ModDamageTypes.BREWING_STAND_DAMAGE_SOURCE) {
                                DnDClasses.respawnMessage.put(entity.getUuidAsString(),
                                                "Maybe get a alchamist to brew next time...");

                        }
                });

                // Warlock fire breath ticks in WarlockRules.register().

                ServerTickEvents.END_WORLD_TICK.register(serverWorld -> {
                        {
                                long currentTick = serverWorld.getServer().getTicks();

                                if (currentTick % DnDClasses.MANA_TICKS_PER_INCREMENT == 0) {
                                        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                                                // Downed players don't regain mana
                                                if (mattonfire.dnd.classes.Downed.Downed.is(player)) {
                                                        continue;
                                                }
                                                ManaManager.regenerateMana(player);
                                        }
                                }
                        }
                });

                AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
                        // The params used to be swapped (hitResult, entity), so `entity` was the hit
                        // result and nothing below ran. A RANGER branch here returned FAIL when the
                        // *target* was a Ranger (making Rangers immune to player melee); it never
                        // took effect and isn't in the README, so it stays disabled.

                        // Blood Hunter: every sword has Fire Aspect II (8 seconds of fire).
                        if (!world.isClient && player instanceof PlayerEntityExt attacker
                                        && attacker.getDndClass() == DndCharacter.BLOODHUNTER
                                        && player.getStackInHand(hand).getItem() instanceof SwordItem
                                        && entity instanceof LivingEntity) {
                                entity.setOnFireFor(8);
                        }

                        return ActionResult.PASS;
                });

                // Register event listener
                // Runs on both sides, so the client refuses before starting the drink or draw animation
                UseItemCallback.EVENT.register((player, world, hand) -> {
                        if (player instanceof PlayerEntityExt) {
                                PlayerEntityExt playerEntityExt = (PlayerEntityExt) player;
                                if (playerEntityExt.getDndClass() == DndCharacter.FIGHTER) {
                                        ItemStack itemStack = player.getStackInHand(hand);
                                        if (itemStack.getItem() instanceof BowItem
                                                        || itemStack.getItem() instanceof CrossbowItem) {
                                                player.sendMessage(Text.of("Fighters cannot use bows!"), true);
                                                return TypedActionResult.fail(itemStack);
                                        }
                                }
                                if (playerEntityExt.getDndClass() == DndCharacter.ARTIFICER
                                                && player.getStackInHand(hand).isOf(Items.POTION)) {
                                        // Drinking would waste it; splash/lingering can still be thrown at others
                                        player.sendMessage(Text.of("Potions have no effect on you!"), true);
                                        return TypedActionResult.fail(player.getStackInHand(hand));
                                }
                                if (playerEntityExt.getDndClass() == DndCharacter.PALADIN
                                                || playerEntityExt.getDndClass() == DndCharacter.FIGHTER) {
                                        ItemStack itemStack = player.getStackInHand(hand);

                                        // Only drinking is blocked. Splash and lingering potions can still be
                                        // thrown (e.g. holy water at a Holy Ward); PotionImmunity keeps their
                                        // buffs off the thrower.
                                        if (itemStack.isOf(Items.POTION)) {
                                                // Prevent the player from using it
                                                player.sendMessage(Text
                                                                .of("You are not allowed to drink potions!"),
                                                                true);
                                                return TypedActionResult.fail(itemStack);
                                        }
                                } else if (playerEntityExt.getDndClass() == DndCharacter.WARLOCK) {
                                        // Empty-hand fireballs are handled in Warlock (client mixin + C2S packet).
                                }
                        }
                        return TypedActionResult.pass(player.getStackInHand(hand));
                });

                UseItemCallback.EVENT.register((player, world, hand) -> {

                        ItemStack stack = player.getStackInHand(hand);
                        if (!world.isClient) {

                                // Get the level
                                int level = EnchantmentHelper.getLevel(ModEnchantments.LUNGE_ENCHANTMENT, stack);
                                if (level > 0) {

                                        if (player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                                                return TypedActionResult.fail(stack);
                                        }

                                        // Perform the lunge movement
                                        Vec3d lookVec = player.getRotationVec(1.0F);
                                        double lungeStrength = 1 * level;

                                        Vec3d lungeMotion = new Vec3d(lookVec.x * lungeStrength, 0.3,
                                                        lookVec.z * lungeStrength);
                                        player.addVelocity(lungeMotion.x, lungeMotion.y, lungeMotion.z);
                                        player.velocityModified = true;

                                        player.getItemCooldownManager().set(stack.getItem(), 100);

                                        // Show effects to all players.
                                        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                                        passedData.writeUuid(player.getUuid());

                                        // Send effects to the current player.
                                        ServerPlayNetworking.send((ServerPlayerEntity) player,
                                                        ClassPackets.S2C_LUNGE_EFFECTS_PACKET_ID,
                                                        passedData);

                                        // Send effects to all players in vicinity
                                        PlayerLookup.tracking(player).forEach(p -> {
                                                ServerPlayNetworking.send(p, ClassPackets.S2C_LUNGE_EFFECTS_PACKET_ID,
                                                                passedData);
                                        });

                                        return TypedActionResult.success(stack);
                                }

                        }
                        return TypedActionResult.pass(stack);

                });
        }
}
