package com.kodyris.bluesteel.network;

import com.kodyris.bluesteel.TlsAdditionalBlocksMod;
import com.kodyris.bluesteel.block.TriggerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: save new trigger settings. Fully re-validated on the server. */
public class SaveTriggerPacket {
    private final BlockPos pos;
    private final String command;
    private final int cooldown;
    private final boolean playersOnly;

    public SaveTriggerPacket(BlockPos pos, String command, int cooldown, boolean playersOnly) {
        this.pos = pos;
        this.command = command;
        this.cooldown = cooldown;
        this.playersOnly = playersOnly;
    }

    public static void encode(SaveTriggerPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeUtf(p.command, 32767);
        buf.writeVarInt(p.cooldown);
        buf.writeBoolean(p.playersOnly);
    }

    public static SaveTriggerPacket decode(FriendlyByteBuf buf) {
        return new SaveTriggerPacket(buf.readBlockPos(), buf.readUtf(32767), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(SaveTriggerPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        // Same requirements as opening the screen: creative-style game master with the stick in hand.
        if (!player.canUseGameMasterBlocks()
                || !player.getMainHandItem().is(TlsAdditionalBlocksMod.CONFIG_STICK.get())) return;
        if (!player.level().isLoaded(p.pos) || player.distanceToSqr(p.pos.getCenter()) > 100.0) return;
        if (player.level().getBlockEntity(p.pos) instanceof TriggerBlockEntity be) {
            be.configure(p.command, p.cooldown, p.playersOnly);
        }
    }
}
