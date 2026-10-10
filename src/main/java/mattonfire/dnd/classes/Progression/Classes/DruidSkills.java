package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.alliesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.spawnSummon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Druid;
import mattonfire.dnd.classes.ManaManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/**
 * Druid skills. Beast Bond, Photosynthesis and Tidecaller are checked in
 * {@link Druid} and {@code PlayerEntityMixin}.
 */
public class DruidSkills extends ClassSkills {
    /**
     * The root special; fired by {@code PowerUpEffect} through {@link Druid#transform}. The rank sets the
     * form's duration (today's 30 s at full rank) and the highest form tier that can be unlocked.
     */
    public static final Ranks WILD_SHAPE = Ranks.of("druid.wild_shape")
            .seconds("Duration", 15, 20, 25, 30)
            .text("Forms", "Tier I forms", "Up to tier II forms", "Up to tier III forms", "Up to tier IV forms");

    /** Wild Shape rank II: bigger hunters and climbers. */
    private static final Set<EntityType<?>> TIER_2 = Set.of(EntityType.WOLF, EntityType.GOAT, EntityType.CAT,
            EntityType.OCELOT, EntityType.AXOLOTL, EntityType.BEE, EntityType.PARROT, EntityType.STRIDER);
    /** Wild Shape rank III: big beasts and mounts. */
    private static final Set<EntityType<?>> TIER_3 = Set.of(EntityType.POLAR_BEAR, EntityType.HORSE,
            EntityType.DONKEY, EntityType.MULE, EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE,
            EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.CAMEL, EntityType.PANDA, EntityType.HOGLIN,
            EntityType.SNIFFER);

    private static final int ANIMAL_KILL_XP = 3;
    private static final int FORM_KILL_BONUS_XP = 4;
    private static final int WOLF_LIFETIME_TICKS = 60 * 20;

    public static final String MOON = "druid.moon";
    public static final String LAND = "druid.land";
    /** Primal Strike: extra damage of every attack in animal form. */
    public static final float PRIMAL_STRIKE_DAMAGE = 2.0F;
    /** Natural Recovery: seconds on natural ground per mana pip. */
    public static final int NATURAL_RECOVERY_SECONDS = 30;
    /** Natural Recovery: seconds each Land Druid has spent on natural ground towards the next pip. */
    private static final Map<UUID, Integer> NATURAL_RECOVERY = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.DRUID;
    }

    @Override
    public List<String> subclassIds() {
        return List.of("druid.moon", "druid.land");
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("druid.wild_shape", "Wild Shape", "Turn into an animal you've killed and unlocked. Sneak + power-up picks the form.",
                        "minecraft:rabbit_foot", 9, 0, 1, 3),
                // Circle of the Moon
                passive("druid.hardy_form", "Hardy Form", "Resistance while in animal form.",
                        "minecraft:turtle_helmet", 1, 2, 3, "druid.wild_shape"),
                active("druid.thorn_burst", "Thorn Burst",
                        "Thorns hurt mobs within 6 blocks and root them for 4 seconds.", "minecraft:sweet_berries",
                        4, 1, 2, 2, "druid.hardy_form"),
                passive("druid.beast_bond", "Beast Bond", "Tamed animals give up to 10 extra hearts instead of 5.",
                        "minecraft:lead", 1, 2, 1, "druid.thorn_burst"),
                // Circle of the Land
                passive("druid.tidecaller", "Tidecaller", "You can swim again.", "minecraft:heart_of_the_sea", 1, 0,
                        3, "druid.wild_shape"),
                active("druid.regrowth", "Regrowth",
                        "Regeneration II for you, nearby players and your animals for 8 seconds.",
                        "minecraft:glow_berries", 4, 1, 0, 2, "druid.tidecaller"),
                passive("druid.photosynthesis", "Photosynthesis", "Bright light also feeds you.",
                        "minecraft:oak_sapling", 1, 0, 1, "druid.regrowth"),
                active("druid.call_of_the_wild", "Call of the Wild", "Three wolves fight for you for a minute.",
                        "minecraft:bone", 9, 2, 1, 0, "druid.beast_bond", "druid.photosynthesis"));
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "druid.thorn_burst" -> {
                for (LivingEntity mob : hostilesNear(player, 6)) {
                    mob.damage(world.getDamageSources().thorns(player), 3.0F);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 3), player);
                }
                effects(player, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PLACE, ParticleTypes.COMPOSTER, 40);
            }
            case "druid.regrowth" -> {
                for (LivingEntity ally : alliesNear(player, 8)) {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1), player);
                    world.spawnParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1, ally.getZ(), 3, 0.4,
                            0.4, 0.4, 0);
                }
                effects(player, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, ParticleTypes.HAPPY_VILLAGER, 20);
            }
            case "druid.call_of_the_wild" -> {
                for (int i = 0; i < 3; i++) {
                    WolfEntity wolf = EntityType.WOLF.create(world);
                    if (wolf == null)
                        continue;
                    double angle = i * Math.PI * 2 / 3;
                    wolf.refreshPositionAndAngles(player.getX() + Math.cos(angle) * 1.5, player.getY(),
                            player.getZ() + Math.sin(angle) * 1.5, player.getYaw(), 0);
                    wolf.setOwner(player);
                    spawnSummon(world, wolf, WOLF_LIFETIME_TICKS);
                }
                effects(player, SoundEvents.ENTITY_WOLF_HOWL, ParticleTypes.POOF, 20);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean usesBestiary() {
        return true;
    }

    /** Animals and wild beasts (the {@code #dndclasses:druid_forms} tag), like the Druid's old kill list. */
    @Override
    public boolean learnsFrom(LivingEntity killed) {
        return killed instanceof AnimalEntity || killed.getType().isIn(Druid.DRUID_FORMS);
    }

    /**
     * Form tiers: small animals at rank I, then {@link #TIER_2}, {@link #TIER_3}, and the mod's own
     * creatures (the Owlbear, and any from other mods) at rank IV. Uses no tags, so the client agrees.
     */
    @Override
    public int bestiaryRank(EntityType<?> type) {
        if (TIER_2.contains(type))
            return 2;
        if (TIER_3.contains(type))
            return 3;
        if (!Registries.ENTITY_TYPE.getId(type).getNamespace().equals("minecraft"))
            return 4;
        return 1;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        int xp = killed instanceof AnimalEntity ? ANIMAL_KILL_XP : 0;
        if (killed instanceof Monster && Druid.isTransformed(player)) {
            xp += FORM_KILL_BONUS_XP;
        }
        return xp;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        // Primal Strike: the form's own attacks, not arrows or skills.
        if (progress.hasSubclass(MOON) && source.getSource() == player && Druid.isTransformed(player)) {
            return amount + PRIMAL_STRIKE_DAMAGE;
        }
        return amount;
    }

    /** Natural Recovery's ground: grass, moss, leaves (and the plants on them). */
    public static boolean onNaturalGround(PlayerEntity player) {
        if (!player.isOnGround())
            return false;
        BlockState below = player.getWorld().getBlockState(player.getBlockPos().down());
        BlockState feet = player.getWorld().getBlockState(player.getBlockPos());
        return isNatural(below) || feet.isOf(Blocks.MOSS_CARPET) || isNatural(feet);
    }

    private static boolean isNatural(BlockState state) {
        return state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.MOSS_BLOCK) || state.isOf(Blocks.MOSS_CARPET)
                || state.isIn(BlockTags.LEAVES);
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (progress.hasPassive("druid.hardy_form") && Druid.isTransformed(player)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, true));
        }
        // Natural Recovery: time on natural ground adds up; every 30 seconds of it is a mana pip.
        if (progress.hasSubclass(LAND) && player.isAlive() && onNaturalGround(player)) {
            int seconds = NATURAL_RECOVERY.getOrDefault(player.getUuid(), 0) + 1;
            if (seconds >= NATURAL_RECOVERY_SECONDS && !ManaManager.hasFullMana(player)) {
                ManaManager.regenerateMana(player);
                mattonfire.dnd.classes.DnDClasses.LOGGER.debug("[Subclass] Natural Recovery: {} +1 mana, now {}",
                        player.getEntityName(), ManaManager.getMana(player));
                ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(),
                        player.getY() + 0.2, player.getZ(), 6, 0.4, 0.1, 0.4, 0);
                seconds = 0;
            }
            NATURAL_RECOVERY.put(player.getUuid(), Math.min(seconds, NATURAL_RECOVERY_SECONDS));
        }
    }

    @Override
    public void forget(ServerPlayerEntity player) {
        NATURAL_RECOVERY.remove(player.getUuid());
    }
}
