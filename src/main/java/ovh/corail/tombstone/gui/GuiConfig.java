package ovh.corail.tombstone.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.OptionSlider;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import ovh.corail.tombstone.block.BlockGraveMarble.MarbleType;
import ovh.corail.tombstone.block.GraveModel;
import ovh.corail.tombstone.block.ItemBlockGrave;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.ConfigTombstone.Client.FogDensity;
import ovh.corail.tombstone.config.ConfigTombstone.Client.FogPeriod;
import ovh.corail.tombstone.config.ConfigTombstone.Client.GraveSkinRule;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;
import static ovh.corail.tombstone.ModTombstone.PROXY;

public class GuiConfig extends TBScreen {
    private static final ResourceLocation tablet = new ResourceLocation(MOD_ID, "textures/block/grave_plate.png");

    enum TabConfig {GRAVE, PLATE, MISC, EFFECT}

    private TabConfig tab = TabConfig.GRAVE;

    private Widget buttonGraveTexture, buttonLeftArrow, buttonRightArrow;

    private GraveModel graveModel;
    private MarbleType marbleType;
    private ColorButtonHandler colorButtonHandler1, colorButtonHandler2, colorButtonHandler3;

    private boolean enhanced_tooltips, highlightGrave, skipRespawnScreen, showShadowStep, enableHalloweenEffect, dateInMCTime, displayKnowledgeMessage, equipElytraInPriority, showInfoOnEnchantment, showMagicCircle, priorizeToolOnHotbar, activateGraveBySneaking;
    private List<Widget> misc1Buttons = new ArrayList<>();

    private GraveSkinRule graveSkinRule;
    private FogDensity fogDensity;
    private FogPeriod fogPeriod;
    private ColorButtonHandler colorButtonHandler0, colorButtonHandler4;
    private List<Widget> effectButtons = new ArrayList<>();
    private TBGuiButton saveButton;
    private boolean isDirty = false;

    public GuiConfig() {
        super(new StringTextComponent("Tombstone Config"));
    }

    public GuiConfig(Minecraft minecraft, Screen screen) {
        this();
    }

    @Override
    public void init() {
        super.init();
        this.buttons.clear();
        int posY = 0;
        for (TabConfig currentTab : TabConfig.values()) {
            addButton(new TBGuiButton(this.guiLeft - 50, this.guiTop + posY, 40, 20, new StringTextComponent(currentTab.name()), pressable -> {
                this.tab = currentTab;
                updateButtons();
            }));
            posY += 20;
        }
        this.saveButton = addButton(new TBGuiButton(this.guiLeft - 50, this.guiTop + posY, 40, 20, new StringTextComponent("SAVE"), pressable -> saveConfig()));

        // MISC
        this.enhanced_tooltips = ConfigTombstone.client.showEnhancedTooltips.get();
        this.skipRespawnScreen = ConfigTombstone.client.skipRespawnScreen.get();
        this.dateInMCTime = ConfigTombstone.client.dateInMCTime.get();
        this.displayKnowledgeMessage = ConfigTombstone.client.displayKnowledgeMessage.get();
        this.equipElytraInPriority = ConfigTombstone.client.equipElytraInPriority.get();
        this.showInfoOnEnchantment = ConfigTombstone.client.showInfoOnEnchantment.get();
        this.graveSkinRule = ConfigTombstone.client.graveSkinRule.get();
        this.fogDensity = ConfigTombstone.client.fogDensity.get();
        this.priorizeToolOnHotbar = ConfigTombstone.client.priorizeToolOnHotbar.get();
        this.activateGraveBySneaking = ConfigTombstone.client.activateGraveBySneaking.get();
        this.misc1Buttons.clear();
        // TODO translate config !!!
        this.misc1Buttons.add(new BooleanConfigOption("Enhanced Tooltips", () -> enhanced_tooltips, (b) -> enhanced_tooltips = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 36, 190));
        this.misc1Buttons.add(new BooleanConfigOption("Skip Respawn Screen", () -> skipRespawnScreen, (b) -> skipRespawnScreen = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 50, 190));
        this.misc1Buttons.add(new BooleanConfigOption("Date in MC Time", () -> dateInMCTime, (b) -> dateInMCTime = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 64, 190));
        this.misc1Buttons.add(new BooleanConfigOption("Display Knowledge Message", () -> displayKnowledgeMessage, (b) -> displayKnowledgeMessage = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 78, 190));
        this.misc1Buttons.add(new BooleanConfigOption("Equip Elytra in Priority", () -> equipElytraInPriority, (b) -> equipElytraInPriority = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 92, 190));
        this.misc1Buttons.add(new BooleanConfigOption("Show Info on Enchantment", () -> showInfoOnEnchantment, (b) -> showInfoOnEnchantment = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 106, 190));
        this.misc1Buttons.add(new IntegerConfigOption("Grave Skin", () -> graveSkinRule.ordinal(), (b) -> graveSkinRule = GraveSkinRule.values()[b], GraveSkinRule.values().length - 1, d -> isDirty = d) {
            @Override
            protected ITextComponent getOptionName() {
                return new TranslationTextComponent("options.generic_value", getBaseMessageTranslation(), new TranslationTextComponent(GraveSkinRule.values()[get()].name()));
            }
        }.createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 120, 190));
        this.misc1Buttons.add(new IntegerConfigOption("Fog Density", () -> fogDensity.ordinal(), (b) -> fogDensity = FogDensity.values()[b], FogDensity.values().length - 1, d -> isDirty = d) {
            @Override
            protected ITextComponent getOptionName() {
                return new TranslationTextComponent("options.generic_value", getBaseMessageTranslation(), new TranslationTextComponent(FogDensity.values()[get()].name()));
            }
        }.createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 134, 190));
        this.misc1Buttons.add(new BooleanConfigOption("Favor Tools On Hotbar", () -> priorizeToolOnHotbar, (b) -> priorizeToolOnHotbar = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 148, 190));
        this.misc1Buttons.add(new BooleanConfigOption("Activate Grave By Sneaking", () -> activateGraveBySneaking, (b) -> activateGraveBySneaking = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 162, 190));
        for (Widget w : this.misc1Buttons) {
            addButton(w);
        }
        // EFFECT
        this.highlightGrave = ConfigTombstone.client.highlight.get();
        this.showShadowStep = ConfigTombstone.client.showShadowStep.get();
        this.enableHalloweenEffect = ConfigTombstone.client.enableHalloweenEffect.get();
        this.showMagicCircle = ConfigTombstone.client.showMagicCircle.get();
        this.fogPeriod = ConfigTombstone.client.fogPeriod.get();
        this.effectButtons.clear();
        this.effectButtons.add(new BooleanConfigOption("Highlight Grave", () -> highlightGrave, (b) -> highlightGrave = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 36, 190));
        this.effectButtons.add(new BooleanConfigOption("Show ShadowStep", () -> showShadowStep, (b) -> showShadowStep = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 50, 190));
        this.effectButtons.add(new BooleanConfigOption("Enable Halloween Effect", () -> enableHalloweenEffect, (b) -> enableHalloweenEffect = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 64, 190));
        this.effectButtons.add(new BooleanConfigOption("Shows Magic circles", () -> showMagicCircle, (b) -> showMagicCircle = b, d -> isDirty = d).createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 78, 190));
        this.effectButtons.add(new IntegerConfigOption("Fog Period", () -> fogPeriod.ordinal(), (b) -> fogPeriod = FogPeriod.values()[b], FogPeriod.values().length - 1, d -> isDirty = d) {
            @Override
            protected ITextComponent getOptionName() {
                return new TranslationTextComponent("options.generic_value", getBaseMessageTranslation(), new TranslationTextComponent(FogPeriod.values()[get()].name()));
            }
        }.createWidget(getMinecraft().gameSettings, this.guiLeft + 3, this.guiTop + 92, 190));
        for (Widget w : this.effectButtons) {
            addButton(w);
        }
        int color = ConfigTombstone.client.particleCastingColor.get();
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        this.colorButtonHandler0 = new ColorButtonHandler(getMinecraft().gameSettings, this.buttons, this.children, this.guiLeft + 100, this.guiTop + 111, 80, 4, r, g, b, "particleCastingColor", d -> isDirty = d);
        color = ConfigTombstone.client.fogColor.get();
        r = color >> 16 & 255;
        g = color >> 8 & 255;
        b = color & 255;
        this.colorButtonHandler4 = new ColorButtonHandler(getMinecraft().gameSettings, this.buttons, this.children, this.guiLeft + 100, this.guiTop + 136, 80, 4, r, g, b, "fogColor", d -> isDirty = d);

        // GRAVE
        this.graveModel = ConfigTombstone.client.favoriteGrave.get();
        this.marbleType = ConfigTombstone.client.favoriteGraveMarble.get();
        addButton(this.buttonGraveTexture = new OptionSlider(getMinecraft().gameSettings, this.guiRight - 10 - 30, this.guiTop + 40, 30, 20, new SliderPercentageOption("texture", 0d, 1d, 1f, settings -> (double) this.marbleType.ordinal(), (settings, d) -> this.marbleType = MarbleType.byId(d.intValue()), (settings, d) -> new TranslationTextComponent(this.marbleType.getString()))) {
            @Override
            protected void func_230972_a_() {
                super.func_230972_a_();
                isDirty = true;
            }
        });
        addButton(this.buttonLeftArrow = new TBGuiButton(this.guiLeft + 10, this.guiBottom - 25, 40, 15, new StringTextComponent("<-"), pressable -> {
            do {
                this.graveModel = this.graveModel.getPrevious();
            } while (this.graveModel.isOnlyContributor() && !Helper.isContributor);
            this.isDirty = true;
        }));
        addButton(this.buttonRightArrow = new TBGuiButton(this.guiRight - 10 - 40, this.guiBottom - 25, 40, 15, new StringTextComponent("->"), pressable -> {
            do {
                this.graveModel = this.graveModel.getNext();
            } while (this.graveModel.isOnlyContributor() && !Helper.isContributor);
            this.isDirty = true;
        }));

        // PLATE
        color = ConfigTombstone.client.textColorRIP.get();
        r = color >> 16 & 255;
        g = color >> 8 & 255;
        b = color & 255;
        this.colorButtonHandler1 = new ColorButtonHandler(getMinecraft().gameSettings, this.buttons, this.children, this.guiLeft + 6, this.guiTop + 57, 40, 4, r, g, b, "ripText", d -> isDirty = d);
        color = ConfigTombstone.client.textColorOwner.get();
        r = color >> 16 & 255;
        g = color >> 8 & 255;
        b = color & 255;
        this.colorButtonHandler2 = new ColorButtonHandler(getMinecraft().gameSettings, this.buttons, this.children, this.guiLeft + 6, this.guiTop + 87, 40, 4, r, g, b, "deathDateText", d -> isDirty = d);
        color = ConfigTombstone.client.textColorDeathDate.get();
        r = color >> 16 & 255;
        g = color >> 8 & 255;
        b = color & 255;
        this.colorButtonHandler3 = new ColorButtonHandler(getMinecraft().gameSettings, this.buttons, this.children, this.guiLeft + 6, this.guiTop + 121, 40, 4, r, g, b, "diedOnText", d -> isDirty = d);

        updateButtons();
    }

    private void updateButtons() {
        switch (this.tab) {
            case GRAVE:
                enableGraveButtons();
                disableColorButtons();
                disableMiscButtons();
                disableEffectButtons();
                break;
            case PLATE:
                enableColorButtons();
                disableGraveButtons();
                disableMiscButtons();
                disableEffectButtons();
                break;
            case MISC:
                enableMiscButtons();
                disableGraveButtons();
                disableColorButtons();
                disableEffectButtons();
                break;
            case EFFECT:
                disableColorButtons();
                enableEffectButtons();
                disableGraveButtons();
                disableMiscButtons();
                break;
        }
    }

    private void enableGraveButtons() {
        this.buttonGraveTexture.active = true;
        this.buttonGraveTexture.visible = true;
        this.buttonLeftArrow.active = true;
        this.buttonLeftArrow.visible = true;
        this.buttonRightArrow.active = true;
        this.buttonRightArrow.visible = true;
    }

    private void disableGraveButtons() {
        this.buttonGraveTexture.active = false;
        this.buttonGraveTexture.visible = false;
        this.buttonLeftArrow.active = false;
        this.buttonLeftArrow.visible = false;
        this.buttonRightArrow.active = false;
        this.buttonRightArrow.visible = false;
    }

    private void enableColorButtons() {
        this.colorButtonHandler1.enableButtons();
        this.colorButtonHandler2.enableButtons();
        this.colorButtonHandler3.enableButtons();
        this.colorButtonHandler4.enableButtons();
    }

    private void disableColorButtons() {
        this.colorButtonHandler1.disableButtons();
        this.colorButtonHandler2.disableButtons();
        this.colorButtonHandler3.disableButtons();
        this.colorButtonHandler4.disableButtons();
    }

    private void enableMiscButtons() {
        for (Widget widget : this.misc1Buttons) {
            if (widget != null) {
                widget.active = true;
                widget.visible = true;
            }
        }
    }

    private void disableMiscButtons() {
        for (Widget widget : this.misc1Buttons) {
            if (widget != null) {
                widget.active = false;
                widget.visible = false;
            }
        }
    }

    private void enableEffectButtons() {
        for (Widget widget : this.effectButtons) {
            if (widget != null) {
                widget.active = true;
                widget.visible = true;
            }
        }
        this.colorButtonHandler0.enableButtons();
        this.colorButtonHandler4.enableButtons();
    }

    private void disableEffectButtons() {
        for (Widget widget : this.effectButtons) {
            if (widget != null) {
                widget.active = false;
                widget.visible = false;
            }
        }
        this.colorButtonHandler0.disableButtons();
        this.colorButtonHandler4.disableButtons();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (this.isDirty) {
            this.isDirty = false;
            this.saveButton.forceHighlight = isChanged(false).getLeft();
        }
        renderBackground(matrixStack);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        String customTitle = title.getString();
        fill(matrixStack, this.guiLeft + 5, this.guiTop + 5, this.guiRight - 5, this.guiTop + 20 + font.FONT_HEIGHT, 0x55000000);
        drawString(matrixStack, this.font, customTitle, this.halfWidth - this.font.getStringWidth(customTitle) / 2, this.guiTop + 14, this.textColor);
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        String playerName = getMinecraft().player != null ? getMinecraft().player.getName().getString() : "Corail31";
        switch (this.tab) {
            case GRAVE:
                ItemStack stack = new ItemStack(ModBlocks.decorative_graves.get(this.graveModel));
                String graveName = stack.getDisplayName().getString();
                ItemBlockGrave.setModelTexture(stack, this.marbleType.ordinal());
                ItemBlockGrave.setEngravedName(stack, playerName);

                String graveString = I18n.format(graveName);
                drawString(matrixStack, this.font, graveString, this.halfWidth - this.font.getStringWidth(graveString) / 2, this.guiBottom - 55, this.textColor);

                fill(matrixStack, this.guiLeft + 5, this.guiTop + 35, this.guiRight - 5, this.guiBottom - 42, 0x50000000);

                Helper.renderStackInGui(stack, this.guiLeft + 40, this.guiTop + 32, 6f, true);
                break;
            case PLATE:
                getMinecraft().textureManager.bindTexture(tablet);
                blit(matrixStack, this.guiLeft + 46, this.guiTop + 36, 0, 0, 154, 154, 154, 154);

                int adjustText = 24;
                // TODO center this properly in the plate
                String ripString = TextFormatting.BOLD + "R.I.P.";
                drawString(matrixStack, this.font, ripString, this.halfWidth - this.font.getStringWidth(ripString) / 2 + adjustText, this.guiTop + 60, this.colorButtonHandler1.getColor());

                String ownerString = TextFormatting.BOLD + playerName;
                drawString(matrixStack, this.font, ownerString, this.halfWidth - this.font.getStringWidth(ownerString) / 2 + adjustText, this.guiTop + 90, this.colorButtonHandler2.getColor());

                String diedOnString = TextFormatting.ITALIC.toString() + "Hasn't died yet";
                drawString(matrixStack, this.font, diedOnString, this.halfWidth - this.font.getStringWidth(diedOnString) / 2 + adjustText, this.guiTop + 120, this.colorButtonHandler3.getColor());
                String soonString = TextFormatting.ITALIC.toString() + "but will soon";
                drawString(matrixStack, this.font, soonString, this.halfWidth - this.font.getStringWidth(soonString) / 2 + adjustText, this.guiTop + 130, this.colorButtonHandler3.getColor());
                break;
            case MISC:
                fill(matrixStack, this.guiLeft + 5, this.guiTop + 35, this.guiLeft + this.xSize - 5, this.guiTop + 194, 0x55000000);
                break;
            case EFFECT:
                fill(matrixStack, this.guiLeft + 5, this.guiTop + 35, this.guiLeft + this.xSize - 5, this.guiTop + 194, 0x55000000);
                drawString(matrixStack, this.font, "Particle Color", this.guiLeft + 20, this.guiTop + 115, this.colorButtonHandler0.getColor());
                drawString(matrixStack, this.font, "Fog Color", this.guiLeft + 20, this.guiTop + 140, this.colorButtonHandler4.getColor());
                break;
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private <T> boolean checkConfig(ForgeConfigSpec.ConfigValue<T> config, T newValue, boolean update) {
        if (!(newValue.equals(config.get()))) {
            if (update) {
                config.set(newValue);
            }
            return true;
        }
        return false;
    }

    private Pair<Boolean, Boolean> isChanged(boolean update) {
        boolean isModified = checkConfig(ConfigTombstone.client.favoriteGrave, this.graveModel, update);
        isModified = checkConfig(ConfigTombstone.client.favoriteGraveMarble, this.marbleType, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.displayKnowledgeMessage, this.displayKnowledgeMessage, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.equipElytraInPriority, this.equipElytraInPriority, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.priorizeToolOnHotbar, this.priorizeToolOnHotbar, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.activateGraveBySneaking, this.activateGraveBySneaking, update) || isModified;
        boolean requirePacket = isModified;
        isModified = checkConfig(ConfigTombstone.client.textColorRIP, this.colorButtonHandler1.getColor(), update) || isModified; // rip
        isModified = checkConfig(ConfigTombstone.client.textColorOwner, this.colorButtonHandler2.getColor(), update) || isModified; // owner
        isModified = checkConfig(ConfigTombstone.client.textColorDeathDate, this.colorButtonHandler3.getColor(), update) || isModified; // date
        isModified = checkConfig(ConfigTombstone.client.showEnhancedTooltips, this.enhanced_tooltips, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.highlight, this.highlightGrave, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.skipRespawnScreen, this.skipRespawnScreen, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.showShadowStep, this.showShadowStep, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.enableHalloweenEffect, this.enableHalloweenEffect, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.dateInMCTime, this.dateInMCTime, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.showInfoOnEnchantment, this.showInfoOnEnchantment, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.graveSkinRule, this.graveSkinRule, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.fogDensity, this.fogDensity, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.showMagicCircle, this.showMagicCircle, update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.particleCastingColor, this.colorButtonHandler0.getColor(), update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.fogColor, this.colorButtonHandler4.getColor(), update) || isModified;
        isModified = checkConfig(ConfigTombstone.client.fogPeriod, this.fogPeriod, update) || isModified;
        return Pair.of(isModified, requirePacket);
    }

    private void saveConfig() {
        if (this.saveButton.forceHighlight) {
            Pair<Boolean, Boolean> result = isChanged(true);
            if (result.getLeft()) {
                ConfigTombstone.CLIENT_SPEC.save();
            }
            if (result.getRight()) {
                PROXY.markConfigDirty();
            }
        }
        closeScreen();
    }
}
