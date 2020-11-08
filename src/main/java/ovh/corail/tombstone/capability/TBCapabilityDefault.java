package ovh.corail.tombstone.capability;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants;
import ovh.corail.tombstone.api.capability.ITBCapability;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.api.cooldown.CooldownType;
import ovh.corail.tombstone.helper.CooldownHandler;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.network.PacketHandler;
import ovh.corail.tombstone.network.SyncCapClientMessage;
import ovh.corail.tombstone.perk.PerkRegistry;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

public class TBCapabilityDefault implements ITBCapability {
    protected long knowledge = 0L;
    private final Map<Perk, Integer> perks = new HashMap<>();
    protected int alignment = 0;
    protected int alignmentLevel = 0;

    public TBCapabilityDefault() {
    }

    @Override
    public long getKnowledge() {
        return this.knowledge;
    }

    @Override
    public ITBCapability setKnowledge(long points) {
        this.knowledge = Math.max(0, points);
        return this;
    }

    @Override
    public ITBCapability addKnowledgeAndSync(ServerPlayerEntity player, long points) {
        if (!player.world.isRemote && points > 0) {
            int oldPerkPoints = getTotalPerkPoints();
            this.knowledge += points;
            if (DeathHandler.INSTANCE.getOptionKnowledgeMessage(player.getUniqueID())) {
                LangKey.MESSAGE_EARN_KNOWLEDGE.sendMessage(player, StyleType.MESSAGE_SPELL, LangKey.MESSAGE_YOUR_KNOWLEDGE.getText(), points);
                int newPerkPoints = getTotalPerkPoints();
                int adjust = newPerkPoints - oldPerkPoints;
                if (adjust > 0) {
                    LangKey.MESSAGE_ACCESS_GUI.sendMessage(player, StyleType.MESSAGE_SPECIAL, adjust, LangKey.createComponentCommand(player, "/tbgui", LangKey.MESSAGE_HERE));
                    if (oldPerkPoints == 0) {
                        ModTriggers.FIRST_KNOWLEDGE.trigger(player);
                    }
                    if (oldPerkPoints < 10 && newPerkPoints >= 10) {
                        ModTriggers.MASTERY_1.trigger(player);
                    }
                }
            }
            PacketHandler.sendToPlayer(new SyncCapClientMessage(SyncCapClientMessage.SyncType.KNOWLEDGE, this.knowledge), player);
        }
        return this;
    }

    @Override
    public ITBCapability removeKnowledgeAndSync(ServerPlayerEntity player, long points) {
        if (!player.world.isRemote) {
            int oldPerkPoints = getTotalPerkPoints();
            setKnowledge(this.knowledge - points);
            int lostPerkPoints = oldPerkPoints - getTotalPerkPoints();
            if (lostPerkPoints > 0) {
                Iterator<Map.Entry<Perk, Integer>> it = this.perks.entrySet().iterator();
                while (it.hasNext() && lostPerkPoints > 0) {
                    Map.Entry<Perk, Integer> entry = it.next();
                    if (Helper.isDisabledPerk(entry.getKey(), player)) {
                        it.remove();
                    }
                    for (int i = entry.getValue(); i >= 1 && lostPerkPoints > 0; i--) {
                        int cost = entry.getKey().getCost(i);
                        lostPerkPoints -= cost;
                        if (i == 1) {
                            it.remove();
                        } else {
                            entry.setValue(i - 1);
                        }
                    }
                }
                PacketHandler.sendToPlayer(new SyncCapClientMessage(SyncCapClientMessage.SyncType.ALL, this.knowledge, this.alignment, this.perks), player);
            } else {
                PacketHandler.sendToPlayer(new SyncCapClientMessage(SyncCapClientMessage.SyncType.KNOWLEDGE, this.knowledge), player);
            }
            if (DeathHandler.INSTANCE.getOptionKnowledgeMessage(player.getUniqueID())) {
                LangKey.MESSAGE_LOSE_KNOWLEDGE.sendMessage(player, StyleType.MESSAGE_SPELL, LangKey.MESSAGE_YOUR_KNOWLEDGE.getText(), points);
            }
        }
        return this;
    }

    @Override
    public long getKnowledgeForLevel(int level) {
        return level <= 0 ? 0 : level * level * 2;
    }

    @Override
    public long getKnowledgeToReachNextLevel(int level) {
        return level <= 0 ? 0 : (4 * level) - 2;
    }

    @Override
    public int getUsedPerkPoints(PlayerEntity player) {
        int perkPoints = 0;
        Iterator<Entry<Perk, Integer>> it = this.perks.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Perk, Integer> entry = it.next();
            if (Helper.isDisabledPerk(entry.getKey(), player)) {
                it.remove();
            } else {
                Perk perk = entry.getKey();
                for (int i = entry.getValue(); i >= 1; i--) {
                    perkPoints += perk.getCost(i);
                }
            }
        }
        return perkPoints;
    }

    @Override
    public int getTotalPerkPoints() {
        return (int) MathHelper.sqrt(this.knowledge / 2f);
    }

    @Override
    public Map<Perk, Integer> getPerks() {
        return this.perks;
    }

    @Override
    public ITBCapability setPerks(Map<Perk, Integer> perks) {
        this.perks.clear();
        for (Entry<Perk, Integer> entry : perks.entrySet()) {
            setPerk(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public ITBCapability setPerk(Perk perk, int level) {
        this.perks.put(perk, level);
        return this;
    }

    @Override
    public boolean removePerk(Perk perk) {
        return this.perks.remove(perk) != null;
    }

    @Override
    public int getPerkLevel(PlayerEntity player, Perk perk) {
        return perk.isDisabled(player) ? 0 : this.perks.getOrDefault(perk, 0);
    }

    @Override
    public int getPerkLevelWithBonus(PlayerEntity player, @Nullable Perk perk) {
        if (EntityHelper.isValidPlayer(player) && perk != null && !perk.isDisabled(player)) {
            return Math.min(getPerkLevel(player, perk) + perk.getLevelBonus(player), perk.getLevelMax());
        }
        return 0;
    }

    @Override
    public boolean canResetPerks(PlayerEntity player) {
        return CooldownHandler.INSTANCE.noCooldown(player, CooldownType.RESET_PERKS) && this.perks.entrySet().stream().anyMatch(entry -> entry.getKey() != null && entry.getValue() > 0 && !entry.getKey().isDisabled(player));
    }

    @Override
    public boolean resetPerks(ServerPlayerEntity player) {
        if (!player.world.isRemote && canResetPerks(player)) {
            setPerks(new HashMap<>());
            CooldownHandler.INSTANCE.resetCooldown(player, CooldownType.RESET_PERKS);
            syncAll(player);
            return true;
        }
        return false;
    }

    @Override
    public ITBCapability copyCapability(ITBCapability otherTBCapability) {
        deserializeNBT(otherTBCapability.serializeNBT());
        return this;
    }

    @Override
    public ITBCapability syncAll(ServerPlayerEntity player) {
        PacketHandler.sendToPlayer(new SyncCapClientMessage(SyncCapClientMessage.SyncType.ALL, this.knowledge, this.alignment, this.perks), player);
        return this;
    }

    @Override
    public ITBCapability increaseAlignment(int amount) {
        if ((amount > 0 && this.alignmentLevel < 0) || (amount < 0 && this.alignmentLevel > 0)) {
            amount *= 2;
        }
        return setAlignment(this.alignment + amount);
    }

    @Override
    public ITBCapability decreaseAlignment(int amount) {
        return increaseAlignment(-amount);
    }

    @Override
    public int getAlignmentValue() {
        return this.alignment;
    }

    @Override
    public int getAlignmentMinValue() {
        return -500;
    }

    @Override
    public int getAlignmentMaxValue() {
        return 500;
    }

    @Override
    public int getAlignmentLevel() {
        if (this.alignment <= -500) {
            return -4;
        } else if (this.alignment <= -300) {
            return -3;
        } else if (this.alignment <= -150) {
            return -2;
        } else if (this.alignment <= -50) {
            return -1;
        } else if (this.alignment < 50) {
            return 0;
        } else if (this.alignment < 150) {
            return 1;
        } else if (this.alignment < 300) {
            return 2;
        } else if (this.alignment < 500) {
            return 3;
        }
        return 4;
    }

    @Override
    public ITBCapability setAlignment(int value) {
        this.alignment = MathHelper.clamp(value, getAlignmentMinValue(), getAlignmentMaxValue());
        this.alignmentLevel = getAlignmentLevel();
        return this;
    }

    @Override
    public ITBCapability addAlignmentAndSync(ServerPlayerEntity player, int amount) {
        if (!player.world.isRemote && amount != 0) {
            int oldAlignment = this.alignmentLevel;
            setAlignment(this.alignment + amount);
            if (oldAlignment != this.alignmentLevel) {
                onAlignmentLevelChange(oldAlignment, this.alignmentLevel);
            }
            PacketHandler.sendToPlayer(new SyncCapClientMessage(SyncCapClientMessage.SyncType.ALIGNMENT, this.alignment), player);
        }
        return this;
    }

    @Override
    public ITBCapability onAlignmentLevelChange(int oldAlignment, int newAlignment) {
        return this;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putLong("knowledge", this.knowledge);
        nbt.putInt("alignment", this.alignment);
        ListNBT tagPerks = new ListNBT();
        this.perks.forEach((perk, value) -> {
            if (perk != null && !perk.isDisabled(null)) {
                CompoundNBT tagPerk = new CompoundNBT();
                tagPerk.putInt("id", PerkRegistry.perkRegistry.getID(perk));
                tagPerk.putInt("level", Math.min(perk.getLevelMax(), value));
                tagPerks.add(tagPerk);
            }
        });
        nbt.put("perks", tagPerks);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        if (nbt.contains("knowledge", Constants.NBT.TAG_LONG)) {
            this.knowledge = nbt.getLong("knowledge");
        }
        if (nbt.contains("alignment", Constants.NBT.TAG_INT)) {
            setAlignment(nbt.getInt("alignment"));
        }
        if (nbt.contains("perks", Constants.NBT.TAG_LIST)) {
            ListNBT tagPerks = nbt.getList("perks", Constants.NBT.TAG_COMPOUND);
            IntStream.range(0, tagPerks.size()).mapToObj(tagPerks::getCompound).filter(tagPerk -> tagPerk.contains("id", Constants.NBT.TAG_INT) && tagPerk.contains("level", Constants.NBT.TAG_INT)).forEach(tagPerk -> {
                Perk perk = PerkRegistry.perkRegistry.getValue(tagPerk.getInt("id"));
                if (perk != null && !perk.isDisabled(null)) {
                    this.perks.put(perk, Math.min(perk.getLevelMax(), tagPerk.getInt("level")));
                }
            });
        }
    }
}
