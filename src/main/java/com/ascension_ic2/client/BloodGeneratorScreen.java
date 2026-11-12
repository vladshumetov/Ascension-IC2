package com.ascension_ic2.client;

import com.ascension_ic2.AscensionIC2;
import com.ascension_ic2.energy.BloodGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BloodGeneratorScreen extends AbstractContainerScreen<BloodGeneratorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AscensionIC2.MOD_ID, "textures/gui/blood_generator.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int BAR_X = 25;
    private static final int BLOOD_BAR_Y = 96;
    private static final int ENERGY_BAR_Y = 103;
    private static final int BAR_WIDTH = 190;
    private static final int BAR_HEIGHT = 5;

    private static final int COLOR_LABEL = 0xFFC9A3A3;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_OK = 0xFFD25A5A;
    private static final int COLOR_IDLE = 0xFF8B6B6B;

    public BloodGeneratorScreen(
            BloodGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 240;
        this.imageHeight = 230;
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

        int blood = this.menu.getBloodScaled(BAR_WIDTH);
        if (blood > 0) {
            int x1 = this.leftPos + BAR_X;
            int y1 = this.topPos + BLOOD_BAR_Y;
            guiGraphics.fill(x1, y1, x1 + blood, y1 + BAR_HEIGHT, 0xFF8E1B1B);
            guiGraphics.fill(x1, y1, x1 + blood, y1 + 1, 0xFFE05A5A);
        }

        int energy = this.menu.getEnergyScaled(BAR_WIDTH);
        if (energy > 0) {
            int x1 = this.leftPos + BAR_X;
            int y1 = this.topPos + ENERGY_BAR_Y;
            guiGraphics.fill(x1, y1, x1 + energy, y1 + BAR_HEIGHT, 0xFFEB352E);
            guiGraphics.fill(x1, y1, x1 + energy, y1 + 1, 0xFFFF8D4F);
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
                4,
                0xFFE0B7B7,
                false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.isHovering(BAR_X, BLOOD_BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable(
                            "gui.ascension_ic2.blood.blood",
                            GuiNumbers.format(this.menu.getBlood()),
                            GuiNumbers.format(this.menu.getBloodCapacity())),
                    mouseX,
                    mouseY);
            return;
        }
        if (this.isHovering(BAR_X, ENERGY_BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
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
