package com.kodyris.bluesteel.network;

import com.kodyris.bluesteel.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: open the trigger config screen with the block's current settings. */
public class OpenTriggerScreenPacket {
    private final BlockPos pos;
    private final String command;
    private final int cooldown;
    private final boolean playersOnly;

    public OpenTriggerScreenPacket(BlockPos pos, String command, int cooldown, boolean playersOnly) {
        this.pos = pos;
        this.command = command;
        this.cooldown = cooldown;
        this.playersOnly = playersOnly;
    }

    public static void encode(OpenTriggerScreenPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeUtf(p.command, 32767);
        buf.writeVarInt(p.cooldown);
        buf.writeBoolean(p.playersOnly);
    }

    public static OpenTriggerScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenTriggerScreenPacket(buf.readBlockPos(), buf.readUtf(32767), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(OpenTriggerScreenPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHooks.openTriggerScreen(p.pos, p.command, p.cooldown, p.playersOnly));
    }
}
