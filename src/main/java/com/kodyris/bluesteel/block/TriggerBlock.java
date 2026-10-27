package com.kodyris.bluesteel.block;

import com.kodyris.bluesteel.TlsAdditionalBlocksMod;
import com.kodyris.bluesteel.network.ModNetwork;
import com.kodyris.bluesteel.network.OpenTriggerScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Invisible, non-solid block that runs a command when an entity enters it.
 * Like vanilla's light block it can't be targeted at all unless you hold an operator utility item
 * (see RedBarrierBlock#getShape); right-clicking it with the config stick opens the config screen.
 */
public class TriggerBlock extends RedBarrierBlock implements EntityBlock {
    public TriggerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TriggerBlockEntity(pos, state);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof TriggerBlockEntity be) {
            be.onEntityInside(serverLevel, entity);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!player.getMainHandItem().is(TlsAdditionalBlocksMod.CONFIG_STICK.get())
                || !player.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof TriggerBlockEntity be) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new OpenTriggerScreenPacket(pos, be.getCommand(), be.getCooldownTicks(), be.isPlayersOnly()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
