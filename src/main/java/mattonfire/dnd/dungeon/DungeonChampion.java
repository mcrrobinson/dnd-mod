package mattonfire.dnd.dungeon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.entity.GelatinousCubeEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A dungeon's mid-boss: an ordinary mob from the theme's pool made tougher, named, and given a
 * white boss bar. The mob's own class doesn't change: the room's ward ticks the bar
 * ({@link #tick}), so the Gelatinous Cube, Owlbear and Mimic stay as they are.
 *
 * Max health is x3 for one player, +0.3 of the base per extra player (4 players: x3.9); damage
 * +25%, knockback resistance +0.3.
 */
public final class DungeonChampion {
    public static final String TAG = "dndclasses.dungeon_champion";
    public static final double HEALTH_SOLO = 3.0;
    public static final double HEALTH_PER_EXTRA_PLAYER = 0.3;
    public static final double DAMAGE_BONUS = 0.25;
    public static final double KNOCKBACK_RESISTANCE = 0.3;
    public static final double BAR_RANGE = 32.0;

    private static final UUID HEALTH_ID = UUID.fromString("c4a1d7e2-55b0-4c1e-a7f3-0d9e2b6c1a01");
    private static final UUID DAMAGE_ID = UUID.fromString("c4a1d7e2-55b0-4c1e-a7f3-0d9e2b6c1a02");
    private static final UUID KNOCKBACK_ID = UUID.fromString("c4a1d7e2-55b0-4c1e-a7f3-0d9e2b6c1a03");

    /** Live bars by champion (server thread only; rebuilt from the tag after a restart). */
    private static final Map<UUID, ServerBossBar> BARS = new HashMap<>();

    private DungeonChampion() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BARS.clear());
    }

    public static double healthMult(int partySize) {
        return HEALTH_SOLO + HEALTH_PER_EXTRA_PLAYER * (MathHelper.clamp(partySize, 1, EncounterBuilder.MAX_PARTY) - 1);
    }

    /** Makes {@code mob} the room's champion for a party of {@code partySize}. */
    public static void make(ServerWorld world, MobEntity mob, DungeonEncounterPools.Champion champion, int partySize) {
        add(mob, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, healthMult(partySize) - 1.0, EntityAttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_ID, DAMAGE_BONUS, EntityAttributeModifier.Operation.MULTIPLY_BASE);
        add(mob, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_ID, KNOCKBACK_RESISTANCE, EntityAttributeModifier.Operation.ADDITION);
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Text.translatable(champion.nameKey()));
        mob.setCustomNameVisible(false);
        mob.addCommandTag(TAG);
        if (mob instanceof GelatinousCubeEntity cube && champion.absorbedLoot() != null && champion.absorbedStacks() > 0) {
            // The Ossuary Cube starts with a few things already inside it, which it drops when it dies.
            LootTable table = world.getServer().getLootManager().getTable(champion.absorbedLoot());
            List<ItemStack> stacks = table.generateLoot(new LootContext.Builder(world).random(world.getRandom())
                    .parameter(LootContextParameters.ORIGIN, mob.getPos()).build(LootContextTypes.CHEST));
            for (int i = 0; i < Math.min(champion.absorbedStacks(), stacks.size()); i++) {
                cube.absorb(stacks.get(i));
            }
        }
    }

    private static void add(LivingEntity mob, net.minecraft.entity.attribute.EntityAttribute attribute, UUID id, double amount,
                            EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = mob.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPersistentModifier(new EntityAttributeModifier(id, "dndclasses.champion", amount, operation));
        }
    }

    public static boolean isChampion(Entity entity) {
        return entity.getCommandTags().contains(TAG);
    }

    /** Updates the champion's bar: its health, and the players within {@link #BAR_RANGE} blocks. */
    public static void tick(ServerWorld world, LivingEntity champion) {
        ServerBossBar bar = BARS.computeIfAbsent(champion.getUuid(), id -> {
            ServerBossBar b = new ServerBossBar(champion.getDisplayName(), BossBar.Color.WHITE, BossBar.Style.NOTCHED_10);
            b.setDarkenSky(false);
            return b;
        });
        bar.setPercent(MathHelper.clamp(champion.getHealth() / champion.getMaxHealth(), 0.0F, 1.0F));
        Vec3d pos = champion.getPos();
        for (ServerPlayerEntity player : List.copyOf(bar.getPlayers())) {
            if (player.isRemoved() || player.getWorld() != world || player.squaredDistanceTo(pos) > BAR_RANGE * BAR_RANGE) {
                bar.removePlayer(player);
            }
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!player.isSpectator() && player.squaredDistanceTo(pos) <= BAR_RANGE * BAR_RANGE) {
                bar.addPlayer(player);
            }
        }
    }

    /** Takes the champion's bar down (it died, or the fight was abandoned). */
    public static void removeBar(UUID champion) {
        ServerBossBar bar = BARS.remove(champion);
        if (bar != null) {
            bar.clearPlayers();
        }
    }
}
