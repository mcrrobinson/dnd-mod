package mattonfire.dnd.classes.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.include.com.google.common.collect.Lists;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Registry.ModEnchantments;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mixin(EnchantmentScreenHandler.class)
public class EnchantingTableMixin {
    EnchantmentScreenHandlerAccessor accessor = (EnchantmentScreenHandlerAccessor) (Object) this;
    Random random = accessor.getRandom();
    Property seed = accessor.getSeed();
    Inventory inventory = accessor.getInventory();
    int[] enchantmentPower = accessor.getEnchantmentPower();
    ScreenHandlerContext context = accessor.getContext();

    private static List<EnchantmentLevelEntry> getSomeEntries(int power, ItemStack stack, boolean treasureAllowed,
            boolean powerfulUser) {
        List<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        boolean bl = stack.isOf(Items.BOOK);
        Iterator<Enchantment> var6 = Registries.ENCHANTMENT.iterator();

        while (true) {
            while (true) {
                Enchantment enchantment;
                do {
                    do {
                        do {
                            do {
                                if (!var6.hasNext()) {
                                    return list;
                                }

                                enchantment = (Enchantment) var6.next();
                            } while (enchantment.isTreasure() && !treasureAllowed);
                        } while (!enchantment.isAvailableForRandomSelection());
                        // The target picks the item group; isAcceptableItem lets an enchantment narrow it
                        // (Tree Feller: DIGGER, axes only). Vanilla overrides only widen it, so they're unaffected.
                    } while (!(enchantment.target.isAcceptableItem(item) && enchantment.isAcceptableItem(stack))
                            && !bl);
                } while (!powerfulUser && (enchantment == ModEnchantments.LUNGE_ENCHANTMENT ||
                        enchantment == ModEnchantments.GRID_MINER_ENCHANTMENT ||
                        enchantment == ModEnchantments.TREE_FELLER_ENCHANTMENT ||
                        enchantment == ModEnchantments.INVULNERABILITY_ENCHANTMENT ||
                        enchantment == ModEnchantments.RETURNING_ENCHANTMENT ||
                        enchantment == ModEnchantments.VAMPIRIC_ENCHANTMENT ||
                        enchantment == ModEnchantments.SMITE_DRAGONS_ENCHANTMENT ||
                        enchantment == ModEnchantments.FEATHERFALL_ENCHANTMENT));

                for (int i = enchantment.getMaxLevel(); i > enchantment.getMinLevel() - 1; --i) {
                    if (power >= enchantment.getMinPower(i) && power <= enchantment.getMaxPower(i)) {
                        list.add(new EnchantmentLevelEntry(enchantment, i));
                        break;
                    }
                }
            }
        }
    }

    private static void removeConflicts(List<EnchantmentLevelEntry> possibleEntries,
            EnchantmentLevelEntry pickedEntry) {
        Iterator<EnchantmentLevelEntry> iterator = possibleEntries.iterator();

        while (iterator.hasNext()) {
            if (!pickedEntry.enchantment.canCombine(((EnchantmentLevelEntry) iterator.next()).enchantment)) {
                iterator.remove();
            }
        }

    }

    private static List<EnchantmentLevelEntry> customHelperGenerateEnchantments(Random random, ItemStack stack,
            int level, boolean treasureAllowed, boolean powerfulUser) {
        List<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        int i = item.getEnchantability();
        if (i <= 0) {
            return list;
        } else {
            level += 1 + random.nextInt(i / 4 + 1) + random.nextInt(i / 4 + 1);
            float f = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
            level = MathHelper.clamp(Math.round((float) level + (float) level * f), 1, Integer.MAX_VALUE);
            List<EnchantmentLevelEntry> list2 = getSomeEntries(level, stack, treasureAllowed, powerfulUser);
            if (!list2.isEmpty()) {
                Optional<EnchantmentLevelEntry> var10000 = Weighting.getRandom(random, list2);
                Objects.requireNonNull(list);
                var10000.ifPresent(list::add);

                while (random.nextInt(50) <= level) {
                    if (!list.isEmpty()) {
                        removeConflicts(list2, (EnchantmentLevelEntry) Util.getLast(list));
                    }

                    if (list2.isEmpty()) {
                        break;
                    }

                    var10000 = Weighting.getRandom(random, list2);
                    Objects.requireNonNull(list);
                    var10000.ifPresent(list::add);
                    level /= 2;
                }
            }

            return list;
        }
    }

    private List<EnchantmentLevelEntry> customGenerateEnchantments(ItemStack stack, int slot, int level,
            boolean powerfulUser) {
        this.random.setSeed((long) (this.seed.get() + slot));
        List<EnchantmentLevelEntry> list = customHelperGenerateEnchantments(this.random, stack, level, false,
                powerfulUser);
        if (stack.isOf(Items.BOOK) && list.size() > 1) {
            list.remove(this.random.nextInt(list.size()));
        }

        return list;
    }

    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void modifyGenerateEnchantments(PlayerEntity player, int id,
            CallbackInfoReturnable<Boolean> cir) {

        boolean isPowerfulUser = false;
        if (player instanceof PlayerEntityExt) {
            PlayerEntityExt playerEntity = (PlayerEntityExt) player;
            // Alchemists cannot enchant
            if (playerEntity.getDndClass() == DndCharacter.ALCHEMIST) {
                player.sendMessage(Text.literal("Alchemists cannot enchant items.").formatted(Formatting.RED), true);
                cir.setReturnValue(false);
                return;
            }
            isPowerfulUser = playerEntity.getDndClass() == DndCharacter.ARTIFICER;

        }

        boolean buttonClick = onButtonClick(player, id, isPowerfulUser);

        // Cancel the original method and return our custom list
        cir.setReturnValue(buttonClick);
    }

    public boolean onButtonClick(PlayerEntity player, int id, boolean isPowerfulUser) {
        if (id >= 0 && id < this.enchantmentPower.length) {
            ItemStack itemStack = this.inventory.getStack(0);
            ItemStack itemStack2 = this.inventory.getStack(1);
            int i = id + 1;
            if ((itemStack2.isEmpty() || itemStack2.getCount() < i) && !player.getAbilities().creativeMode) {
                return false;
            } else if (this.enchantmentPower[id] <= 0 || itemStack.isEmpty()
                    || (player.experienceLevel < i || player.experienceLevel < this.enchantmentPower[id])
                            && !player.getAbilities().creativeMode) {
                return false;
            } else {
                this.context.run((world, pos) -> {
                    ItemStack itemStack3 = itemStack;
                    List<EnchantmentLevelEntry> list = this.customGenerateEnchantments(itemStack, id,
                            this.enchantmentPower[id], isPowerfulUser);
                    if (!list.isEmpty()) {
                        player.applyEnchantmentCosts(itemStack, i);
                        boolean bl = itemStack.isOf(Items.BOOK);
                        if (bl) {
                            itemStack3 = new ItemStack(Items.ENCHANTED_BOOK);
                            NbtCompound nbtCompound = itemStack.getNbt();
                            if (nbtCompound != null) {
                                itemStack3.setNbt(nbtCompound.copy());
                            }

                            this.inventory.setStack(0, itemStack3);
                        }

                        for (int k = 0; k < list.size(); ++k) {
                            EnchantmentLevelEntry enchantmentLevelEntry = (EnchantmentLevelEntry) list.get(k);
                            if (bl) {
                                EnchantedBookItem.addEnchantment(itemStack3, enchantmentLevelEntry);
                            } else {
                                itemStack3.addEnchantment(enchantmentLevelEntry.enchantment,
                                        enchantmentLevelEntry.level);
                            }
                        }

                        if (!player.getAbilities().creativeMode) {
                            itemStack2.decrement(i);
                            if (itemStack2.isEmpty()) {
                                this.inventory.setStack(1, ItemStack.EMPTY);
                            }
                        }

                        player.incrementStat(Stats.ENCHANT_ITEM);
                        if (player instanceof ServerPlayerEntity) {
                            Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity) player, itemStack3, i);
                        }

                        this.inventory.markDirty();
                        this.seed.set(player.getEnchantmentTableSeed());
                        ((EnchantmentScreenHandlerInvoker) this).invokeOnContentChanged(this.inventory);
                        world.playSound((PlayerEntity) null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                                SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
                    }

                });
                return true;
            }
        } else {
            Text var10000 = player.getName();
            Util.error("" + var10000 + " pressed invalid button id: " + id);
            return false;
        }
    }
}
