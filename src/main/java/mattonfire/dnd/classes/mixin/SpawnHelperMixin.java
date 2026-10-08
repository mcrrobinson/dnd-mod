package mattonfire.dnd.classes.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.util.collection.Pool;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.SpawnSettings;

/** Adds Goblin Warriors to the monsters that spawn on Nether Fortress bricks. */
@Mixin(SpawnHelper.class)
public abstract class SpawnHelperMixin {
    @Unique
    private static Pool<SpawnSettings.SpawnEntry> dnd$fortressSpawns;

    @Unique
    private static Pool<SpawnSettings.SpawnEntry> dnd$fortressSpawnsBase;

    @WrapOperation(method = "getSpawnEntries", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/gen/structure/NetherFortressStructure;MONSTER_SPAWNS:Lnet/minecraft/util/collection/Pool;"))
    private static Pool<SpawnSettings.SpawnEntry> dnd$addGoblinWarriors(Operation<Pool<SpawnSettings.SpawnEntry>> original) {
        Pool<SpawnSettings.SpawnEntry> base = original.call();
        if (dnd$fortressSpawns == null || dnd$fortressSpawnsBase != base) {
            dnd$fortressSpawnsBase = base;
            List<SpawnSettings.SpawnEntry> entries = new ArrayList<>(base.getEntries());
            entries.add(new SpawnSettings.SpawnEntry(ModEntityTypes.GOBLIN_WARRIOR, 6, 1, 3));
            dnd$fortressSpawns = Pool.of(entries);
        }
        return dnd$fortressSpawns;
    }
}
