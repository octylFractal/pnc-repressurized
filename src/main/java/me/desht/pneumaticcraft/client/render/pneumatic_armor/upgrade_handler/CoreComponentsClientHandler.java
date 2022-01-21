/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.client.render.pneumatic_armor.upgrade_handler;

import com.google.common.base.Strings;
import com.mojang.blaze3d.vertex.PoseStack;
import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IArmorUpgradeClientHandler;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IGuiScreen;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IOptionPage;
import me.desht.pneumaticcraft.api.pneumatic_armor.ICommonArmorHandler;
import me.desht.pneumaticcraft.client.KeyHandler;
import me.desht.pneumaticcraft.client.gui.pneumatic_armor.GuiArmorMainScreen;
import me.desht.pneumaticcraft.client.gui.pneumatic_armor.option_screens.CoreComponentsOptions;
import me.desht.pneumaticcraft.client.gui.widget.WidgetAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetButtonExtended;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.common.config.ConfigHelper;
import me.desht.pneumaticcraft.common.config.subconfig.ArmorHUDLayout;
import me.desht.pneumaticcraft.common.item.ItemPneumaticArmor;
import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import me.desht.pneumaticcraft.common.pneumatic_armor.handlers.CoreComponentsHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CoreComponentsClientHandler extends IArmorUpgradeClientHandler.AbstractHandler<CoreComponentsHandler> {
    private static final int MAX_BARS = 40;
    private static final String[] BAR_STR_CACHE = new String[MAX_BARS + 1];
    private static final Component NO_ARMOR = new TextComponent("-").withStyle(ChatFormatting.DARK_GRAY);

    private final float[] lastPressure = new float[] { -1, -1, -1, -1 };
    private WidgetAnimatedStat powerStat;
    private final List<WidgetButtonExtended> pressureButtons = new ArrayList<>();
    public IGuiAnimatedStat testMessageStat;
    private boolean showPressureNumerically;  // false for numeric readout, true for horizontal bars
    private boolean forceUpdatePressureStat = true;

    public CoreComponentsClientHandler() {
        super(ArmorUpgradeRegistry.getInstance().coreComponentsHandler);
    }

    @Override
    public Optional<KeyMapping> getTriggerKeyBinding() {
        return Optional.of(KeyHandler.getInstance().keybindOpenOptions);
    }

    @Override
    public void onTriggered(ICommonArmorHandler armorHandler) {
        Minecraft mc = Minecraft.getInstance();
        if (ItemPneumaticArmor.isPlayerWearingAnyPneumaticArmor(mc.player)) {
            mc.setScreen(GuiArmorMainScreen.getInstance());
        }
    }

    @Override
    public void tickClient(ICommonArmorHandler armorHandler) {
        boolean needUpdate = forceUpdatePressureStat;
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = ArmorUpgradeRegistry.ARMOR_SLOTS[i];
            if (lastPressure[i] != armorHandler.getArmorPressure(slot)) {
                lastPressure[i] = armorHandler.getArmorPressure(slot);
                needUpdate = true;
            }
            ItemStack stack = armorHandler.getPlayer().getItemBySlot(slot);
            pressureButtons.get(i).setRenderStacks(stack.getItem() instanceof ItemPneumaticArmor ? stack : ItemStack.EMPTY);
        }
        if (needUpdate) {
            List<Component> l = Arrays.stream(ArmorUpgradeRegistry.ARMOR_SLOTS)
                    .map(slot -> getPressureStr(armorHandler, slot))
                    .collect(Collectors.toList());
            powerStat.setText(l);
            forceUpdatePressureStat = false;
        }
    }

    @Override
    public void initConfig() {
        showPressureNumerically = ConfigHelper.client().armor.showPressureNumerically.get();
    }

    @Override
    public void saveToConfig() {
        ConfigHelper.setShowPressureNumerically(showPressureNumerically);
    }

    public boolean shouldShowPressureNumerically() {
        return showPressureNumerically;
    }

    public void setShowPressureNumerically(boolean showPressureNumerically) {
        if (showPressureNumerically != this.showPressureNumerically) {
            reset();
            forceUpdatePressureStat = true;
        }
        this.showPressureNumerically = showPressureNumerically;
    }

    private Component getPressureStr(ICommonArmorHandler handler, EquipmentSlot slot) {
        if (!ItemPneumaticArmor.isPneumaticArmorPiece(handler.getPlayer(), slot))
            return NO_ARMOR;
        float pressure = handler.getArmorPressure(slot);
        if (showPressureNumerically) {
            return new TextComponent(String.format("%4.1f", Math.max(0f, pressure))).withStyle(getColourForPressure(pressure));
        } else {
            return new TextComponent(getBarStr(pressure));
        }
    }

    private ChatFormatting getColourForPressure(float pressure) {
        if (pressure <= 0.1F) {
            return ChatFormatting.GRAY;
        } else if (pressure < 0.5F) {
            return ChatFormatting.RED;
        } else if (pressure < 2.0F) {
            return ChatFormatting.GOLD;
        } else if (pressure < 4.0F) {
            return ChatFormatting.YELLOW;
        } else {
            return ChatFormatting.GREEN;
        }
    }

    private String getBarStr(float pressure) {
        int scaled = (int) (MAX_BARS * pressure / 10f);
        int idx = Mth.clamp(scaled, 0, MAX_BARS);
        if (BAR_STR_CACHE[idx] == null) {
            int n2 = MAX_BARS - scaled;
            BAR_STR_CACHE[idx] = getColourForPressure(pressure) + Strings.repeat("|", scaled)
                    + ChatFormatting.DARK_GRAY + Strings.repeat("|", n2);
        }
        return BAR_STR_CACHE[idx];
    }

    @Override
    public void render3D(PoseStack matrixStack, MultiBufferSource buffer, float partialTicks) {
    }

    @Override
    public void render2D(PoseStack matrixStack, float partialTicks, boolean armorPieceHasPressure) {
    }

    @Override
    public IGuiAnimatedStat getAnimatedStat() {
        if (powerStat == null) {
            forceUpdatePressureStat = true;
            powerStat = new WidgetAnimatedStat(null, TextComponent.EMPTY, WidgetAnimatedStat.StatIcon.NONE, HUDHandler.getInstance().getStatOverlayColor(), null, ArmorHUDLayout.INSTANCE.powerStat);
            powerStat.setLineSpacing(14);
            powerStat.setSubwidgetRenderOffsets(-18, 0);  // ensure armor icons are rendered in the right place
            pressureButtons.clear();
            for (EquipmentSlot slot : ArmorUpgradeRegistry.ARMOR_SLOTS) {
                WidgetButtonExtended pressureButton = new WidgetButtonExtended(0, 5 + (3 - slot.getIndex()) * 14, 18, 18, TextComponent.EMPTY) ;
                ItemStack stack = GuiArmorMainScreen.ARMOR_STACKS[slot.getIndex()];
                pressureButton.setVisible(false);
                pressureButton.setRenderStacks(stack);
                powerStat.addSubWidget(pressureButton);
                pressureButtons.add(pressureButton);
            }
            powerStat.setMinimumContractedDimensions(0, 0);
            powerStat.setAutoLineWrap(false);
            powerStat.openStat();
        }
        return powerStat;
    }

    @Override
    public void reset() {
        powerStat = null;
    }

    @Override
    public IOptionPage getGuiOptionsPage(IGuiScreen screen) {
        return new CoreComponentsOptions(screen,this);
    }

    @Override
    public void onResolutionChanged() {
        powerStat = null;
        forceUpdatePressureStat = true;
        Arrays.fill(lastPressure, -1);
    }
}
