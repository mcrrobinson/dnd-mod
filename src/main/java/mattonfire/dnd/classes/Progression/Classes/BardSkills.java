package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.alliesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.enemiesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Misc.BardCompanions;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Ranks;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.util.Identifier;
import mattonfire.dnd.entity.boss.Boss;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class BardSkills extends ClassSkills {
    /**
     * Animal Friends: the radius it reaches, how many companions the Bard can
     * have at once, and the highest animal tier that can be unlocked.
     */
    public static final Ranks ANIMAL_FRIENDS = Ranks.of("bard.animal_friends")
            .amount("Radius", "blocks", 10, 12, 14, 16)
            .amount("Companions", "", 3, 4, 5, 6)
            .text("Animals", "Tier I animals", "Up to tier II animals", "Up to tier III animals",
                    "Up to tier IV animals");

    /** Every new Bard has these unlocked. */
    public static final String STARTER_ANIMAL = "minecraft:wolf";

    /** Animal Friends rank needed to unlock each animal; others can't be learned. */
    private static final Map<EntityType<?>, Integer> TIERS = Map.ofEntries(
            Map.entry(EntityType.WOLF, 1), Map.entry(EntityType.CAT, 1), Map.entry(EntityType.FOX, 1),
            Map.entry(EntityType.PARROT, 1),
            // Farm animals: tier I, for fun. They follow but can't fight.
            Map.entry(EntityType.COW, 1), Map.entry(EntityType.PIG, 1), Map.entry(EntityType.SHEEP, 1),
            Map.entry(EntityType.CHICKEN, 1), Map.entry(EntityType.RABBIT, 1), Map.entry(EntityType.MOOSHROOM, 1),
            Map.entry(EntityType.HORSE, 1), Map.entry(EntityType.DONKEY, 1), Map.entry(EntityType.MULE, 1),
            Map.entry(EntityType.GOAT, 2), Map.entry(EntityType.LLAMA, 2), Map.entry(EntityType.TRADER_LLAMA, 2),
            Map.entry(EntityType.BEE, 2),
            Map.entry(EntityType.POLAR_BEAR, 3), Map.entry(EntityType.PANDA, 3),
            Map.entry(EntityType.IRON_GOLEM, 4), Map.entry(EntityType.HOGLIN, 4));

    /** Animals within this many blocks of a Bard's song are learned. */
    public static final double CHARM_RADIUS = 8;

    private static final int PET_KILL_BONUS_XP = 3;
    private static final int GROUP_KILL_BONUS_XP = 1;
    private static final double GROUP_RADIUS = 16;

    private static final double AURA_RADIUS = 8;
    /** Aura effects outlast the 1-second refresh so they don't flicker. */
    private static final int AURA_TICKS = 60;
    private static final int HERO_TICKS = 300;

    private static final double THUNDERWAVE_RADIUS = 6;
    private static final float THUNDERWAVE_DAMAGE = 4.0F;
    private static final double SONG_RADIUS = 8;
    private static final float SONG_HEAL = 6.0F;
    private static final double CRESCENDO_RADIUS = 12;
    private static final int CRESCENDO_TICKS = 400;
    private static final float JACK_DAMAGE_MULTIPLIER = 1.1F;

    public static final String VALOR = "bard.valor";
    public static final String LORE = "bard.lore";
    /** Combat Inspiration: armor for everyone who gets a Valor Bard's instrument buff, for as long as it. */
    public static final double COMBAT_INSPIRATION_ARMOR = 2;
    private static final UUID COMBAT_INSPIRATION_ID = UUID.nameUUIDFromBytes("dndclasses:bard.valor".getBytes());
    /** Bardic Lore: added to Persuasion checks. */
    public static final int BARDIC_LORE_PERSUASION = 2;
    /** Server tick each inspired player's armor runs out. */
    private static final Map<UUID, Integer> INSPIRED_UNTIL = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.BARD;
    }

    /**
     * Song of Rest: a short rest with a Bard in it (the resting player included) heals an
     * extra 1d6, or 2d6 if a Bard there has unlocked the {@code bard.song_of_rest} active.
     * Only the best Bard counts. Called by the campfire rest once the player's rest finishes.
     *
     * @param companions everyone resting together, the player included
     */
    public static void songOfRest(ServerPlayerEntity player, List<ServerPlayerEntity> companions) {
        ServerPlayerEntity bard = null;
        int dice = 0;
        for (ServerPlayerEntity companion : companions) {
            if (Progression.classOf(companion) != DndCharacter.BARD) {
                continue;
            }
            int n = Progression.get(companion, DndCharacter.BARD).isUnlocked("bard.song_of_rest") ? 2 : 1;
            if (n > dice) {
                dice = n;
                bard = companion;
            }
        }
        if (bard == null) {
            return;
        }
        int healed = 0;
        StringBuilder rolls = new StringBuilder();
        for (int i = 0; i < dice; i++) {
            int roll = 1 + player.getRandom().nextInt(6);
            healed += roll;
            rolls.append(i == 0 ? "" : " + ").append(roll);
        }
        player.heal(healed);
        String who = bard == player ? "" : " (" + bard.getName().getString() + ")";
        player.sendMessage(Text.literal("Song of Rest" + who + ": " + dice + "d6: " + rolls + " = " + healed + " HP")
                .formatted(Formatting.LIGHT_PURPLE), false);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(),
                net.minecraft.sound.SoundCategory.PLAYERS, 0.8F, 1.2F);
    }

    @Override
    public List<String> subclassIds() {
        return List.of("bard.valor", "bard.lore");
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("bard.animal_friends", "Animal Friends",
                        "Unlocked animals nearby become your companions: they follow you and fight for you.", "minecraft:lead", 9, 0, 1, 3),
                // College of Valor
                passive("bard.inspiring_presence", "Inspiring Presence",
                        "Other players within 8 blocks get Speed I.", "minecraft:sugar", 1, 2, 3,
                        "bard.animal_friends"),
                active("bard.thunderwave", "Thunderwave",
                        "Hit mobs within 6 blocks for 4 damage and blast them away.", "minecraft:goat_horn", 4, 1,
                        2, 2, "bard.inspiring_presence"),
                passive("bard.battle_hymn", "Battle Hymn", "Other players within 8 blocks get Strength I.",
                        "minecraft:blaze_powder", 1, 2, 1, "bard.thunderwave"),
                // College of Lore
                passive("bard.silver_tongue", "Silver Tongue", "Permanent Hero of the Village.",
                        "minecraft:emerald", 1, 0, 3, "bard.animal_friends"),
                active("bard.song_of_rest", "Song of Rest",
                        "Heal you and allies within 8 blocks by 3 hearts and clear bad effects.",
                        "minecraft:note_block", 3, 1, 0, 2, "bard.silver_tongue"),
                passive("bard.jack_of_all_trades", "Jack of All Trades", "+1 heart and 10% more damage.",
                        "minecraft:book", 1, 0, 1, "bard.song_of_rest"),
                active("bard.crescendo", "Crescendo",
                        "Allies within 12 blocks get Strength II, Speed II and Resistance for 20 seconds; "
                                + "mobs glow and are weakened.",
                        "minecraft:jukebox", 9, 2, 1, 0, "bard.battle_hymn", "bard.jack_of_all_trades"));
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        return List.of(new AttributeBonus("bard.jack_of_all_trades", EntityAttributes.GENERIC_MAX_HEALTH, 2,
                EntityAttributeModifier.Operation.ADDITION));
    }

    @Override
    public void register() {
        BardCompanions.register();
        // Bardic Lore. Identifying magic items on pickup comes with the identification card.
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "subclass/bard_lore"), (player, c) -> {
            if (Progression.classOf(player) == DndCharacter.BARD && Progression.current(player).hasSubclass(LORE)) {
                c.skillBonus(Skill.PERSUASION, BARDIC_LORE_PERSUASION, "Bardic Lore");
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (INSPIRED_UNTIL.isEmpty() || server.getTicks() % 20 != 0)
                return;
            int now = server.getTicks();
            INSPIRED_UNTIL.entrySet().removeIf(entry -> {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null && now < entry.getValue())
                    return false;
                if (player != null)
                    setInspiredArmor(player, false);
                return true; // Run out, or logged off (the modifier is temporary, so it went with them)
            });
        });
        mattonfire.dnd.classes.Music.BardInstrumentSlot.register();
        // Kills by a bard's pets aren't the bard's own kills, so their XP is handed out here.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof Monster) || source.getAttacker() == null)
                return;
            Entity killer = source.getAttacker();
            UUID ownerId = killer instanceof TameableEntity pet ? pet.getOwnerUuid() : BardCompanions.ownerOf(killer);
            if (ownerId != null && killer.getWorld().getPlayerByUuid(ownerId) instanceof ServerPlayerEntity owner
                    && Progression.classOf(owner) == DndCharacter.BARD) {
                Progression.addXp(owner, PET_KILL_BONUS_XP);
            }
        });
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "bard.thunderwave" -> {
                for (LivingEntity mob : enemiesNear(player, THUNDERWAVE_RADIUS)) {
                    mob.damage(world.getDamageSources().playerAttack(player), THUNDERWAVE_DAMAGE);
                    Vec3d push = mob.getPos().subtract(player.getPos()).multiply(1, 0, 1).normalize();
                    mob.takeKnockback(2.0, -push.x, -push.z);
                    mob.addVelocity(0, 0.5, 0);
                    mob.velocityModified = true;
                }
                world.spawnParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 1,
                        0, 0, 0, 0);
                effects(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, ParticleTypes.NOTE, 30);
            }
            case "bard.song_of_rest" -> {
                for (LivingEntity ally : alliesNear(player, SONG_RADIUS)) {
                    ally.heal(SONG_HEAL);
                    List<StatusEffect> bad = ally.getStatusEffects().stream().map(StatusEffectInstance::getEffectType)
                            .filter(e -> e.getCategory() == StatusEffectCategory.HARMFUL).toList();
                    bad.forEach(ally::removeStatusEffect);
                }
                effects(player, SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), ParticleTypes.HEART, 20);
            }
            case "bard.crescendo" -> {
                for (LivingEntity ally : alliesNear(player, CRESCENDO_RADIUS)) {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, CRESCENDO_TICKS, 1));
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, CRESCENDO_TICKS, 1));
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, CRESCENDO_TICKS, 0));
                }
                for (LivingEntity mob : hostilesNear(player, CRESCENDO_RADIUS)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, CRESCENDO_TICKS, 0), player);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, CRESCENDO_TICKS, 0),
                            player);
                }
                effects(player, SoundEvents.ENTITY_PLAYER_LEVELUP, ParticleTypes.NOTE, 40);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Combat Inspiration: a Valor Bard's instrument also gives everyone who got its buff +2 armor for
     * the buff's length. Called from {@code InstrumentItem.playAsBard}.
     */
    public static void combatInspiration(ServerPlayerEntity bard, List<PlayerEntity> listeners, int ticks) {
        if (Progression.classOf(bard) != DndCharacter.BARD || !Progression.current(bard).hasSubclass(VALOR))
            return;
        int until = bard.getServer().getTicks() + ticks;
        for (PlayerEntity listener : listeners) {
            if (listener instanceof ServerPlayerEntity player) {
                setInspiredArmor(player, true);
                INSPIRED_UNTIL.put(player.getUuid(), until);
            }
        }
    }

    /** Whether the player has Combat Inspiration's armor. */
    public static boolean isInspired(PlayerEntity player) {
        EntityAttributeInstance armor = player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        return armor != null && armor.getModifier(COMBAT_INSPIRATION_ID) != null;
    }

    private static void setInspiredArmor(ServerPlayerEntity player, boolean on) {
        EntityAttributeInstance armor = player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armor == null)
            return;
        armor.removeModifier(COMBAT_INSPIRATION_ID);
        if (on) {
            armor.addTemporaryModifier(new EntityAttributeModifier(COMBAT_INSPIRATION_ID, "Combat Inspiration",
                    COMBAT_INSPIRATION_ARMOR, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public boolean usesBestiary() {
        return true;
    }

    /** Bards learn animals by charming them with music ({@link #charmAnimals}), not by killing them. */
    @Override
    public boolean learnsFrom(LivingEntity killed) {
        return false;
    }

    /**
     * Played an instrument: every tiered animal within {@link #CHARM_RADIUS}
     * blocks that's alive, not tamed and not a companion is learned. Called
     * from {@code InstrumentItem.use} on the server, after its own message.
     */
    public static void charmAnimals(ServerPlayerEntity player) {
        if (Progression.classOf(player) != DndCharacter.BARD)
            return;
        ServerWorld world = (ServerWorld) player.getWorld();
        ClassProgress progress = Progression.current(player);
        List<LivingEntity> animals = world.getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(CHARM_RADIUS),
                e -> e.isAlive() && TIERS.containsKey(e.getType()) && !BardCompanions.isCompanion(e)
                        && !(e instanceof Tameable pet && pet.getOwnerUuid() != null)
                        && e.squaredDistanceTo(player) <= CHARM_RADIUS * CHARM_RADIUS);
        List<String> charmed = new java.util.ArrayList<>();
        for (LivingEntity animal : animals) {
            String id = Registries.ENTITY_TYPE.getId(animal.getType()).toString();
            if (progress.learned.contains(id) || charmed.contains(id))
                continue;
            if (Progression.learn(player, id)) {
                charmed.add(id);
                world.spawnParticles(ParticleTypes.HEART, animal.getX(), animal.getBodyY(1), animal.getZ(), 5, 0.4,
                        0.3, 0.4, 0);
            }
        }
        if (!charmed.isEmpty()) {
            // Replaces Progression.learn's "Learned ..." line with one for the whole song.
            String names = String.join(", ", charmed.stream().map(Progression::entityName)
                    .map(String::toLowerCase).toList());
            player.sendMessage(Text.literal("Charmed " + (charmed.size() == 1 ? "a " : "") + names
                    + ": unlock " + (charmed.size() == 1 ? "it" : "them") + " at an Attunement Table.")
                    .formatted(Formatting.DARK_GREEN), true);
        }
    }

    @Override
    public int bestiaryRank(EntityType<?> type) {
        return TIERS.getOrDefault(type, Integer.MAX_VALUE);
    }

    /**
     * The Animal Friends special: unlocked animals within the radius become
     * companions, up to the cap, and all of the Bard's companions there are
     * set on nearby hostile mobs.
     */
    public static void animalFriends(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        ClassProgress progress = Progression.current(player);
        double radius = ANIMAL_FRIENDS.get(player, "Radius");
        int cap = ANIMAL_FRIENDS.getInt(player, "Companions");
        int count = BardCompanions.count(world, player);

        List<PathAwareEntity> nearby = world.getEntitiesByClass(PathAwareEntity.class,
                player.getBoundingBox().expand(radius),
                e -> e.isAlive() && !(e instanceof Boss)
                        && progress.bestiary.contains(Registries.ENTITY_TYPE.getId(e.getType()).toString()));
        nearby.sort(java.util.Comparator.comparingDouble(e -> e.squaredDistanceTo(player)));
        int adopted = 0;
        for (PathAwareEntity mob : nearby) {
            if (BardCompanions.isCompanion(mob))
                continue;
            // Someone else's pet or horse stays theirs.
            if (mob instanceof Tameable pet && pet.getOwnerUuid() != null
                    && !player.getUuid().equals(pet.getOwnerUuid()))
                continue;
            if (count >= cap) {
                player.sendMessage(Text.literal("You can't lead more than " + cap + " companions.")
                        .formatted(Formatting.YELLOW), true);
                break;
            }
            BardCompanions.adopt(mob, player);
            world.spawnParticles(ParticleTypes.HEART, mob.getX(), mob.getBodyY(1), mob.getZ(), 4, 0.3, 0.3, 0.3, 0);
            count++;
            adopted++;
        }

        // Point every companion in range that can fight at the nearest hostile mob.
        List<LivingEntity> hostiles = hostilesNear(player, radius).stream()
                .filter(h -> !(h instanceof CreeperEntity) && !(h instanceof Boss) && !BardCompanions.isCompanion(h))
                .toList();
        for (PathAwareEntity mob : world.getEntitiesByClass(PathAwareEntity.class,
                player.getBoundingBox().expand(radius), e -> BardCompanions.isCompanionOf(e, player) && BardCompanions.canFight(e))) {
            hostiles.stream().min(java.util.Comparator.comparingDouble(h -> h.squaredDistanceTo(mob)))
                    .ifPresent(mob::setTarget);
        }
        if (adopted == 0 && count < cap && nearby.isEmpty()) {
            player.sendMessage(Text.literal(progress.bestiary.isEmpty() ? "You haven't unlocked any animals."
                    : "None of your unlocked animals are nearby.").formatted(Formatting.GRAY), true);
        }
        effects(player, SoundEvents.ENTITY_PARROT_AMBIENT, ParticleTypes.NOTE, 20);
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        if (!(killed instanceof Monster))
            return 0;
        boolean withFriend = !player.getWorld().getEntitiesByClass(PlayerEntity.class,
                player.getBoundingBox().expand(GROUP_RADIUS), p -> p != player && p.isAlive()).isEmpty();
        return withFriend ? GROUP_KILL_BONUS_XP : 0;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        return progress.hasPassive("bard.jack_of_all_trades") ? amount * JACK_DAMAGE_MULTIPLIER : amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (!progress.bestiary.contains(STARTER_ANIMAL)) {
            Progression.unlockBestiary(player, STARTER_ANIMAL, true);
        }
        if (progress.hasPassive("bard.silver_tongue")) {
            player.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, HERO_TICKS, 0, true, false, true));
        }
        boolean speed = progress.hasPassive("bard.inspiring_presence");
        boolean strength = progress.hasPassive("bard.battle_hymn");
        if (!speed && !strength)
            return;
        for (PlayerEntity other : player.getWorld().getEntitiesByClass(PlayerEntity.class,
                player.getBoundingBox().expand(AURA_RADIUS), p -> p != player && p.isAlive() && !p.isSpectator())) {
            if (speed) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, AURA_TICKS, 0, true, true, true));
            }
            if (strength) {
                other.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.STRENGTH, AURA_TICKS, 0, true, true, true));
            }
        }
    }
}
