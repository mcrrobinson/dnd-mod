package mattonfire.dnd.classes.Client;

import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.SetPlayerClass;
import mattonfire.dnd.classes.Misc.DoubleJumpEffect;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.particle.ModParticles;
import mattonfire.dnd.particle.TranslucentFlameParticle;
import mattonfire.dnd.classes.Client.Hud.AchievementMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
    private static final KeyBinding OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.achievement_menu.open", GLFW.GLFW_KEY_O, "category.achievement_menu"));

    private static void handleClassQuery(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        int classID = buf.readInt();
        client.execute(() -> {
            if (client.player instanceof PlayerEntityExt) {
                ((PlayerEntityExt) (PlayerEntity) client.player).setDndClass(DndCharacter.fromValue(classID));
                SetPlayerClass.setPlayerClass(client, client.player, classID);
            }
        });
    }

    private static void receiveDoubleJumpEffectsRequest(MinecraftClient client, ClientPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender responseSender) {
        client.execute(() -> {
            PlayerEntity effectPlayer = client.player.getEntityWorld().getPlayerByUuid(buf.readUuid());
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
        if (client.player != null) {
            ((IEntityDataSaver) client.player).getPersistentData().putInt("mana", 0);
        }
    }

    private void handleFireBreathPacket(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        boolean active = buf.readBoolean();
        if (active) {
            fireBreathEndTick = buf.readLong();
            isBreathingFire = true;
        } else {
            isBreathingFire = false;
        }
    }

    private void handleWizardPowerupPacket(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf,
            PacketSender responseSender) {
        MySphereRenderState.shouldRenderSphere = true;
        MySphereRenderState.spherePos = client.player.getPos().add(0,
                client.player.getStandingEyeHeight() / 2.0, 0);
        MySphereRenderState.startTick = client.world.getTime();
        client.execute(() -> {
            if (client.world != null && client.player != null) {
                client.world.playSound(
                        client.player.getPos().getX(), client.player.getPos().getY(), client.player.getPos().getZ(),
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
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.WYVERN, mattonfire.dnd.client.renderer.WyvernRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.LIGHTNING_CHASER, mattonfire.dnd.client.renderer.LightningChaserRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.RIVER_PIKEHORN, mattonfire.dnd.client.renderer.RiverPikehornRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.MAGMAMUNCHER, mattonfire.dnd.client.renderer.MagmamuncherRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(mattonfire.dnd.entity.ModEntityTypes.GOBLIN_WARRIOR, mattonfire.dnd.client.renderer.GoblinWarriorRenderer::new);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_LOAD.register(mattonfire.dnd.entity.DragonPartTracker::onLoad);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register(mattonfire.dnd.entity.DragonPartTracker::onUnload);
        DevScript.register();
        mattonfire.dnd.classes.Client.Music.EventMusic.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU_KEY.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AchievementMenu());
                }
            }
        });

        WorldRenderEvents.END.register(context -> {
            if (MySphereRenderState.shouldRenderSphere) {
                long now = MinecraftClient.getInstance().world.getTime();
                float progress = (now - MySphereRenderState.startTick) / (float) MySphereRenderState.DURATION_TICKS;
                if (progress >= 1.0f) {
                    MySphereRenderState.shouldRenderSphere = false;
                    return;
                }
                float radius = 0.1f + progress * 49.9f; // Expands from 1 to 5 blocks
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

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_SYNC_MANA,
                DndClassesClient::setMana);

        ClientPlayNetworking.registerGlobalReceiver(DnDClasses.S2C_POWERUP_EFFECTS_PACKET_ID,
                DndClassesClient::removeManor);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && isBreathingFire(client.player)) {
                // Example: spawn a semi-transparent red particle in front of the player
                Vec3d look = client.player.getRotationVec(1.0F);
                Vec3d start = client.player.getPos().add(0, client.player.getStandingEyeHeight(), 0);
                for (int i = 1; i <= 5; i++) {
                    Vec3d pos = start.add(look.multiply(i));
                    client.world.addParticle(ModParticles.TRANSLUCENT_FLAME, pos.x, pos.y, pos.z, 0, 0, 0);
                }
            }
        });

        ParticleFactoryRegistry.getInstance().register(
                ModParticles.TRANSLUCENT_FLAME,
                TranslucentFlameParticle.Factory::new);

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
        return isBreathingFire && (player.age < fireBreathEndTick);
    }
}