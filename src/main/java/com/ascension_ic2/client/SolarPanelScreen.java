package com.ascension_ic2.client;

import com.ascension_ic2.AscensionIC2;
import com.ascension_ic2.energy.SolarPanelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SolarPanelScreen extends AbstractContainerScreen<SolarPanelMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AscensionIC2.MOD_ID, "textures/gui/solar_panel.png");
    private static final int BAR_X = 15;
    private static final int BAR_Y = 89;
    private static final int BAR_WIDTH = 146;
    private static final int BAR_HEIGHT = 5;

    private static final int COLOR_LABEL = 0xFFA9C1CF;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_OK = 0xFF7CD992;
    private static final int COLOR_IDLE = 0xFF8B8B8B;

    public SolarPanelScreen(
            SolarPanelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 120;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
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
                6,
                0xFFB7E0EA,
                false);

        label(guiGraphics, "gui.ascension_ic2.solar.generation", 16, 28,
                this.menu.getGeneration() + " EU/t");
        label(guiGraphics, "gui.ascension_ic2.solar.output", 16, 42,
                this.menu.getOutput() + " EU/t");
        label(guiGraphics, "gui.ascension_ic2.solar.buffer", 16, 56,
                this.menu.getStored() + " / " + this.menu.getCapacity() + " EU");
        label(guiGraphics, "gui.ascension_ic2.solar.tier", 16, 70,
                Integer.toString(this.menu.getTier()));

        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.ascension_ic2.solar.status"),
                16,
                82,
                COLOR_LABEL,
                false);
        Component status = Component.translatable(
                this.menu.isProducing()
                        ? "gui.ascension_ic2.solar.day"
                        : "gui.ascension_ic2.solar.night");
        guiGraphics.drawString(
                this.font,
                status,
                86,
                82,
                this.menu.isProducing() ? COLOR_OK : COLOR_IDLE,
                false);
    }

    private void label(
            GuiGraphics guiGraphics, String key, int x, int y, String value) {
        guiGraphics.drawString(
                this.font, Component.translatable(key), x, y, COLOR_LABEL, false);
        guiGraphics.drawString(
                this.font,
                value,
                this.imageWidth - 16 - this.font.width(value),
                y,
                COLOR_VALUE,
                false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
