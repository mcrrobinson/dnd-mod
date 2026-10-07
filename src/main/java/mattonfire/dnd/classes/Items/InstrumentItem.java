package mattonfire.dnd.classes.Items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Music.InstrumentSongs;
import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A Bard instrument (lute, drum, flute). Right click plays its song for everyone in earshot.
 * When a Bard plays it, every player within {@link #BUFF_RADIUS} blocks (the Bard included) gets
 * the instrument's buff; anyone else just plays the tune.
 */
public class InstrumentItem extends Item {
    public static final double BUFF_RADIUS = 16.0;
    public static final int BUFF_TICKS = 30 * 20;
    /** All instruments share it, so a Bard plays one song at a time. */
    public static final int BARD_COOLDOWN_TICKS = 10 * 20;
    /** Anyone else can play again once the song has finished. */
    public static final int OTHER_COOLDOWN_TICKS = InstrumentSongs.SONG_TICKS;

    private final SoundEvent song;
    private final StatusEffect buff;

    public InstrumentItem(SoundEvent song, StatusEffect buff, Settings settings) {
        super(settings);
        this.song = song;
        this.buff = buff;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        boolean bard = user instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.BARD;
        world.playSound(null, user.getX(), user.getY(), user.getZ(), song, SoundCategory.RECORDS, 2.0F, 1.0F);
        ServerWorld serverWorld = (ServerWorld) world;
        InstrumentSongs.sendDuck(serverWorld, user.getPos());
        serverWorld.spawnParticles(ParticleTypes.NOTE, user.getX(), user.getY() + 2.2, user.getZ(),
                bard ? 8 : 3, 0.6, 0.3, 0.6, 1.0);

        if (bard) {
            List<PlayerEntity> listeners = world.getEntitiesByClass(PlayerEntity.class,
                    user.getBoundingBox().expand(BUFF_RADIUS),
                    p -> p.isAlive() && !p.isSpectator() && p.squaredDistanceTo(user) <= BUFF_RADIUS * BUFF_RADIUS);
            for (PlayerEntity listener : listeners) {
                listener.addStatusEffect(new StatusEffectInstance(buff, BUFF_TICKS, 0), user);
                Vec3d p = listener.getPos();
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y + 1.0, p.z, 6, 0.4, 0.5, 0.4, 0.0);
            }
            user.sendMessage(Text.translatable("message.dndclasses.instrument.bard",
                    Text.translatable(buff.getTranslationKey()), listeners.size()), true);
            setCooldown(user, BARD_COOLDOWN_TICKS);
        } else {
            user.sendMessage(Text.translatable("message.dndclasses.instrument.not_bard"), true);
            setCooldown(user, OTHER_COOLDOWN_TICKS);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(stack);
    }

    private static void setCooldown(PlayerEntity user, int ticks) {
        for (Item instrument : new Item[] { ModItems.LUTE,
                ModItems.DRUM, ModItems.FLUTE }) {
            user.getItemCooldownManager().set(instrument, ticks);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.dndclasses.instrument.tooltip",
                Text.translatable(buff.getTranslationKey()), BUFF_TICKS / 20, (int) BUFF_RADIUS)
                .formatted(Formatting.GRAY));
    }
}
