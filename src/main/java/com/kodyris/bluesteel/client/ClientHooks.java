package com.kodyris.bluesteel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client-only entry points, kept out of common classes so a dedicated server never loads them. */
public class ClientHooks {
    public static void openTriggerScreen(BlockPos pos, String command, int cooldown, boolean playersOnly) {
        Minecraft.getInstance().setScreen(new TriggerConfigScreen(pos, command, cooldown, playersOnly));
    }
}
