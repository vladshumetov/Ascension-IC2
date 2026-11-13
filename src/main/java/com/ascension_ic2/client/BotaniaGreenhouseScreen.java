package com.ascension_ic2.client;

import com.ascension_ic2.AscensionIC2;
import com.ascension_ic2.energy.BotaniaGreenhouseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BotaniaGreenhouseScreen extends AbstractContainerScreen<BotaniaGreenhouseMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AscensionIC2.MOD_ID, "textures/gui/botania_greenhouse.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int BAR_X = 25;
    private static final int MANA_BAR_Y = 96;
    private static final int ENERGY_BAR_Y = 103;
    private static final int BAR_WIDTH = 190;
    private static final int BAR_HEIGHT = 5;

    public BotaniaGreenhouseScreen(
            BotaniaGreenhouseMenu menu, Inventory playerInventory, Component title) {
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

        int mana = this.menu.getManaScaled(BAR_WIDTH);
        if (mana > 0) {
            int x1 = this.leftPos + BAR_X;
            int y1 = this.topPos + MANA_BAR_Y;
            guiGraphics.fill(x1, y1, x1 + mana, y1 + BAR_HEIGHT, 0xFF3F6FD8);
            guiGraphics.fill(x1, y1, x1 + mana, y1 + 1, 0xFF9FD8FF);
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
                0xFFB7E0EA,
                false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.isHovering(BAR_X, MANA_BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable(
                            "gui.ascension_ic2.mana",
                            GuiNumbers.format(this.menu.getMana()),
                            GuiNumbers.format(this.menu.getManaCapacity())),
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
