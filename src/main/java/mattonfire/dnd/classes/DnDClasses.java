package mattonfire.dnd.classes;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.PowerupKeybind.PowerUpEffect;

import java.util.stream.Stream;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.registry.Registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.context.CommandContext;

public class DnDClasses implements ModInitializer {
        public static final Logger LOGGER = LogManager.getLogger();
        public static final Identifier C2S_DOUBLEJUMP_EFFECTS_REQUEST_PACKET_ID = new Identifier("doublejump",
                        "request_doublejump_effects");
        public static final Identifier S2C_DOUBLEJUMP_EFFECTS_PACKET_ID = new Identifier("doublejump",
                        "play_doublejump_effects");

        public static final Identifier C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID = new Identifier("powerup",
                        "request_powerup_effects");
        public static final Identifier S2C_POWERUP_EFFECTS_PACKET_ID = new Identifier("powerup",
                        "play_powerup_effects");
        public static final String MOD_ID = "dndclasses";

        // If the client has no class it returns 0 which prompts the screen where they
        // pick. Once they pick a class.
        // C2S sends packet with the class ID. If the class is approved the server will
        // return the classID.
        public static final Identifier C2S_CLASS_PICK_PACKET_ID = new Identifier("classpick", "class_pick");
        public static final Identifier S2C_APPROVE_CLASS_PICK_PACKET_ID = new Identifier("classpick",
                        "approve_class_pick");

        // Queries whether or not the player has a class existing.
        public static final Identifier S2C_CLASS_QUERY_PACKET_ID = new Identifier("classpick", "class_query");
        public static final Item MUSIC_DISC_STEEL_ON_STEEL = registerMusicDisk("music_disc_steel_on_steel", 14,
                        Sound.STEEL_ON_STEEL);
        public static final Item MUSIC_DISC_AWAKE_CART = registerMusicDisk("music_disc_awake_cart", 15,
                        Sound.AWAKE_CART);
        public static final Item MUSIC_DISC_TOOTH_AND_CLAW = registerMusicDisk("music_disc_tooth_and_claw", 16,
                        Sound.TOOTH_AND_CLAW);
        public static final Item MUSIC_DISC_SILENT_FOOTSTEPS = registerMusicDisk("music_disc_silent_footsteps", 17,
                        Sound.SILENT_FOOTSTEPS);

        public static final StatusEffect FREEZE_EFFECT = new FreezeEffect();

        @Override
        public void onInitialize() {

                // Runs clientside right now.
                // DisallowSwordServer.onInitializeServer();
                Registry.register(Registry.STATUS_EFFECT, new Identifier("freeze", "freeze_effect"), FREEZE_EFFECT);
                CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
                        dispatcher.register(CommandManager.literal("freeze")
                                        .executes(context -> {
                                                ServerCommandSource source = context.getSource();
                                                if (source.getEntity() instanceof LivingEntity) {
                                                        LivingEntity player = (LivingEntity) source.getEntity();
                                                        HitResult hitResult = player.raycast(10, 0.5F, false); // Raycast
                                                                                                               // to
                                                                                                               // find a
                                                                                                               // target

                                                        if (hitResult instanceof EntityHitResult) {
                                                                EntityHitResult entityHitResult = (EntityHitResult) hitResult;
                                                                if (entityHitResult
                                                                                .getEntity() instanceof LivingEntity) {
                                                                        LivingEntity targetEntity = (LivingEntity) entityHitResult
                                                                                        .getEntity();
                                                                        targetEntity.addStatusEffect(
                                                                                        new StatusEffectInstance(
                                                                                                        DnDClasses.FREEZE_EFFECT,
                                                                                                        200)); // Freeze
                                                                                                               // for
                                                                                                               // 200z
                                                                                                               // ticks
                                                                        source.sendFeedback(Text.of(
                                                                                        "Entity frozen for 10 seconds!"),
                                                                                        false);
                                                                        return 1; // Success
                                                                }
                                                        } else if (hitResult instanceof BlockHitResult) {
                                                                source.sendFeedback(Text.of(
                                                                                "No entity in sight, block detected!"),
                                                                                false);
                                                        } else {
                                                                source.sendFeedback(Text.of(
                                                                                "No valid target detected!"), false);
                                                        }
                                                }
                                                return 0; // Failure
                                        }));
                });
                Sound.registerSounds();

                // Register staff entity.
                LOGGER.info("Registering Staffs...");
                Registry.register(Registry.STATUS_EFFECT, new Identifier(DnDClasses.MOD_ID, "super_strength"),
                                new SuperStrengthStatusEffect());
                Registry.register(Registry.ITEM, new Identifier(DnDClasses.MOD_ID, "staff_of_fire"),
                                new ExtendedSwordItem(new ToolMaterialFabric(),
                                                new Item.Settings().group(ItemGroup.TOOLS)));
                Registry.register(Registry.ITEM, new Identifier(DnDClasses.MOD_ID, "staff_of_lightning"),
                                new ExtendedSwordItem(new ToolMaterialFabric(),
                                                new Item.Settings().group(ItemGroup.TOOLS)));
                Registry.register(Registry.ITEM, new Identifier(DnDClasses.MOD_ID, "staff_of_ice"),
                                new ExtendedSwordItem(new ToolMaterialFabric(),
                                                new Item.Settings().group(ItemGroup.TOOLS)));

                Registry.register(Registry.ITEM, new Identifier(DnDClasses.MOD_ID, "monk_staff"),
                                new MonkStaff(new ToolMaterialFabric()));

                // Register doublejump registry.
                ServerPlayNetworking.registerGlobalReceiver(C2S_DOUBLEJUMP_EFFECTS_REQUEST_PACKET_ID,
                                (server, player, handler, buf, responseSender) -> {
                                        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                                        passedData.writeUuid(buf.readUuid());
                                        PlayerEntity entity = (PlayerEntity) player;
                                        server.execute(() -> {
                                                Stream<PlayerEntity> watchingPlayers = PlayerStream.watching(
                                                                entity.getEntityWorld(),
                                                                entity.getBlockPos());
                                                watchingPlayers.forEach(p -> {
                                                        if (p != player) {
                                                                ServerPlayNetworking.send((ServerPlayerEntity) p,
                                                                                DnDClasses.S2C_DOUBLEJUMP_EFFECTS_PACKET_ID,
                                                                                passedData);
                                                        }
                                                });
                                        });
                                });

                // Register powerup registry.
                ServerPlayNetworking.registerGlobalReceiver(C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID,
                                (server, player, handler, buf, responseSender) -> {
                                        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                                        passedData.writeUuid(buf.readUuid());
                                        System.out.println("Requested powerup...");
                                        server.execute(() -> {
                                                PowerUpEffect.play(player, ((PlayerEntityExt) (PlayerEntity) player)
                                                                .dndClassExist());
                                        });
                                });

                // Register classpick registry.
                ServerPlayNetworking.registerGlobalReceiver(C2S_CLASS_PICK_PACKET_ID,
                                (server, player, handler, buf, responseSender) -> {
                                        SetClassAttributes playerClasses = new SetClassAttributes();
                                        int bufferInteger = buf.readInt();
                                        server.execute(() -> {
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
                                                PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                                                passedData.writeInt(bufferInteger);
                                                ServerPlayNetworking.send(player,
                                                                DnDClasses.S2C_APPROVE_CLASS_PICK_PACKET_ID,
                                                                passedData);

                                                // If run with no errors declare in the NBT.
                                                ((PlayerEntityExt) (PlayerEntity) player).setDndClass(bufferInteger);
                                        });

                                });
        }

        // Helper method to register a music disk
        private static Item registerMusicDisk(String name, int comparatorSignalStrength, SoundEvent soundEvent) {
                Item musicDisk = new MusicDiscItem(7, soundEvent,
                                new FabricItemSettings().maxCount(1).group(ItemGroup.MISC)) {
                };
                Registry.register(Registry.ITEM, new Identifier(DnDClasses.MOD_ID, name), musicDisk);
                return musicDisk;
        }
}
