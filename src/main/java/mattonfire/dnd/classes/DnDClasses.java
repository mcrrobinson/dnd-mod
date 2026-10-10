package mattonfire.dnd.classes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Items.ClassGuidebook;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Effects.SuperStrengthStatusEffect;
import mattonfire.dnd.classes.Goals.PriorityPlayerTargetGoal;
import mattonfire.dnd.classes.Items.lib.FAArmorEffectHandler;
import mattonfire.dnd.classes.Misc.BloodHunterControl;
import mattonfire.dnd.classes.Misc.PowerUpEffect;
import mattonfire.dnd.classes.Progression.Abilities;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.classes.Rest.Charges;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Registry.ModEnchantments;
import mattonfire.dnd.classes.Registry.ModEntities;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Registry.ModPotions;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import mattonfire.dnd.particle.ModParticles;
import mattonfire.dnd.classes.Commands.DndClassCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class DnDClasses implements ModInitializer {

        public static final Map<UUID, Long> WARLOCK_FIREBREATH = new HashMap<>();

        public static final Identifier C2S_DOUBLEJUMP_EFFECTS_REQUEST_PACKET_ID = Identifier.of("doublejump",
                        "request_doublejump_effects");
        public static final Identifier S2C_DOUBLEJUMP_EFFECTS_PACKET_ID = Identifier.of("doublejump",
                        "play_doublejump_effects");

        public static final Identifier C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID = Identifier.of("powerup",
                        "request_powerup_effects");
        public static final Identifier S2C_POWERUP_EFFECTS_PACKET_ID = Identifier.of("powerup",
                        "play_powerup_effects");

        public static final Identifier C2S_LUNGE_REQUEST_PACKET_ID = Identifier.of("lunge",
                        "request_lunge_effects");
        public static final Identifier S2C_LUNGE_EFFECTS_PACKET_ID = Identifier.of("lunge", "play_lunge_effects");

        public static final Identifier S2C_SYNC_MANA = Identifier.of("mana",
                        "sync_mana");

        public static int MANA_FULL_SECONDS = 18; // Should be 2 seconds per increment
        public static int MANA_ICONS = 9;
        public static int MANA_TICKS_PER_INCREMENT = Math.round(MANA_FULL_SECONDS / MANA_ICONS) * 20; // Ticks per
                                                                                                      // second;

        public static final String MOD_ID = "dndclasses";
        public static final Logger LOGGER = LogManager.getLogger("dndclasses");

        public static final Identifier S2C_WARLOCK_FIREBREATH = Identifier.of(MOD_ID, "warlock_firebreath");
        public static final Identifier S2C_WIZARD_EFFECTS_PACKET_ID = Identifier.of(MOD_ID, "wizard_powerup");

        public static final Identifier C2S_CLASS_PICK_PACKET_ID = Identifier.of("classpick", "class_pick");
        public static final Identifier S2C_CLASS_QUERY_PACKET_ID = Identifier.of("classpick", "class_query");
        public static final Identifier S2C_APPROVE_CLASS_PICK_PACKET_ID = Identifier.of("classpick",
                        "approve_class_pick");

        public static final Map<String, String> respawnMessage = new HashMap<String, String>();

        private static void sendPowerupPacket(MinecraftServer server, ServerPlayerEntity player,
                        ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
                server.execute(() -> {
                        // Held by the Dungeon Master's freeze: no powers.
                        if (mattonfire.dnd.dm.DmFreeze.isFrozen(player)) {
                                return;
                        }
                        // The equipped active skill; classes without a tree yet use their power-up at full mana.
                        SkillNode skill = Progression.current(player).activeNode();
                        int cost = skill == null ? MANA_ICONS : skill.manaCost();
                        // Sneak + power-up picks the Druid's Wild Shape form; free, so before the mana check.
                        if (Druid.cycleForm(player)) {
                                return;
                        }
                        int mana = ManaManager.getMana(player);
                        if (mana < cost) {
                                return;
                        }
                        // Major actives also cost charges, which only rests restore (off with dndRests false)
                        if (!Charges.canAfford(player, skill)) {
                                player.sendMessage(Text.literal("No charges left: rest to recover")
                                                .formatted(Formatting.RED), true);
                                return;
                        }
                        // A Beholder's anti-magic cone: the power fizzles and the mana is kept
                        if (mattonfire.dnd.classes.Effects.AntiMagicEffect.blocks(player)) {
                                return;
                        }

                        boolean success = skill == null
                                        ? PowerUpEffect.play(server, player, Progression.classOf(player))
                                        : Abilities.activate(player, skill);
                        if (success) {
                                ManaManager.setMana(player, mana - cost);
                                ManaManager.sync(player);
                                Charges.spend(player, skill);
                                ServerPlayNetworking.send(player,
                                                DnDClasses.S2C_POWERUP_EFFECTS_PACKET_ID,
                                                new PacketByteBuf(Unpooled.buffer()));
                        }
                });

        }

        /** Shows the hint left by a player's last death, if any, once. */
        public static void sendRespawnHint(ServerPlayerEntity player) {
                String message = DnDClasses.respawnMessage.remove(player.getUuidAsString());
                if (message == null) {
                        return;
                }
                Text titleText = Text.literal("Subtle hint").formatted(Formatting.BOLD, Formatting.GOLD);
                Text subtitleText = Text.literal(message).formatted(Formatting.ITALIC, Formatting.YELLOW);

                TitleS2CPacket packet = new TitleS2CPacket(titleText);
                SubtitleS2CPacket subtitlePacket = new SubtitleS2CPacket(subtitleText);
                // Send the title packet
                player.networkHandler.sendPacket(packet);

                // Send the subtitle packet
                player.networkHandler.sendPacket(subtitlePacket);
        }

        public static void createParticleRing(ServerWorld world, Vec3d center, double radius, int particleCount) {
                for (int i = 0; i < particleCount; i++) {
                        double time = world.getTime() % 360;
                        double angle = 2 * Math.PI * i / particleCount + time * 0.01;
                        double x = center.x + radius * Math.cos(angle);
                        double z = center.z + radius * Math.sin(angle);
                        double y = center.y + 0.1; // Slightly above the player's feet

                        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 1, 0, 0, 0, 0);
                }
        }

        public static final Map<UUID, Long> effectTimestamps = new HashMap<>();

        @Override
        public void onInitialize() {

                if (FAConfig.exists()) {
                        FAConfig.load();
                } else {
                        FAConfig.save();
                }

                mattonfire.dnd.entity.ModEntityTypes.registerEntityTypes();
                net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(mattonfire.dnd.entity.DragonPartTracker::onLoad);
                net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register(mattonfire.dnd.entity.DragonPartTracker::onUnload);
                mattonfire.dnd.world.gen.ModSpawns.addSpawns();
                mattonfire.dnd.world.gen.village.HobbitVillageStructures.register();
                mattonfire.dnd.tavern.Tavern.register();
                mattonfire.dnd.world.gen.fortress.DwarvenFortressStructures.register();
                mattonfire.dnd.world.gen.lair.DragonLairStructures.register();
                mattonfire.dnd.world.gen.camp.GoblinCampStructures.register();
                // Before DwarfGrudges: a failed lockpick stops the chest opening, so the dwarves see nothing
                mattonfire.dnd.classes.Abilities.AbilityScores.bootstrap();
                mattonfire.dnd.classes.SkillChecks.D20.register();
                mattonfire.dnd.classes.Obstacles.ObstacleTypes.register();
                mattonfire.dnd.world.gen.beholder.BeholderLairStructures.register();
                mattonfire.dnd.world.gen.dungeon.DungeonStructures.register();
                mattonfire.dnd.entity.DwarfGrudges.register();
                mattonfire.dnd.entity.raid.GoblinRaids.register();
                mattonfire.dnd.faction.FactionEvents.register();
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.faction.RepCommand.register(dispatcher));
                mattonfire.dnd.quest.QuestEvents.register();
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.quest.QuestCommand.register(dispatcher));
                mattonfire.dnd.entity.boss.StructureBosses.register();

                // Runs clientside right now.
                // DisallowSwordServer.onInitializeServer();
                Registry.register(Registries.STATUS_EFFECT, Identifier.of(DnDClasses.MOD_ID, "super_strength"),
                                new SuperStrengthStatusEffect());

                FAArmorEffectHandler.register();
                mattonfire.dnd.classes.Misc.PaladinNetherWeakness.register();
                mattonfire.dnd.classes.Misc.ArtificerDamage.register();
                mattonfire.dnd.classes.Misc.ClericHandler.register();
                MonkHandler.register();

                ModSounds.registerSounds();
                mattonfire.dnd.classes.Music.DungeonMusic.register();
                ModItemGroup.registerItemGroups();
                ModItems.registerModItems();
                ModEffects.registerEffects();
                ModPotions.registerPotions();
                mattonfire.dnd.magic.Magic.register();
                ModEntities.registerBlockEntities();
                ModBlocks.registerBlocks();
                ModEnchantments.registerEnchantments();
                ModParticles.registerParticles();

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

                // Warlock fire breath ticks in Warlock.register().

                ServerTickEvents.END_WORLD_TICK.register(serverWorld -> {
                        {
                                long currentTick = serverWorld.getServer().getTicks();

                                if (currentTick % MANA_TICKS_PER_INCREMENT == 0) {
                                        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                                                ManaManager.regenerateMana(player);
                                        }
                                }

                                // Iterate and remove expired players
                                Iterator<Map.Entry<UUID, Long>> iterator = DnDClasses.effectTimestamps.entrySet()
                                                .iterator();
                                while (iterator.hasNext()) {
                                        Map.Entry<UUID, Long> entry = iterator.next();
                                        UUID playerId = entry.getKey();
                                        long startTick = entry.getValue();

                                        // If 5 seconds (100 ticks) passed, remove effect
                                        if (currentTick - startTick >= 100) {
                                                iterator.remove();
                                        } else {
                                                // Player is still affected, show particles
                                                ServerPlayerEntity player = serverWorld.getServer().getPlayerManager()
                                                                .getPlayer(playerId);
                                                if (player != null) {
                                                        createParticleRing(serverWorld, player.getPos(), 16, 100);
                                                }
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

                                        // Check if the item is a potion
                                        if (itemStack.isOf(Items.POTION) || itemStack.isOf(Items.SPLASH_POTION)
                                                        || itemStack.isOf(Items.LINGERING_POTION)) {
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
                                                        DnDClasses.S2C_LUNGE_EFFECTS_PACKET_ID,
                                                        passedData);

                                        // Send effects to all players in vicinity
                                        PlayerLookup.tracking(player).forEach(p -> {
                                                ServerPlayNetworking.send(p, DnDClasses.S2C_LUNGE_EFFECTS_PACKET_ID,
                                                                passedData);
                                        });

                                        return TypedActionResult.success(stack);
                                }

                        }
                        return TypedActionResult.pass(stack);

                });

                FabricBrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, Ingredient.ofItems(Items.ICE),
                                ModPotions.FREEZE_POTION.value());

                // Register doublejump registry.
                // The double-jump packet is handled in MonkHandler.register().

                // Register powerup registry.
                ServerPlayNetworking.registerGlobalReceiver(C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID,
                                DnDClasses::sendPowerupPacket);

                // Register classpick registry.

                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> DndClassCommand.register(dispatcher));
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.classes.Commands.DndRaceCommand
                                                .register(dispatcher));
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.classes.Party.PartyCommand
                                                .register(dispatcher));
                mattonfire.dnd.classes.Party.PartyEvents.register();
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.classes.Commands.GoblinRaidCommand.register(dispatcher));
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.classes.Commands.DungeonCommand.register(dispatcher));
                // Dungeon Master toolkit: /dm, the veil, encounters and freeze.
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.dm.DmCommand.register(dispatcher));
                mattonfire.dnd.dm.DmVeil.register();
                mattonfire.dnd.dm.DmFreeze.register();
                mattonfire.dnd.dm.encounter.Encounters.register();
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.classes.Commands.ObstacleCommand.register(dispatcher));

                // tree feller enchantment
                TreeFeller.register();

                GridMiner.register();

                Invulnerability.register();

                BloodHunterControl.register();
                Warlock.register();
                Progression.register();
                ClassLifecycle.register();
                mattonfire.dnd.classes.Race.RaceLifecycle.register();
                mattonfire.dnd.classes.Rest.Rests.register();

                if (FabricLoader.getInstance().isModLoaded("identity")) {
                        System.out.println("Identity Mod is loaded!");
                        // Safely use Identity's API here
                } else {
                        System.out.println("Identity Mod is NOT loaded!");
                }

                ServerEntityEvents.ENTITY_LOAD.register((entity, serverWorld) -> {
                        if (entity instanceof MobEntity mob) {
                                // Fighters attract mobs that already hunt players
                                PriorityPlayerTargetGoal.attach(mob);
                        }

                });

                ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                        ServerPlayerEntity player = handler.getPlayer();

                        ClassGuidebook.onJoin(player);

                        int mana = ManaManager.getMana(player);
                        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                        passedData.writeInt(mana);
                        System.out.println("sending packet update" + Integer.toString(mana));
                        ServerPlayNetworking.send((ServerPlayerEntity) player,
                                        DnDClasses.S2C_SYNC_MANA,
                                        passedData);
                });

        }

}

// remove diamonds type create own again...
// Change shade of the potion
// Custom projectile for the staffs
