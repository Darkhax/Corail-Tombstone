package ovh.corail.tombstone.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.registry.ModPerks;

import java.util.function.Supplier;

public abstract class ItemTablet extends ItemGraveMagic {

    public ItemTablet(String name, Supplier<Boolean> supplierBoolean) {
        super(name, supplierBoolean);
    }

    @Override
    public int getUseMax() {
        return SharedConfigTombstone.decorative_grave.tabletMaxUse.get();
    }

    @Override
    public int getCastingCooldown() {
        return 1200;
    }

    @Override
    public boolean canConsumeOnUse() {
        return true;
    }

    @Override
    protected ItemStack onConsumeItem(PlayerEntity player, ItemStack stack) {
        return Helper.getRandom(1, 10) < EntityHelper.getPerkLevelWithBonus(player, ModPerks.rune_inscriber) ? stack : ItemStack.EMPTY;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        return super.onItemRightClick(world, player, hand);
    }

    public ItemStack createAncient() {
        return NBTStackHelper.setBoolean(new ItemStack(this), ANCIENT_NBT_BOOL, true);
    }
}
