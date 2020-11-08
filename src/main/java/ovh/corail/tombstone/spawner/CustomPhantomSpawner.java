package ovh.corail.tombstone.spawner;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.stats.ServerStatisticsManager;
import net.minecraft.stats.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.PhantomSpawner;
import net.minecraft.world.spawner.WorldEntitySpawner;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.config.ConfigTombstone;

public class CustomPhantomSpawner extends PhantomSpawner {

    @Override
    public int func_230253_a_(ServerWorld world, boolean spawnHostileMobs, boolean spawnPeacefulMobs) {
        if (!spawnHostileMobs || !world.getGameRules().getBoolean(GameRules.DO_INSOMNIA)) {
            return 0;
        }
        --this.ticksUntilSpawn;
        if (this.ticksUntilSpawn > 0) {
            return 0;
        }
        this.ticksUntilSpawn += (60 + world.rand.nextInt(60)) * 20;
        if (world.getSkylightSubtracted() < 5 && world.getDimensionType().hasSkyLight()) {
            return 0;
        }
        int i = 0;
        for (PlayerEntity player : world.getPlayers()) {
            if (!player.isSpectator()) {
                BlockPos currentPos = player.getPosition();
                if (!world.getDimensionType().hasSkyLight() || currentPos.getY() >= world.getSeaLevel() && world.canSeeSky(currentPos)) {
                    DifficultyInstance diffForLocation = world.getDifficultyForLocation(currentPos);
                    if (diffForLocation.isHarderThan(world.rand.nextFloat() * 3f)) {
                        ServerStatisticsManager statManager = ((ServerPlayerEntity) player).getStats();
                        int timeSinceRest = MathHelper.clamp(statManager.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
                        float adjust = ConfigTombstone.general.knowledgeReducePhantomSpawn.get() ? player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(cap -> 1 + (cap.getTotalPerkPoints() * 0.1f)).orElse(1f) : 1f;
                        int timeForCheck = (int) (ConfigTombstone.general.timeForPhantomSpawn.get() * adjust);
                        if (world.rand.nextInt(timeSinceRest) >= timeForCheck) {
                            BlockPos blockpos1 = currentPos.up(20 + world.rand.nextInt(15)).east(-10 + world.rand.nextInt(21)).south(-10 + world.rand.nextInt(21));
                            BlockState iblockstate = world.getBlockState(blockpos1);
                            FluidState ifluidstate = world.getFluidState(blockpos1);
                            if (WorldEntitySpawner.func_234968_a_(world, blockpos1, iblockstate, ifluidstate, EntityType.PHANTOM)) {
                                ILivingEntityData ientitylivingdata = null;
                                int l = 1 + world.rand.nextInt(diffForLocation.getDifficulty().getId() + 1);
                                for (int i1 = 0; i1 < l; ++i1) {
                                    PhantomEntity phantom = EntityType.PHANTOM.create(world);
                                    if (phantom != null) {
                                        phantom.moveToBlockPosAndAngles(blockpos1, 0f, 0f);
                                        ientitylivingdata = phantom.onInitialSpawn(world, diffForLocation, SpawnReason.NATURAL, ientitylivingdata, null);
                                        world.addEntity(phantom);
                                    }
                                }
                                i += l;
                            }
                        }
                    }
                }
            }
        }
        return i;
    }
}
