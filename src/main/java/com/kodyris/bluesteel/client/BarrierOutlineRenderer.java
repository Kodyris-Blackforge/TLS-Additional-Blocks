package com.kodyris.bluesteel.client;

import com.kodyris.bluesteel.TlsAdditionalBlocksMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * While a barrier, kill block, trigger block or config stick is held, draws an outlined box with a
 * small cross at the center of every barrier (red), kill block (yellow) and trigger block (cyan) in the player's current
 * 16x16x16 sub-chunk and the 26 sub-chunks around it (a 3x3x3 block of sections).
 */
@Mod.EventBusSubscriber(modid = TlsAdditionalBlocksMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BarrierOutlineRenderer {
    private static List<BlockPos> barriers = new ArrayList<>();
    private static List<BlockPos> killBlocks = new ArrayList<>();
    private static List<BlockPos> triggers = new ArrayList<>();
    private static int counter = 0;
    private static long lastSection = Long.MIN_VALUE;

    /** How many sub-chunks out from the player's own sub-chunk to include. */
    private static final int SECTION_RANGE = 1;

    /** Half the length of each arm of the center marker drawn in every block (in blocks). */
    private static final float MARKER_HALF_SIZE = 0.15F;

    private static boolean isMarkerItem(ItemStack stack) {
        return stack.is(TlsAdditionalBlocksMod.BARRIER_ITEM.get())
                || stack.is(TlsAdditionalBlocksMod.KILL_BLOCK_ITEM.get())
                || stack.is(TlsAdditionalBlocksMod.TRIGGER_BLOCK_ITEM.get())
                || stack.is(TlsAdditionalBlocksMod.CONFIG_STICK.get());
    }

    private static boolean holdingMarkerItem(Player player) {
        return isMarkerItem(player.getMainHandItem()) || isMarkerItem(player.getOffhandItem());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !holdingMarkerItem(mc.player)) {
            if (!barriers.isEmpty()) barriers = new ArrayList<>();
            if (!killBlocks.isEmpty()) killBlocks = new ArrayList<>();
            if (!triggers.isEmpty()) triggers = new ArrayList<>();
            counter = 0;
            lastSection = Long.MIN_VALUE;
            return;
        }
        // Rescan every 10 ticks, and immediately when the player enters a new sub-chunk.
        long section = SectionPos.asLong(mc.player.blockPosition());
        boolean moved = section != lastSection;
        lastSection = section;
        if (!moved && counter++ % 10 != 0) return;
        if (moved) counter = 1;
        scan(mc, mc.level);
    }

    private static void scan(Minecraft mc, ClientLevel level) {
        List<BlockPos> foundBarriers = new ArrayList<>();
        List<BlockPos> foundKill = new ArrayList<>();
        List<BlockPos> foundTriggers = new ArrayList<>();
        BlockPos pp = mc.player.blockPosition();
        int psx = SectionPos.blockToSectionCoord(pp.getX());
        int psy = SectionPos.blockToSectionCoord(pp.getY());
        int psz = SectionPos.blockToSectionCoord(pp.getZ());
        Block barrier = TlsAdditionalBlocksMod.BARRIER.get();
        Block kill = TlsAdditionalBlocksMod.KILL_BLOCK.get();
        Block trigger = TlsAdditionalBlocksMod.TRIGGER_BLOCK.get();

        for (int cx = psx - SECTION_RANGE; cx <= psx + SECTION_RANGE; cx++) {
            for (int cz = psz - SECTION_RANGE; cz <= psz + SECTION_RANGE; cz++) {
                ChunkAccess access = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) continue;

                LevelChunkSection[] sections = chunk.getSections();
                for (int sy = psy - SECTION_RANGE; sy <= psy + SECTION_RANGE; sy++) {
                    int i = chunk.getSectionIndexFromSectionY(sy);
                    if (i < 0 || i >= sections.length) continue; // above/below the world

                    LevelChunkSection section = sections[i];
                    // Palette check first: sections without our blocks are skipped cheaply.
                    if (section.hasOnlyAir() || !section.maybeHas(s -> s.is(barrier) || s.is(kill) || s.is(trigger))) continue;

                    int baseY = SectionPos.sectionToBlockCoord(sy);
                    for (int x = 0; x < 16; x++)
                        for (int y = 0; y < 16; y++)
                            for (int z = 0; z < 16; z++) {
                                var state = section.getBlockState(x, y, z);
                                if (state.is(barrier)) {
                                    foundBarriers.add(new BlockPos(cx * 16 + x, baseY + y, cz * 16 + z));
                                } else if (state.is(kill)) {
                                    foundKill.add(new BlockPos(cx * 16 + x, baseY + y, cz * 16 + z));
                                } else if (state.is(trigger)) {
                                    foundTriggers.add(new BlockPos(cx * 16 + x, baseY + y, cz * 16 + z));
                                }
                            }
                }
            }
        }
        barriers = foundBarriers;
        killBlocks = foundKill;
        triggers = foundTriggers;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (barriers.isEmpty() && killBlocks.isEmpty() && triggers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        draw(ps, lines, barriers, 1.0F, 0.0F, 0.0F);   // red
        draw(ps, lines, killBlocks, 1.0F, 0.9F, 0.0F); // yellow
        draw(ps, lines, triggers, 0.0F, 0.9F, 1.0F);   // cyan
        ps.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void draw(PoseStack ps, VertexConsumer lines, List<BlockPos> list, float r, float g, float b) {
        for (BlockPos p : list) {
            LevelRenderer.renderLineBox(ps, lines,
                    p.getX(), p.getY(), p.getZ(),
                    p.getX() + 1, p.getY() + 1, p.getZ() + 1,
                    r, g, b, 1.0F);

            // Small 3D cross at the block's center, so gaps in a wall of blocks stand out.
            float mx = p.getX() + 0.5F, my = p.getY() + 0.5F, mz = p.getZ() + 0.5F;
            line(ps, lines, mx - MARKER_HALF_SIZE, my, mz, mx + MARKER_HALF_SIZE, my, mz, 1, 0, 0, r, g, b);
            line(ps, lines, mx, my - MARKER_HALF_SIZE, mz, mx, my + MARKER_HALF_SIZE, mz, 0, 1, 0, r, g, b);
            line(ps, lines, mx, my, mz - MARKER_HALF_SIZE, mx, my, mz + MARKER_HALF_SIZE, 0, 0, 1, r, g, b);
        }
    }

    /** One line segment in the lines render type. (nx, ny, nz) is the segment direction. */
    private static void line(PoseStack ps, VertexConsumer c,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float nx, float ny, float nz, float r, float g, float b) {
        PoseStack.Pose pose = ps.last();
        c.vertex(pose.pose(), x1, y1, z1).color(r, g, b, 1.0F).normal(pose.normal(), nx, ny, nz).endVertex();
        c.vertex(pose.pose(), x2, y2, z2).color(r, g, b, 1.0F).normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
