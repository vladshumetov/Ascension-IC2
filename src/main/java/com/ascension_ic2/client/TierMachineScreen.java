package com.ascension_ic2.client;

import com.ascension_ic2.AscensionIC2;
import com.ascension_ic2.machine.MachineTier;
import com.ascension_ic2.machine.TierMachineKind;
import com.ascension_ic2.machine.TierMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TierMachineScreen extends AbstractContainerScreen<TierMachineMenu> {
    // Progress arrow box between the slot rows: arrows sit under each input lane.
    private static final int ARROW_TOP = 48;
    private static final int ARROW_BOX_HEIGHT = 23;
    private static final int ARROW_WIDTH = 16;
    private static final int ARROW_SPRITE_WIDTH = 32;
    private static final int ENERGY_WIDTH = 190;

    // Energy bar hit area (texture frame) and inner fill, relative to the GUI.
    private static final int ENERGY_HIT_X = 24;
    private static final int ENERGY_HIT_Y = 100;
    private static final int ENERGY_HIT_W = 192;
    private static final int ENERGY_HIT_H = 7;

    public TierMachineScreen(
            TierMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 240;
        this.imageHeight = 230;
    }

    @Override
    protected void renderBg(
            GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        MachineTier tier = this.menu.getTier();
        guiGraphics.blit(
                tier.guiTexture(), this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Horizontal energy bar: inner well x 25..214, y 101..105 (5 px high).
        int energy = this.menu.getEnergyScaled(ENERGY_WIDTH);
        if (energy > 0) {
            int x1 = this.leftPos + 25;
            int y1 = this.topPos + 101;
            int y2 = this.topPos + 106;
            guiGraphics.fill(x1, y1, x1 + energy, y2, 0xFFEB352E);
            guiGraphics.fill(x1, y1, x1 + energy, y1 + 1, 0xFFFF8D4F);
        }

        // Progress arrows under each input lane. The arrow shape depends on the machine type,
        // not on the tier (textures/gui/progress/<machine>.png, left half track, right lit).
        TierMachineKind kind = this.menu.getKind();
        if (kind == null) {
            return;
        }
        int arrowH = kind.progressHeight();
        ResourceLocation arrow =
                ResourceLocation.fromNamespaceAndPath(
                        AscensionIC2.MOD_ID,
                        "textures/gui/progress/" + kind.progressSprite() + ".png");
        int arrowY = this.topPos + ARROW_TOP + (ARROW_BOX_HEIGHT - arrowH) / 2;
        for (int lane = 0; lane < this.menu.getInputSlots(); lane++) {
            int x = this.leftPos + tier.slotX(lane);
            guiGraphics.blit(arrow, x, arrowY, 0, 0, ARROW_WIDTH, arrowH, ARROW_SPRITE_WIDTH, arrowH);
            int lit = this.menu.getProgressScaled(lane, arrowH);
            if (lit > 0) {
                guiGraphics.blit(
                        arrow,
                        x,
                        arrowY,
                        ARROW_WIDTH,
                        0,
                        ARROW_WIDTH,
                        lit,
                        ARROW_SPRITE_WIDTH,
                        arrowH);
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
                    "gui.ascension_ic2.energy", this.menu.getEnergy(), this.menu.getCapacity());
        }
        MachineTier tier = this.menu.getTier();
        for (int lane = 0; lane < this.menu.getInputSlots(); lane++) {
            if (this.isHovering(tier.slotX(lane), 51, 16, 16, mouseX, mouseY)) {
                return Component.translatable(
                        "gui.ascension_ic2.progress", this.menu.getProgressPercent(lane));
            }
        }
        return null;
    }
}
