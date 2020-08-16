package ovh.corail.tombstone.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

public class StyleType {
    public static final Style MESSAGE_NORMAL = Style.EMPTY.applyFormatting(TextFormatting.WHITE).setItalic(false).setBold(false);
    public static final Style MESSAGE_SPECIAL = Style.EMPTY.applyFormatting(TextFormatting.GOLD).setItalic(false).setBold(false);
    public static final Style MESSAGE_SPELL = Style.EMPTY.applyFormatting(TextFormatting.BLUE).setItalic(true).setBold(false);
    public static final Style TOOLTIP_DESC = Style.EMPTY.applyFormatting(TextFormatting.GRAY).setItalic(true).setBold(false);
    public static final Style TOOLTIP_USE = Style.EMPTY.applyFormatting(TextFormatting.DARK_PURPLE).setItalic(true).setBold(false);
    public static final Style TOOLTIP_IN_BETA = MESSAGE_SPELL;
    public static final Style TOOLTIP_ENCHANT = Style.EMPTY.applyFormatting(TextFormatting.DARK_GRAY).setItalic(false).setBold(false);
    public static final Style TOOLTIP_ITEM = Style.EMPTY.applyFormatting(TextFormatting.YELLOW).setItalic(false).setBold(false);
    public static final Style COLOR_ON = Style.EMPTY.applyFormatting(TextFormatting.GREEN).setItalic(false).setBold(false);
    public static final Style COLOR_OFF = Style.EMPTY.applyFormatting(TextFormatting.RED).setItalic(false).setBold(false);
    public static final Style STANDARD_GALACTIC = Style.EMPTY.setFontId(Minecraft.standardGalacticFontRenderer);
}
