package com.kodyris.bluesteel.network;

import com.kodyris.bluesteel.TlsAdditionalBlocksMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TlsAdditionalBlocksMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(OpenTriggerScreenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenTriggerScreenPacket::encode)
                .decoder(OpenTriggerScreenPacket::decode)
                .consumerMainThread(OpenTriggerScreenPacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveTriggerPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveTriggerPacket::encode)
                .decoder(SaveTriggerPacket::decode)
                .consumerMainThread(SaveTriggerPacket::handle)
                .add();
    }
}
