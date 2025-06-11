package mattonfire.dnd.classes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Effects.SuperStrengthStatusEffect;
import mattonfire.dnd.classes.Goals.PriorityPlayerTargetGoal;
import mattonfire.dnd.classes.Items.lib.FAArmorEffectHandler;
import mattonfire.dnd.classes.Misc.PowerUpEffect;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Registry.ModEnchantments;
import mattonfire.dnd.classes.Registry.ModEntities;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Registry.ModPotions;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import mattonfire.dnd.particle.ModParticles;
import net.fabricmc.api.ModInitializer;
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
import net.minecraft.item.ItemStack;
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

        public static final Map<UUID, BloodhunterIdentityData> BLOODHUNTER_IDENTITY_EXPIRY = new HashMap<>();
        public static final Map<UUID, Long> WARLOCK_FIREBREATH = new HashMap<>();
        public static final int FIREBREATH_DURATION_TICKS = 20 * 20; // 20 seconds

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

        public static final Identifier CUSTOM_TEXTURE = new Identifier(DnDClasses.MOD_ID,
                        "textures/gui/alchemist_brewing_stand.png");

        public static final Map<String, String> respawnMessage = new HashMap<String, String>();

        private static void sendDoubleJumpPacket(MinecraftServer server, ServerPlayerEntity player,
                        ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
                PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                passedData.writeUuid(buf.readUuid());
                server.execute(() -> {
                        PlayerLookup.tracking(player).forEach(p -> {
                                ServerPlayNetworking.send(p, DnDClasses.S2C_DOUBLEJUMP_EFFECTS_PACKET_ID, passedData);
                        });
                });
        }

        private static void sendPowerupPacket(MinecraftServer server, ServerPlayerEntity player,
                        ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
                PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                passedData.writeUuid(buf.readUuid());

                if (!ManaManager.hasFullMana(player)) {
                        return;
                }

                server.execute(() -> {
                        if (player instanceof PlayerEntityExt) {

                                boolean success = PowerUpEffect.play(server, player,
                                                ((PlayerEntityExt) (PlayerEntity) player)
                                                                .getDndClass());
                                if (success) {
                                        PacketByteBuf returnData = new PacketByteBuf(Unpooled.buffer());
                                        ServerPlayNetworking.send(player,
                                                        DnDClasses.S2C_POWERUP_EFFECTS_PACKET_ID,
                                                        returnData);

                                        ManaManager.resetMana(player);
                                }
                        }
                });

        }

        private static void sendClassPickPacket(MinecraftServer server, ServerPlayerEntity player,
                        ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
                SetClassAttributes playerClasses = new SetClassAttributes();
                int bufferInteger = buf.readInt();
                playerClasses.resetToDefault(player);
                switch (bufferInteger) {
                        case 1:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Barbarian",
                                                "Strength is highly buffed & your health rivals dragons.",
                                                "You can't see very far & you move like a slug.",
                                                "You watch the one punch man anime... yeah.");
                                playerClasses.typeBarbarian(player);
                                player.setHealth(25);
                                break;
                        case 2:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Bard",
                                                "Unnoticed by mobs, fast and can jump further than an gymnist.",
                                                "You have less health & cannot use anything higher than Diamond.",
                                                "Passive animals briefly defend you & attack the entity.");
                                playerClasses.typeBard(player);
                                player.setHealth(15);
                                break;
                        case 3:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Cleric",
                                                "Mining is no challenge for you with high mining speed & night vision.",
                                                "Viweing distance is shorter & attack damage is slightly reduced.",
                                                "You significantly heal players in your area.");
                                playerClasses.typeCleric(player);
                                break;
                        case 4:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Druid",
                                                "Every taimed animal adds a heart (capped at 5). Regen in the light.",
                                                "Druids cannot swim & get hungry in dark enviroments.",
                                                "Once an animal is killed you can turn into it for a short amount of time.");

                                playerClasses.typeDruid(player);
                                break;
                        case 5:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Fighter",
                                                "High health. High Strength. Attracts mobs.",
                                                "Can't use bows. No potions.",
                                                "Super regen.");
                                playerClasses.typeFighter(player);
                                player.setHealth(25);
                                break;
                        case 6:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Monk",
                                                "Your mobility & attack speed highly increased.",
                                                "Decreases your damage output. You're also unable to use anything but a staff to attack.",
                                                "You can jump infinitly and your attack speed is unrivaled.");
                                playerClasses.typeMonk(player);
                                break;
                        case 7:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Paladin",
                                                "The power of christ compelles, your high health also helps...",
                                                "You can't use potions & you're very weak in the nether.",
                                                "You get an enormous boost to health. Tank's fire!");
                                playerClasses.typePaladin(player);
                                player.setHealth(25);
                                break;
                        case 8:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Ranger",
                                                "You can zoom in with your bow seeing far into the distance. You also get natural looting.",
                                                "You can't pickup swords and you're weak to fire.",
                                                "Your bow fires instantly, no need to reload.");
                                playerClasses.typeRanger(player);
                                break;
                        case 9:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Rogue",
                                                "You don't take poison damage nor do you need to eat.",
                                                "Nether mobs are alies but all overworld mobs will attempt to kill. So be careful!",
                                                "You become invisible for a short period of time.");
                                playerClasses.typeRogue(player);
                                break;
                        case 10:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Necromancer",
                                                "Wither debuff to all attacked & not attacked by the undead.",
                                                "You have slightly less health & deal significantly less damage.",
                                                "You spawn allied undead that attack your enemies!");
                                playerClasses.typeNecromancer(player);
                                break;
                        case 11:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Warlock",
                                                "With an empty hand, the ability to throw fireballs & resistant to both fire and lava.",
                                                "Decreases your damage output. You're also unable to use anything but a staff to attack.",
                                                "You breathe fire by holding your special key.");
                                playerClasses.typeWarlock(player);
                                break;
                        case 12:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Wizard",
                                                "Able to wield elemental staffs scattered over the map.",
                                                "Your health is greatly reduced & you're only able to weild Iron armor or lower.",
                                                "You create a massive explosion on your person and invulnerable for a few seconds.");
                                playerClasses.typeWizard(player);
                                player.setHealth(10);
                                break;
                        case 13:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Artificer",
                                                "Greater movement speed & automatic enchanting chance or Armor upgrade.",
                                                "You deal less damage & you are unaffected by all potions (unless a characters ability)",
                                                "All armor is buffed for a period of time.");
                                playerClasses.typeArtificer(player);
                                break;
                        case 14:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Blood Hunter",
                                                "Fire aspect is applied to all swords & x2 Damage during Night.",
                                                "Swords cannot be dropped & 1/2 Damage is dealt during the Day.",
                                                "Ability to take control of ANY mob within 30m");
                                playerClasses.typeBloodHunter(player);
                                break;
                        case 15:
                                playerClasses.sendPlayerMessage(
                                                player,
                                                "Alchemist",
                                                "The ability to craft special potions. That only alchemists can wield!",
                                                "A potion can backfire so be very careful, revisit to the guide for more information.",
                                                "All potions are buffed by II tiers for a period of time.");
                                playerClasses.typeAlchemist(player);
                                break;
                        default:
                                break;
                }
                ;

                // Close the user's class pick GUI.
                player.closeHandledScreen();

                // Create a new pool to pass the int.
                PacketByteBuf approveClassBuf = new PacketByteBuf(Unpooled.buffer());
                approveClassBuf.writeInt(bufferInteger);
                ServerPlayNetworking.send(player,
                                DnDClasses.S2C_APPROVE_CLASS_PICK_PACKET_ID,
                                approveClassBuf);

                // If run with no errors declare in the NBT.
                if (player instanceof PlayerEntityExt) {
                        ((PlayerEntityExt) player).setDndClass(DndCharacter.fromValue(bufferInteger));
                }

                String message = DnDClasses.respawnMessage.get(player.getUuidAsString());
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

        private static void createParticleRing(ServerWorld world, Vec3d center, double radius, int particleCount) {
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

                // Runs clientside right now.
                // DisallowSwordServer.onInitializeServer();
                Registry.register(Registries.STATUS_EFFECT, Identifier.of(DnDClasses.MOD_ID, "super_strength"),
                                new SuperStrengthStatusEffect());

                FAArmorEffectHandler.register();

                ModSounds.registerSounds();
                ModItemGroup.registerItemGroups();
                ModItems.registerModItems();
                ModEffects.registerEffects();
                ModPotions.registerPotions();
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

                ServerTickEvents.END_WORLD_TICK.register(world -> {
                        if (!(world instanceof ServerWorld serverWorld))
                                return;
                        long now = serverWorld.getTime();

                        Iterator<Map.Entry<UUID, BloodhunterIdentityData>> it = BLOODHUNTER_IDENTITY_EXPIRY.entrySet()
                                        .iterator();
                        while (it.hasNext()) {
                                Map.Entry<UUID, BloodhunterIdentityData> entry = it.next();
                                BloodhunterIdentityData identityData = entry.getValue();
                                if (now >= identityData.expiryTick) {
                                        ServerPlayerEntity player = (ServerPlayerEntity) serverWorld
                                                        .getPlayerByUuid(entry.getKey());
                                        if (player != null) {
                                                // Move the entity to the player's current position before spawning
                                                identityData.entity.refreshPositionAndAngles(
                                                                player.getX(),
                                                                player.getY(),
                                                                player.getZ(),
                                                                identityData.entity.getYaw(),
                                                                identityData.entity.getPitch());

                                                // TODO: There are bugs with the entity when copying it. This
                                                // resets some properties. There is probably a better way to do
                                                // this. Either copy it properly or don't do the copy at all and find
                                                // why I slide when I become an entity.

                                                identityData.entity.setNoGravity(false);
                                                identityData.entity.setInvulnerable(false);
                                                identityData.entity.setSprinting(false);

                                                draylar.identity.api.PlayerIdentity.updateIdentity(player, null, null);
                                                serverWorld.spawnEntity(identityData.entity);

                                        }

                                        // Spawn the old entity back
                                        it.remove();
                                }
                        }

                        DnDClasses.WARLOCK_FIREBREATH.entrySet().removeIf(entry -> {
                                UUID uuid = entry.getKey();
                                long endTick = entry.getValue();
                                ServerPlayerEntity player = serverWorld.getServer().getPlayerManager().getPlayer(uuid);

                                if (now > endTick) {
                                        // When ending fire breath:
                                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                                        buf.writeBoolean(false);
                                        ServerPlayNetworking.send(player, DnDClasses.S2C_WARLOCK_FIREBREATH, buf);
                                        return true; // Remove expired
                                }

                                if (player == null)
                                        return true;

                                // TODO: Not sure if this is where it goes
                                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                                buf.writeBoolean(true);
                                buf.writeLong(endTick);
                                ServerPlayNetworking.send(player, DnDClasses.S2C_WARLOCK_FIREBREATH, buf);

                                // Fire breath logic
                                Vec3d look = player.getRotationVec(1.0F);
                                Vec3d start = player.getPos().add(0, player.getStandingEyeHeight(), 0);

                                // Beam: 5 blocks long, 1 block wide
                                for (int i = 1; i <= 5; i++) {
                                        Vec3d pos = start.add(look.multiply(i));

                                        for (ServerPlayerEntity otherPlayer : serverWorld.getPlayers()) {
                                                if (!otherPlayer.getUuid().equals(player.getUuid())) {

                                                        System.out.println("Spawning particles for player: "
                                                                        + otherPlayer.getName().getString());
                                                        serverWorld.spawnParticles(
                                                                        otherPlayer,
                                                                        ParticleTypes.FLAME,
                                                                        false, // longDistance
                                                                        pos.x, pos.y, pos.z,
                                                                        8, 0.2, 0.2, 0.2, 0.01);
                                                }
                                        }

                                        // serverWorld.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 8, 0.2,
                                        // 0.2, 0.2, 0.01);

                                        // Damage entities in the beam
                                        Box box = new Box(pos.x - 0.5, pos.y - 0.5, pos.z - 0.5, pos.x + 0.5,
                                                        pos.y + 0.5, pos.z + 0.5);
                                        for (LivingEntity entity : serverWorld.getEntitiesByClass(LivingEntity.class,
                                                        box, e -> e != player)) {
                                                entity.setOnFireFor(2);
                                                entity.damage(serverWorld.getDamageSources().magic(), 2.0F);
                                        }
                                }
                                return false;
                        });
                });

                ServerTickEvents.END_WORLD_TICK.register(world -> {
                        if (world instanceof ServerWorld serverWorld) {
                                long currentTick = serverWorld.getServer().getTicks();

                                if (currentTick % MANA_TICKS_PER_INCREMENT == 0) {
                                        for (ServerPlayerEntity player : world.getPlayers()) {
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

                AttackEntityCallback.EVENT.register((player, world, hand, hitResult, entity) -> {
                        if (entity instanceof PlayerEntityExt) {

                                PlayerEntityExt playerEntityExt = (PlayerEntityExt) entity;
                                switch (playerEntityExt.getDndClass()) {
                                        case RANGER:
                                                return ActionResult.FAIL;
                                        case BLOODHUNTER:

                                                // DamageSource customExplosionSource = ModDamageTypes.of(world,
                                                // ModDamageTypes.BREWING_STAND_DAMAGE_SOURCE);
                                                // hitResult.damage()
                                                break;
                                        default:
                                                break;
                                }
                        }

                        return ActionResult.PASS;
                });

                // Register event listener
                UseItemCallback.EVENT.register((player, world, hand) -> {
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                                if (serverPlayer instanceof PlayerEntityExt) {
                                        PlayerEntityExt playerEntityExt = (PlayerEntityExt) serverPlayer;
                                        if (playerEntityExt.getDndClass() == DndCharacter.PALADIN) {
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
                                                // if (player.getStackInHand(hand).isEmpty()) {

                                                // }

                                        }
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
                ServerPlayNetworking.registerGlobalReceiver(C2S_DOUBLEJUMP_EFFECTS_REQUEST_PACKET_ID,
                                DnDClasses::sendDoubleJumpPacket);

                // Register powerup registry.
                ServerPlayNetworking.registerGlobalReceiver(C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID,
                                DnDClasses::sendPowerupPacket);

                // Register classpick registry.
                ServerPlayNetworking.registerGlobalReceiver(C2S_CLASS_PICK_PACKET_ID, DnDClasses::sendClassPickPacket);

                // tree feller enchantment
                TreeFeller.register();

                GridMiner.register();

                Invulnerability.register();

                if (FabricLoader.getInstance().isModLoaded("identity")) {
                        System.out.println("Identity Mod is loaded!");
                        // Safely use Identity's API here
                } else {
                        System.out.println("Identity Mod is NOT loaded!");
                }

                ServerEntityEvents.ENTITY_LOAD.register((entity, serverWorld) -> {
                        if (entity instanceof MobEntity mob) {
                                MobEntityAccessor accessor = (MobEntityAccessor) mob;

                                accessor.getTargetSelector().add(2,
                                                new PriorityPlayerTargetGoal<>(mob, PlayerEntity.class));
                        }

                });

                ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                        ServerPlayerEntity player = handler.getPlayer();

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
// Make the fireball no damage to caster.
// Custom projectile for the staffs
