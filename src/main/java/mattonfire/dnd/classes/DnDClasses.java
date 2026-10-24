package mattonfire.dnd.classes;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Items.ClassGuidebook;
import mattonfire.dnd.classes.Effects.SuperStrengthStatusEffect;
import mattonfire.dnd.classes.Goals.PriorityPlayerTargetGoal;
import mattonfire.dnd.classes.Items.lib.FAArmorEffectHandler;
import mattonfire.dnd.classes.Misc.BloodHunterControl;
import mattonfire.dnd.classes.Progression.Abilities;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModBannerPatterns;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Registry.ModEnchantments;
import mattonfire.dnd.classes.Registry.ModEntities;
import mattonfire.dnd.classes.Registry.ModBlockFamilies;
import mattonfire.dnd.classes.Registry.DwarvenBlocks;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Registry.ModPotions;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.particle.ModParticles;
import mattonfire.dnd.classes.Commands.DndClassCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class DnDClasses implements ModInitializer {

        public static int MANA_FULL_SECONDS = 18; // Should be 2 seconds per increment
        public static int MANA_ICONS = 9;
        public static int MANA_TICKS_PER_INCREMENT = Math.round(MANA_FULL_SECONDS / MANA_ICONS) * 20; // Ticks per
                                                                                                      // second;

        public static final String MOD_ID = "dndclasses";
        public static final Logger LOGGER = LoggerFactory.getLogger("dndclasses");

        public static final Map<String, String> respawnMessage = new HashMap<String, String>();

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

        /**
         * Draws one of {@code phases} interleaved subsets of a {@code particleCount}-point ring: point i is
         * drawn on ticks where {@code i % phases == time % phases}. Called every tick, each point is redrawn
         * every {@code phases} ticks, so the ring keeps all its points while sending 1/phases of the particle
         * packets (each spawnParticles call is one packet per nearby player). Keep {@code phases} at or under
         * the particle's shortest visible life so no point blinks out.
         */
        public static void createParticleRing(ServerWorld world, Vec3d center, double radius, int particleCount,
                        int phases) {
                long tick = world.getTime();
                double time = tick % 360;
                for (int i = (int) (tick % phases); i < particleCount; i += phases) {
                        double angle = 2 * Math.PI * i / particleCount + time * 0.01;
                        double x = center.x + radius * Math.cos(angle);
                        double z = center.z + radius * Math.sin(angle);
                        double y = center.y + 0.1; // Slightly above the player's feet

                        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 1, 0, 0, 0, 0);
                }
        }

        @Override
        public void onInitialize() {

                if (FAConfig.exists()) {
                        FAConfig.load();
                } else {
                        FAConfig.save();
                }

                mattonfire.dnd.entity.ModEntityTypes.registerEntityTypes();
                mattonfire.dnd.entity.DragonSaves.register();
                mattonfire.dnd.entity.BossSaves.register();
                mattonfire.dnd.entity.GrabSaves.register();
                mattonfire.dnd.entity.MindFlayerSaves.register();
                net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(mattonfire.dnd.entity.DragonPartTracker::onLoad);
                net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register(mattonfire.dnd.entity.DragonPartTracker::onUnload);
                mattonfire.dnd.world.gen.ModSpawns.addSpawns();
                mattonfire.dnd.world.gen.village.HobbitVillageStructures.register();
                mattonfire.dnd.tavern.Tavern.register();
                mattonfire.dnd.world.gen.fortress.DwarvenFortressStructures.register();
                mattonfire.dnd.world.gen.lair.DragonLairStructures.register();
                mattonfire.dnd.world.gen.camp.GoblinCampStructures.register();
                mattonfire.dnd.world.gen.enclave.ElvenEnclaveStructures.register();
                mattonfire.dnd.world.gen.enclave.EnclaveRegrowth.register();
                mattonfire.dnd.world.gen.enclave.Moonwell.register();
                mattonfire.dnd.world.gen.temple.DragonTempleStructures.register();
                mattonfire.dnd.world.gen.temple.TempleShrine.register();
                mattonfire.dnd.world.gen.stronghold.OrcStrongholdStructures.register();
                mattonfire.dnd.world.gen.sanctum.BrimstoneSanctumStructures.register();
                mattonfire.dnd.world.gen.sanctum.SanctumRefuge.register();
                mattonfire.dnd.world.gen.burrow.CogsdeepStructures.register();
                mattonfire.dnd.classes.Items.racial.TinkersGoggles.register();
                mattonfire.dnd.entity.OrcTrial.register();
                // Before SettlementGrudges: a failed lockpick stops the chest opening, so the dwarves see nothing
                mattonfire.dnd.classes.Abilities.AbilityScores.bootstrap();
                mattonfire.dnd.classes.SkillChecks.D20.register();
                mattonfire.dnd.classes.SkillChecks.SaveRules.register();
                mattonfire.dnd.classes.Obstacles.ObstacleTypes.register();
                mattonfire.dnd.world.gen.beholder.BeholderLairStructures.register();
                mattonfire.dnd.world.gen.dungeon.DungeonStructures.register();
                mattonfire.dnd.dungeon.DungeonCombat.register();
                mattonfire.dnd.entity.SettlementGrudges.register();
                mattonfire.dnd.world.gen.HomeBonuses.register();
                mattonfire.dnd.entity.raid.GoblinRaids.register();
                mattonfire.dnd.entity.DwarfGrudges.register();
                mattonfire.dnd.faction.FactionEvents.register();
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.faction.RepCommand.register(dispatcher));
                mattonfire.dnd.quest.QuestEvents.register();
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.quest.QuestCommand.register(dispatcher));
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.classes.Commands.DndHomeCommand.register(dispatcher));
                mattonfire.dnd.entity.boss.StructureBosses.register();
                // Dungeon loot: the dungeon_tier loot condition, Veteran drops and the repopulation timer
                mattonfire.dnd.dungeon.DungeonLoot.register();

                // Runs clientside right now.
                // DisallowSwordServer.onInitializeServer();
                Registry.register(Registries.STATUS_EFFECT, Identifier.of(DnDClasses.MOD_ID, "super_strength"),
                                new SuperStrengthStatusEffect());

                FAArmorEffectHandler.register();
                // Class attribute modifiers (Paladin Nether weakness, Artificer damage, Monk armour penalty)
                ClassAttributeRules.register();
                mattonfire.dnd.classes.Misc.PaladinNetherWeakness.register();
                mattonfire.dnd.classes.Misc.ArtificerDamage.register();
                ClericRules.register();
                MonkRules.register();

                ModSounds.registerSounds();
                mattonfire.dnd.classes.Music.DungeonMusic.register();
                ModItemGroup.registerItemGroups();
                ModItems.registerModItems();
                ModEffects.registerEffects();
                ModPotions.registerPotions();
                mattonfire.dnd.magic.Magic.register();
                ModEntities.registerBlockEntities();
                ModBlocks.registerBlocks();
                ModBlockFamilies.register();
                mattonfire.dnd.classes.Registry.SanctumBlocks.register();
                mattonfire.dnd.classes.Registry.ElvenBlocks.register();
                DwarvenBlocks.register();
                mattonfire.dnd.classes.Registry.OrcBuildingBlocks.register();
                mattonfire.dnd.classes.Blocks.hobbit.HobbitBlocks.register();
                mattonfire.dnd.classes.Registry.CogsdeepBlocks.register();
                // --- Dragon Temples ---
                mattonfire.dnd.classes.Registry.ModTempleBlocks.register();
                mattonfire.dnd.classes.Registry.CryptBlocks.register();
                mattonfire.dnd.classes.Registry.GoblinBlocks.register();
                mattonfire.dnd.classes.Registry.LairBlocks.register();
                mattonfire.dnd.classes.Registry.RuinBlocks.register();
                mattonfire.dnd.classes.Registry.DesolateBlocks.register();
                mattonfire.dnd.classes.Registry.AdventuringBlocks.register();
                mattonfire.dnd.classes.Blocks.TrapBlocks.register();
                ModBannerPatterns.register();
                ModEnchantments.registerEnchantments();
                ModParticles.registerParticles();

                // Class rules (Fighter bows, potion bans, Necromancer wither, Blood Hunter fire), the Lunge
                // enchantment, mana regeneration and the brewing-death hint.
                ClassEvents.register();

                FabricBrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, Ingredient.ofItems(Items.ICE),
                                ModPotions.FREEZE_POTION.value());

                // Register doublejump registry.
                // The double-jump packet is handled in MonkRules.register().

                // Register powerup registry.
                PowerUpRequests.register();

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
                mattonfire.dnd.dm.DmWand.register();
                mattonfire.dnd.dm.DmRollCalls.register();
                mattonfire.dnd.dm.DmPanelPackets.register();
                mattonfire.dnd.dm.encounter.Encounters.register();
                CommandRegistrationCallback.EVENT.register(
                                (dispatcher, registryAccess, environment) -> mattonfire.dnd.classes.Commands.ObstacleCommand.register(dispatcher));

                // tree feller enchantment
                TreeFeller.register();

                GridMiner.register();

                Invulnerability.register();

                BloodHunterControl.register();
                WarlockRules.register();
                Progression.register();
                ClassLifecycle.register();
                CharacterCreation.register();
                mattonfire.dnd.classes.Race.RaceLifecycle.register();
                mattonfire.dnd.classes.Rest.Rests.register();
                mattonfire.dnd.classes.Downed.DownedEvents.register();
                mattonfire.dnd.classes.Downed.Revives.register();
                mattonfire.dnd.classes.Downed.Stabilise.register();
                mattonfire.dnd.classes.Downed.DownedHudSync.register();
                // Conditions (Prone, Frightened, Restrained, Stunned, Petrified...): after Downed, which it clears on
                mattonfire.dnd.classes.Conditions.Conditions.bootstrap();

                if (FabricLoader.getInstance().isModLoaded("identity")) {
                        LOGGER.info("Identity mod is loaded");
                        // Safely use Identity's API here
                } else {
                        LOGGER.info("Identity mod is not loaded");
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
                        LOGGER.debug("Sending mana {} to {}", mana, player.getName().getString());
                        ServerPlayNetworking.send((ServerPlayerEntity) player,
                                        ClassPackets.S2C_SYNC_MANA,
                                        passedData);
                });

        }

}

// remove diamonds type create own again...
// Change shade of the potion
// Custom projectile for the staffs
