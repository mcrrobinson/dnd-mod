package mattonfire.dnd.classes.Client;

import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Client.Hud.ClassSelectionHud;
import mattonfire.dnd.classes.Client.Keybinds.ModKeybinds;
import mattonfire.dnd.classes.Misc.DoubleJumpEffect;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.particle.ModParticles;
import mattonfire.dnd.particle.TranslucentFlameParticle;
import mattonfire.dnd.classes.Client.Hud.SkillTreeScreen;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import mattonfire.dnd.classes.Client.Render.Color;
import mattonfire.dnd.classes.Client.Render.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class DndClassesClient implements ClientModInitializer {
    public static final MinecraftClient MC = MinecraftClient.getInstance();
    public static Color chestESPColor = new Color(1, 1, 0, 1);

    private boolean isBreathingFire = false;
    private long fireBreathEndTick = 0;
    /** Beam length in blocks, sent by the server so the flames match the rank's reach. */
    private int fireBreathReach = 0;
    private net.minecraft.world.World fireBreathWorld = null;
    private static final KeyBinding OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.dnd-classes.skill-tree", GLFW.GLFW_KEY_O, "category.dnd-classes.dnd-classes"));

    private static void handleClassQuery(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        int classID = buf.readInt();
        client.execute(() -> {
            if (client.player instanceof PlayerEntityExt ext) {
                DndCharacter dndClass = DndCharacter.fromValue(classID);
                ext.setDndClass(dndClass);
                // The server applies the class's stats; the client only opens the picker.
                if (dndClass == DndCharacter.NONE) {
                    client.setScreen(new ModKeybinds(new ClassSelectionHud()));
                }
            }
        });
    }

    private static void receiveDoubleJumpEffectsRequest(MinecraftClient client, ClientPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender responseSender) {
        // Read before execute: the buffer is released once this handler returns
        UUID effectPlayerUuid = buf.readUuid();
        client.execute(() -> {
            if (client.player == null) {
                return;
            }
            PlayerEntity effectPlayer = client.player.getEntityWorld().getPlayerByUuid(effectPlayerUuid);
            if (effectPlayer != null) {
                DoubleJumpEffect.play(client.player, effectPlayer);
            }
        });
    }

    private static void receiveLungeEffectsRequest(MinecraftClient client, ClientPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender responseSender) {
        UUID effectPlayerUuid = buf.readUuid();
        client.execute(() -> {
            if (client.player == null) {
                return;
            }
            PlayerEntity effectPlayer = client.player.getEntityWorld().getPlayerByUuid(effectPlayerUuid);
            if (effectPlayer != null) {
                DoubleJumpEffect.play(client.player, effectPlayer);
            }
        });
    }

    private static void setMana(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        int amount = buf.readInt();

        client.execute(() -> { // Ensure this runs on the main client thread
            if (client.player != null) {
                System.out.println("Setting mana to " + amount);
                ((IEntityDataSaver) client.player).getPersistentData().putInt("mana", amount);
            } else {
                System.out.println("Player is not yet initialized, deferring mana update...");
            }
        });
    }

    private static void removeManor(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        // The server syncs the mana left separately.
        // The server only sends this when the special actually fired
        client.execute(() -> mattonfire.dnd.classes.Client.Music.MusicStings.onSpecialFired(client));
    }

    private void handleFireBreathPacket(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        boolean active = buf.readBoolean();
        long endTick = active ? buf.readLong() : 0;
        int reach = active ? buf.readVarInt() : 0;
        client.execute(() -> {
            // The end is in world time; remember the world so flames don't carry into the next one.
            isBreathingFire = active;
            fireBreathEndTick = endTick;
            fireBreathReach = reach;
            fireBreathWorld = client.world;
        });
    }

    private void handleWizardPowerupPacket(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        Vec3d pos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float reach = buf.readFloat();
        client.execute(() -> {
            if (client.world != null && client.player != null) {
                MySphereRenderState.shouldRenderSphere = true;
                MySphereRenderState.spherePos = pos;
                MySphereRenderState.maxRadius = reach;
                MySphereRenderState.startTick = client.world.getTime();
                client.world.playSound(pos.x, pos.y, pos.z,
                        ModSounds.WIZARD_EXPLOSION,
                        SoundCategory.BLOCKS,
                        4.0F,
                        1.0F,
                        false);
            }

        });
    }

    @Override
    public void onInitializeClient() {
        mattonfire.dnd.classes.Items.ClassGuidebookItem.clientOpener = mattonfire.dnd.classes.Client.Hud.ClassGuidebookScreen::open;
        mattonfire.dnd.classes.Client.Render.LayeredArmorRenderer.registerAll();
        // The model has see-through quads like the vanilla brewing stand, which draw black on the default solid layer
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                mattonfire.dnd.classes.Registry.ModBlocks.FAST_BREWING_STAND_BLOCK, net.minecraft.client.render.RenderLayer.getCutout());
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.WYVERN, mattonfire.dnd.client.renderer.WyvernRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.LIGHTNING_CHASER, mattonfire.dnd.client.renderer.LightningChaserRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.FROST_DRAKE, mattonfire.dnd.client.renderer.FrostDrakeRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.EMBER_WYVERN, mattonfire.dnd.client.renderer.EmberWyvernRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.BONE_WYVERN, mattonfire.dnd.client.renderer.BoneWyvernRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.RIVER_PIKEHORN, mattonfire.dnd.client.renderer.RiverPikehornRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.MAGMAMUNCHER, mattonfire.dnd.client.renderer.MagmamuncherRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.MAGMAMUNCHER_ALPHA, mattonfire.dnd.client.renderer.MagmamuncherAlphaRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.GOBLIN_WARRIOR, mattonfire.dnd.client.renderer.GoblinWarriorRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.GOBLIN_WARLORD, mattonfire.dnd.client.renderer.GoblinWarlordRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.LICH, mattonfire.dnd.client.renderer.LichRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.PHYLACTERY, mattonfire.dnd.client.renderer.PhylacteryRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.HOBBIT, mattonfire.dnd.client.renderer.HobbitRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.tavern.Tavern.INNKEEPER, mattonfire.dnd.client.renderer.HobbitRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.MOUNTAIN_DWARF, mattonfire.dnd.client.renderer.MountainDwarfRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.MIMIC, mattonfire.dnd.client.renderer.MimicRenderer::new);
        mattonfire.dnd.client.MimicTexture.register();
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.OWLBEAR, mattonfire.dnd.client.renderer.OwlbearRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.GELATINOUS_CUBE, mattonfire.dnd.client.renderer.GelatinousCubeRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.BEHOLDER, mattonfire.dnd.client.renderer.BeholderRenderer::new);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_LOAD.register(mattonfire.dnd.entity.DragonPartTracker::onLoad);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register(mattonfire.dnd.entity.DragonPartTracker::onUnload);
        DevScript.register();
        mattonfire.dnd.classes.Client.Hud.PartyHud.register();
        mattonfire.dnd.classes.Client.Hud.DiceRollHud.register();
        mattonfire.dnd.classes.Client.Hud.ObstacleHintHud.register();
        // Arcane Seals are translucent glyph walls
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
                net.minecraft.client.render.RenderLayer.getTranslucent(),
                mattonfire.dnd.classes.Obstacles.ObstacleTypes.LESSER_ARCANE_SEAL_BLOCK,
                mattonfire.dnd.classes.Obstacles.ObstacleTypes.GREATER_ARCANE_SEAL_BLOCK);
        mattonfire.dnd.classes.Client.Hud.InstrumentSlotHud.register();
        mattonfire.dnd.classes.Client.Music.EventMusic.register();
        mattonfire.dnd.classes.Client.Music.MusicStings.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU_KEY.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new SkillTreeScreen(false));
                }
            }
        });

        WorldRenderEvents.END.register(context -> {
            if (MySphereRenderState.shouldRenderSphere) {
                if (context.world() == null) {
                    MySphereRenderState.shouldRenderSphere = false;
                    return;
                }
                long now = context.world().getTime();
                float progress = (now - MySphereRenderState.startTick) / (float) MySphereRenderState.DURATION_TICKS;
                // Below 0 if the world clock went backwards (another world, /time set)
                if (progress < 0.0f || progress >= 1.0f) {
                    MySphereRenderState.shouldRenderSphere = false;
                    return;
                }
                float radius = 0.1f + progress * (MySphereRenderState.maxRadius - 0.1f); // Grows to the blast's reach
                int baseColor = 0xFFffec64;
                int originalAlpha = 0xFF;
                int newAlpha = (int) ((1.0f - progress) * originalAlpha);
                int color = (newAlpha << 24) | (baseColor & 0x00FFFFFF);
                RenderUtils.renderSolidSphere(
                        context.matrixStack(),
                        MySphereRenderState.spherePos,
                        radius,
                        color,
                        context.camera().getPos(),
                        24,
                        32);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_CLASS_QUERY_PACKET_ID,
                DndClassesClient::handleClassQuery);

        // Once a class is requested and approved this will set the player class.
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_APPROVE_CLASS_PICK_PACKET_ID,
                DndClassesClient::handleClassQuery);

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_WARLOCK_FIREBREATH, this::handleFireBreathPacket);
        // Leaving a world mid-effect must not carry the flame particles or the sphere into the next one
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            isBreathingFire = false;
            fireBreathEndTick = 0;
            fireBreathWorld = null;
            MySphereRenderState.shouldRenderSphere = false;
        }));
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_WIZARD_EFFECTS_PACKET_ID,
                this::handleWizardPowerupPacket);

        KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dnd-classes.power-up", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_Z, // The keycode of the key
                "category.dnd-classes.dnd-classes" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                // Power effect request
                PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                passedData.writeUuid(client.player.getUuid());
                ClientPlayNetworking.send(DnDClasses.C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID, passedData);
            }
        });

        // The response from the server to make the special effects.
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_DOUBLEJUMP_EFFECTS_PACKET_ID,
                DndClassesClient::receiveDoubleJumpEffectsRequest);

        // The response from the server to make the special effects.
        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_LUNGE_EFFECTS_PACKET_ID,
                DndClassesClient::receiveLungeEffectsRequest);

        ClientPlayNetworking.registerGlobalReceiver(Progression.S2C_SYNC, (client, handler, buf, sender) -> {
            ClassProgress progress = ClassProgress.read(buf);
            client.execute(() -> ClassProgress.client = progress);
        });
        ClientPlayNetworking.registerGlobalReceiver(Progression.S2C_OPEN_ATTUNEMENT,
                (client, handler, buf, sender) -> client.execute(() -> client.setScreen(new SkillTreeScreen(true))));

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_SYNC_MANA,
                DndClassesClient::setMana);

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_POWERUP_EFFECTS_PACKET_ID,
                DndClassesClient::removeManor);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && isBreathingFire(client.player)) {
                // Semi-transparent flames along the beam, as far as it reaches
                Vec3d look = client.player.getRotationVec(1.0F);
                Vec3d start = client.player.getPos().add(0, client.player.getStandingEyeHeight(), 0);
                for (int i = 1; i <= fireBreathReach; i++) {
                    Vec3d pos = start.add(look.multiply(i));
                    client.world.addParticle(ModParticles.TRANSLUCENT_FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
                }
            }
        });

        ParticleFactoryRegistry.getInstance().register(
                ModParticles.TRANSLUCENT_FLAME,
                TranslucentFlameParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(
                ModParticles.DRAGON_FLAME,
                mattonfire.dnd.particle.DragonFlameParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(
                ModParticles.DRAGON_FROST,
                mattonfire.dnd.particle.DragonFlameParticle.FrostFactory::new);

        ModelPredicateProviderRegistry.register(Items.BOW, new Identifier("pull"),
                (stack, world, entity, seed) -> {
                    if (entity == null)
                        return 0.0F;

                    int useTicks = entity.getItemUseTimeLeft();
                    int maxUseTicks = stack.getMaxUseTime();
                    float drawSpeed = 20.0F;

                    // Instead of dividing by 20 (default), divide by 5 (your faster draw)

                    if (entity instanceof PlayerEntityExt playerEntityExt) {
                        drawSpeed = playerEntityExt.getDndClass() == DndCharacter.RANGER ? 3
                                : 20;
                    }
                    float pull = (float) (maxUseTicks - useTicks) / drawSpeed;

                    if (pull > 1.0F) {
                        pull = 1.0F;
                    }

                    return pull;
                });
        // EntityRendererRegistry.register(EntityType.PLAYER, (ctx) -> new HybridPlayerRenderer(ctx, false));
        // model

    }

    private boolean isBreathingFire(PlayerEntity player) {
        return isBreathingFire && player.getWorld() == fireBreathWorld
                && player.getWorld().getTime() < fireBreathEndTick;
    }
}