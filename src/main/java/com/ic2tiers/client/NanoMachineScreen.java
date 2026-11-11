package com.ic2tiers.client;

import com.ic2tiers.IC2Tiers;
import com.ic2tiers.nano.NanoMachineBlockEntity;
import com.ic2tiers.nano.NanoMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NanoMachineScreen extends AbstractContainerScreen<NanoMachineMenu> {
    private static final ResourceLocation TEXTURE =
     ResourceLocation.fromNamespaceAndPath(IC2Tiers.MOD_ID, "textures/gui/nano_machine.png");

    private static final int ARROW_HEIGHT = 14;
    private static final int ENERGY_WIDTH = 190;

    // Energy bar hit area (texture frame) and inner fill, relative to the GUI.
    private static final int ENERGY_HIT_X = 24;
    private static final int ENERGY_HIT_Y = 100;
    private static final int ENERGY_HIT_W = 192;
    private static final int ENERGY_HIT_H = 7;

    public NanoMachineScreen(
     NanoMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 240;
        this.imageHeight = 230;
    }

    @Override
    protected void renderBg(
     GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
         TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Horizontal energy bar: inner well x 25..214, y 101..105 (5 px high).
 int energy = this.menu.getEnergyScaled(ENERGY_WIDTH);
 if (energy > 0) {
     int x1 = this.leftPos + 25;
     int y1 = this.topPos + 101;
     int y2 = this.topPos + 106;
     guiGraphics.fill(x1, y1, x1 + energy, y2, 0xFFEB352E);
     guiGraphics.fill(x1, y1, x1 + energy, y1 + 1, 0xFFFF8D4F);
        }

        // Progress arrows under each input lane.
        for (int lane = 0; lane < NanoMachineBlockEntity.INPUT_SLOTS; lane++) {
     int progress = this.menu.getProgressScaled(lane, ARROW_HEIGHT);
     if (progress > 0) {
  guiGraphics.blit(
   TEXTURE,
   this.leftPos + 86 + lane * 26,
   this.topPos + 51,
   240,
   0,
   16,
          progress);
     }
        }
    }

    @Override
    public void renderBackground(
     GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(
     GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String title = this.title.getString();
 int titleX = (this.imageWidth - this.font.width(title)) / 2;
 guiGraphics.drawString(this.font, title, titleX, 4, 0xFFB7E0EA, false);
    }

    @Override
    public void render(
     GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.hoveredSlot == null) {
     Component tip = hoverTooltip(mouseX, mouseY);
     if (tip != null) {
         guiGraphics.renderTooltip(this.font, tip, mouseX, mouseY);
     } else {
         this.renderTooltip(guiGraphics, mouseX, mouseY);
     }
        }
    }

    private Component hoverTooltip(int mouseX, int mouseY) {
 if (this.isHovering(ENERGY_HIT_X, ENERGY_HIT_Y, ENERGY_HIT_W, ENERGY_HIT_H, mouseX, mouseY)) {
     return Component.translatable(
      "gui.ic2_tiers.energy",
      this.menu.getEnergy(),
      this.menu.getCapacity());
        }
        for (int lane = 0; lane < NanoMachineBlockEntity.INPUT_SLOTS; lane++) {
     if (this.isHovering(86 + lane * 26, 51, 16, 16, mouseX, mouseY)) {
         return Component.translatable(
          "gui.ic2_tiers.progress", this.menu.getProgressPercent(lane));
     }
        }
        return null;
    }
}
