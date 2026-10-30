package mattonfire.dnd.magic.items;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.SkillChecks.AttackRolls;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.entity.boss.Boss;
import mattonfire.dnd.magic.MagicEffect;
import mattonfire.dnd.magic.MagicEffects;
import mattonfire.dnd.magic.MagicGear;
import mattonfire.dnd.magic.MagicItemDef;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicKind;
import mattonfire.dnd.magic.MagicTier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.block.Blocks;

/**
 * The Vorpal Sword (Legendary weapon, attunement; design 10, 8.1; docs/items/magic-items.md). A netherite sword (8
 * damage, 1.6 speed) with a built-in +{@value #PLUS}, unbreakable. On a melee swing whose d20 shows a <b>natural
 * 20</b> (the die itself: not a Champion's 19, not an auto-crit) it takes the head off anything that has one: the
 * target dies at once to {@code dndclasses:vorpal} damage, credited to the wielder, and drops its head where vanilla
 * has one. Players and bosses ({@link Boss}, {@code #dndclasses:vorpal_resistant}) keep their heads and take
 * {@value #BOSS_DICE}d8 extra instead; creatures in {@code #dndclasses:headless} (slimes, cubes, Mimics) just take
 * the hit.
 */
public class VorpalSwordItem extends NamedWeaponItem implements MagicGear.FixedPlus {
    public static final int PLUS = 3;
    /** d8s of extra slashing damage on a natural 20 against a player or a boss. */
    public static final int BOSS_DICE = 6;

    public static final RegistryKey<DamageType> VORPAL = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "vorpal"));
    /** Creatures with no head to lose: a natural 20 is just a hit. */
    public static final TagKey<EntityType<?>> HEADLESS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "headless"));
    /** Boss-like creatures that aren't {@link Boss}es: they take the extra dice instead. */
    public static final TagKey<EntityType<?>> VORPAL_RESISTANT = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "vorpal_resistant"));

    /** Netherite's stats with no durability, so the blade never wears (and isn't damageable). */
    private static final ToolMaterial UNBREAKABLE_NETHERITE = new ToolMaterial() {
        @Override
        public int getDurability() {
            return 0;
        }

        @Override
        public float getMiningSpeedMultiplier() {
            return ToolMaterials.NETHERITE.getMiningSpeedMultiplier();
        }

        @Override
        public float getAttackDamage() {
            return ToolMaterials.NETHERITE.getAttackDamage();
        }

        @Override
        public int getMiningLevel() {
            return ToolMaterials.NETHERITE.getMiningLevel();
        }

        @Override
        public int getEnchantability() {
            return ToolMaterials.NETHERITE.getEnchantability();
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    /** Heads to take off at the end of this tick: the killing blow can't land inside the hit that rolled it. */
    private static final List<Pending> PENDING = new ArrayList<>();

    private record Pending(PlayerEntity player, LivingEntity target) {
    }

    /** 8 attack damage (1 + netherite's 4 + 3), 1.6 attack speed (4 - 2.4). */
    public VorpalSwordItem(Settings settings) {
        super(UNBREAKABLE_NETHERITE, 3, -2.4F, "item.dndclasses.vorpal_sword.tooltip", true, settings);
    }

    @Override
    public int fixedPlus() {
        return PLUS;
    }

    public static void register() {
        MagicItems.register(MagicItemDef.of(ModItems.VORPAL_SWORD, MagicTier.LEGENDARY, MagicKind.WEAPON, "sword")
                .withAttunement());
        MagicEffects.register(ModItems.VORPAL_SWORD, new MagicEffect() {
            @Override
            public float modifyDealtDamage(PlayerEntity player, ItemStack stack, LivingEntity target,
                    DamageSource source, float amount) {
                if (source.getSource() != player || !source.isOf(DamageTypes.PLAYER_ATTACK) || !target.isAlive()
                        || AttackRolls.naturalOf(player, target) != 20
                        || mattonfire.dnd.classes.SkillChecks.D20.wishedThisTick(player)) { // a Moon wish isn't the die
                    return amount;
                }
                return vorpal(player, target, amount);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(VorpalSwordItem::tick);
    }

    /** Whether a natural 20 takes this creature's head (else it takes the extra dice, or nothing if headless). */
    public static boolean decapitates(LivingEntity target) {
        return !(target instanceof PlayerEntity) && !isBossLike(target) && !target.getType().isIn(HEADLESS);
    }

    private static boolean isBossLike(LivingEntity target) {
        return target instanceof Boss || target.getType().isIn(VORPAL_RESISTANT);
    }

    private static float vorpal(PlayerEntity player, LivingEntity target, float amount) {
        if (target.getType().isIn(HEADLESS) && !isBossLike(target)) {
            DnDClasses.LOGGER.info("[Magic] {}'s Vorpal Sword: {} has no head to take", player.getEntityName(),
                    target.getName().getString());
            return amount;
        }
        if (decapitates(target)) {
            // The roll is only known during the swing, so the HUD line goes now; the head comes off this tick
            D20.Roll roll = AttackRolls.rollOf(player, target);
            Text detail = Text.translatable("item.dndclasses.vorpal_sword.beheads", target.getDisplayName());
            if (roll != null)
                D20.show(player, roll, detail);
            PENDING.add(new Pending(player, target));
            return amount;
        }
        int extra = 0;
        for (int i = 0; i < BOSS_DICE; i++)
            extra += 1 + player.getRandom().nextInt(8);
        DnDClasses.LOGGER.info("[Magic] {}'s Vorpal Sword bites {}: +{} ({}d8)", player.getEntityName(),
                target.getName().getString(), extra, BOSS_DICE);
        D20.Roll roll = AttackRolls.rollOf(player, target);
        Text detail = Text.translatable("item.dndclasses.vorpal_sword.bites", extra);
        if (roll != null)
            D20.show(player, roll, detail);
        player.sendMessage(detail.copy().formatted(Formatting.LIGHT_PURPLE), true);
        player.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 0.6F);
        return amount + extra;
    }

    private static void tick(MinecraftServer server) {
        if (PENDING.isEmpty())
            return;
        List<Pending> due = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Pending p : due)
            behead(p.player(), p.target());
    }

    /** Takes the head: the creature dies to vorpal damage from the wielder and drops its head if vanilla has one. */
    private static void behead(PlayerEntity player, LivingEntity target) {
        if (target.isRemoved() || !(target.getWorld() instanceof ServerWorld world))
            return;
        String name = target.getName().getString();
        if (target.isAlive()) {
            DamageSource source = new DamageSource(
                    world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(VORPAL), player);
            target.damage(source, target.getMaxHealth() * 10.0F + 1000.0F);
        }
        if (target.isAlive()) {
            DnDClasses.LOGGER.info("[Magic] {}'s Vorpal Sword failed to behead {}", player.getEntityName(), name);
            return;
        }
        ItemStack head = headOf(target);
        if (!head.isEmpty())
            target.dropStack(head, (float) (target.getEyeY() - target.getY()));
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState()),
                target.getX(), target.getEyeY(), target.getZ(), 30, 0.2, 0.2, 0.2, 0.1);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_SLIME_SQUISH,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                SoundCategory.PLAYERS, 1.0F, 0.8F);
        player.sendMessage(Text.translatable("item.dndclasses.vorpal_sword.beheads", target.getDisplayName())
                .formatted(Formatting.DARK_RED), true);
        DnDClasses.LOGGER.info("[Magic] {}'s Vorpal Sword beheads {}{}", player.getEntityName(), name,
                head.isEmpty() ? "" : " (drops its head)");
    }

    /** The head vanilla has for this creature, or empty. */
    public static ItemStack headOf(LivingEntity target) {
        EntityType<?> type = target.getType();
        Item head = type == EntityType.ZOMBIE ? Items.ZOMBIE_HEAD
                : type == EntityType.SKELETON ? Items.SKELETON_SKULL
                : type == EntityType.WITHER_SKELETON ? Items.WITHER_SKELETON_SKULL
                : type == EntityType.CREEPER ? Items.CREEPER_HEAD
                : type == EntityType.PIGLIN ? Items.PIGLIN_HEAD
                : null;
        return head == null ? ItemStack.EMPTY : new ItemStack(head);
    }
}
