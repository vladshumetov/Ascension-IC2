package com.ascension_ic2.client;

import com.ascension_ic2.AscensionIC2;
import com.ascension_ic2.energy.GasGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GasGeneratorScreen extends AbstractContainerScreen<GasGeneratorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AscensionIC2.MOD_ID, "textures/gui/solar_panel.png");
    private static final int TEXTURE_SIZE = 512;
    private static final int BAR_X = 22;
    private static final int BAR_Y = 133;
    private static final int BAR_WIDTH = 219;
    private static final int BAR_HEIGHT = 10;
    private static final int COLOR_LABEL = 0xFFA9C1CF;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_OK = 0xFF7CD992;
    private static final int COLOR_IDLE = 0xFF8B8B8B;

    public GasGeneratorScreen(
            GasGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 264;
        this.imageHeight = 180;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
                TEXTURE,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
        int fill = this.menu.getEnergyScaled(BAR_WIDTH);
        if (fill > 0) {
            int x1 = this.leftPos + BAR_X;
            int y1 = this.topPos + BAR_Y;
            guiGraphics.fill(x1, y1, x1 + fill, y1 + BAR_HEIGHT, 0xFFEB352E);
            guiGraphics.fill(x1, y1, x1 + fill, y1 + 1, 0xFFFF8D4F);
        }
    }

    @Override
    public void renderBackground(
            GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String title = this.title.getString();
        guiGraphics.drawString(
                this.font,
                title,
                (this.imageWidth - this.font.width(title)) / 2,
                9,
                0xFFB7E0EA,
                false);
        label(guiGraphics, "gui.ascension_ic2.gas.gas", 42,
                GuiNumbers.format(this.menu.getGas()) + " / " + GuiNumbers.format(this.menu.getGasCapacity()) + " mB");
        label(guiGraphics, "gui.ascension_ic2.gas.buffer", 63,
                GuiNumbers.format(this.menu.getEnergy()) + " / " + GuiNumbers.format(this.menu.getEnergyCapacity()) + " EU");
        label(guiGraphics, "gui.ascension_ic2.gas.output", 84, com.ascension_ic2.energy.GasGeneratorBlockEntity.EU_PER_TICK + " EU/t");
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.ascension_ic2.gas.status"),
                24,
                123,
                COLOR_LABEL,
                false);
        Component status = Component.translatable(
                this.menu.isGenerating()
                        ? "gui.ascension_ic2.gas.running"
                        : "gui.ascension_ic2.gas.idle");
        guiGraphics.drawString(
                this.font,
                status,
                108,
                123,
                this.menu.isGenerating() ? COLOR_OK : COLOR_IDLE,
                false);
    }

    private void label(GuiGraphics guiGraphics, String key, int y, String value) {
        guiGraphics.drawString(
                this.font, Component.translatable(key), 24, y, COLOR_LABEL, false);
        guiGraphics.drawString(
                this.font,
                value,
                this.imageWidth - 24 - this.font.width(value),
                y,
                COLOR_VALUE,
                false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable(
                            "gui.ascension_ic2.energy",
                            GuiNumbers.format(this.menu.getEnergy()),
                            GuiNumbers.format(this.menu.getEnergyCapacity())),
                    mouseX,
                    mouseY);
            return;
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
