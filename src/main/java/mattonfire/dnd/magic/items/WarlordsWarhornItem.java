package mattonfire.dnd.magic.items;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.entity.GoblinWarriorEntity;
import mattonfire.dnd.magic.MagicData;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Warlord's Warhorn (Rare wondrous item, no attunement): right-click to sound it. You and your party members
 * within {@value #RADIUS} blocks get Strength I and Speed I for {@value #BUFF_SECONDS} s, and goblins within
 * {@value #RADIUS} blocks are Frightened for {@value #FRIGHTEN_SECONDS} s (half damage, and they flee). A
 * {@value #COOLDOWN_SECONDS} s cooldown kept on the item.
 */
public class WarlordsWarhornItem extends Item {
    public static final double RADIUS = 16.0;
    public static final int BUFF_SECONDS = 15;
    public static final int FRIGHTEN_SECONDS = 6;
    public static final int COOLDOWN_SECONDS = 120;
    private static final String READY_KEY = "hornReadyAt";

    public WarlordsWarhornItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || !(player instanceof ServerPlayerEntity server))
            return TypedActionResult.success(stack, true);
        if (!MagicItemUse.ready(player, stack))
            return TypedActionResult.fail(stack);
        long now = world.getTime();
        if (MagicData.readyIn(stack, READY_KEY, now) > 0 && !player.getAbilities().creativeMode) {
            sendCooldown(player, stack);
            return TypedActionResult.fail(stack);
        }
        MagicData.setReadyAt(stack, READY_KEY, now + COOLDOWN_SECONDS * 20L);
        player.getItemCooldownManager().set(this, COOLDOWN_SECONDS * 20);

        List<ServerPlayerEntity> allies = new ArrayList<>();
        allies.add(server);
        allies.addAll(PartyManager.nearbyMembers(server, RADIUS));
        for (ServerPlayerEntity ally : allies) {
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, BUFF_SECONDS * 20, 0), player);
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, BUFF_SECONDS * 20, 0), player);
        }
        int frightened = 0;
        for (GoblinWarriorEntity goblin : world.getEntitiesByClass(GoblinWarriorEntity.class,
                player.getBoundingBox().expand(RADIUS),
                g -> g.isAlive() && g.squaredDistanceTo(player) <= RADIUS * RADIUS)) {
            if (mattonfire.dnd.classes.Conditions.Conditions.of(goblin, mattonfire.dnd.classes.Conditions.Condition.FRIGHTENED).source(player)
                    .ticks(FRIGHTEN_SECONDS * 20).apply() != null) {
                frightened++;
                if (goblin.getTarget() == player)
                    goblin.setTarget(null);
            }
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.PLAYERS,
                4.0F, 1.0F);
        ((ServerWorld) world).spawnParticles(ParticleTypes.NOTE, player.getX(), player.getEyeY() + 0.5,
                player.getZ(), 6, 0.6, 0.3, 0.6, 1.0);
        player.sendMessage(Text.translatable("item.dndclasses.warlords_warhorn.sounded", allies.size(), frightened)
                .formatted(Formatting.GOLD), true);
        DnDClasses.LOGGER.info("[Magic] {} sounded the Warlord's Warhorn: {} allies rallied, {} goblins frightened",
                player.getEntityName(), allies.size(), frightened);
        return TypedActionResult.success(stack, false);
    }

    /**
     * Vanilla skips {@code Item.use} while the item cooldown sweep is showing, so a second use inside the
     * cooldown never reached {@link #use}. This callback runs first (server side) and says how long is left.
     */
    public static void registerCooldownMessage() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient || !(stack.getItem() instanceof WarlordsWarhornItem horn)
                    || !player.getItemCooldownManager().isCoolingDown(horn) || player.isSpectator())
                return TypedActionResult.pass(stack);
            sendCooldown(player, stack);
            return TypedActionResult.fail(stack);
        });
    }

    /** "Your lungs need a rest (N s)" on the action bar, N from {@value #READY_KEY} on the item. */
    private static void sendCooldown(PlayerEntity player, ItemStack stack) {
        long left = MagicData.readyIn(stack, READY_KEY, player.getWorld().getTime());
        if (left <= 0) // cooldown sweep without a stored time (e.g. /item replace): use the sweep instead
            left = (long) (player.getItemCooldownManager().getCooldownProgress(stack.getItem(), 0.0F)
                    * COOLDOWN_SECONDS * 20);
        player.sendMessage(Text.translatable("item.dndclasses.warlords_warhorn.cooldown",
                Math.max(1, (left + 19) / 20)).formatted(Formatting.GRAY), true);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack))
            return;
        tooltip.add(Text.translatable("item.dndclasses.warlords_warhorn.tooltip").formatted(Formatting.GRAY));
    }
}
