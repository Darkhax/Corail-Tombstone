package ovh.corail.tombstone.helper;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public enum LangKey {
	
    MESSAGE_SPELL_CAST_ON_YOU("message.spell_cast_on_you"),
    MESSAGE_DISPEL_BAD_OMEN("message.dispel_bad_omen"),
    MESSAGE_STORED_EXPERIENCE("message.stored_experience"),
    MESSAGE_EARN_EXPERIENCE_SUCCESS("message.earn_experience.success"),
    MESSAGE_EARN_EXPERIENCE_FAILED("message.earn_experience.failed"),
    MESSAGE_LOSE_EXPERIENCE_SUCCESS("message.lose_experience.success"),
    MESSAGE_LOSE_EXPERIENCE_FAILED("message.lose_experience.failed"),
    MESSAGE_TOMB_RAIDER_SUCCESS("message.tomb_raider.success"),
    MESSAGE_TOMB_RAIDER_VISITED("message.tomb_raider.visited"),
    MESSAGE_TOMB_RAIDER_NOT_ABANDONED("message.tomb_raider.not_abandoned"),
    MESSAGE_TOMB_RAIDER_FAILED("message.tomb_raider.failed"),
    TOOLTIP_BETA("tooltip.in_beta"),
    TOOLTIP_MORE_INFO("tooltip.more_info"),
    TOOLTIP_ACTUAL_BONUS("tooltip.actual_bonus"),
    MESSAGE_ENGRAVABLE("message.engravable"),
    MESSAGE_ENGRAVED("message.engraved"),
    MESSAGE_ENGRAVED_ITEM("message.engraved_item"),
    MESSAGE_ENCHANTED_ITEM("message.enchanted_item"),
    MESSAGE_ANCIENT_ITEM("message.ancient_item"),
    MESSAGE_IMPREGNATE("message.impregnate"),
    MESSAGE_IMPREGNATE_DURATION("message.impregnate_duration"),
    MESSAGE_ITEM_BOUND_TO_PLACE("message.item_bound_to_place"),
    MESSAGE_USE_LEFT("message.use_left"),
    MESSAGE_DISTANCE("message.distance"),
    MESSAGE_IN_COOLDOWN("message.in_cooldown"),
    MESSAGE_COMMAND_IN_COOLDOWN("message.command_in_cooldown"),
    MESSAGE_CRAFTING_INGREDIENT("message.crafting_ingredient"),
    MESSAGE_RIP("message.rip"),
    MESSAGE_DIED_ON("message.died_on"),
    MESSAGE_AT("message.at"),
    MESSAGE_ENCHANT_ITEM_SUCCESS("message.enchant_item.success"),
    MESSAGE_ENCHANT_ITEM_FAILED("message.enchant_item.failed"),
    MESSAGE_ENCHANT_ITEM_ALREADY_ENCHANTED("message.enchant_item.already_enchanted"),
    MESSAGE_ENCHANT_ITEM_NO_SOUL("message.enchant_item.no_soul"),
    MESSAGE_ENCHANT_ITEM_NOT_ALLOWED("message.enchant_item.not_allowed"),
    MESSAGE_CANT_ENCHANT_GRAVE_KEY("message.cant_enchant_grave_key"),
    MESSAGE_TELEPORT_SUCCESS("message.teleport.success"),
    MESSAGE_TELEPORT_FAILED("message.teleport.failed"),
    MESSAGE_TELEPORT_TOO_CLOSE_FROM_GRAVE("message.teleport.too_close_from_grave"),
    MESSAGE_TELEPORT_SAME_DIMENSION("message.teleport.same_dimension"),
    MESSAGE_TELEPORT_SAME_PLAYER("message.teleport.same_player"),
    MESSAGE_OPEN_GRAVE_SUCCESS("message.open_grave.success"),
    MESSAGE_RECOVER_LOST_ITEMS("message.recover_lost_items"),
    MESSAGE_OPEN_GRAVE_NEED_KEY("message.open_grave.need_key"),
    MESSAGE_OPEN_GRAVE_WRONG_KEY("message.open_grave.wrong_key"),
    MESSAGE_NO_LOOT_FOR_GRAVE("message.no_loot_for_grave"),
    MESSAGE_NO_GRAVE_LOCATION("message.no_grave_location"),
    MESSAGE_FAIL_TO_PLACE_GRAVE("message.fail_to_place_grave"),
    MESSAGE_EXISTING_GRAVE("message.existing_grave"),
    MESSAGE_NEW_GRAVE("message.new_grave"),
    MESSAGE_JOURNEYMAP("message.journeymap"),
    MESSAGE_LOCKED("message.locked"),
    MESSAGE_UNLOCKED("message.unlocked"),
    MESSAGE_LOSSES_ON_DEATH("message.losses_on_death"),
    MESSAGE_LOST_TABLET_WAKE_UP_SUCCESS("message.lost_tablet.wake_up.success"),
    MESSAGE_LOST_TABLET_WAKE_UP_FAILED("message.lost_tablet.wake_up.failed"),
    MESSAGE_ENCHANT_FISHING_ROD_SUCCESS("message.enchant_fishing_rod.success"),
    MESSAGE_FREESOUL_SUCCESS("message.freeSoul.success"),
    MESSAGE_FREESOUL_FAILED("message.freeSoul.failed"),
    MESSAGE_SOUL_PREVENT_DEATH("message.soul_prevent_death"),
    MESSAGE_CONFIG_PREVENT_DEATH("message.config_prevent_death"),
    MESSAGE_PREVENT_DEATH_SUFFOCATION("message.prevent_death.suffocation"),
    MESSAGE_PREVENT_DEATH_BURN("message.prevent_death.burn"),
    MESSAGE_PREVENT_DEATH_LIGHTNING("message.prevent_death.lightning"),
    MESSAGE_PREVENT_DEATH_FALL("message.prevent_death.fall"),
    MESSAGE_PREVENT_DEATH_DEGENERATION("message.prevent_death.degeneration"),
    MESSAGE_NO_PROTECTION_TO_SEAL("message.no_protection_to_seal"),
    MESSAGE_DISENCHANTMENT_SUCCESS("message.disenchantment.success"),
    MESSAGE_DISENCHANTMENT_FAILED("message.disenchantment.failed"),
    MESSAGE_YOUR_KNOWLEDGE("message.your_knowledge"),
    MESSAGE_PLAYER_KNOWLEDGE("message.player_knowledge"),
    MESSAGE_EARN_KNOWLEDGE("message.earn_knowledge"),
    MESSAGE_LOSE_KNOWLEDGE("message.lose_knowledge"),
    MESSAGE_LAST_GRAVE("message.last_grave"),
    MESSAGE_REQUEST_TO_JOIN_SENDER("message.request_to_join_sender"),
    MESSAGE_REQUEST_TO_JOIN_RECEIVER("message.request_to_join_receiver"),
    MESSAGE_HERE("message.here"),
    MESSAGE_JOIN_YOU("message.join_you"),
    MESSAGE_NO_TICKET("message.no_ticket"),
    MESSAGE_KNOWLEDGE_OF_DEATH("message.knowledge_of_death"),
    MESSAGE_COST("message.cost"),
    MESSAGE_CLICK_TO_UPGRADE("message.click_to_upgrade"),
    MESSAGE_CANT_UPGRADE("message.cant_upgrade"),
    MESSAGE_MAX("message.max"),
    MESSAGE_BREATHING("message.breathing"),
    MESSAGE_INVULNERABLE("message.invulnerable"),
    MESSAGE_DAY("message.day"),
    MESSAGE_CANT_PRAY("message.cant_pray"),
    MESSAGE_EXORCISM("message.exorcism"),
    MESSAGE_ACCESS_GUI("message.access_gui"),
    MESSAGE_PERK_RESET_SUCCESS("message.perk_reset.success"),
    MESSAGE_PERK_RESET_FAILED("message.perk_reset.failed"),
    MESSAGE_PERK_RESET_IN_COOLDOWN("message.perk_reset.in_cooldown"),
    MESSAGE_PERK_REQUIRED("message.perk_required"),
    MESSAGE_KNOWLEDGE_REQUIRED("message.knowledge_required"),
    MESSAGE_PLAYER_ONLINE("message.player_online"),
    MESSAGE_PLAYER_OFFLINE("message.player_offline"),
    MESSAGE_PLAYER_INVALID("message.player_invalid"),
    MESSAGE_DEAD_ENTITY("message.dead_entity"),
    MESSAGE_PLAYER_SPECTATOR("message.player_spectator"),
    MESSAGE_NO_GRAVE("message.no_grave"),
    MESSAGE_NO_DEATH_LOCATION("message.no_death_location"),
    MESSAGE_NO_DIMENSION("message.no_dimension"),
    MESSAGE_NO_BIOME("message.no_biome"),
    MESSAGE_NO_BIOME_FOR_DIMENSION("message.no_biome_for_dimension"),
    MESSAGE_EXISTING_BIND_LOCATION("message.existing_bind_location"),
    MESSAGE_BIND_LOCATION("message.bind_location"),
    MESSAGE_NO_BIND_LOCATION("message.no_bind_location"),
    MESSAGE_SAME_LOCATION("message.same_location"),
    MESSAGE_INVALID_LOCATION("message.invalid_location"),
    MESSAGE_LAST_GRAVE_PLACE("message.last_grave_place"),
    MESSAGE_NO_SPAWN("message.no_spawn"),
    MESSAGE_SHOW_KNOWLEDGE("message.show_knowledge"),
    MESSAGE_DISABLED_COMMAND("message.disabled_command"),
    MESSAGE_DISABLED("message.disabled"),
    MESSAGE_UNLOADED_DIMENSION("message.unloaded_dimension"),
    MESSAGE_DIFFICULTY_PEACEFUL("message.difficulty_peaceful"),
    MESSAGE_DENIED_DIMENSION("message.denied_dimension"),
    MESSAGE_ONLY_AT_NIGHT("message.only_at_night"),
    MESSAGE_START_SIEGE_FAILED("message.start_siege.failed"),
    MESSAGE_START_SIEGE_SUCCESS("message.start_siege.success"),
    MESSAGE_NO_PLACE_FOR_GRAVE("message.no_place_for_grave"),
    MESSAGE_INVALID_VILLAGE("message.invalid_village"),
    MESSAGE_INVALID_STRUCTURE("message.invalid_structure"),
    MESSAGE_INVALID_BIOME("message.invalid_biome"),
    MESSAGE_NO_STRUCTURE("message.no_structure"),
    MESSAGE_POSITIVE_INTEGER("message.positive_integer"),
    MESSAGE_BACK("message.back"),
    MESSAGE_UNKNOWN("message.unknown"),
    MESSAGE_CAPTURE_FAMILIAR("message.capture_familiar"),
    MESSAGE_RECOVERING_RECEPTACLE("message.recovering_receptacle"),
    MESSAGE_REVIVE_FAMILIAR("message.revive_familiar"),
    MESSAGE_EMPTY_RECEPTACLE("message.empty_receptacle"),
    MESSAGE_CANT_REVIVE_FAMILIAR("message.cant_revive_familiar"),
    MESSAGE_BRING_BACK_TO_LIFE("message.bring_back_to_life"),
    MESSAGE_YOUR_FAMILIAR("message.your_familiar"),
    MESSAGE_FAMILIAR_OF("message.familiar_of"),
    MESSAGE_RECOVERY_SAVE_ALL_PLAYERS_SUCCESS("message.recovery.save_all_players.success"),
    MESSAGE_RECOVERY_SAVE_ALL_PLAYERS_FAILED("message.recovery.save_all_players.failed"),
    MESSAGE_RECOVERY_SAVE_PLAYER_SUCCESS("message.recovery.save_player.success"),
    MESSAGE_RECOVERY_SAVE_PLAYER_FAILED("message.recovery.save_player.failed"),
    MESSAGE_RECOVERY_LOAD_PLAYER_TARGET_SUCCESS("message.recovery.load_player.target_success"),
    MESSAGE_RECOVERY_NO_FOLDER("message.recovery.no_folder"),
    MESSAGE_RECOVERY_NO_FILE("message.recovery.no_file"),
    MESSAGE_RECOVERY_LOAD_PLAYER_SUCCESS("message.recovery.load_player.success"),
    MESSAGE_RECOVERY_LOAD_PLAYER_FAILED("message.recovery.load_player.failed"),
    MESSAGE_TELEPORT_TARGET_TO_LOCATION("message.teleport_target_to_location"),
    MESSAGE_NO_SAVE_TO_RESTORE("message.no_save_to_restore"),
    MESSAGE_CHOOSE_FAVORITE_GRAVE("message.choose_favorite_grave"),
    MESSAGE_TABLET_SEARCH_FAILED("message.tablet_search.failed"),
    MESSAGE_IMPREGNATE_NEEDLE_SUCCESS("message.impregnate_needle.success"),
    MESSAGE_IMPREGNATE_NEEDLE_FAILED("message.impregnate_needle.failed"),
    BUTTON_CONFIG("button.config"),
    ITEM_SCROLL_BUFF_USE1("item.scroll_buff.use1"),
    ITEM_SCROLL_BUFF_USE2("item.scroll_buff.use2");

    private final String key;

    LangKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return MOD_ID + "." + this.key;
    }
    
    public String asLog() {
    	return this.getText().getString();
    }
    
    public IFormattableTextComponent getText(TextFormatting format, Object... args) {
    	return this.getText(args).mergeStyle(format);
    }
    
    public IFormattableTextComponent getText(Style style, Object... args) {
    	return this.getText(args).mergeStyle(style);
    }
    
    public IFormattableTextComponent getText(Object... args) {
    	return args.length > 0 ? new TranslationTextComponent(this.getKey(), args) : new TranslationTextComponent(this.getKey());
    }
    
    public void sendMessage(PlayerEntity player, TextFormatting format, Object... args) {
    	this.sendMessage(player, Util.DUMMY_UUID, format, args);
    }
    
    public void sendMessage(PlayerEntity player, Style style, Object... args) {
    	this.sendMessage(player, Util.DUMMY_UUID, style, args);
    }
    
    public void sendMessage(PlayerEntity player, Object... args) {
    	this.sendMessage(player, Util.DUMMY_UUID, args);
    }
    
    public void sendMessage(PlayerEntity player, UUID senderId, TextFormatting format, Object... args) {
    	player.sendMessage(this.getText(format, args), senderId);
    }
    
    public void sendMessage(PlayerEntity player, UUID senderId, Style style, Object... args) {
    	player.sendMessage(this.getText(style, args), senderId);
    }
    
    public void sendMessage(PlayerEntity player, UUID senderId, Object... args) {
    	player.sendMessage(this.getText(args), senderId);
    }

    public CommandException asCommandException(Object... params) {
        return new CommandException(getText(params));
    }

    @OnlyIn(Dist.CLIENT)
    public String getClientTranslation(Object... params) {
        return I18n.format(getKey(), params);
    }

    @OnlyIn(Dist.CLIENT)
    public String getClientTranslationWithStyle(Style style, Object... params) {
        return Helper.getFormattingCode(style) + getClientTranslation(params);
    }

    public static ITextComponent createComponentCommand(String command, LangKey langKey, Object... params) {
        return langKey.getText(StyleType.COLOR_ON, params).modifyStyle(s -> s.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }
}
