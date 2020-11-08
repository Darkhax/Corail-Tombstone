package ovh.corail.tombstone.event;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillageSiege;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.ISpecialSpawner;
import net.minecraft.world.spawner.PhantomSpawner;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import ovh.corail.tombstone.api.capability.ITBCapability;
import ovh.corail.tombstone.api.magic.ModDamages;
import ovh.corail.tombstone.block.BlockGrave;
import ovh.corail.tombstone.block.BlockGraveBase;
import ovh.corail.tombstone.block.BlockGraveMarble.MarbleType;
import ovh.corail.tombstone.block.GraveModel;
import ovh.corail.tombstone.block.ItemBlockGrave;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.command.CommandTBAcceptTeleport;
import ovh.corail.tombstone.command.CommandTBRecovery;
import ovh.corail.tombstone.compatibility.CompatibilityMinecolonies;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.CooldownHandler;
import ovh.corail.tombstone.spawner.CustomPhantomSpawner;
import ovh.corail.tombstone.spawner.CustomVillageSiege;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.DummyTargetEntity;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.InventoryHelper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.LootHelper;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.SpawnProtectionHandler;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.item.ItemGraveMagic;
import ovh.corail.tombstone.item.ItemVoodooPoppet;
import ovh.corail.tombstone.network.PacketHandler;
import ovh.corail.tombstone.network.TombstoneActivatedMessage;
import ovh.corail.tombstone.network.UpdateClientMessage;
import ovh.corail.tombstone.registry.ModBlocks;
import ovh.corail.tombstone.registry.ModEffects;
import ovh.corail.tombstone.registry.ModEnchantments;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModPerks;
import ovh.corail.tombstone.registry.ModTags;
import ovh.corail.tombstone.registry.ModTriggers;
import ovh.corail.tombstone.tileentity.TileEntityGrave;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.IntStream;

import static ovh.corail.tombstone.ModTombstone.*;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event) {
        Helper.initCommands(event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (event.getName().equals(LootTables.GAMEPLAY_FISHING_JUNK)) {
            LootHelper.addLostEntries(event.getTable());
        } else if (ConfigTombstone.loot.treasureLootTable.get().contains(event.getName().toString())) {
            LootHelper.addChestEntries(event.getTable());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerLogued(PlayerEvent.PlayerLoggedInEvent event) {
        // sync server/client
        if (EntityHelper.isValidPlayerMP(event.getPlayer())) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            /* spawn protection & shared datas to client */
            MinecraftServer server = player.getServer();
            assert server != null;
            BlockPos spawnPos = server.getWorld(World.OVERWORLD).getSpawnPoint();
            int range = server.isDedicatedServer() ? server.getSpawnProtectionSize() : 0;
            PacketHandler.sendToPlayer(new UpdateClientMessage(spawnPos, range, Helper.isDateAroundHalloween(LocalDate.now()), Helper.isContributor(player)), player);
            PacketHandler.sendToPlayer(CooldownHandler.INSTANCE.getCooldownPacket(player), player);
            EntityHelper.syncTBCapability(player);
            Helper.triggerAprilFoolsDay(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
            MinecraftServer server = Helper.getServer();
            if (server == null) {
                LOGGER.warn("A mod called the ServerTickEvent from the client side");
            } else {
                long systemTicks = TimeHelper.systemTicks();
                if (TimeHelper.atInterval(systemTicks, TimeHelper.tickFromMinute(1))) {
                    if (ConfigTombstone.recovery.recoveryPlayerEnable.get() && TimeHelper.atInterval(systemTicks, TimeHelper.tickFromMinute(ConfigTombstone.recovery.recoveryPlayerTimer.get()))) {
                        CommandTBRecovery.saveAllPlayers(server, success -> LOGGER.info((success ? LangKey.MESSAGE_RECOVERY_SAVE_PLAYER_SUCCESS : LangKey.MESSAGE_RECOVERY_SAVE_PLAYER_FAILED).asLog()));
                    }
                    CommandTBAcceptTeleport.cleanTickets(systemTicks);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(blockPos -> {
            Block block = event.getWorld().getBlockState(blockPos).getBlock();
            return block.isIn(ModTags.Blocks.graves) || block.isIn(ModTags.Blocks.grave_marbles);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        // prevent abuses with 'ghostly shape' (break block)
        if (ConfigTombstone.general.nerfGhostlyShape.get() && EffectHelper.isPotionActive(event.getPlayer(), ModEffects.ghostly_shape)) {
            event.getPlayer().removePotionEffect(ModEffects.ghostly_shape);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickChest(PlayerInteractEvent.RightClickBlock event) {
        // prevent abuses with 'ghostly shape' (open chest)
        if (!event.getWorld().isRemote && event.getHand() == Hand.MAIN_HAND && ConfigTombstone.general.nerfGhostlyShape.get() && EffectHelper.isPotionActive(event.getPlayer(), ModEffects.ghostly_shape) && event.getWorld().getBlockState(event.getPos()).getBlock() instanceof ChestBlock) {
            event.getPlayer().removePotionEffect(ModEffects.ghostly_shape);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote()) {
            ServerWorld world = (ServerWorld) event.getWorld();

            DimensionType overworld = ((ServerWorld) event.getWorld()).getServer().func_244267_aX().func_230520_a_().getOrThrow(DimensionType.OVERWORLD);
            if (!world.getDimensionType().equals(overworld)) {
                return;
            }
            ImmutableList.Builder<ISpecialSpawner> builder = new ImmutableList.Builder<>();
            boolean valid = false;
            for (ISpecialSpawner spawner : world.field_241104_N_) {
                if (spawner instanceof PhantomSpawner && !(spawner instanceof CustomPhantomSpawner)) {
                    builder.add(new CustomPhantomSpawner());
                    valid = true;
                } else if (spawner instanceof VillageSiege && !(spawner instanceof CustomVillageSiege)) {
                    builder.add(new CustomVillageSiege());
                    valid = true;
                } else {
                    builder.add(spawner);
                }
            }
            if (valid) {
                world.field_241104_N_ = builder.build();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // prevents lightning damages with the effect lightning resistance
        if (event.getSource() == DamageSource.LIGHTNING_BOLT && EffectHelper.isPotionActive(event.getEntityLiving(), ModEffects.lightning_resistance)) {
            event.setCanceled(true);
            return;
        }
        // prevents damages with the effect unstable intangibleness
        if (EffectHelper.isUnstableIntangiblenessActive(event.getEntityLiving())) {
            event.setCanceled(true);
            return;
        }

        // prevents damages with the effect ghostly shape
        if (EntityHelper.isValidPlayer(event.getEntityLiving()) && EffectHelper.isPotionActive(event.getEntityLiving(), ModEffects.ghostly_shape, 5) && Helper.isValidPos(event.getEntityLiving().world, event.getEntityLiving().getPosition())) {
            event.setCanceled(true);
            return;
        }
        // cancel diversion/ghostly shape if needed
        if (event.getSource() != null && EntityHelper.isValidPlayer(event.getSource().getTrueSource())) {
            PlayerEntity player = (PlayerEntity) event.getSource().getTrueSource();
            if (!event.getEntityLiving().equals(player)) {
                if (EffectHelper.isPotionActive(player, ModEffects.ghostly_shape)) {
                    player.removePotionEffect(ModEffects.ghostly_shape);
                    if (EntityHelper.isValidPlayerMP(player)) {
                        ModTriggers.CANCEL_GHOSTLY_SHAPE.trigger((ServerPlayerEntity) player);
                    }
                }
                if (EffectHelper.isPotionActive(player, ModEffects.diversion)) {
                    player.removePotionEffect(ModEffects.diversion);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPreventDeath(LivingDamageEvent event) {
        if (EntityHelper.isValidPlayerMP(event.getEntityLiving()) && event.getEntityLiving().isAlive() && event.getEntityLiving().getHealth() <= event.getAmount()) {
            // prevent death if the player has a 'voodoo poppet'
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
            ItemVoodooPoppet.PoppetProtections prot = ModItems.voodoo_poppet.getPoppetProtections(event.getSource());
            if (prot != null) {
                if (ModItems.voodoo_poppet.preventDeath(player, InventoryHelper.findItemInMainInventory(player, p -> ModItems.voodoo_poppet.canPreventDeath(p, prot)), prot)) {
                    event.setCanceled(true);
                    prot.getLangKey().sendMessage(player, StyleType.MESSAGE_SPECIAL);
                    ModTriggers.PREVENT_DEATH.get(prot).trigger(player);
                    return;
                }
            }
            // prevent death if the player has a 'soul receptacle', or with the config option preventDeathOutsideWorld
            boolean preventDeathOutsideWorld = ConfigTombstone.player_death.preventDeathOutsideWorld.get() && !Helper.isValidPos(player.world, player.getPosition());
            ItemStack soul = !preventDeathOutsideWorld ? InventoryHelper.findItemInMainInventory(player, p -> p.getItem() == ModItems.soul_receptacle) : ItemStack.EMPTY;
            if (preventDeathOutsideWorld || !soul.isEmpty()) {
                Location spawnPos = new SpawnHelper((ServerWorld) player.world, Helper.getCloserValidPos(player.world, new BlockPos(player.getPositionVec()))).findSpawnPlace(false);
                if (!spawnPos.isOrigin()) {
                    event.setCanceled(true);
                    if (!preventDeathOutsideWorld) {
                        soul.shrink(1);
                    }
                    player.setHealth(player.getMaxHealth());
                    EffectHelper.clearBadEffects(player);
                    EffectHelper.addEffect(player, Effects.SATURATION, 1200, 9);
                    EffectHelper.addEffect(player, Effects.REGENERATION, 1200, 9);
                    EffectHelper.addEffect(player, ModEffects.diversion, 1200);
                    (preventDeathOutsideWorld ? LangKey.MESSAGE_CONFIG_PREVENT_DEATH : LangKey.MESSAGE_SOUL_PREVENT_DEATH).sendMessage(player);
                    Helper.teleportEntity(player, spawnPos);
                    player.fallDistance = 0f;
                    return;
                }
            }
            if (ConfigTombstone.recovery.backupOnDeath.get() && !event.getEntityLiving().isSpectator()) {
                CommandTBRecovery.savePlayer((ServerPlayerEntity) event.getEntityLiving(), success -> LOGGER.info((success ? LangKey.MESSAGE_RECOVERY_SAVE_PLAYER_SUCCESS : LangKey.MESSAGE_RECOVERY_SAVE_PLAYER_FAILED).getText(event.getEntityLiving().getName()).getString()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCheckAttack(LivingAttackEvent event) {
        if (Helper.isAprilFoolsDaySnowball(event.getEntityLiving(), event.getSource())) {
            EffectHelper.addEffect(event.getEntityLiving(), Effects.SLOWNESS, 200);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCheckAttack(LivingSetAttackTargetEvent event) {
        if (EntityHelper.isValidPlayer(event.getEntityLiving()) || event.getTarget() == null) {
            return;
        }
        // apply the effect 'ghostly shape' & the enchant 'shadow_step'
        LivingEntity attacker = event.getEntityLiving();
        if (EffectHelper.isPotionActive(event.getTarget(), ModEffects.ghostly_shape) || EffectHelper.isPotionActive(event.getTarget(), ModEffects.diversion)) {
            // AT to avoid to fire this event again
            ((MobEntity) attacker).attackTarget = null;
            if (event.getTarget().equals(attacker.revengeTarget)) {
                attacker.revengeTarget = new DummyTargetEntity(attacker.world);
            }
            return;
        }
        if (!SharedConfigTombstone.enchantments.enableEnchantmentShadowStep.get() || (ConfigTombstone.enchantments.restrictShadowStepToPlayer.get() && !EntityHelper.isValidPlayer(event.getTarget()))) {
            return;
        }
        if (SharedConfigTombstone.enchantments.nerfShadowStep.get() && !event.getTarget().isSneaking()) {
            return;
        }
        if (!event.getTarget().equals(attacker.revengeTarget) && !event.getTarget().isPassenger()) {
            int lvl = Math.min(EntityHelper.getEnchantmentLevel(event.getTarget(), ModEnchantments.shadow_step), 5);
            if (lvl > 0) {
                AttributeModifierManager attributeManager = attacker.getAttributeManager();
                double range = attributeManager.hasAttributeInstance(Attributes.FOLLOW_RANGE) ? attributeManager.getAttributeValue(Attributes.FOLLOW_RANGE) : 16d;
                double mult = MathHelper.clamp((event.getTarget().isSneaking() ? 0.6d : 1d) - ((double) lvl * 0.2d) + (attacker.world.isDaytime() ? 0.5d : 0d), 0.05d, 1d);
                if (attacker.getDistance(event.getTarget()) < (range * mult)) {
                    attacker.revengeTarget = event.getTarget();
                    attacker.revengeTimer = attacker.ticksExisted;
                } else {
                    ((MobEntity) attacker).attackTarget = null;
                    if (event.getTarget().equals(attacker.revengeTarget)) {
                        attacker.revengeTarget = new DummyTargetEntity(attacker.world);
                    }
                }
            }
        }
    }

    /**
     * restore effects on death & provide the effect 'ghostly shape'
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!EntityHelper.isValidPlayerMP(event.getPlayer()) || event.getPlayer().isSpectator()) {
            return;
        }
        EntityHelper.syncTBCapability((ServerPlayerEntity) event.getPlayer());

        CompoundNBT persistentTag = EntityHelper.getPersistentTag(event.getPlayer());
        ListNBT stackList = persistentTag.getList("tb_soulbound_stacks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < stackList.size(); i++) {
            ItemStack stack = ItemStack.read(stackList.getCompound(i));
            if (!stack.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(event.getPlayer(), stack);
            }
        }
        persistentTag.remove("tb_soulbound_stacks");

        DeathHandler deathHandler = DeathHandler.INSTANCE;
        /* if dead not in creative */
        if (deathHandler.isPlayerDead(event.getPlayer())) {
            /* auto equip items */
            if (ConfigTombstone.player_death.autoEquipOnDeathRespawn.get()) {
                ListIterator<ItemStack> it = event.getPlayer().inventory.mainInventory.listIterator();
                while (it.hasNext()) {
                    if (InventoryHelper.autoequip(it.next(), event.getPlayer())) {
                        it.set(ItemStack.EMPTY);
                    }
                }
            }
            /* add effects if needed */
            deathHandler.restorePlayerDead(event.getPlayer());
        }
        event.getPlayer().container.detectAndSendChanges();
    }

    /**
     * chance to add drops to undead
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUndeadDrops(LivingDropsEvent event) {
        /* if it's not a player */
        if (!(event.getEntityLiving() instanceof PlayerEntity)) {
            LootHelper.handleMobDrops(event.getDrops(), event.getEntityLiving(), event.getSource());
        }
    }

    /**
     * restore xp, transfer grave's keys
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void give(PlayerEvent.Clone event) {
        if (!EntityHelper.isValidPlayer(event.getOriginal()) || !EntityHelper.isValidPlayer(event.getPlayer())) {
            return;
        }
        PlayerEntity player = event.getPlayer();
        PlayerEntity original = event.getOriginal();
        /* copy player capability */
        player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).ifPresent(capPlayer -> {
            LazyOptional<ITBCapability> capHolder = original.getCapability(TBCapabilityProvider.TB_CAPABILITY, null);
            if (!capHolder.isPresent()) {
                original.revive();
            }
            capHolder.ifPresent(capPlayer::copyCapability);
        });
        /* only if the player was dead */
        if (!event.isWasDeath()) {
            return;
        }
        /* restore experience if needed */
        if (EntityHelper.isValidPlayer(player) && !player.isSpectator()) {
            boolean hasPreservation = EffectHelper.isPotionActive(player, ModEffects.preservation);
            if (hasPreservation || SharedConfigTombstone.player_death.handlePlayerXp.get()) {
                CompoundNBT originalPersistentTag = EntityHelper.getPersistentTag(original);
                int originalExperienceTotal = originalPersistentTag.getInt("tb_experience_total");
                original.experienceTotal = EntityHelper.getPlayerTotalXp(original);
                if (hasPreservation || SharedConfigTombstone.player_death.xpLoss.get() == 0) {
                    player.experienceTotal = originalExperienceTotal;
                    player.experience = originalPersistentTag.getFloat("tb_experience_bar");
                    player.experienceLevel = originalPersistentTag.getInt("tb_experience_level");
                } else {
                    int bonus = EntityHelper.getPerkLevelWithBonus(player, ModPerks.memento_mori) * 20;
                    EntityHelper.setPlayerXp(player, MathHelper.floor(originalExperienceTotal * MathHelper.clamp(100 + bonus - SharedConfigTombstone.player_death.xpLoss.get(), 0, 100) / 100d));
                }
            }
        }
    }

    /**
     * log if this event is canceled for player if tombstone handles player's death
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!EntityHelper.isValidPlayerMP(event.getEntityLiving())) {
            return;
        }
        final ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        if (event.isCanceled()) {
            if (ConfigTombstone.player_death.handlePlayerDeath.get()) {
                LOGGER.warn("The death event of the player " + player.getName() + " was cancelled by another mod");
            }
        } else {
            player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).ifPresent(cap -> {
                int knowledgeLoss = ConfigTombstone.general.allowBeyondTheGraveDamage.get() && event.getSource() == ModDamages.BEYOND_THE_GRAVE ? 10 : 0;
                if (ConfigTombstone.player_death.knowledgeLoss.get() > 0) {
                    knowledgeLoss += ConfigTombstone.player_death.knowledgeLoss.get();
                }
                if (knowledgeLoss > 0) {
                    cap.removeKnowledgeAndSync(player, knowledgeLoss);
                }
            });
            DeathHandler.INSTANCE.addPlayerDead(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() != null && event.getSource() != null && EntityHelper.isValidPlayerMP(event.getSource().getTrueSource())) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getSource().getTrueSource();
            assert player != null;
            if (!CompatibilityMinecolonies.instance.applyKillResult(player, event.getEntityLiving().getType())) {
                if (event.getEntityLiving() instanceof VillagerEntity) {
                    EntityHelper.addAlignment((ServerPlayerEntity) event.getSource().getTrueSource(), ConfigTombstone.alignment.pointsKillVillager.get());
                } else if (EntityTypeTags.RAIDERS.contains(event.getEntityLiving().getType()) || (event.getEntityLiving() instanceof ZombieEntity && event.getEntityLiving().getPersistentData().getBoolean("siege")) || event.getEntityLiving() instanceof PhantomEntity) {
                    EntityHelper.addAlignment((ServerPlayerEntity) event.getSource().getTrueSource(), ConfigTombstone.alignment.pointsKillRaider.get(), ConfigTombstone.alignment.chanceKillRaider.get());
                }
            }
        }
    }

    /**
     * remove experience balls
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (SharedConfigTombstone.player_death.handlePlayerXp.get() && EntityHelper.isValidPlayer(event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    private static void storeSoulboundsOnBody(PlayerEntity player, List<ItemStack> keys, List<ItemStack> soulbounds) {
        CompoundNBT persistentTag = EntityHelper.getPersistentTag(player);
        ListNBT stackList = new ListNBT();
        persistentTag.put("tb_soulbound_stacks", stackList);
        for (ItemStack key : keys) {
            stackList.add(key.serializeNBT());
        }
        keys.clear();
        for (ItemStack soulbound : soulbounds) {
            stackList.add(soulbound.serializeNBT());
        }
        soulbounds.clear();
    }

    /**
     * handle the drops of the player when dying
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!EntityHelper.isValidPlayerMP(event.getEntityLiving())) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        if (Helper.isRuleKeepInventory(player)) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        if (event.isCanceled()) {
            LOGGER.warn("The drop event of the player " + player.getGameProfile().getName() + " was cancelled by another mod");
            return;
        }

        // collect all grave's keys & soulbound items
        Iterator<ItemEntity> it = event.getDrops().iterator();
        List<ItemStack> keys = new ArrayList<>();
        List<ItemStack> soulbounds = new ArrayList<>();
        while (it.hasNext()) {
            ItemEntity entityItem = it.next();
            if (entityItem == null || entityItem.getItem().isEmpty()) {
                continue;
            }
            ItemStack stack = entityItem.getItem();
            if (stack.getItem() == ModItems.grave_key) {
                keys.add(stack.copy());
                it.remove();
            } else if (SharedConfigTombstone.enchantments.enableEnchantmentSoulbound.get() && EnchantmentHelper.getEnchantmentLevel(ModEnchantments.soulbound, stack) > 0) {
                soulbounds.add(stack.copy());
                it.remove();
            }
        }
        // sniffer of entityItems, using player's death position
        double range = (double) ConfigTombstone.player_death.snifferRange.get();
        List<ItemEntity> itemList = player.world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB(player.getPosX() - range, player.getPosY() - range, player.getPosZ() - range, player.getPosX() + range, player.getPosY() + range, player.getPosZ() + range));
        it = itemList.listIterator();
        while (it.hasNext()) {
            ItemEntity entityItem = it.next();
            if (entityItem == null || entityItem.getItem().isEmpty()) {
                continue;
            }
            ItemStack stack = entityItem.getItem();
            if (stack.getItem() == ModItems.grave_key) {
                keys.add(stack.copy());
                it.remove();
            } else if (SharedConfigTombstone.enchantments.enableEnchantmentSoulbound.get() && EnchantmentHelper.getEnchantmentLevel(ModEnchantments.soulbound, stack) > 0) {
                soulbounds.add(stack.copy());
                it.remove();
            }
        }
        // no drop
        boolean hasDrop = itemList.size() > 0 || event.getDrops().size() > 0;
        if (!ConfigTombstone.player_death.handlePlayerDeath.get() || !hasDrop) {
            ModItems.grave_key.reenchantOnDeath(player, keys.stream().filter(p -> !ModItems.grave_key.isEnchanted(p)).findFirst().orElse(ItemStack.EMPTY));
            storeSoulboundsOnBody(player, keys, soulbounds);
            if (!hasDrop) {
            	LangKey.MESSAGE_NO_LOOT_FOR_GRAVE.sendMessage(player, StyleType.MESSAGE_SPECIAL);
            }
            return;
        }

        DeathHandler deathHandler = DeathHandler.INSTANCE;

        // check if the area requires no grave
        if (deathHandler.isNoGraveLocation(new Location(player))) {
        	LangKey.MESSAGE_NO_GRAVE_LOCATION.sendMessage(player, StyleType.MESSAGE_SPECIAL);
            storeSoulboundsOnBody(player, keys, soulbounds);
            return;
        }

        BlockPos initPos = Helper.getCloserValidPos(world, new BlockPos(player.getPosition()));

        // check if there's a grave of this player in the chunk and also if the last grave is close (with enough spaces to fill it with drops)
        Location spawnPos = Location.ORIGIN;
        if (ConfigTombstone.player_death.allowToFillExistingGrave.get()) {
            TileEntityGrave tileGrave = ((Chunk) world.getChunk(initPos)).getTileEntityMap().values().stream().filter(grave -> grave instanceof TileEntityGrave && ((TileEntityGrave) grave).getOwnerName().equals(player.getGameProfile().getName()) && ((TileEntityGrave) grave).hasEmptySlots(event.getDrops().size())).map(grave -> (TileEntityGrave) grave).findFirst().orElse(null);
            if (tileGrave != null) {
                if (ModBlocks.graves.containsValue(world.getBlockState(tileGrave.getPos()).getBlock())) { // sensible place to use isIn(ModTags.Blocks.player_graves)
                    spawnPos = new Location(tileGrave.getPos(), world);
                } else {
                    tileGrave.dropOnGroundAndRemove();
                }
            }
            if (spawnPos.isOrigin()) {
                Location lastGrave = deathHandler.getLastGrave(player.getGameProfile().getName());
                if (!lastGrave.isOrigin() && lastGrave.dim.equals(world.getDimensionKey()) && Helper.getDistanceSq(lastGrave.getPos(), initPos) <= 400d) {
                    TileEntity tile = world.getTileEntity(lastGrave.getPos());
                    if (tile instanceof TileEntityGrave) {
                        TileEntityGrave grave = (TileEntityGrave) tile;
                        if (world.getBlockState(lastGrave.getPos()).isIn(ModTags.Blocks.player_graves)) {
                            if (grave.hasEmptySlots(event.getDrops().size())) {
                                spawnPos = lastGrave;
                            }
                        } else {
                            grave.dropOnGroundAndRemove();
                        }
                    }
                }
            }
        }
        boolean hasGrave = !spawnPos.isOrigin();

        // search for a grave placement
        if (!hasGrave) {
            spawnPos = new SpawnHelper(world, initPos).findSpawnPlace(true);
            if (spawnPos.isOrigin()) {
                storeSoulboundsOnBody(player, keys, soulbounds);
                LangKey.MESSAGE_NO_PLACE_FOR_GRAVE.sendMessage(player, StyleType.MESSAGE_SPECIAL);
                LOGGER.info("There was nowhere to place the grave!");
                return;
            }
        }

        BlockState state;
        if (!hasGrave) {
            deathHandler.logLastGrave(player, spawnPos.x, spawnPos.y, spawnPos.z, spawnPos.dim);
            Direction facing = player.getHorizontalFacing().getOpposite();
            Pair<GraveModel, MarbleType> favoriteGrave = deathHandler.getFavoriteGrave(player);
            Block graveBlock = ModBlocks.graves.get(favoriteGrave.getLeft());
            state = graveBlock.getDefaultState().with(BlockGraveBase.FACING, facing).with(BlockGraveBase.IS_ENGRAVED, true).with(BlockGraveBase.MODEL_TEXTURE, favoriteGrave.getRight().ordinal());
            Helper.placeNoEvent(world, spawnPos.getPos(), state);
        } else {
            state = world.getBlockState(spawnPos.getPos());
        }
        TileEntity tile = world.getTileEntity(spawnPos.getPos());
        if (!(tile instanceof TileEntityGrave)) {
            storeSoulboundsOnBody(player, keys, soulbounds);
            LangKey.MESSAGE_FAIL_TO_PLACE_GRAVE.sendMessage(player, StyleType.MESSAGE_SPECIAL);
            LOGGER.info(LangKey.MESSAGE_FAIL_TO_PLACE_GRAVE.getText()); // Server lang
            return;
        }
        deathHandler.setLastDeathLocation(player, new Location(spawnPos.x, spawnPos.y + 1, spawnPos.z, spawnPos.dim));
        TileEntityGrave grave = (TileEntityGrave) tile;
        if (hasGrave) {
            grave.resetDeathTime();
            grave.countTicks = 0;
        }
        boolean needAccess = ConfigTombstone.player_death.playerGraveAccess.get() && SharedConfigTombstone.player_death.decayTime.get() != 0;
        if (needAccess && ConfigTombstone.player_death.pvpMode.get() && event.getSource() != null && event.getSource().getTrueSource() instanceof PlayerEntity) {
            needAccess = false;
        }
        (hasGrave ? LangKey.MESSAGE_EXISTING_GRAVE : LangKey.MESSAGE_NEW_GRAVE).sendMessage(player, StyleType.MESSAGE_SPECIAL,
                LangKey.MESSAGE_JOURNEYMAP.getText(StyleType.TOOLTIP_DESC, LangKey.MESSAGE_LAST_GRAVE.getText(), spawnPos.x, spawnPos.y, spawnPos.z, spawnPos.dim.getLocation().toString()),
                (needAccess ? LangKey.MESSAGE_LOCKED : LangKey.MESSAGE_UNLOCKED).getText(needAccess && SharedConfigTombstone.player_death.decayTime.get() > 0 ? SharedConfigTombstone.player_death.decayTime.get() + " min" : "").setStyle(needAccess ? StyleType.COLOR_OFF : StyleType.COLOR_ON)
        );
        if (ConfigTombstone.player_death.playerGraveAccess.get()) {
            if (hasGrave) {
                ItemStack key = ItemStack.EMPTY;
                for (ItemStack aKey : keys) {
                    if (ModItems.grave_key.getTombPos(aKey).equals(spawnPos)) {
                        ModItems.grave_key.reenchantOnDeath(player, aKey);
                        key = aKey;
                        break;
                    }
                }
                if (key.isEmpty()) {
                    key = new ItemStack(ModItems.grave_key);
                    ModItems.grave_key.setTombPos(key, spawnPos);
                    ModItems.grave_key.reenchantOnDeath(player, key);
                    keys.add(0, key);
                }
                // if it's a new grave, add a grave key to player inventory
            } else {
                ItemStack key = new ItemStack(ModItems.grave_key);
                ModItems.grave_key.setTombPos(key, spawnPos);
                ModItems.grave_key.reenchantOnDeath(player, key);
                keys.add(0, key);
            }
        }
        // store the soulbounds and keys in the dead body
        storeSoulboundsOnBody(player, keys, soulbounds);
        // owner infos
        grave.setOwner(player, TimeHelper.systemTime(), needAccess);
        // fill tombstone with items
        for (ItemEntity entityItem : event.getDrops()) {
            if (entityItem != null && !entityItem.getItem().isEmpty()) {
                grave.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(itemHandler -> ItemHandlerHelper.insertItemStacked(itemHandler, entityItem.getItem().copy(), false));
                entityItem.setItem(ItemStack.EMPTY);
            }
        }
        // sniffer fills tombstone
        for (ItemEntity entityItem : itemList) {
            grave.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(itemHandler -> ItemHandlerHelper.insertItemStacked(itemHandler, entityItem.getItem().copy(), false));
            entityItem.setItem(ItemStack.EMPTY);
        }
        world.notifyBlockUpdate(spawnPos.getPos(), Blocks.AIR.getDefaultState(), state, 2);
        ModTriggers.FIRST_GRAVE.trigger(player);
    }

    /**
     * recipes for engravement
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (event.getName() == null || event.getLeft().isEmpty() || event.getRight().isEmpty()) {
            return;
        }
        if (!event.getName().isEmpty() && event.getRight().getItem().isIn(Tags.Items.INGOTS_IRON) && event.getLeft().getItem() instanceof ItemBlockGrave) {
            ItemStack output = event.getLeft().copy();
            if (ItemBlockGrave.setEngravedName(output, event.getName())) {
                event.setCost(2);
                event.setOutput(output);
                event.setMaterialCost(1);
            } else {
                cancelAnvilRecipe(event);
            }
        } else if (!event.getName().isEmpty() && ModItems.tablet_of_assistance.isEnchanted(event.getLeft()) && event.getRight().getItem() == ModItems.grave_dust) {
            ItemStack output = event.getLeft().copy();
            if (ModItems.tablet_of_assistance.setEngravedName(output, event.getName())) {
                event.setCost(2);
                event.setOutput(output);
                event.setMaterialCost(1);
            } else {
                cancelAnvilRecipe(event);
            }
        }
    }

    private static void cancelAnvilRecipe(AnvilUpdateEvent event) {
        event.setCost(0);
        event.setMaterialCost(0);
        event.setOutput(ItemStack.EMPTY);
    }

    /**
     * only for the advancement
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (EntityHelper.isValidPlayerMP(event.getPlayer()) && event.getItemInput().getItem() instanceof ItemBlockGrave && event.getIngredientInput().getItem().isIn(Tags.Items.INGOTS_IRON) && ItemBlockGrave.isEngraved(event.getItemResult())) {
            ModTriggers.ENGRAVE_DECORATIVE_GRAVE.trigger((ServerPlayerEntity) event.getPlayer());
        }
    }

    /**
     * allow to activate graves with Claimed Block mod & Vanilla Spawn Protection
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void uncancelGraveRightClick(PlayerInteractEvent.RightClickBlock event) {
        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
        if (block instanceof BlockGrave) {
            if (event.isCanceled()) {
                event.setCanceled(false);
                event.setUseBlock(Event.Result.DEFAULT);
                event.setUseItem(Event.Result.DEFAULT);
            }
            if (event.getWorld().isRemote && SpawnProtectionHandler.getInstance().isBlockProtected(event.getWorld().getDimensionKey(), event.getPos())) {
                PacketHandler.sendToServer(new TombstoneActivatedMessage(event.getPos()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUpdateShadowStep(LivingUpdateEvent event) {
        if (event.getEntityLiving().world.isRemote && SharedConfigTombstone.enchantments.enableEnchantmentShadowStep.get() && ConfigTombstone.client.showShadowStep.get() && (!SharedConfigTombstone.enchantments.nerfShadowStep.get() || event.getEntity().isSneaking()) && EntityHelper.hasEnchantment(event.getEntityLiving(), ModEnchantments.shadow_step)) {
            PROXY.produceShadowStep(event.getEntityLiving());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getItem().getItem() == Items.POTION && EntityHelper.isValidPlayerMP(event.getEntityLiving()) && PotionUtils.getEffectsFromStack(event.getItem()).size() > 0) {
            event.getEntityLiving().getPersistentData().putBoolean("is_drinking_potion", true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
        if (isDrinking(event.getEntityLiving())) {
            resetDrinking(event.getEntityLiving());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (isDrinking(event.getEntityLiving())) {
            resetDrinking(event.getEntityLiving());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionAdded(PotionEvent.PotionAddedEvent event) {
        if (EntityHelper.isValidPlayer(event.getEntityLiving()) && isDrinking(event.getEntityLiving()) && event.getPotionEffect().duration > 1200 && EffectHelper.isAllowedEffect(event.getPotionEffect().getPotion())) {
            float bonus = 1 + (EntityHelper.getPerkLevelWithBonus((PlayerEntity) event.getEntityLiving(), ModPerks.alchemist) * 0.1f);
            EffectHelper.modifyEffectDuration(event.getPotionEffect(), e -> MathHelper.floor(e.duration * bonus));
        }
    }

    private static boolean isDrinking(LivingEntity entityLiving) {
        return entityLiving.getPersistentData().contains("is_drinking_potion", Constants.NBT.TAG_BYTE);
    }

    private static void resetDrinking(LivingEntity entityLiving) {
        entityLiving.getPersistentData().remove("is_drinking_potion");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPreventFamiliarDeath(LivingDamageEvent event) {
        if (event.getEntityLiving() != null && event.getEntityLiving().isAlive() && event.getEntityLiving().getHealth() <= event.getAmount() && ModItems.familiar_receptacle.captureSoul(event.getEntityLiving())) {
            event.setCanceled(true);
            event.getEntityLiving().remove(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityDamage(LivingDamageEvent event) {
        if (event.getSource() == null || !EntityHelper.isValidPlayerMP(event.getEntityLiving()) || !event.getEntityLiving().isAlive()) {
            return;
        }
        if (event.getSource() == ModDamages.BEYOND_THE_GRAVE) {
            // scale BeyondTheGrave damage
            if (ConfigTombstone.general.allowBeyondTheGraveDamage.get()) {
                event.getEntityLiving().getCapability(TBCapabilityProvider.TB_CAPABILITY, null).ifPresent(cap -> {
                    int knowledgeLevel = cap.getTotalPerkPoints();
                    if (knowledgeLevel > 0) {
                        event.setAmount(event.getAmount() * (1 - Math.min(20, knowledgeLevel) * 0.05f));
                    }
                });
            }
        } else {
            // scale damages based on alignment
            int alignmentLevel = event.getEntityLiving().getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(ITBCapability::getAlignmentLevel).orElse(0);
            if (alignmentLevel != 0 && event.getSource().getTrueSource() instanceof MobEntity) {
                boolean valid = ((MobEntity) event.getSource().getTrueSource()).isEntityUndead() ? alignmentLevel > 0 : alignmentLevel < 0;
                if (valid) {
                    float amount = event.getAmount() * (1f - (0.1f * Math.abs(alignmentLevel)));
                    if (amount < 0.5f) {
                        event.setCanceled(true);
                    } else {
                        event.setAmount(amount);
                    }
                }
            }
        }
    }

    /**
     * prevents some items to interact with entities
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        Item heldItem = event.getPlayer().getHeldItem(event.getHand()).getItem();
        if (heldItem == ModItems.dust_of_vanishing || heldItem instanceof ItemGraveMagic) {
            event.setCancellationResult(ActionResultType.PASS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            event.addCapability(TBCapabilityProvider.RL, new TBCapabilityProvider());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSyncOnChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (EntityHelper.isValidPlayer(event.getPlayer()) && !event.getPlayer().world.isRemote) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            EntityHelper.syncTBCapability(player);
            MinecraftServer server = event.getPlayer().getServer();
            if (server == null) {
                return;
            }
            ServerWorld fromDim = server.getWorld(event.getFrom());
            long fromTime = fromDim.getGameTime();
            ServerWorld toDim = server.getWorld(event.getTo());
            long toTime = toDim.getGameTime();
            if (fromTime != toTime) {
                CooldownHandler.INSTANCE.updateWorldTime(player, toTime - fromTime);
                event.getPlayer().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(inventPlayer -> IntStream.range(0, inventPlayer.getSlots()).mapToObj(inventPlayer::getStackInSlot).filter(stack -> stack.getItem() == ModItems.lost_tablet).forEach(stack -> ModItems.lost_tablet.setCooldown(toDim, stack, Math.min(ModItems.lost_tablet.getCooldown(fromDim, stack), TimeHelper.tickFromMinute(10)))));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        ItemStack mainHandStack;
        if (event.getHand() == Hand.MAIN_HAND && EntityHelper.isValidPlayer(event.getPlayer()) && (mainHandStack = event.getPlayer().getHeldItemMainhand()).getItem() == ModItems.bone_needle && event.getTarget() instanceof LivingEntity) {
            if (mainHandStack.interactWithEntity(event.getPlayer(), (LivingEntity) event.getTarget(), event.getHand()).isSuccess()) {
                event.setCancellationResult(ActionResultType.SUCCESS);
                event.setCanceled(true);
            }
        }
    }
}
