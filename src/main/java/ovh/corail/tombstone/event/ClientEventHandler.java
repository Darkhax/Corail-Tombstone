package ovh.corail.tombstone.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import ovh.corail.tombstone.api.cooldown.CooldownType;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.compatibility.SupportMods;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.enchantment.TombstoneEnchantment;
import ovh.corail.tombstone.gui.GuiKnowledge;
import ovh.corail.tombstone.helper.CooldownHandler;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.item.ItemGraveMagic;
import ovh.corail.tombstone.registry.ModEffects;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModParticleTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;
import static ovh.corail.tombstone.ModTombstone.MOD_NAME;

@SuppressWarnings({ "unused", "deprecated" })
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    private static final KeyBinding keybindKnowledge;

    static {
        ClientRegistry.registerKeyBinding(keybindKnowledge = new KeyBinding(LangKey.MESSAGE_KNOWLEDGE_OF_DEATH.getKey(), KeyConflictContext.IN_GAME, InputMappings.INPUT_INVALID, MOD_NAME));
    }

    private static boolean isGhostlyRender = false, hasTrueSight = false, isFirstGuiGameOver = false, requireRemovalNightVision = false, requireRemovalVisibility = false;
    private static boolean delayedGui = false;
    private static long nextGhostTime = -1L;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientChatEvent(ClientChatEvent event) {
        if ("/tbgui".equals(event.getMessage())) {
            delayedGui = true;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRenderCreatureEvent(RenderLivingEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (hasTrueSight && EntityHelper.isValidPlayer(mc.player) && event.getEntity().isInvisibleToPlayer(mc.player)) {
            event.getEntity().setInvisible(false);
            requireRemovalVisibility = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderCreatureEvent(RenderLivingEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (requireRemovalVisibility) {
            event.getEntity().setInvisible(true);
            requireRemovalVisibility = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.side == LogicalSide.SERVER) {
            return;
        }
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (EntityHelper.isValidPlayer(player)) {
            // provide night vision to ghostly shape & true sight
            if (event.phase == TickEvent.Phase.START) {
                hasTrueSight = EffectHelper.isPotionActive(player, ModEffects.ghostly_shape, 4) || EffectHelper.isPotionActive(player, ModEffects.true_sight);
                if (hasTrueSight) {
                    if (!EffectHelper.isPotionActive(player, Effects.NIGHT_VISION)) {
                        player.addPotionEffect(new EffectInstance(Effects.NIGHT_VISION, 1200, 0, true, false));
                        requireRemovalNightVision = true;
                    }
                }
            } else if (requireRemovalNightVision) {
                player.removeActivePotionEffect(Effects.NIGHT_VISION);
                requireRemovalNightVision = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlayerTickEvent(TickEvent.ClientTickEvent event) {
        if (event.side == LogicalSide.SERVER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.world == null || mc.isGamePaused()) {
            return;
        }
        if (event.phase == TickEvent.Phase.END && EntityHelper.isValidPlayer(mc.player)) {
            // open gui knowledge
            if (keybindKnowledge.isPressed() || delayedGui) {
                if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) {
                    mc.player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).ifPresent(cap -> {
                        delayedGui = false;
                        mc.displayGuiScreen(new GuiKnowledge(cap));
                    });
                }
            }
            // halloween ghost
            if (ConfigTombstone.client.enableHalloweenEffect.get() && Helper.isDateAroundHalloween()) {
                if (Helper.isNight(mc.world)) {
                    long time = mc.world.getGameTime();
                    if (nextGhostTime == -1 || time > nextGhostTime) {
                        if (nextGhostTime > -1 && CooldownHandler.INSTANCE.noCooldown(mc.player, CooldownType.NEXT_PRAY)) {
                            Vector3d ghostVec = mc.player.getPositionVec().add(Helper.getRandom(-9d, 9d), 0d, Helper.getRandom(-9d, 9d));
                            mc.particles.addParticle(ModParticleTypes.GHOST, ghostVec.x, ghostVec.y, ghostVec.z, 0d, 0d, 0d);
                        }
                        nextGhostTime = time + Helper.getRandom(60, 6000);
                    }
                } else {
                    nextGhostTime = -1;
                }
            }
        }
    }

    /**
     * skip the respawn screen, except in hardcore
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuiOpened(GuiOpenEvent event) {
        // TODO spawn xp balls if not handle xp
        if (event.getGui() instanceof DeathScreen && ConfigTombstone.client.skipRespawnScreen.get()) {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null && !player.world.getWorldInfo().isHardcore()) {
                isFirstGuiGameOver = !isFirstGuiGameOver;
                event.setCanceled(true);
                if (!isFirstGuiGameOver) {
                    player.respawnPlayer();
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (hasTrueSight && event.getInfo().getBlockAtCamera().getMaterial() == Material.WATER) {
            event.setCanceled(true);
            event.setDensity(event.getDensity() / 4f);
            RenderSystem.fogMode(GlStateManager.FogMode.EXP);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderFog(EntityViewRenderEvent.FogColors event) {
        if (hasTrueSight && event.getInfo().getBlockAtCamera().getMaterial() == Material.WATER) {
            event.setRed(23f / 255f);
            event.setGreen(106f / 255f);
            event.setBlue(236f / 255f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderBlockLayer(RenderBlockOverlayEvent event) {
        if (hasTrueSight && event.getOverlayType() == OverlayType.WATER) {
            event.setCanceled(true);
        }
    }

    /**
     * avoid to see the icon of night vision
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGuiRender(DrawScreenEvent event) {
        if (requireRemovalNightVision) {
            Minecraft.getInstance().player.removeActivePotionEffect(Effects.NIGHT_VISION);
            requireRemovalNightVision = false;
        }
    }

    /**
     * handle custom tooltips for itemstack
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handleTooltip(ItemTooltipEvent event) {
        /* insert infos about enchantments */
        if (SupportMods.canDisplayTooltipOnEnchant() && event.getPlayer() != null && event.getPlayer().world != null && ConfigTombstone.client.showInfoOnEnchantment.get() && event.getItemStack().getItem() == Items.ENCHANTED_BOOK && event.getItemStack().hasTag()) {
            Helper.getTombstoneEnchantments(event.getItemStack()).forEach(enchant ->
                    IntStream.range(0, event.getToolTip().size()).filter(line -> {
                        ITextComponent currentTooltip = event.getToolTip().get(line);
                        return currentTooltip instanceof TranslationTextComponent && enchant.getName().equals(((TranslationTextComponent) currentTooltip).getKey());
                    }).findFirst().ifPresent(line -> {
                        List<String> infos = ((TombstoneEnchantment) enchant).getTooltipInfos(event.getItemStack());
                        for (String info : infos) {
                            event.getToolTip().add(++line, new StringTextComponent(info));
                        }
                    })
            );
        }
    }

    private static boolean SKIP_RENDER_EVENT = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (SKIP_RENDER_EVENT) {
            SKIP_RENDER_EVENT = false;
            return;
        }
        isGhostlyRender = EffectHelper.isPotionActive(event.getPlayer(), ModEffects.ghostly_shape) || EffectHelper.isUnstableIntangiblenessActive(event.getPlayer());
        if (isGhostlyRender) {
            event.setCanceled(true);
            IRenderTypeBuffer.Impl iRenderTypeBuffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
            event.getMatrixStack().push();
            SKIP_RENDER_EVENT = true;
            event.getRenderer().render((AbstractClientPlayerEntity) event.getPlayer(), event.getPlayer().rotationYaw, event.getPartialRenderTick(), event.getMatrixStack(), iRenderTypeBuffer, 0xffffff);

            event.getMatrixStack().pop();
        }
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        if (!ConfigTombstone.client.highlight.get()) {
            return;
        }
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null || player.world == null) {
            return;
        }
        // show the target tomb when holding the tomb's key
        ItemStack stack = player.getHeldItemMainhand();
        Location location;
        if (stack.getItem() == ModItems.grave_key) {
            location = ModItems.grave_key.getTombPos(stack);
        } else if (stack.getItem() == ModItems.tablet_of_recall) {
            location = ModItems.tablet_of_recall.getTombPos(stack);
        } else if (ModItems.lost_tablet.isWakeUp(stack)) {
            location = ModItems.lost_tablet.getStructurePos(stack);
        } else {
            return;
        }
        if (location.isOrigin() || !location.dim.equals(player.world.func_234923_W_()) || !World.isValid(location.getPos())) {
            return;
        }
        createBox(event.getMatrixStack(), location.x, location.y, location.z, 1f);
    }

    private static void createBox(MatrixStack matrixStack, double x, double y, double z, double offset) {
        Minecraft mc = Minecraft.getInstance();

        RenderSystem.disableTexture();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        RenderSystem.pushMatrix();

        Vector3d projectedView = mc.gameRenderer.getActiveRenderInfo().getProjectedView();

        long c = (TimeHelper.systemTime() / 15L) % 360L;
        float[] color = Helper.getHSBtoRGBF(c / 360f, 1f, 1f);

        matrixStack.push();
        // get a closer pos if too far
        Vector3d vec = new Vector3d(x, y, z).subtract(projectedView);
        if (vec.distanceTo(Vector3d.ZERO) > 200d) { // could be 300
            vec = vec.normalize().scale(200d);
            x += vec.x;
            y += vec.y;
            z += vec.z;
        }

        x -= projectedView.getX();
        y -= projectedView.getY();
        z -= projectedView.getZ();

        Matrix4f matrix = matrixStack.getLast().getMatrix();
        RenderSystem.multMatrix(matrix);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder renderer = tessellator.getBuffer();
        renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        RenderSystem.color4f(color[0], color[1], color[2], 1f);

        RenderSystem.lineWidth(2.5f);
        renderer.pos(x, y, z).endVertex();
        renderer.pos(x + offset, y, z).endVertex();
        renderer.pos(x, y, z).endVertex();
        renderer.pos(x, y + offset, z).endVertex();
        renderer.pos(x, y, z).endVertex();
        renderer.pos(x, y, z + offset).endVertex();
        renderer.pos(x + offset, y + offset, z + offset).endVertex();
        renderer.pos(x, y + offset, z + offset).endVertex();
        renderer.pos(x + offset, y + offset, z + offset).endVertex();
        renderer.pos(x + offset, y, z + offset).endVertex();
        renderer.pos(x + offset, y + offset, z + offset).endVertex();
        renderer.pos(x + offset, y + offset, z).endVertex();
        renderer.pos(x, y + offset, z).endVertex();
        renderer.pos(x, y + offset, z + offset).endVertex();
        renderer.pos(x, y + offset, z).endVertex();
        renderer.pos(x + offset, y + offset, z).endVertex();
        renderer.pos(x + offset, y, z).endVertex();
        renderer.pos(x + offset, y, z + offset).endVertex();
        renderer.pos(x + offset, y, z).endVertex();
        renderer.pos(x + offset, y + offset, z).endVertex();
        renderer.pos(x, y, z + offset).endVertex();
        renderer.pos(x + offset, y, z + offset).endVertex();
        renderer.pos(x, y, z + offset).endVertex();
        renderer.pos(x, y + offset, z + offset).endVertex();
        tessellator.draw();

        matrixStack.pop();
        RenderSystem.popMatrix();

        RenderSystem.lineWidth(1f);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    @SubscribeEvent
    public static void setAuraToRender(RenderLivingEvent.Pre event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && ConfigTombstone.client.showMagicCircle.get() && !event.getEntity().isInvisibleToPlayer(player)) {
            Aura.addAura(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderAuras(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameSettings.thirdPersonView == 0 && mc.player != null && ConfigTombstone.client.showMagicCircle.get()) {
            Aura.addAura(mc.player);
        }
        if (!AURAS.isEmpty()) {
            double ratio = (System.currentTimeMillis() * 0.03d) % 360d;
            AURAS.forEach(aura -> renderAura(mc, event.getMatrixStack(), aura, ratio));
            AURAS.clear();
        }
    }

    enum AuraType {
        PRAY(0.5d + 2d * 1.5d, 0.88f, 0.78f, 0.48f, 1f),
        SCROLL(0.8d, 0.38f, 0.28f, 0.88f, 1f),
        SIMPLE_TELEPORT(0.8d, 0.23f, 0.78f, 0.86f, 1f),
        AREA_TELEPORT(0.5d + 3d * 1.5d, 0.23f, 0.78f, 0.86f, 1f),
        MARKER(1d, 0.38f, 0.48f, 0.18f, 1f);

        private final double radius;
        private final float r, g, b, a;

        AuraType(double radius, float r, float g, float b, float a) {
            this.radius = radius;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        private static AuraType getAuraType(LivingEntity entity) {
            if (!entity.isHandActive()) {
                return null;
            }
            ItemStack heldStack = entity.getHeldItemMainhand();
            Item heldItem = heldStack.getItem();
            if (heldItem == ModItems.ankh_of_pray) {
                return AuraType.PRAY;
            } else if (heldItem == ModItems.grave_key || heldItem == ModItems.lost_tablet || heldItem == ModItems.tablet_of_assistance) {
                return AuraType.SIMPLE_TELEPORT;
            } else if (heldItem == ModItems.tablet_of_recall || heldItem == ModItems.tablet_of_home) {
                return ((ItemGraveMagic) heldStack.getItem()).isAncient(heldStack) ? AuraType.AREA_TELEPORT : AuraType.SIMPLE_TELEPORT;
            } else if (Arrays.stream(ModItems.scroll_buff).anyMatch(item -> item == heldItem) || heldItem == ModItems.scroll_of_knowledge) {
                return AuraType.SCROLL;
            }
            return null;
        }
    }

    private static class Aura {
        private final Vector3d position;
        private final AuraType auraType;

        private Aura(LivingEntity entity, AuraType auraType) {
            float partialTicks = Minecraft.getInstance().getRenderPartialTicks();
            this.position = new Vector3d(MathHelper.lerp(partialTicks, entity.lastTickPosX, entity.getPosX()), MathHelper.lerp(partialTicks, entity.lastTickPosY, entity.getPosY()) + 0.1111d, MathHelper.lerp(partialTicks, entity.lastTickPosZ, entity.getPosZ()));
            this.auraType = auraType;
        }

        private Aura(double x, double y, double z) {
            this.position = new Vector3d(x, y, z);
            this.auraType = AuraType.MARKER;
        }

        private static void addAura(LivingEntity entity) {
            AuraType auraType = AuraType.getAuraType(entity);
            if (auraType != null) {
                Aura aura = new Aura(entity, auraType);
                double opaque = 1d;
                double distanceToCamera = Minecraft.getInstance().getRenderManager().getDistanceToCamera(aura.position.x, aura.position.y, aura.position.z);
                double f = (1d - distanceToCamera * 0.00390625d) * opaque;
                if (f > 0d) {
                    AURAS.add(aura);
                }
            }
        }
    }

    private static final List<Aura> AURAS = new ArrayList<>();
    private static final ResourceLocation[] AURA_TEXTURES = {
            new ResourceLocation(MOD_ID, "textures/aura/aura1.png")
    };

    @SuppressWarnings("deprecation")
    private static void renderAura(Minecraft mc, MatrixStack matrixStack, Aura aura, double ratio) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        RenderSystem.disableCull();
        mc.getRenderManager().textureManager.bindTexture(AURA_TEXTURES[0]);

        Vector3d projectedView = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        Vector3d vec3 = aura.position.subtract(projectedView);

        matrixStack.push();

        matrixStack.translate((float) vec3.x, (float) vec3.y, (float) vec3.z);
        matrixStack.rotate(Vector3f.YN.rotationDegrees((float) ratio));
        matrixStack.translate((float) -vec3.x, (float) -vec3.y, (float) -vec3.z);

        Matrix4f matrix = matrixStack.getLast().getMatrix();
        RenderSystem.multMatrix(matrix);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        Vector3d vec1 = new Vector3d(vec3.x - aura.auraType.radius, vec3.y + 0.014625d, vec3.z - aura.auraType.radius);
        Vector3d vec2 = new Vector3d(vec3.x + aura.auraType.radius, vec3.y + 0.014625d, vec3.z + aura.auraType.radius);
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        bufferbuilder.pos(vec1.x, vec1.y, vec1.z).tex(0f, 0f).color(aura.auraType.r, aura.auraType.g, aura.auraType.b, aura.auraType.a).normal(0f, 1f, 0f).endVertex();
        bufferbuilder.pos(vec1.x, vec1.y, vec2.z).tex(0f, 1f).color(aura.auraType.r, aura.auraType.g, aura.auraType.b, aura.auraType.a).normal(0f, 1f, 0f).endVertex();
        bufferbuilder.pos(vec2.x, vec1.y, vec2.z).tex(1f, 1f).color(aura.auraType.r, aura.auraType.g, aura.auraType.b, aura.auraType.a).normal(0f, 1f, 0f).endVertex();
        bufferbuilder.pos(vec2.x, vec1.y, vec1.z).tex(1f, 0f).color(aura.auraType.r, aura.auraType.g, aura.auraType.b, aura.auraType.a).normal(0f, 1f, 0f).endVertex();
        tessellator.draw();

        matrixStack.pop();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }
}
