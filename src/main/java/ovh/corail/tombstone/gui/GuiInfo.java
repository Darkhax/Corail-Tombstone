package ovh.corail.tombstone.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ICharacterConsumer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.block.GraveModel;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.registry.ModBlocks;
import ovh.corail.tombstone.registry.ModItems;

import javax.annotation.Nullable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

@OnlyIn(Dist.CLIENT)
public class GuiInfo extends TBScreen {
    private Info currentInfo, lastInfo, hoveredInfo = null;
    private int linesByPage;
    private int pageCount;
    private int currentPage;
    private String                           title;
    private final List<IReorderingProcessor> contentLines = new ArrayList<>();
    private final List<Rectangle2d>          underlines   = new ArrayList<>();
    private final List<InfoLink> infoLinks = new ArrayList<>();
    private ItemStack icon;

    GuiInfo(@Nullable Info currentInfo) {
        super(new TranslationTextComponent(MOD_ID + ".compendium.main.title"));
        this.currentInfo = this.lastInfo = currentInfo;
        this.currentPage = 0;
    }

    public class InfoLink {
        private final String title;
        private final Info info;
        private final int x, y, width, height;

        InfoLink(Info info, int x, int y) {
            this.title = I18n.format(info.getTitle());
            this.info = info;
            this.x = x;
            this.y = y;
            this.width = font.getStringWidth(title);
            this.height = font.FONT_HEIGHT + 1;
        }

        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseY >= this.y && mouseX <= this.x + this.width & mouseY <= this.y + this.height;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        } else if (mouseButton == 0 && this.hoveredInfo != null) {
            this.currentInfo = this.hoveredInfo;
            return true;
        }
        return false;
    }

    @Override
    public void init() {
        super.init();
        addButton(new TBGuiButton(this.halfWidth - 35, this.guiBottom - 25, 70, 15, LangKey.MESSAGE_BACK.getText(), pressable -> {
            if (this.currentInfo == null) {
                getMinecraft().player.closeScreen();
            } else {
                this.currentInfo = null;
            }
        }));
        addButton(new TBGuiButton(this.halfWidth - 75, this.guiBottom - 25, 20, 15, new StringTextComponent("<-"), pressable -> {
            this.currentPage--;
            updateButtons();
        }));
        addButton(new TBGuiButton(this.halfWidth + 55, this.guiBottom - 25, 20, 15, new StringTextComponent("->"), pressable -> {
            this.currentPage++;
            updateButtons();
        }));
        updatePage(this.currentInfo);
    }

    private void updateContent(@Nullable Info currentInfo) {
        this.contentLines.clear();
        this.underlines.clear();
        this.infoLinks.clear();
        this.icon = currentInfo == null ? new ItemStack(ModBlocks.decorative_graves.get(GraveModel.TOMBSTONE)) : currentInfo.icon.get();
        String content = I18n.format(currentInfo == null ? MOD_ID + ".compendium.main.desc" : currentInfo.getContent());
        Arrays.stream(content.split("[\\r\\n]+")).filter(p -> p.length() > 0).forEach(c -> this.contentLines.addAll(this.font.trimStringToWidth(new StringTextComponent(c), this.xSize - 15)));
        for (final IReorderingProcessor line : this.contentLines) {
            // only search the underlines at start of line
            String lineString = "";// line.getString();
            if (lineString.startsWith(TextFormatting.UNDERLINE.toString())) {
                int endIndex = lineString.indexOf(TextFormatting.RESET.toString());
                if (endIndex == -1) {
                    endIndex = lineString.length() - 1;
                }
                this.underlines.add(new Rectangle2d(0, 0, this.font.getStringWidth(lineString.substring(2, endIndex)) - 2, 0));
            } else {
                this.underlines.add(null);
            }
        }

        int x = this.guiLeft + this.xSize + 5;
        int y = this.guiTop + 10;
        List<Info> infos;
        if (currentInfo == null) {
            infos = Arrays.stream(Info.values()).filter(p -> p.isMainEntry).collect(Collectors.toList());
        } else {
            infos = Info.getRelated(currentInfo);
            infos.sort(Comparator.comparing(p -> Normalizer.normalize(I18n.format(p.getTitle()), Normalizer.Form.NFD)));
        }
        for (Info info : infos) {
            this.infoLinks.add(new InfoLink(info, x, y));
            y += this.font.FONT_HEIGHT + 1;
        }
    }

    private void updatePage(@Nullable Info currentInfo) {
        this.lastInfo = currentInfo;
        this.title = I18n.format(currentInfo == null ? MOD_ID + ".compendium.main.title" : currentInfo.getTitle());
        updateContent(currentInfo);
        this.linesByPage = 12;
        this.pageCount = MathHelper.ceil(this.contentLines.size() / (float) this.linesByPage);
        this.currentPage = 0;
        updateButtons();
    }

    private void updateButtons() {
        this.buttons.get(1).active = this.buttons.get(1).visible = this.currentPage > 0;
        this.buttons.get(2).active = this.buttons.get(2).visible = this.currentPage < this.pageCount - 1;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(matrixStack);
        this.hoveredInfo = null;
        if (this.currentInfo != this.lastInfo) {
            updatePage(this.currentInfo);
        }
        // draw title
        fill(matrixStack, this.guiLeft + 5, this.guiTop + 5, this.guiLeft + this.xSize - 5, this.guiTop + 20 + this.font.FONT_HEIGHT, 0x55000000);
        this.font.drawString(matrixStack, this.title, this.guiLeft + 30, this.guiTop + 15, this.textColor);
        // draw page
        if (this.currentInfo != null) {
            this.font.drawString(matrixStack, (this.currentPage + 1) + "/" + this.pageCount, this.guiLeft + this.xSize - 30, this.guiTop + 6, this.textColor);
        }
        // draw picture
        if (!this.icon.isEmpty()) {
            fill(matrixStack, this.guiLeft + 7, this.guiTop + 7, this.guiLeft + 18 + this.font.FONT_HEIGHT, this.guiTop + 18 + this.font.FONT_HEIGHT, this.textColor);
            this.itemRenderer.renderItemAndEffectIntoGUI(this.icon, (int) ((this.guiLeft + 9) / 1f), (int) ((this.guiTop + 9) / 1f));
        }
        // draw content
        int indexStart = Math.min(this.currentPage * linesByPage, this.contentLines.size() - 1);
        int indexEnd = Math.min(indexStart + this.linesByPage - 1, this.contentLines.size() - 1);
        if (indexStart >= 0) {
            fill(matrixStack, this.guiLeft + 5, this.guiTop + 34, this.guiLeft + this.xSize - 5, this.guiTop + 44 + (this.font.FONT_HEIGHT + 1) * (indexEnd - indexStart + 1), 0x55000000);
            int count = 0;
            for (int i = indexStart; i <= indexEnd; i++) {
                IReorderingProcessor line = this.contentLines.get(i);
                // draw text line
                this.font.func_238407_a_(matrixStack, line, this.guiLeft + 10, this.guiTop + 39 + (count * (this.font.FONT_HEIGHT + 1)), this.textColor);
                // draw underlines
                Rectangle2d underline = this.underlines.get(i);
                if (underline != null) {
                    int startX = this.guiLeft + 10 + underline.getX();
                    hLine(matrixStack, startX, startX + underline.getWidth(), this.guiTop + 39 + (count * (this.font.FONT_HEIGHT + 1)) + this.font.FONT_HEIGHT - 1, this.textColor);
                }
                count++;
            }
        }
        // draw links
        for (InfoLink link : this.infoLinks) {
            if (this.hoveredInfo == null && link.isHovered(mouseX, mouseY)) {
                this.hoveredInfo = link.info;
                this.font.drawString(matrixStack, link.title, link.x, link.y, 0xff897235);
            } else {
                this.font.drawString(matrixStack, link.title, link.x, link.y, this.textColor);
            }
        }
        super.render(matrixStack, mouseX, mouseY, partialTick);
    }

    enum Info {
        DEATH(() -> new ItemStack(ModItems.advancement[0]), true),
        DECORATIVE_GRAVE(() -> new ItemStack(ModBlocks.decorative_graves.get(GraveModel.GRAVE_SIMPLE)), true, "tombstone.config_cat.decorative_grave"),
        MAGIC_ITEMS(() -> new ItemStack(ModItems.advancement[2]), true),
        ENCHANTMENTS(() -> new ItemStack(Items.ENCHANTED_BOOK), true),
        KNOWLEDGE_OF_DEATH(() -> new ItemStack(ModItems.ankh_of_pray), true, "tombstone.message.knowledge_of_death"),
        SPECIAL_BONUS(() -> ItemStack.EMPTY, true),

        ALIGNMENT(() -> new ItemStack(ModItems.advancement[7]), false),
        ANKH_OF_PRAY(() -> new ItemStack(ModItems.ankh_of_pray), false, "tombstone.item.ankh_of_pray"),
        BONE_NEEDLE(() -> new ItemStack(ModItems.bone_needle), false, "tombstone.item.bone_needle"),
        BOOK_OF_DISENCHANTMENT(() -> new ItemStack(ModItems.book_of_disenchantment), false, "tombstone.item.book_of_disenchantment"),
        CONTRIBUTOR(() -> ItemStack.EMPTY, false),
        GRAVE_MARBLE(() -> new ItemStack(ModBlocks.dark_marble), false, "tombstone.block.dark_marble"),
        DUST_OF_VANISHING(() -> new ItemStack(ModItems.dust_of_vanishing), false, "tombstone.item.dust_of_vanishing"),
        FAMILIAR_RECEPTACLE(() -> new ItemStack(ModItems.familiar_receptacle), false, "tombstone.item.familiar_receptacle"),
        FISHING_ROD_OF_MISADVENTURE(() -> new ItemStack(ModItems.fishing_rod_of_misadventure), false, "tombstone.item.fishing_rod_of_misadventure"),
        GHOSTLY_SHAPE(() -> new ItemStack(ModItems.advancement[1]), false, "effect.tombstone.ghostly_shape"),
        GRAVE_DUST(() -> new ItemStack(ModItems.grave_dust), false, "tombstone.item.grave_dust"),
        GRAVE_KEY(() -> new ItemStack(ModItems.grave_key), false, "tombstone.item.grave_key"),
        HALLOWEEN(() -> new ItemStack(ModItems.advancement[6]), false),
        IMPREGNATED_DIAMOND(() -> new ItemStack(ModItems.impregnated_diamond), false, "tombstone.item.impregnated_diamond"),
        LOLLIPOP(() -> new ItemStack(ModItems.lollipop[0]), false, "tombstone.item.lollipop"),
        LOST_TABLET(() -> new ItemStack(ModItems.lost_tablet), false, "tombstone.item.lost_tablet"),
        PERK(() -> ItemStack.EMPTY, false),
        SCROLL_OF_FEATHER_FALL(() -> new ItemStack(ModItems.scroll_buff[2]), false, "tombstone.item.scroll_of_feather_fall"),
        SCROLL_OF_KNOWLEDGE(() -> new ItemStack(ModItems.scroll_of_knowledge), false, "tombstone.item.scroll_of_knowledge"),
        SCROLL_OF_PRESERVATION(() -> new ItemStack(ModItems.scroll_buff[0]), false, "tombstone.item.scroll_of_preservation"),
        SCROLL_OF_PURIFICATION(() -> new ItemStack(ModItems.scroll_buff[3]), false, "tombstone.item.scroll_of_purification"),
        SCROLL_OF_TRUE_SIGHT(() -> new ItemStack(ModItems.scroll_buff[4]), false, "tombstone.item.scroll_of_true_sight"),
        SCROLL_OF_UNSTABLE_INTANGIBLENESS(() -> new ItemStack(ModItems.scroll_buff[1]), false, "tombstone.item.scroll_of_unstable_intangibleness"),
        SCROLL_OF_REACH(() -> new ItemStack(ModItems.scroll_buff[5]), false, "tombstone.item.scroll_of_reach"),
        SCROLL_OF_LIGHTNING_RESISTANCE(() -> new ItemStack(ModItems.scroll_buff[6]), false, "tombstone.item.scroll_of_lightning_resistance"),
        MAGIC_SCROLLS(() -> new ItemStack(ModItems.strange_scroll), false),
        SOUL(() -> new ItemStack(ModItems.advancement[5]), false),
        SOUL_RECEPTACLE(() -> new ItemStack(ModItems.soul_receptacle), false, "tombstone.item.soul_receptacle"),
        STRANGE_SCROLL(() -> new ItemStack(ModItems.strange_scroll), false, "tombstone.item.strange_scroll"),
        STRANGE_TABLET(() -> new ItemStack(ModItems.strange_tablet), false, "tombstone.item.strange_tablet"),
        TABLET_OF_HOME(() -> new ItemStack(ModItems.tablet_of_home), false, "tombstone.item.tablet_of_home"),
        TABLET_OF_RECALL(() -> new ItemStack(ModItems.tablet_of_recall), false, "tombstone.item.tablet_of_recall"),
        TABLET_OF_ASSISTANCE(() -> new ItemStack(ModItems.tablet_of_assistance), false, "tombstone.item.tablet_of_assistance"),
        TABLET_OF_CUPIDITY(() -> new ItemStack(ModItems.tablet_of_cupidity), false, "tombstone.item.tablet_of_cupidity"),
        MAGIC_TABLETS(() -> new ItemStack(ModItems.strange_tablet), false),
        VOODOO_POPPET(() -> new ItemStack(ModItems.voodoo_poppet), false, "tombstone.item.voodoo_poppet");
        private final Supplier<ItemStack> icon;
        private final boolean isMainEntry;
        private final String title;

        Info(Supplier<ItemStack> icon, boolean isMainEntry) {
            this(icon, isMainEntry, null);
        }

        Info(Supplier<ItemStack> icon, boolean isMainEntry, @Nullable String titleKey) {
            this.icon = icon;
            this.isMainEntry = isMainEntry;
            this.title = titleKey == null ? MOD_ID + ".compendium." + name().toLowerCase() + ".title" : titleKey;
        }

        public final String getTitle() {
            return this.title;
        }

        public final String getContent() {
            return MOD_ID + ".compendium." + name().toLowerCase() + ".desc";
        }

        public static List<Info> getRelated(Info info) {
            List<Info> infos = new ArrayList<>();
            switch (info) {
                case ALIGNMENT:
                    infos.add(KNOWLEDGE_OF_DEATH);
                    break;
                case ANKH_OF_PRAY:
                    infos.add(DECORATIVE_GRAVE);
                    infos.add(FAMILIAR_RECEPTACLE);
                    infos.add(KNOWLEDGE_OF_DEATH);
                    infos.add(MAGIC_ITEMS);
                    infos.add(PERK);
                    infos.add(SOUL);
                    break;
                case BONE_NEEDLE:
                    infos.add(GRAVE_DUST);
                    infos.add(IMPREGNATED_DIAMOND);
                    infos.add(FAMILIAR_RECEPTACLE);
                    break;
                case BOOK_OF_DISENCHANTMENT:
                    infos.add(MAGIC_ITEMS);
                    infos.add(PERK);
                    infos.add(SOUL);
                    break;
                case CONTRIBUTOR:
                    infos.add(LOLLIPOP);
                    infos.add(SPECIAL_BONUS);
                    break;
                case GRAVE_MARBLE:
                    infos.add(DECORATIVE_GRAVE);
                    infos.add(GRAVE_DUST);
                    break;
                case DEATH:
                    infos.add(GHOSTLY_SHAPE);
                    infos.add(GRAVE_KEY);
                    infos.add(PERK);
                    break;
                case DECORATIVE_GRAVE:
                    infos.add(GRAVE_MARBLE);
                    infos.add(MAGIC_ITEMS);
                    infos.add(SOUL);
                    break;
                case DUST_OF_VANISHING:
                    infos.add(GRAVE_DUST);
                    infos.add(MAGIC_ITEMS);
                    break;
                case ENCHANTMENTS:
                    break;
                case FAMILIAR_RECEPTACLE:
                    infos.add(ANKH_OF_PRAY);
                    infos.add(IMPREGNATED_DIAMOND);
                    infos.add(KNOWLEDGE_OF_DEATH);
                    infos.add(MAGIC_ITEMS);
                    infos.add(SOUL);
                    infos.add(BONE_NEEDLE);
                    break;
                case FISHING_ROD_OF_MISADVENTURE:
                    infos.add(LOST_TABLET);
                    infos.add(MAGIC_ITEMS);
                    infos.add(SOUL);
                    break;
                case GHOSTLY_SHAPE:
                    infos.add(DEATH);
                    break;
                case GRAVE_DUST:
                    infos.add(GRAVE_MARBLE);
                    infos.add(DUST_OF_VANISHING);
                    infos.add(IMPREGNATED_DIAMOND);
                    infos.add(BONE_NEEDLE);
                    break;
                case GRAVE_KEY:
                    infos.add(DEATH);
                    infos.add(DECORATIVE_GRAVE);
                    infos.add(MAGIC_ITEMS);
                    infos.add(SOUL);
                    break;
                case HALLOWEEN:
                    infos.add(LOLLIPOP);
                    infos.add(SPECIAL_BONUS);
                    break;
                case KNOWLEDGE_OF_DEATH:
                    infos.add(ALIGNMENT);
                    infos.add(ANKH_OF_PRAY);
                    infos.add(FAMILIAR_RECEPTACLE);
                    infos.add(SOUL);
                    infos.add(SOUL_RECEPTACLE);
                    break;
                case IMPREGNATED_DIAMOND:
                    infos.add(GRAVE_DUST);
                    infos.add(FAMILIAR_RECEPTACLE);
                    infos.add(BONE_NEEDLE);
                    break;
                case LOLLIPOP:
                    infos.add(CONTRIBUTOR);
                    infos.add(HALLOWEEN);
                    infos.add(MAGIC_ITEMS);
                    break;
                case LOST_TABLET:
                    infos.add(FISHING_ROD_OF_MISADVENTURE);
                    infos.add(MAGIC_TABLETS);
                    infos.add(PERK);
                    infos.add(SOUL);
                    break;
                case MAGIC_ITEMS:
                    infos.add(ANKH_OF_PRAY);
                    infos.add(BOOK_OF_DISENCHANTMENT);
                    infos.add(DUST_OF_VANISHING);
                    infos.add(FAMILIAR_RECEPTACLE);
                    infos.add(FISHING_ROD_OF_MISADVENTURE);
                    infos.add(GRAVE_KEY);
                    infos.add(LOLLIPOP);
                    infos.add(MAGIC_SCROLLS);
                    infos.add(SOUL_RECEPTACLE);
                    infos.add(MAGIC_TABLETS);
                    infos.add(VOODOO_POPPET);
                    break;
                case MAGIC_SCROLLS:
                    infos.add(DECORATIVE_GRAVE);
                    infos.add(SCROLL_OF_FEATHER_FALL);
                    infos.add(SCROLL_OF_KNOWLEDGE);
                    infos.add(SCROLL_OF_LIGHTNING_RESISTANCE);
                    infos.add(SCROLL_OF_PRESERVATION);
                    infos.add(SCROLL_OF_PURIFICATION);
                    infos.add(SCROLL_OF_REACH);
                    infos.add(SCROLL_OF_TRUE_SIGHT);
                    infos.add(SCROLL_OF_UNSTABLE_INTANGIBLENESS);
                    infos.add(SOUL);
                    infos.add(STRANGE_SCROLL);
                    break;
                case STRANGE_SCROLL:
                    infos.add(GRAVE_DUST);
                    infos.add(MAGIC_SCROLLS);
                    break;
                case STRANGE_TABLET:
                    infos.add(GRAVE_MARBLE);
                    infos.add(GRAVE_DUST);
                    infos.add(MAGIC_TABLETS);
                    break;
                case MAGIC_TABLETS:
                    infos.add(DECORATIVE_GRAVE);
                    infos.add(PERK);
                    infos.add(LOST_TABLET);
                    infos.add(SOUL);
                    infos.add(STRANGE_TABLET);
                    infos.add(TABLET_OF_ASSISTANCE);
                    infos.add(TABLET_OF_CUPIDITY);
                    infos.add(TABLET_OF_HOME);
                    infos.add(TABLET_OF_RECALL);
                    break;
                case PERK:
                    infos.add(DEATH);
                    infos.add(GHOSTLY_SHAPE);
                    infos.add(KNOWLEDGE_OF_DEATH);
                    infos.add(MAGIC_ITEMS);
                    break;
                case SCROLL_OF_FEATHER_FALL:
                case SCROLL_OF_KNOWLEDGE:
                case SCROLL_OF_PRESERVATION:
                case SCROLL_OF_PURIFICATION:
                case SCROLL_OF_TRUE_SIGHT:
                case SCROLL_OF_REACH:
                case SCROLL_OF_LIGHTNING_RESISTANCE:
                case SCROLL_OF_UNSTABLE_INTANGIBLENESS:
                    infos.add(MAGIC_SCROLLS);
                    infos.add(SOUL);
                    break;
                case SOUL:
                    infos.add(DECORATIVE_GRAVE);
                    infos.add(FAMILIAR_RECEPTACLE);
                    infos.add(MAGIC_ITEMS);
                    infos.add(SOUL_RECEPTACLE);
                    break;
                case SOUL_RECEPTACLE:
                    infos.add(DECORATIVE_GRAVE);
                    infos.add(KNOWLEDGE_OF_DEATH);
                    infos.add(MAGIC_ITEMS);
                    infos.add(SOUL);
                    break;
                case SPECIAL_BONUS:
                    infos.add(CONTRIBUTOR);
                    infos.add(HALLOWEEN);
                    break;
                case TABLET_OF_ASSISTANCE:
                case TABLET_OF_CUPIDITY:
                case TABLET_OF_HOME:
                case TABLET_OF_RECALL:
                    infos.add(MAGIC_TABLETS);
                    infos.add(SOUL);
                    break;
                case VOODOO_POPPET:
                    infos.add(DECORATIVE_GRAVE);
                    infos.add(MAGIC_ITEMS);
                    infos.add(SOUL);
                    break;
            }
            return infos;
        }
    }
}
