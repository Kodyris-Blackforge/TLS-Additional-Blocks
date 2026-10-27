package com.kodyris.bluesteel.client;

import com.kodyris.bluesteel.block.TriggerBlockEntity;
import com.kodyris.bluesteel.network.ModNetwork;
import com.kodyris.bluesteel.network.SaveTriggerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class TriggerConfigScreen extends Screen {
    private final BlockPos pos;
    private final String initialCommand;
    private final int initialCooldown;
    private boolean playersOnly;

    private EditBox commandBox;
    private EditBox cooldownBox;
    private Button modeButton;

    public TriggerConfigScreen(BlockPos pos, String command, int cooldown, boolean playersOnly) {
        super(Component.literal("Trigger Block"));
        this.pos = pos;
        this.initialCommand = command;
        this.initialCooldown = cooldown;
        this.playersOnly = playersOnly;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        commandBox = new EditBox(this.font, cx - 150, 50, 300, 20, Component.literal("Command"));
        commandBox.setMaxLength(32767);
        commandBox.setValue(initialCommand);
        this.addRenderableWidget(commandBox);

        cooldownBox = new EditBox(this.font, cx - 150, 95, 100, 20, Component.literal("Cooldown"));
        cooldownBox.setFilter(s -> s.matches("\\d{0,7}"));
        cooldownBox.setValue(String.valueOf(initialCooldown));
        this.addRenderableWidget(cooldownBox);

        modeButton = this.addRenderableWidget(Button.builder(modeText(), b -> {
            playersOnly = !playersOnly;
            b.setMessage(modeText());
        }).bounds(cx - 150, 135, 300, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose())
                .bounds(cx - 154, 175, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 4, 175, 150, 20).build());

        this.setInitialFocus(commandBox);
    }

    private Component modeText() {
        return Component.literal("Triggered by: " + (playersOnly ? "Players only" : "Any living entity"));
    }

    private void saveAndClose() {
        int cooldown = 0;
        try {
            cooldown = Integer.parseInt(cooldownBox.getValue());
        } catch (NumberFormatException ignored) {
        }
        cooldown = Math.max(0, Math.min(TriggerBlockEntity.MAX_COOLDOWN, cooldown));
        ModNetwork.CHANNEL.sendToServer(new SaveTriggerPacket(pos, commandBox.getValue(), cooldown, playersOnly));
        onClose();
    }

    @Override
    public void tick() {
        commandBox.tick();
        cooldownBox.tick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, 20, 0xFFFFFF);
        g.drawString(this.font, "Command (runs as the entity, op level 2; @s = the entity)", cx - 150, 38, 0xA0A0A0);
        g.drawString(this.font, "Cooldown (ticks)", cx - 150, 83, 0xA0A0A0);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
