package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.network.PacketHandler;
import ovh.corail.tombstone.network.SmokeColumnMessage;
import ovh.corail.tombstone.registry.ModEffects;

import javax.annotation.Nullable;
import java.util.List;

public class ItemDustOfVanishing extends ItemGeneric {

    public ItemDustOfVanishing() {
        super("dust_of_vanishing", getBuilder(true), SharedConfigTombstone.allowed_magic_items.allowDustOfVanishing::get);
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return super.getDisplayName(stack).copyRaw().setStyle(StyleType.MESSAGE_SPECIAL);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            addItemUse(list);
        } else {
            addInfoShowTooltip(list);
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!stack.isEmpty() && stack.getItem() == this && !EntityHelper.hasCooldown(player, this)) {
            Vector3d pVec = player.getPositionVec();
            if (!world.isRemote) {
                world.playSound(null, pVec.x, pVec.y, pVec.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5f, 0.5f);
                Location spawnPos = findBackwardLocation((ServerWorld) world, player, 8d);
                if (!spawnPos.isOrigin()) {
                    Helper.teleportEntity(player, spawnPos);
                } else {
                    spawnPos = findBackwardLocation((ServerWorld) world, player, 3d);
                    if (!spawnPos.isOrigin()) {
                        Helper.teleportEntity(player, spawnPos);
                    }
                }
                player.fallDistance = 0f;
                stack.shrink(1);
                EffectHelper.addEffect(player, ModEffects.diversion, 100);
                EntityHelper.setCooldown(player, this, 200);
                PacketHandler.sendToAllTrackingPlayers(new SmokeColumnMessage(player.getEntityId()), player);
            }
            ModTombstone.PROXY.produceSmokeColumn(world, pVec.x, pVec.y, pVec.z);
            return new ActionResult<>(ActionResultType.SUCCESS, stack);
        }
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        return ActionResultType.FAIL;
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        if (context.getPlayer() != null) {
            onItemRightClick(context.getWorld(), context.getPlayer(), context.getHand());
        }
        return ActionResultType.SUCCESS;
    }

    private Location findBackwardLocation(ServerWorld world, PlayerEntity player, double range) {
        return new SpawnHelper(world, new BlockPos(player.getPositionVec().subtract(player.getLookVec().x * range, 0, player.getLookVec().z * range))).findSafePlace(2, true);
    }
}
