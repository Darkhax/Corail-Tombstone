package ovh.corail.tombstone.helper;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.NetworkEvent;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.api.magic.ISoulConsumer;
import ovh.corail.tombstone.block.ItemBlockGrave;
import ovh.corail.tombstone.command.CommandTBAcceptTeleport;
import ovh.corail.tombstone.command.CommandTBBind;
import ovh.corail.tombstone.command.CommandTBKnownledge;
import ovh.corail.tombstone.command.CommandTBRecovery;
import ovh.corail.tombstone.command.CommandTBRequestTeleport;
import ovh.corail.tombstone.command.CommandTBRestoreInventory;
import ovh.corail.tombstone.command.CommandTBReviveFamiliar;
import ovh.corail.tombstone.command.CommandTBShowLastGrave;
import ovh.corail.tombstone.command.CommandTBSiege;
import ovh.corail.tombstone.command.CommandTBTeleport;
import ovh.corail.tombstone.command.CommandTBTeleportBiome;
import ovh.corail.tombstone.command.CommandTBTeleportDeath;
import ovh.corail.tombstone.command.CommandTBTeleportDiscovery;
import ovh.corail.tombstone.command.CommandTBTeleportGrave;
import ovh.corail.tombstone.command.CommandTBTeleportHome;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.registry.ModBlocks;
import ovh.corail.tombstone.registry.ModEnchantments;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

@SuppressWarnings({ "WeakerAccess", "unused" })
public class Helper {
    public static final Random random = new Random();
    public static Boolean isHalloween = null;
    private static Boolean isAprilFools = null;
    public static boolean isContributor = false;
    private static ContributorStore CONTRIBUTORS = ContributorStore.of();

    public static boolean isContributor(PlayerEntity player) {
        return player.world.isRemote ? isContributor : CONTRIBUTORS.contains(player);
    }

    public static boolean isDisabledPerk(@Nullable Perk perk, @Nullable PlayerEntity player) {
        return perk == null || perk.isDisabled(player);
    }

    public static int getRandom(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public static float getRandom(float min, float max) {
        return random.nextFloat() * (max - min) + min;
    }

    public static double getRandom(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    public static boolean getRandom() {
        return random.nextBoolean();
    }

    public static String dump(Object o) {
        return ToStringBuilder.reflectionToString(o, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static boolean existClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isModLoad(String modid) {
        return ModList.get() != null && ModList.get().getModContainerById(modid).isPresent();
    }

    public static <T> List<T> iterableToList(Iterable<T> it) {
        List<T> list = new ArrayList<>();
        it.forEach(list::add);
        return list;
    }

    public static RegistryKey<World> getDimensionType(World world) {
        return world.func_234923_W_();
    }

    public static BlockPos getCloserValidPos(World world, BlockPos pos) {
        WorldBorder border = world.getWorldBorder();
        boolean validXZ = border.contains(pos);
        boolean validY = !World.isOutsideBuildHeight(pos);
        if (validXZ && validY) {
            return pos;
        }
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (!validXZ) {
            x = Math.min(Math.max(pos.getX(), (int) border.minX()), (int) border.maxX());
            z = Math.min(Math.max(pos.getZ(), (int) border.minZ()), (int) border.maxZ());
        }
        if (!validY) {
            y = Math.max(Math.min(pos.getY(), world.func_230315_m_().func_241513_m_()), 0);
        }
        return new BlockPos(x, y, z);
    }

    public static boolean isValidPos(@Nullable World world, BlockPos pos) {
        return world != null && world.getWorldBorder().contains(pos) && !World.isOutsideBuildHeight(pos);
    }

    public static boolean isInvalidDimension(MinecraftServer server, RegistryKey<World> key) {
    	return server.getWorld(key) == null;
    }

    @Nullable
    public static <T> T getRandomInList(List<T> list) {
        return list.isEmpty() ? null : list.get(random.nextInt(list.size()));
    }

    public static Pair<String, String> parseRLString(String rlString) {
        String[] splits = rlString.split(":");
        boolean noDomain = splits.length == 1;
        return noDomain ? Pair.of("minecraft", rlString) : Pair.of(splits[0], splits[1]);
    }

    public static Location findNearestStructure(ServerWorld world, BlockPos pos, ResourceLocation structureRL, int y, boolean unexplored) {
        return findNearestStructure(world, pos, structureRL.toString(), y, unexplored);
    }

    public static Location findNearestStructure(ServerWorld world, BlockPos pos, String structureName, int y, boolean unexplored) {
        BlockPos startingPos = pos;
        for (int nbTry = 0; nbTry < 5; nbTry++) {
            startingPos = getCloserValidPos(world, startingPos.add(nbTry * random.nextGaussian() * 2000, 0d, nbTry * random.nextGaussian() * 2000));
            Structure<?> structure = SupportStructures.getStructure(structureName);
            if (structure != null) {
                final BlockPos foundPos = world.func_241117_a_(structure, startingPos, 100, unexplored);
                if (foundPos != null && isValidPos(world, foundPos)) {
                    return new Location(foundPos.getX(), y, foundPos.getZ(), world);
                }
            }
        }
        return Location.ORIGIN;
    }

    public static Location findNearestStructure(ServerWorld world, BlockPos pos, String structureRLString, boolean unexplored) {
        return findNearestStructure(world, pos, structureRLString, pos.getY(), unexplored);
    }

    public static BlockPos getTopSolidOrLiquidBlock(World world, BlockPos pos) {
        IChunk chunk = world.getChunk(pos);

        BlockPos blockpos;
        BlockPos blockpos1;
        for (blockpos = new BlockPos(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ()); blockpos.getY() >= 0; blockpos = blockpos1) {
            blockpos1 = blockpos.down();
            BlockState state = chunk.getBlockState(blockpos1);
            if (state.getMaterial().blocksMovement() && state.getMaterial() != Material.LEAVES) {// && !state.getBlock().isFoliage(this, blockpos1)) {
                break;
            }
        }

        return blockpos;
    }

    public static <T extends Entity> T teleportEntity(T entity, Location loc) {
        if (!entity.world.isRemote) {
            MinecraftServer server = entity.getServer();
            if (server != null) {
                ServerWorld targetWorld = server.getWorld(loc.dim);
                if (targetWorld != null) {
                    TeleportationHandler.teleportEntity(entity, targetWorld, (double) loc.x + 0.5d, (double) loc.y + 0.1d, (double) loc.z + 0.5d);
                }
            }
        }
        return entity;
    }

    public static <T extends Entity> T teleportToGrave(T entity, Location loc) {
        return teleportEntity(entity, new Location(loc.getPos().up(), loc.dim));
    }

    public static boolean isRuleKeepInventory(PlayerEntity player) {
        return isRuleKeepInventory(player.world);
    }

    public static boolean isRuleKeepInventory(World world) {
        return world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
    }

    public static void removeNoEvent(World world, BlockPos pos) {
        placeNoEvent(world, pos, Blocks.AIR.getDefaultState());
    }

    @SuppressWarnings("deprecation")
    public static void placeNoEvent(World world, BlockPos pos, BlockState state) {
        pos = pos.toImmutable();
        if (!ConfigTombstone.player_death.gravesBypassGriefingRules.get()) {
            world.setBlockState(pos, state, 3);
            return;
        }
        int i = pos.getX() & 15;
        int j = pos.getY();
        int k = pos.getZ() & 15;
        Chunk chunk = (Chunk) world.getChunk(pos);
        ChunkSection[] storageArray = chunk.getSections();
        ChunkSection chunksection = storageArray[j >> 4];
        if (chunksection == Chunk.EMPTY_SECTION) {
            if (state.isAir()) {
                return;
            }
            chunksection = new ChunkSection(j >> 4 << 4);
            storageArray[j >> 4] = chunksection;
        }
        boolean flag = chunksection.isEmpty();
        BlockState blockstate = chunksection.setBlockState(i, j & 15, k, state);
        if (blockstate != state) {
            Block block = state.getBlock();
            Block block1 = blockstate.getBlock();
            chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING).update(i, j, k, state);
            chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES).update(i, j, k, state);
            chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR).update(i, j, k, state);
            chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).update(i, j, k, state);
            boolean flag1 = chunksection.isEmpty();
            if (flag != flag1) {
                world.getChunkProvider().getLightManager().func_215567_a(pos, flag1);
            }
            if (!world.isRemote) {
                blockstate.onReplaced(world, pos, state, false);
            } else if (block1 != block && blockstate.hasTileEntity()) {
                world.removeTileEntity(pos);
            }
            if (chunksection.getBlockState(i, j & 15, k).getBlock() == block) {
                if (blockstate.hasTileEntity()) {
                    TileEntity tileentity = chunk.getTileEntity(pos, Chunk.CreateEntityType.CHECK);
                    if (tileentity != null) {
                        tileentity.updateContainingBlockInfo();
                    }
                }
                if (!world.isRemote) {
                    state.onBlockAdded(world, pos, blockstate, false);
                }
                if (state.hasTileEntity()) {
                    TileEntity tileentity1 = chunk.getTileEntity(pos, Chunk.CreateEntityType.CHECK);
                    if (tileentity1 == null) {
                        tileentity1 = state.createTileEntity(world);
                        world.setTileEntity(pos, tileentity1);
                    } else {
                        tileentity1.updateContainingBlockInfo();
                    }
                }
                chunk.markDirty();
            }
            int oldLight = blockstate.getLightValue(world, pos);
            int oldOpacity = blockstate.getOpacity(world, pos);
            if (state.getOpacity(world, pos) != oldOpacity || state.getLightValue(world, pos) != oldLight || state.isTransparent() || blockstate.isTransparent()) {
                world.getChunkProvider().getLightManager().checkBlock(pos);
            }
            world.markAndNotifyBlock(pos, chunk, blockstate, state, 3, 512);
        }
    }

    public static boolean canShowTooltip(@Nullable World world, ItemStack stack) {
        return world != null && (ConfigTombstone.client.showEnhancedTooltips.get() || Screen.hasShiftDown());
    }

	// TODO
    public static boolean isDateAroundHalloween() {
        return isHalloween != null && isHalloween;
    }

    public static boolean isDateAroundHalloween(LocalDate date) {
        if (isHalloween == null) {
            isHalloween = ConfigTombstone.general.persistantHalloween.get() || (date.get(ChronoField.MONTH_OF_YEAR) == 10 && date.get(ChronoField.DAY_OF_MONTH) >= 20) || (date.get(ChronoField.MONTH_OF_YEAR) == 11 && date.get(ChronoField.DAY_OF_MONTH) <= 3);
        }
        return isHalloween;
    }

    public static final String PASS_APRIL_FOOLS_DAY_NBT_BOOL = "pass_april_fools_day";
    public static final String APRIL_FOOLS_DAY_SLOWNESS_NBT_BOOL = "april_fools_day_slowness";

    public static boolean isAprilFoolsDay() {
        // only done for server side
        if (isAprilFools == null) {
            LocalDate date = LocalDate.now();
            isAprilFools = date.get(ChronoField.MONTH_OF_YEAR) == 4 && date.get(ChronoField.DAY_OF_MONTH) == 1;
        }
        return isAprilFools;
    }

    public static void triggerAprilFoolsDay(ServerPlayerEntity player) {
        CompoundNBT persistentTag = EntityHelper.getPersistentTag(player);
        if (isAprilFoolsDay()) {
            persistentTag.putBoolean(PASS_APRIL_FOOLS_DAY_NBT_BOOL, true);
        } else if (persistentTag.contains(PASS_APRIL_FOOLS_DAY_NBT_BOOL)) {
            persistentTag.remove(PASS_APRIL_FOOLS_DAY_NBT_BOOL);
            ModTriggers.PASS_APRIL_FOOL.trigger(player);
        }
    }

    public static boolean isAprilFoolsDaySnowball(LivingEntity entity, @Nullable DamageSource source) {
        Entity immediateSource;
        return isAprilFoolsDay() && EntityHelper.isValidPlayerMP(entity) && source != null && (immediateSource = source.getImmediateSource()) != null && immediateSource.getType() == EntityType.SNOWBALL && immediateSource.getPersistentData().contains(APRIL_FOOLS_DAY_SLOWNESS_NBT_BOOL);
    }

    public static void handleAprilFoolsDayGrave(World world, BlockPos pos) {
        final Vector3d centerVec = new Vector3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5d, 1d, 0.5d);
        final PlayerEntity closestPlayer = world.getClosestPlayer(centerVec.x, centerVec.y, centerVec.z, 20d, false);
        if (closestPlayer != null) {
            final SnowballEntity snowballentity = new SnowballEntity(world, centerVec.x, centerVec.y, centerVec.z);
            if (canSeeEntity(closestPlayer, centerVec)) {
                snowballentity.setItem(new ItemStack(Items.BONE));
                snowballentity.getPersistentData().putBoolean(APRIL_FOOLS_DAY_SLOWNESS_NBT_BOOL, true);
                Vector3d vec = closestPlayer.getPositionVec().add(0d, closestPlayer.getEyeHeight(), 0d).subtract(snowballentity.getPositionVec());
                snowballentity.shoot(vec.x, vec.y, vec.z, 1.5f, 1f);
                world.addEntity(snowballentity);
            }
        }
    }

    public static boolean canSeeEntity(Entity entity, Vector3d vec3d) {
        Vector3d vec3d1 = entity.getPositionVec().add(0d, entity.getEyeHeight(), 0d);
        return entity.world.rayTraceBlocks(new RayTraceContext(vec3d, vec3d1, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity)).getType() == RayTraceResult.Type.MISS;
    }

    @Nullable
    public static MinecraftServer getServer() {
        try {
            MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
            if (server != null && server.isOnExecutionThread()) {
                return server;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean isNight(World world) {
        float angle = world.getCelestialAngle(0.0f);
        return angle >= 0.245f && angle <= 0.755f;
    }

    public static boolean isDay(World world) {
        return !isNight(world);
    }

    public static boolean containRL(List<String> listRL, @Nullable ResourceLocation rl) {
        return rl != null && containRL(listRL, rl.getNamespace(), rl.getPath());
    }

    public static boolean containRL(List<String> listRL, String domain, String path) {
        return listRL.stream().anyMatch(p -> p.contains(":") ? p.equals(domain + ":" + path) : p.equals(domain));
    }

    public static float[] getRGBColor3F(int color) {
        float[] rgb = new float[3];
        rgb[0] = (float) (color >> 16 & 255) / 255f;
        rgb[1] = (float) (color >> 8 & 255) / 255f;
        rgb[2] = (float) (color & 255) / 255f;
        return rgb;
    }

    public static float[] getRGBColor4F(int color) {
        float[] rgb = new float[4];
        rgb[0] = (float) (color >> 16 & 255) / 255f;
        rgb[1] = (float) (color >> 8 & 255) / 255f;
        rgb[2] = (float) (color & 255) / 255f;
        rgb[3] = (float) (color >> 24 & 255) / 255f;
        return rgb;
    }

    public static float[] getHSBtoRGBF(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        float[] rgb = new float[3];
        rgb[0] = r / 255f;
        rgb[1] = g / 255f;
        rgb[2] = b / 255f;
        return rgb;
    }

    @OnlyIn(Dist.CLIENT)
    public static void fillGradient(Matrix4f matrix, int left, int top, int right, int bottom, int color1, int color2, int zLevel, boolean isHorizontal) {
        float[] argb1 = getRGBColor4F(color1);
        float[] argb2 = getRGBColor4F(color2);

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        makeVertex(matrix, bufferbuilder, right, top, zLevel, isHorizontal ? argb2 : argb1);
        makeVertex(matrix, bufferbuilder, left, top, zLevel, argb1);
        makeVertex(matrix, bufferbuilder, left, bottom, zLevel, isHorizontal ? argb1 : argb2);
        makeVertex(matrix, bufferbuilder, right, bottom, zLevel, argb2);
        tessellator.draw();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }

    @SuppressWarnings("deprecation")
    @OnlyIn(Dist.CLIENT)
    public static void renderStackInGui(ItemStack stack, int xPosition, int yPosition, double scale, boolean isSpinning) {
        Minecraft mc = Minecraft.getInstance();

        RenderSystem.pushMatrix();
        //PortingHelper.enableStandardItemLighting();

        mc.textureManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        mc.textureManager.getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE).setBlurMipmapDirect(false, false);

        RenderSystem.enableRescaleNormal();
        RenderSystem.scaled(scale, scale, scale);
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        RenderSystem.translatef((float) (xPosition / scale), (float) (yPosition / scale), 100f + mc.getItemRenderer().zLevel);
        RenderSystem.translatef(8f, 8f, 0f);
        RenderSystem.scalef(16f, -16f, 16f);

        MatrixStack matrixStack = new MatrixStack();
        IRenderTypeBuffer.Impl irendertypebuffer$impl = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        IBakedModel bakedModel = mc.getItemRenderer().getItemModelWithOverrides(stack, null, null);
        boolean flag = !bakedModel.isGui3d();
        if (flag) {
            RenderHelper.setupGuiFlatDiffuseLighting();
        }
        //ForgeHooksClient.handleCameraTransforms(matrixStack, bakedModel, net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType.GUI, false);
        if (isSpinning) {
            //GL11.glRotated((System.currentTimeMillis() * 0.03d) % 360d, 0f, 1f, 0f);
        }

        mc.getItemRenderer().renderItem(stack, net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType.GUI, false, matrixStack, irendertypebuffer$impl, 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
        irendertypebuffer$impl.finish();
        RenderSystem.enableDepthTest();
        if (flag) {
            RenderHelper.setupGui3DDiffuseLighting();
        }

        RenderSystem.disableAlphaTest();
        RenderSystem.disableRescaleNormal();
        //RenderSystem.disableLighting();
        double scaleRev = 1d / scale;
        RenderSystem.scaled(scaleRev, scaleRev, scaleRev);
        //RenderHelper.disableStandardItemLighting();
        RenderSystem.popMatrix();

        //getMinecraft().textureManager.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        //getMinecraft().textureManager.func_229267_b_(AtlasTexture.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    @OnlyIn(Dist.CLIENT)
    private static void makeVertex(Matrix4f matrix, BufferBuilder bufferbuilder, int x, int y, int zLevel, float[] colorArray) {
        bufferbuilder.pos(matrix, x, y, zLevel).color(colorArray[0], colorArray[1], colorArray[2], colorArray[3]).endVertex();
    }

    public static Set<Enchantment> getTombstoneEnchantments(ItemStack stack) {
        boolean isEnchantedBook = stack.getItem() == Items.ENCHANTED_BOOK;
        if (!isEnchantedBook && SharedConfigTombstone.enchantments.enableEnchantmentSoulbound.get() && SharedConfigTombstone.enchantments.enableEnchantmentShadowStep.get() && SharedConfigTombstone.enchantments.enableEnchantmentMagicSiphon.get() && SharedConfigTombstone.enchantments.enableEnchantmentPlagueBringer.get()) {
            return Sets.newLinkedHashSet();
        }
        Set<Enchantment> list = Sets.newLinkedHashSet();
        ListNBT nbttaglist = isEnchantedBook ? EnchantedBookItem.getEnchantments(stack) : stack.getEnchantmentTagList();
        if (nbttaglist.size() > 0) {
            Map<String, Enchantment> enchants = new HashMap<>();
            Arrays.stream(new Enchantment[] { ModEnchantments.soulbound, ModEnchantments.shadow_step, ModEnchantments.magic_siphon, ModEnchantments.plague_bringer }).forEach(enchant -> enchants.put(enchant.getRegistryName().toString(), enchant));
            IntStream.range(0, nbttaglist.size()).mapToObj(nbttaglist::getCompound).filter(nbt -> nbt.contains("id", Constants.NBT.TAG_STRING)).forEach(nbt -> Optional.ofNullable(enchants.get(nbt.getString("id"))).ifPresent(list::add));
        }
        return list;
    }

    public static void damageItem(ItemStack stack, int amount, ServerPlayerEntity player, Hand hand) {
        stack.damageItem(amount, player, (serverPlayer) -> serverPlayer.sendBreakAnimation(hand));
    }

    private static final Map<ResourceLocation, Boolean> TAMEABLE = new HashMap<>();

    @SuppressWarnings("deprecation")
    public static boolean isTameable(World world, String entityTypeString) {
        if (!entityTypeString.isEmpty()) {
            ResourceLocation resourceLocation = ResourceLocation.tryCreate(entityTypeString);
            if (resourceLocation != null) {
                return TAMEABLE.computeIfAbsent(resourceLocation, rl -> Registry.ENTITY_TYPE.getValue(rl).map(entry -> {
                    Entity entity = entry.create(world);
                    return isTameable(entity);
                }).orElse(false));
            }
        }
        return false;
    }

    public static boolean isTameable(@Nullable Entity entity) {
        return entity instanceof TameableEntity || entity instanceof AbstractHorseEntity;
    }

    public static double getDistance(Vector3i vec1, Vector3i vec2) {
        return Math.sqrt(getDistanceSq(vec1, vec2));
    }

    public static double getDistanceSq(Vector3i vec1, Vector3i vec2) {
        return getDistanceSq(vec1, vec2.getX(), vec2.getY(), vec2.getZ());
    }

    public static double getDistanceSq(Vector3i vec1, int x2, int y2, int z2) {
        return vec1.distanceSq(x2, y2, z2, false);
    }

    public static boolean isPacketToClient(NetworkEvent.Context ctx) {
        return ctx.getDirection().getOriginationSide() == LogicalSide.SERVER && ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT;
    }

    public static boolean isPacketToServer(NetworkEvent.Context ctx) {
        return ctx.getDirection().getOriginationSide() == LogicalSide.CLIENT && ctx.getDirection().getReceptionSide() == LogicalSide.SERVER;
    }

    public static String getFormattingCode(Style style) {
        if (style.isEmpty()) {
            return "";
        }
        StringBuilder stringbuilder = new StringBuilder();
        if (style.getColor() != null) {
            fromColor(style.getColor()).ifPresent(stringbuilder::append);
        }
        if (style.getBold()) {
            stringbuilder.append(TextFormatting.BOLD);
        }
        if (style.getItalic()) {
            stringbuilder.append(TextFormatting.ITALIC);
        }
        if (style.getUnderlined()) {
            stringbuilder.append(TextFormatting.UNDERLINE);
        }
        if (style.getObfuscated()) {
            stringbuilder.append(TextFormatting.OBFUSCATED);
        }
        if (style.getStrikethrough()) {
            stringbuilder.append(TextFormatting.STRIKETHROUGH);
        }
        return stringbuilder.toString();
    }

    public static Optional<TextFormatting> fromColor(Color color) {
        return Stream.of(TextFormatting.values()).filter(f -> f.getColor() != null && f.getColor().equals(color.field_240740_c_)).findFirst();
    }

    @SuppressWarnings("ConstantConditions")
    public static <T> T unsafeNullCast() {
        return null;
    }

    public static Capability.IStorage<ISoulConsumer> getNullStorage() {
        return new Capability.IStorage<ISoulConsumer>() {

            @Override
            @Nullable
            public INBT writeNBT(Capability<ISoulConsumer> capability, ISoulConsumer instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<ISoulConsumer> capability, ISoulConsumer instance, Direction side, INBT nbt) {
            }
        };
    }

    public static WorldFunctionInjector buildKnowledgeFunction() {
        return WorldFunctionInjector.builder().put(new ResourceLocation(MOD_ID, "knowledge"), (sender, params) -> {
            Entity entity = sender.getEntity();
            if (entity instanceof ServerPlayerEntity) {
                EntityHelper.addKnowledge((ServerPlayerEntity) entity, params.getInt("amount", 1));
            }
        }).build();
    }

    public static void initCommands(CommandDispatcher<CommandSource> commandDispatcher) {
        new CommandTBRestoreInventory(commandDispatcher).registerCommand();
        new CommandTBShowLastGrave(commandDispatcher).registerCommand();
        new CommandTBTeleportGrave(commandDispatcher).registerCommand();
        new CommandTBKnownledge(commandDispatcher).registerCommand();
        new CommandTBAcceptTeleport(commandDispatcher).registerCommand();
        new CommandTBSiege(commandDispatcher).registerCommand();
        new CommandTBTeleportDiscovery(commandDispatcher).registerCommand();
        new CommandTBRecovery(commandDispatcher).registerCommand();
        new CommandTBTeleport(commandDispatcher).registerCommand();
        new CommandTBReviveFamiliar(commandDispatcher).registerCommand();
        new CommandTBTeleportHome(commandDispatcher).registerCommand();
        new CommandTBTeleportBiome(commandDispatcher).registerCommand();
        new CommandTBRequestTeleport(commandDispatcher).registerCommand();
        new CommandTBTeleportDeath(commandDispatcher).registerCommand();
        new CommandTBBind(commandDispatcher).registerCommand();
    }

    @OnlyIn(Dist.CLIENT)
    public static void initModelProperties() {
        for (Block decorativeGrave : ModBlocks.decorative_graves.values()) {
            ItemModelsProperties.func_239418_a_(decorativeGrave.asItem(), new ResourceLocation("model_texture"), (stack, world, entity) -> (ItemBlockGrave.isEngraved(stack) ? 0.1f : 0f) + (ItemBlockGrave.getModelTexture(stack) == 1 ? 0.01f : 0f));
        }
        ItemModelsProperties.func_239418_a_(ModItems.bone_needle, new ResourceLocation("filled"), (stack, world, entity) -> ModItems.bone_needle.getEntityType(stack).isEmpty() ? 0f : 1f);
        ItemModelsProperties.func_239418_a_(ModItems.lost_tablet, new ResourceLocation("structure"), (stack, world, player) -> {
            String structureId = ModItems.lost_tablet.getStructureId(stack);
            return structureId != null ? SupportStructures.VILLAGE.is(structureId) ? 0.5f : 1f : 0f;
        });
        ItemModelsProperties.func_239418_a_(ModItems.tablet_of_home, new ResourceLocation("ancient"), (stack, worldIn, entityIn) -> ModItems.tablet_of_home.isAncient(stack) ? 1f : 0f);
        ItemModelsProperties.func_239418_a_(ModItems.tablet_of_recall, new ResourceLocation("ancient"), (stack, worldIn, entityIn) -> ModItems.tablet_of_recall.isAncient(stack) ? 1f : 0f);
        ItemModelsProperties.func_239418_a_(ModItems.fishing_rod_of_misadventure, new ResourceLocation("cast"), (stack, world, entity) -> {
            if (entity == null) {
                return 0f;
            }
            boolean isMainHand = entity.getHeldItemMainhand() == stack;
            boolean isOffhand = entity.getHeldItemOffhand() == stack;
            if (entity.getHeldItemMainhand().getItem() instanceof FishingRodItem) {
                isOffhand = false;
            }
            return (isMainHand || isOffhand) && entity instanceof PlayerEntity && ((PlayerEntity)entity).fishingBobber != null ? 1f : 0f;
        });
    }
}
