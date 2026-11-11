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

    public NanoMachineScreen(
            NanoMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 240;
 this.imageHeight = 230;
        this.inventoryLabelY = 123;
    }

    @Override
    protected void renderBg(
     GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
         TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        for (int lane = 0; lane < NanoMachineBlockEntity.INPUT_SLOTS; lane++) {
     int progress = this.menu.getProgressScaled(lane, 16);
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
 guiGraphics.drawString(
         this.font,
         Component.translatable("container.inventory").getString(),
  40,
  this.inventoryLabelY,
         0xFFB7E0EA,
         false);
    }

    @Override
    public void render(
     GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
 super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
