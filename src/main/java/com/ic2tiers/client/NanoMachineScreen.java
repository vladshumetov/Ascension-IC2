package com.ic2tiers.client;

import com.ic2tiers.IC2Tiers;
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
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
                TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.getProgressScaled(24);
        if (progress > 0) {
            guiGraphics.blit(
                    TEXTURE, this.leftPos + 70, this.topPos + 38, 176, 0, progress, 16);
        }
    }

    @Override
    public void render(
            GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
