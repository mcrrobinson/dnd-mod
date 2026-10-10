package mattonfire.dnd.magic.items;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Items.InstrumentItem;
import mattonfire.dnd.magic.Attunement;
import mattonfire.dnd.magic.MagicData;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Doss Lute (Rare instrument, attunement by a Bard): plays like the Lute, but for its attuned Bard the buff
 * is one level higher (Regeneration II) and reaches {@value #RADIUS} blocks instead of 16. Animals the Bard
 * gathers with Animal Friends while carrying it get +{@value #COMPANION_HEALTH} max health. Unattuned, or
 * in anyone else's hands, it's an ordinary lute. The Bard's instrument slot plays it too when it's carried
 * ({@code BardInstrumentSlot}).
 */
public class DossLuteItem extends InstrumentItem {
    public static final double RADIUS = 24.0;
    public static final int AMPLIFIER = 1;
    public static final double COMPANION_HEALTH = 4.0;
    private static final UUID COMPANION_HEALTH_ID = UUID.fromString("5d8f1e0a-3b6c-4f2a-9e7d-1c4b8a6f2e01");

    public DossLuteItem(SoundEvent song, StatusEffect buff, Settings settings) {
        super(song, buff, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient)
            return TypedActionResult.success(stack);
        boolean bard = user instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.BARD;
        if (bard && Attunement.isActive(user, stack)) {
            playEmpowered(user);
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            return TypedActionResult.success(stack);
        }
        if (bard) // an ordinary lute until a Bard attunes to it
            MagicItemUse.ready(user, stack);
        return super.use(world, user, hand);
    }

    /** The empowered song: Regeneration II for everyone within 24 blocks. Server side, for a Bard. */
    public void playEmpowered(PlayerEntity user) {
        playAsBard(user, AMPLIFIER, RADIUS);
    }

    /** The player's Doss Lute whose magic works for them, or null. */
    public static @Nullable ItemStack activeLute(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof DossLuteItem && Attunement.isActive(player, stack))
                return stack;
        }
        return null;
    }

    /** Animal Friends: a new companion of a Bard carrying an active Doss Lute gets +4 max health. */
    public static void empowerCompanion(LivingEntity companion, PlayerEntity bard) {
        if (activeLute(bard) == null)
            return;
        EntityAttributeInstance health = companion.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health == null || health.getModifier(COMPANION_HEALTH_ID) != null)
            return;
        health.addPersistentModifier(new EntityAttributeModifier(COMPANION_HEALTH_ID, "Doss Lute",
                COMPANION_HEALTH, EntityAttributeModifier.Operation.ADDITION));
        companion.heal((float) COMPANION_HEALTH);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!MagicData.isIdentified(stack)) {
            super.appendTooltip(stack, world, tooltip, context);
            return;
        }
        tooltip.add(Text.translatable("item.dndclasses.doss_lute.tooltip",
                Text.translatable(buff.getTranslationKey()), BUFF_TICKS / 20, (int) RADIUS)
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.dndclasses.doss_lute.tooltip.companions", (int) COMPANION_HEALTH)
                .formatted(Formatting.GRAY));
    }
}
