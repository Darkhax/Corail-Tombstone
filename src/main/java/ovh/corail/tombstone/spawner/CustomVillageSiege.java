package ovh.corail.tombstone.spawner;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.village.VillageSiege;
import net.minecraft.world.Difficulty;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.village.VillageSiegeEvent;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.event.EventFactory;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.TimeHelper;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static ovh.corail.tombstone.ModTombstone.LOGGER;

public class CustomVillageSiege extends VillageSiege {
    public SiegeState siegeState = SiegeState.SIEGE_END;
    public boolean hasFailedTrySiege = false;
    int nextSpawnTime = 0;
    int siegeCount = 0;
    int spawnX;
    int spawnY;
    int spawnZ;

    private static void sendDevLog(String message) {
        if (ConfigTombstone.village_siege.logSiegeState.get()) {
            LOGGER.info(message);
        }
    }

    @Override
    public int func_230253_a_(ServerWorld serverWorld, boolean spawnHostileMobs, boolean spawnPeacefulMobs) {
        /* when it's night */
        if (!serverWorld.isDaytime() && spawnHostileMobs) {
            float angle = serverWorld.getCelestialAngleRadians(0f);
            /* when switch to night */
            if (angle == 0.5d) {
                sendDevLog("Launching a village siege");
                this.siegeState = Helper.getRandom(1, 100) <= ConfigTombstone.village_siege.siegeChance.get() ? SiegeState.SIEGE_START : SiegeState.SIEGE_END;
                sendDevLog("Random chance : Siege " + (this.siegeState == SiegeState.SIEGE_START ? "started" : "aborted"));
            }
            if (siegeState == SiegeState.SIEGE_END) {
                return 0;
            }

            /* try to setup the siege */
            if (this.siegeState == SiegeState.SIEGE_START) {
                if (ConfigTombstone.village_siege.delaySiegeTest.get() > 0 && this.hasFailedTrySiege && !TimeHelper.atInterval(TimeHelper.worldTicks(serverWorld), ConfigTombstone.village_siege.delaySiegeTest.get())) {
                    return 0;
                }
                if (!trySetupSiege(serverWorld) || EventFactory.onVillageSiegeStart(new BlockPos(this.spawnX, this.spawnY, this.spawnZ))) {
                    this.hasFailedTrySiege = true;
                    sendDevLog("Try to siege : failed");
                    return 0;
                }
                this.siegeState = SiegeState.SPAWN_MOBS;
                sendDevLog("Spawning " + ConfigTombstone.village_siege.siegeMaxCreature.get() + " zombie" + (ConfigTombstone.village_siege.siegeMaxCreature.get() > 1 ? "s" : ""));
            }
            /* spawn slowly */
            if (this.siegeState == SiegeState.SPAWN_MOBS) {
                if (this.nextSpawnTime > 0) {
                    --this.nextSpawnTime;
                    return 0;
                } else {
                    this.nextSpawnTime = 2;
                    if (this.siegeCount > 0) {
                        spawnZombie(serverWorld);
                        --this.siegeCount;
                    } else {
                        this.siegeState = SiegeState.SIEGE_END; /* finished */
                        sendDevLog("Siege ended");
                    }
                }
            }
            return 1;
            /* when it's day */
        } else {
            this.siegeState = SiegeState.SIEGE_END;
            this.hasFailedTrySiege = false;
            return 0;
        }
    }

    private boolean trySetupSiege(ServerWorld serverWorld) {
        List<ServerPlayerEntity> playerList = serverWorld.getPlayers();
        if (ConfigTombstone.village_siege.shufflePlayersForSiege.get()) {
            Collections.shuffle(playerList, Helper.random);
        }
        for (ServerPlayerEntity player : playerList) {
            if (!player.isSpectator() && (!player.isCreative() || ConfigTombstone.village_siege.allowCreativePlayersForSiege.get())) {
                BlockPos blockpos = player.getPosition();
                if (serverWorld.isVillage(blockpos) && serverWorld.getBiome(blockpos).getCategory() != Biome.Category.MUSHROOM) {
                    for (int i = 0; i < 10; ++i) {
                        float f = serverWorld.rand.nextFloat() * ((float) Math.PI * 2f);
                        this.spawnX = blockpos.getX() + MathHelper.floor(MathHelper.cos(f) * 32f);
                        this.spawnY = blockpos.getY();
                        this.spawnZ = blockpos.getZ() + MathHelper.floor(MathHelper.sin(f) * 32f);
                    }
                    Vector3d siegeLocation = findRandomSpawnPos(serverWorld, new BlockPos(this.spawnX, this.spawnY, this.spawnZ));
                    // small difference with forge to allow to iterate the players
                    if (siegeLocation != null) {
                        if (MinecraftForge.EVENT_BUS.post(new VillageSiegeEvent(this, serverWorld, player, siegeLocation))) {
                            return false;
                        }
                        this.nextSpawnTime = 0;
                        this.siegeCount = ConfigTombstone.village_siege.siegeMaxCreature.get();
                        break;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void spawnZombie(ServerWorld serverWorld) {
        Vector3d Vector3d = findRandomSpawnPos(serverWorld, new BlockPos(this.spawnX, this.spawnY, this.spawnZ));
        if (Vector3d != null) {
            final ZombieEntity mob;
            try {
                mob = EntityType.ZOMBIE.create(serverWorld);
                if (mob == null) {
                    return;
                }
                mob.onInitialSpawn(serverWorld, serverWorld.getDifficultyForLocation(mob.getPosition()), SpawnReason.EVENT, null, null);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            if (ConfigTombstone.village_siege.persistentMobInSiege.get()) {
                mob.enablePersistence();
            }
            if (ConfigTombstone.village_siege.undeadWearHelmInSiege.get() && mob.getItemStackFromSlot(EquipmentSlotType.HEAD).isEmpty()) {
                Difficulty difficulty = serverWorld.getDifficulty();
                mob.setItemStackToSlot(EquipmentSlotType.HEAD, new ItemStack(difficulty == Difficulty.HARD ? Items.DIAMOND_HELMET : (difficulty == Difficulty.NORMAL ? Items.IRON_HELMET : Items.LEATHER_HELMET)));
            }
            mob.setLocationAndAngles(Vector3d.x, Vector3d.y, Vector3d.z, serverWorld.rand.nextFloat() * 360f, 0f);
            if (EventFactory.onVillageSiegeZombieSpawn(mob)) {
                return;
            }
            mob.getPersistentData().putBoolean("siege", true);
            serverWorld.addEntity(mob);
            if (ConfigTombstone.village_siege.glowingCreatureTest.get()) {
                EffectHelper.addEffect(mob, Effects.GLOWING, 10000);
            }
        }
    }

    @Nullable
    private Vector3d findRandomSpawnPos(ServerWorld serverWorld, BlockPos pos) {
        for (int i = 0; i < 10; ++i) {
            int x = pos.getX() + serverWorld.rand.nextInt(16) - 8;
            int z = pos.getZ() + serverWorld.rand.nextInt(16) - 8;
            int y = serverWorld.getHeight(Heightmap.Type.WORLD_SURFACE, x, z);
            BlockPos blockpos = new BlockPos(x, y, z);
            //isBlockPosWithinSqVillageRadius
            if (serverWorld.isVillage(blockpos) && MonsterEntity.canMonsterSpawnInLight(EntityType.ZOMBIE, serverWorld, SpawnReason.EVENT, blockpos, serverWorld.rand)) {
                return new Vector3d((double) blockpos.getX(), (double) blockpos.getY(), (double) blockpos.getZ());
            }
        }

        return null;
    }

    public enum SiegeState {SIEGE_START, SIEGE_END, SPAWN_MOBS}
}
