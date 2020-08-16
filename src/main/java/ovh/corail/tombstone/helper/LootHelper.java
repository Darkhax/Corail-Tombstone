package ovh.corail.tombstone.helper;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.EmptyLootEntry;
import net.minecraft.loot.ItemLootEntry;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.functions.SetNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Util;
import ovh.corail.tombstone.block.ItemBlockGrave;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.item.ItemGeneric;
import ovh.corail.tombstone.item.ItemScrollBuff;
import ovh.corail.tombstone.item.ItemTablet;
import ovh.corail.tombstone.item.ItemVoodooPoppet;
import ovh.corail.tombstone.loot.InOpenWaterCondition;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModPerks;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;
import java.util.function.Consumer;

public class LootHelper {

    private static void addEntry(LootPool.Builder builder, Item item, int weight, Consumer<CompoundNBT> consumer) {
        builder.addEntry(ItemLootEntry.builder(item).quality(-2).weight(weight).acceptFunction(SetNBT.builder(Util.make(new CompoundNBT(), consumer))));
    }

    private static void addEnchantedEntry(LootPool.Builder builder, Item item, int weight) {
        addEntry(builder, item, weight, nbt -> nbt.putBoolean("enchant", true));
    }

    private static void addAncientTablet(LootPool.Builder builder, ItemTablet item) {
        addEntry(builder, item, 3, nbt -> {
            nbt.putBoolean("enchant", true);
            nbt.putBoolean("ancient", true);
        });
    }

    public static void addLostEntries(LootTable table) {
        // adds lost tablet in fishing junk loot table, as a separate pool
        LootPool.Builder builder = new LootPool.Builder().name("tombstone:lost_treasure");
        builder.acceptCondition(InOpenWaterCondition.builder());
        boolean valid = false;
        if (ModItems.tablet_of_recall.isEnabled()) {
            addAncientTablet(builder, ModItems.tablet_of_recall);
            valid = true;
        }
        if (ModItems.tablet_of_home.isEnabled()) {
            addAncientTablet(builder, ModItems.tablet_of_home);
            valid = true;
        }
        int lostTabletChance = SharedConfigTombstone.loot.chanceLootLostTablet.get();
        if (ModItems.lost_tablet.isEnabled() && lostTabletChance > 0) {
            builder.addEntry(ItemLootEntry.builder(ModItems.lost_tablet).quality(-2).weight(lostTabletChance));
            valid = true;
        }
        if (valid) {
            if (lostTabletChance < 1000) {
                builder.addEntry(EmptyLootEntry.func_216167_a().quality(-2).weight(1000 - lostTabletChance));
            }
            table.addPool(builder.build());
        }
    }

    public static void addChestEntries(LootTable table) {
        // adds loots in structure chests
        LootPool.Builder builder = new LootPool.Builder().name("tombstone:chest_treasure");
        int weight = 0;
        for (ItemGeneric scroll : ModItems.scroll_buff) {
            if (scroll.isEnabled()) {
                addEnchantedEntry(builder, scroll, 1);
                weight++;
            }
        }
        if (ModItems.scroll_of_knowledge.isEnabled()) {
            addEntry(builder, ModItems.scroll_of_knowledge, 1, nbt -> nbt.putInt("stored_xp", 2000));
            weight++;
        }
        if (ModItems.tablet_of_cupidity.isEnabled()) {
            addEnchantedEntry(builder, ModItems.tablet_of_cupidity, 1);
            weight++;
        }
        if (ModItems.tablet_of_home.isEnabled()) {
            addEnchantedEntry(builder, ModItems.tablet_of_home, 1);
            weight++;
        }
        if (weight == 0) {
            return;
        }
        builder.addEntry(EmptyLootEntry.func_216167_a().weight(100 - weight));
        table.addPool(builder.build());
    }

    public static void handleMobDrops(Collection<ItemEntity> drops, LivingEntity entity, @Nullable DamageSource damageSource) {
        ServerPlayerEntity player = damageSource != null && EntityHelper.isValidPlayerMP(damageSource.getTrueSource()) ? (ServerPlayerEntity) damageSource.getTrueSource() : null;
        if (player == null) {
            return;
        }
        Random rng = player.getRNG();
        if (entity.isEntityUndead()) {
            final float boneCollectorBonus = EntityHelper.getPerkLevelWithBonus(player, ModPerks.bone_collector) * 0.01f;

            float chanceGraveDust = ConfigTombstone.loot.chanceGraveDust.get() * 0.001f + boneCollectorBonus;
            if (chanceGraveDust >= 1f || rng.nextFloat() <= chanceGraveDust) {
                addDropToEntity(drops, entity, new ItemStack(ModItems.grave_dust, rng.nextBoolean() ? 1 : 2));
            }
            if (ConfigTombstone.loot.undeadCanDropSkull.get() && rng.nextFloat() <= 0.005f) {
                ItemStack skullStack = new ItemStack(entity instanceof ZombieEntity ? Items.ZOMBIE_HEAD : Items.SKELETON_SKULL);
                addDropToEntity(drops, entity, skullStack);
            }
            if (!entity.isNonBoss()) {
                float chanceSoulReceptacleOnBoss = ConfigTombstone.loot.chanceSoulReceptacleOnBoss.get() * 0.001f + boneCollectorBonus;
                if (chanceSoulReceptacleOnBoss >= 1f || rng.nextFloat() <= chanceSoulReceptacleOnBoss) {
                    addDropToEntity(drops, entity, new ItemStack(ModItems.soul_receptacle));
                }
                float chanceRandomScrollOnBoss = ConfigTombstone.loot.chanceRandomScrollOnBoss.get() * 0.001f + boneCollectorBonus;
                if (chanceRandomScrollOnBoss >= 1f || rng.nextFloat() <= chanceRandomScrollOnBoss) {
                    ItemScrollBuff scroll = ModItems.scroll_buff[ItemScrollBuff.SpellBuff.getRandomBuff().ordinal()];
                    if (scroll.isEnabled()) {
                        addDropToEntity(drops, entity, NBTStackHelper.setBoolean(new ItemStack(scroll), "enchant", true));
                    }
                }
                float chanceRandomPoppetOnBoss = ConfigTombstone.loot.chanceRandomPoppetOnBoss.get() * 0.001f + boneCollectorBonus;
                if (ModItems.voodoo_poppet.isEnabled() && chanceRandomPoppetOnBoss >= 1f || rng.nextFloat() <= chanceRandomPoppetOnBoss) {
                    addDropToEntity(drops, entity, NBTStackHelper.setBoolean(ModItems.voodoo_poppet.addProtection(new ItemStack(ModItems.voodoo_poppet), ItemVoodooPoppet.PoppetProtections.getRandomProtection()), "enchant", true));
                }
                float chanceDecorativeGraveOnBoss = ConfigTombstone.loot.chanceDecorativeGraveOnBoss.get() * 0.001f + boneCollectorBonus;
                if (chanceDecorativeGraveOnBoss >= 1f || rng.nextFloat() <= chanceDecorativeGraveOnBoss) {
                    addDropToEntity(drops, entity, ItemBlockGrave.createRandomDecorativeStack());
                }
            }
        }
        if ((Helper.isContributor(player) || Helper.isDateAroundHalloween()) && entity instanceof MonsterEntity && rng.nextFloat() <= 0.1f) {
            addDropToEntity(drops, entity, new ItemStack(ModItems.lollipop[player.world.rand.nextInt(ModItems.lollipop.length)]));
        }
    }

    private static void addDropToEntity(Collection<ItemEntity> drops, LivingEntity entity, ItemStack... stacks) {
        for (ItemStack stack : stacks) {
            drops.add(new ItemEntity(entity.world, entity.getPosX(), entity.getPosY(), entity.getPosZ(), stack));
        }
    }
}
