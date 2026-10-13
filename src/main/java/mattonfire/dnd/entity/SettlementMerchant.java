package mattonfire.dnd.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.Nullable;

/**
 * A settlement merchant NPC: vanilla's {@link Merchant}, answered by its {@link NpcMerchant}. None of them level
 * up or take offers from the server; override a method for anything special (the innkeeper's Room Key).
 */
public interface SettlementMerchant extends Merchant {
    NpcMerchant merchant();

    @Override
    default void setCustomer(@Nullable PlayerEntity customer) {
        this.merchant().setCustomer(customer);
    }

    @Nullable
    @Override
    default PlayerEntity getCustomer() {
        return this.merchant().customer();
    }

    @Override
    default TradeOfferList getOffers() {
        return this.merchant().offers();
    }

    @Override
    default void setOffersFromServer(TradeOfferList offers) {
    }

    @Override
    default void trade(TradeOffer offer) {
        this.merchant().trade(offer);
    }

    @Override
    default void onSellingItem(ItemStack stack) {
    }

    @Override
    default int getExperience() {
        return 0;
    }

    @Override
    default void setExperienceFromServer(int experience) {
    }

    @Override
    default boolean isLeveledMerchant() {
        return false;
    }

    @Override
    default SoundEvent getYesSound() {
        return this.merchant().yesSound();
    }

    @Override
    default boolean isClient() {
        return this.merchant().isClient();
    }
}
