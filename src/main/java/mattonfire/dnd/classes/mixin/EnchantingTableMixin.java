package mattonfire.dnd.classes.mixin;

import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Lists;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Enchanting table rules: only Artificers can roll the mod's enchantments, mod enchantments
 * only roll on items they work on (Tree Feller: axes; Grid Miner: pickaxes and shovels), and
 * Alchemists can't enchant.
 *
 * The filter replaces the handler's {@code generateEnchantments}, which vanilla uses both for
 * the hint shown on each button and for the roll on click, so the preview always matches the
 * result.
 */
@Mixin(EnchantmentScreenHandler.class)
public class EnchantingTableMixin {
    @Shadow
    @Final
    private Random random;
    @Shadow
    @Final
    private Property seed;

    /** The player using the table, or null if it isn't a {@link PlayerEntityExt}. */
    @Unique
    private PlayerEntity dndclasses$player;

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V", at = @At("TAIL"))
    private void dndclasses$rememberPlayer(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context,
            CallbackInfo ci) {
        this.dndclasses$player = playerInventory.player;
    }

    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void dndclasses$blockAlchemists(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.ALCHEMIST) {
            player.sendMessage(Text.literal("Alchemists cannot enchant items.").formatted(Formatting.RED), true);
            cir.setReturnValue(false);
        }
    }

    /** Same as vanilla's generateEnchantments, with the class and item filters applied. */
    @Inject(method = "generateEnchantments", at = @At("HEAD"), cancellable = true)
    private void dndclasses$generateEnchantments(ItemStack stack, int slot, int level,
            CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        boolean powerfulUser = dndclasses$player instanceof PlayerEntityExt ext
                && ext.getDndClass() == DndCharacter.ARTIFICER;
        this.random.setSeed((long) (this.seed.get() + slot));
        List<EnchantmentLevelEntry> list = generate(this.random, stack, level, powerfulUser);
        if (stack.isOf(Items.BOOK) && list.size() > 1) {
            list.remove(this.random.nextInt(list.size()));
        }
        cir.setReturnValue(list);
    }

    /** Vanilla EnchantmentHelper.generateEnchantments, using {@link #possibleEntries}. */
    @Unique
    private static List<EnchantmentLevelEntry> generate(Random random, ItemStack stack, int level,
            boolean powerfulUser) {
        List<EnchantmentLevelEntry> list = Lists.newArrayList();
        int enchantability = stack.getItem().getEnchantability();
        if (enchantability <= 0) {
            return list;
        }
        level += 1 + random.nextInt(enchantability / 4 + 1) + random.nextInt(enchantability / 4 + 1);
        float f = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
        level = MathHelper.clamp(Math.round((float) level + (float) level * f), 1, Integer.MAX_VALUE);
        List<EnchantmentLevelEntry> possible = possibleEntries(level, stack, powerfulUser);
        if (!possible.isEmpty()) {
            Weighting.getRandom(random, possible).ifPresent(list::add);
            while (random.nextInt(50) <= level) {
                if (!list.isEmpty()) {
                    removeConflicts(possible, Util.getLast(list));
                }
                if (possible.isEmpty()) {
                    break;
                }
                Weighting.getRandom(random, possible).ifPresent(list::add);
                level /= 2;
            }
        }
        return list;
    }

    /** Vanilla EnchantmentHelper.getPossibleEntries (no treasure), plus the mod's filters. */
    @Unique
    private static List<EnchantmentLevelEntry> possibleEntries(int power, ItemStack stack, boolean powerfulUser) {
        List<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        boolean book = stack.isOf(Items.BOOK);
        for (Enchantment enchantment : Registries.ENCHANTMENT) {
            if (enchantment.isTreasure() || !enchantment.isAvailableForRandomSelection())
                continue;
            // The target picks the item group; isAcceptableItem lets an enchantment narrow it
            // (Tree Feller: DIGGER, axes only). Vanilla overrides only widen it, so they're unaffected.
            if (!book && !(enchantment.target.isAcceptableItem(item) && enchantment.isAcceptableItem(stack)))
                continue;
            if (!powerfulUser && isArtificerOnly(enchantment))
                continue;
            for (int i = enchantment.getMaxLevel(); i > enchantment.getMinLevel() - 1; --i) {
                if (power >= enchantment.getMinPower(i) && power <= enchantment.getMaxPower(i)) {
                    list.add(new EnchantmentLevelEntry(enchantment, i));
                    break;
                }
            }
        }
        return list;
    }

    @Unique
    private static boolean isArtificerOnly(Enchantment enchantment) {
        return enchantment == ModEnchantments.LUNGE_ENCHANTMENT
                || enchantment == ModEnchantments.GRID_MINER_ENCHANTMENT
                || enchantment == ModEnchantments.TREE_FELLER_ENCHANTMENT
                || enchantment == ModEnchantments.INVULNERABILITY_ENCHANTMENT
                || enchantment == ModEnchantments.RETURNING_ENCHANTMENT
                || enchantment == ModEnchantments.VAMPIRIC_ENCHANTMENT
                || enchantment == ModEnchantments.SMITE_DRAGONS_ENCHANTMENT
                || enchantment == ModEnchantments.FEATHERFALL_ENCHANTMENT;
    }

    @Unique
    private static void removeConflicts(List<EnchantmentLevelEntry> possibleEntries, EnchantmentLevelEntry picked) {
        Iterator<EnchantmentLevelEntry> iterator = possibleEntries.iterator();
        while (iterator.hasNext()) {
            if (!picked.enchantment.canCombine(iterator.next().enchantment)) {
                iterator.remove();
            }
        }
    }
}
