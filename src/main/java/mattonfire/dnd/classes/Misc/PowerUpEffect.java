package mattonfire.dnd.classes.Misc;

import java.util.Iterator;
import java.util.List;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses; // For DnDClasses.WARLOCK_FIREBREATH
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Druid;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Goals.FollowSummonerGoal;
import mattonfire.dnd.classes.Progression.SkillHelpers;
import mattonfire.dnd.classes.Progression.Classes.NecromancerSkills;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import mattonfire.dnd.classes.Progression.Classes.AlchemistSkills;
import mattonfire.dnd.classes.Progression.Classes.ClericSkills;
import mattonfire.dnd.classes.Progression.Classes.BarbarianSkills;
import mattonfire.dnd.classes.Progression.Classes.FighterSkills;
import mattonfire.dnd.classes.Progression.Classes.RogueSkills;
import mattonfire.dnd.classes.Progression.Classes.RangerSkills;
import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import mattonfire.dnd.classes.Progression.Classes.WarlockSkills;
import mattonfire.dnd.classes.Progression.Classes.WizardSkills;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;

public class PowerUpEffect {


    /** How long the Necromancer special's zombie and skeleton last. */
    public static final int UNDEAD_LIFETIME_TICKS = 10 * 20;

    /**
     * Adds AI goals that make the summon target hostile mobs and follow the player.
     */
    private static void addHostileTargetGoal(MobEntity mob, PlayerEntity player) {
        if (mob instanceof ZombieEntity || mob instanceof SkeletonEntity) {
            MobEntityAccessor mobAccessor = (MobEntityAccessor) mob;
            mobAccessor.getTargetSelector().add(2, new ActiveTargetGoal<LivingEntity>(
                    mob, LivingEntity.class, 10, true, false, entity -> entity instanceof Monster));

            mobAccessor.getGoalSelector().add(3,
                    new FollowSummonerGoal((PathAwareEntity) mob, player, 2.0D, 5.0F, 10.0F));
        }
    }

    public static void spawnUndead(PlayerEntity player) {
        if (!(player.world instanceof ServerWorld world))
            return; // Ensure it's server-side

        BlockPos pos = player.getBlockPos(); // Get necromancer's position
        Team allyTeam = NecromancerSkills.allyTeam(world, player);

        spawnAlly(world, player, EntityType.ZOMBIE.create(world), pos.getX(), pos.getY(), pos.getZ(), allyTeam);
        spawnAlly(world, player, EntityType.SKELETON.create(world), pos.getX() + 1, pos.getY(), pos.getZ(),
                allyTeam);
    }

    /**
     * Summons go through {@link SkillHelpers#spawnSummon}, which removes them when
     * their time is up and when they'd come back from a chunk reload or restart
     * without their AI goals.
     */
    private static void spawnAlly(ServerWorld world, PlayerEntity player, HostileEntity mob, double x, double y,
            double z, Team allyTeam) {
        if (mob == null)
            return;
        mob.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360F, 0F);
        addHostileTargetGoal(mob, player); // Add AI to target hostiles
        world.getScoreboard().addPlayerToTeam(mob.getUuidAsString(), allyTeam);
        mob.addCommandTag(NecromancerSkills.SUMMON_TAG);
        mob.setPersistent();
        SkillHelpers.spawnSummon(world, mob, UNDEAD_LIFETIME_TICKS);
    }

    public static boolean play(MinecraftServer server, PlayerEntity player, DndCharacter character) {
        if (character == null || character == DndCharacter.NONE)
            return false; // No class picked yet, keep the mana

        System.out.println("Starting powerup on: " + character.toString());
        switch (character) {
            case RANGER:
                // Fire rate and arrow speed are read from the rank while it runs (PlayerEntityMixin, BowItemMixin).
                // No particles: they fill the first-person view with green squares while aiming.
                player.addStatusEffect(new StatusEffectInstance(ModEffects.ARROW_STORM,
                        RangerSkills.ARROW_STORM.ticks(player, "Duration"), 1, false, false, true));
                break;
            case WIZARD:

                // TODO: This code comes from ServerWorld for create explosion
                // thought that extracting this would make it easier to make a
                // custom explosion. But we need to know the ExplosionS2CPacket
                // handler
                ServerWorld world = (ServerWorld) player.getEntityWorld();
                double playerX = player.getX();
                double playerY = player.getY();
                double playerZ = player.getZ();
                // The rank sets the reach; a vanilla explosion reaches twice its power.
                float reach = (float) WizardSkills.ARCANE_EXPLOSION.get(player, "Radius");
                float radius = reach / 2;

                DamageSource damageSource = world.getDamageSources()
                        .create(ModDamageTypes.WIZARD_EXPLOSION_DAMAGE_SOURCE, player);

                Explosion explosion = new Explosion(world, player, damageSource, null,
                        playerX,
                        playerY, playerZ, radius, false,
                        Explosion.DestructionType.KEEP);

                // A few seconds of invulnerability: Resistance V blocks all normal damage
                // and wears off on its own (unlike setInvulnerable, which is saved).
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,
                        WizardSkills.ARCANE_EXPLOSION.ticks(player, "Resistance V"), 4));

                explosion.collectBlocksAndDamageEntities();
                explosion.affectWorld(true);

                if (!explosion.shouldDestroy()) {
                    explosion.clearAffectedBlocks();
                }

                Iterator<? extends LivingEntity> var14 = world.getPlayers().iterator();
                while (var14.hasNext()) {
                    ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) var14.next();
                    if (serverPlayerEntity.squaredDistanceTo(playerX, playerY, playerZ) < 4096.0) {
                        serverPlayerEntity.networkHandler
                                .sendPacket(new ExplosionS2CPacket(playerX, playerY, playerZ, radius,
                                        explosion.getAffectedBlocks(),
                                        (Vec3d) explosion.getAffectedPlayers().get(serverPlayerEntity)));

                        // Where the blast is and how far it reaches, so the sphere matches it
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeDouble(playerX);
                        buf.writeDouble(playerY + player.getStandingEyeHeight() / 2.0);
                        buf.writeDouble(playerZ);
                        buf.writeFloat(reach);
                        ServerPlayNetworking.send(serverPlayerEntity, DnDClasses.S2C_WIZARD_EFFECTS_PACKET_ID, buf);
                    }
                }

                break;
            case BARBARIAN:
                // Rage: Strength I for 8 s at rank I, up to Strength III for 12 s at rank IV.
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,
                        BarbarianSkills.RAGE.ticks(player, "Duration"),
                        BarbarianSkills.RAGE.amplifier(player, "Strength")));
                break;
            case MONK:
                // Flurry Rush: a chain of blink strikes; hits, targets and damage come from its rank.
                if (!mattonfire.dnd.classes.Progression.Classes.MonkSkills.flurryRush(player))
                    return false; // Nothing in the crosshair, keep the mana
                break;
            case FIGHTER:
                // Super regeneration: Regeneration V (~3 hearts/sec), longer with each rank.
                // Not potion-sourced, so the Fighter's potion block doesn't stop it.
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                        FighterSkills.SUPER_REGEN.ticks(player, "Duration"),
                        FighterSkills.SUPER_REGEN.amplifier(player, "Regeneration")));
                break;
            case BARD:
                if (player instanceof ServerPlayerEntity bard) {
                    mattonfire.dnd.classes.Progression.Classes.BardSkills.animalFriends(bard);
                }
                break;
            case CLERIC: {
                // Sanctuary: duration, party reach and the party's Regeneration come from its rank.
                int duration = ClericSkills.SANCTUARY.ticks(player, "Duration");
                player.addStatusEffect(new StatusEffectInstance(ModEffects.MOB_REPEL, duration));
                double partyReach = ClericSkills.sanctuaryPartyReach(player);
                if (partyReach > 0 && player instanceof ServerPlayerEntity cleric) {
                    int regen = ClericSkills.sanctuaryPartyRegenAmplifier(player);
                    for (ServerPlayerEntity member : PartyManager.nearbyMembers(cleric, partyReach)) {
                        member.addStatusEffect(new StatusEffectInstance(ModEffects.MOB_REPEL, duration));
                        if (regen >= 0) {
                            member.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                                    ClericSkills.sanctuaryPartyRegenTicks(player), regen));
                        }
                    }
                }
                break;
            }
            case PALADIN:
                // Divine Judgment: a beam of holy light on the mob in sight. Nothing in sight keeps the mana.
                if (!(player instanceof ServerPlayerEntity paladin) || !PaladinSkills.divineJudgment(paladin))
                    return false;
                break;
            case ROGUE:
                // Vanish: invisibility, longer with each rank.
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,
                        RogueSkills.VANISH.ticks(player, "Duration"), 0));
                break;
            case DRUID:
                if (!(player instanceof ServerPlayerEntity serverPlayer) || !Druid.transform(serverPlayer))
                    return false; // Nothing to transform into, keep the mana
                break;
            case NECROMANCER:
                spawnUndead(player);
                // Spawn undead enemies
                break;
            case WARLOCK:
                // Breathe fire; duration, reach, damage and burn time scale with the rank.
                DnDClasses.WARLOCK_FIREBREATH.put(player.getUuid(),
                        player.getWorld().getTime() + WarlockSkills.FIRE_BREATH.ticks(player, "Duration"));

                break;
            case ARTIFICER:
                // Temporary buff to armor (+8 armor, +4 toughness for 30 seconds)
                player.addStatusEffect(new StatusEffectInstance(ModEffects.ARMOR_BUFF, 600, 0));
                break;
            case BLOODHUNTER:
                // Take control of the mob being looked at; range, duration and success chance by rank.
                // A resisted attempt still spends the mana.
                if (!(player instanceof ServerPlayerEntity serverPlayer)
                        || !BloodHunterControl.takeControl(serverPlayer)) {
                    return false;
                }
                break;
            case ALCHEMIST:
                // Transmute: throws the held potion (or an unstable brew) as a big cloud.
                if (!(player instanceof ServerPlayerEntity alchemist) || !AlchemistSkills.transmute(alchemist))
                    return false;
                break;
            default:
                break;

        }
        return true; // Indicate that the power-up was successfully applied

    }
}
