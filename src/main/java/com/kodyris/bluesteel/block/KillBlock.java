package com.kodyris.bluesteel.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Invisible barrier-style block that kills any living entity inside it.
 * Uses the generic-kill damage type, which bypasses invulnerability, so creative
 * players, operators and totems of undying are all ignored.
 * Must be registered with .noCollission() so entities can actually enter it.
 */
public class KillBlock extends RedBarrierBlock {
    public KillBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && living.isAlive()) {
            living.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
        }
    }
}
