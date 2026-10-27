package com.kodyris.bluesteel.block;

import com.kodyris.bluesteel.TlsAdditionalBlocksMod;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TriggerBlockEntity extends BlockEntity {
    public static final int MAX_COOLDOWN = 1_000_000;

    private String command = "";
    private int cooldownTicks = 20;
    private boolean playersOnly = true;
    private long lastTriggered = Long.MIN_VALUE / 2; // not saved; resets on load

    public TriggerBlockEntity(BlockPos pos, BlockState state) {
        super(TlsAdditionalBlocksMod.TRIGGER_BLOCK_ENTITY.get(), pos, state);
    }

    public String getCommand() { return command; }
    public int getCooldownTicks() { return cooldownTicks; }
    public boolean isPlayersOnly() { return playersOnly; }

    public void configure(String command, int cooldownTicks, boolean playersOnly) {
        this.command = command == null ? "" : command;
        this.cooldownTicks = Math.max(0, Math.min(MAX_COOLDOWN, cooldownTicks));
        this.playersOnly = playersOnly;
        this.setChanged();
    }

    /** Called (server side) every time an entity's bounding box overlaps this block. */
    public void onEntityInside(ServerLevel level, Entity entity) {
        if (command.isBlank()) return;
        if (playersOnly ? !(entity instanceof Player) : !(entity instanceof LivingEntity)) return;

        long now = level.getGameTime();
        if (now - lastTriggered < cooldownTicks) return;
        lastTriggered = now;

        MinecraftServer server = level.getServer();
        // Runs at op level 2 with the triggering entity as the executor (@s), positioned at the block's center.
        CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(worldPosition),
                Vec2.ZERO, level, 2, "Trigger", Component.literal("Trigger"), server, entity)
                .withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Command", command);
        tag.putInt("Cooldown", cooldownTicks);
        tag.putBoolean("PlayersOnly", playersOnly);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        command = tag.getString("Command");
        cooldownTicks = tag.contains("Cooldown") ? tag.getInt("Cooldown") : 20;
        playersOnly = !tag.contains("PlayersOnly") || tag.getBoolean("PlayersOnly");
    }
}
