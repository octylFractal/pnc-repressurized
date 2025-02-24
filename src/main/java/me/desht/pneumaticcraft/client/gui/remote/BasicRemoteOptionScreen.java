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

package me.desht.pneumaticcraft.client.gui.remote;

import com.mojang.blaze3d.vertex.PoseStack;
import me.desht.pneumaticcraft.client.gui.AbstractPneumaticCraftScreen;
import me.desht.pneumaticcraft.client.gui.RemoteEditorScreen;
import me.desht.pneumaticcraft.client.gui.remote.actionwidget.ActionWidget;
import me.desht.pneumaticcraft.client.gui.remote.actionwidget.IActionWidgetLabeled;
import me.desht.pneumaticcraft.client.gui.widget.*;
import me.desht.pneumaticcraft.common.variables.GlobalVariableHelper;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class BasicRemoteOptionScreen<A extends ActionWidget<?>> extends AbstractPneumaticCraftScreen {
    private static final String TOOLTIP_DELIMITER = "//";

    protected final A actionWidget;
    final RemoteEditorScreen guiRemote;
    private WidgetTextField labelField, tooltipField;
    private WidgetComboBox enableField;
    private WidgetTextFieldNumber xValueField, yValueField, zValueField;
    private WidgetButtonExtended enableVarTypeButton;
    private boolean playerGlobalEnableVar;

    public BasicRemoteOptionScreen(A actionWidget, RemoteEditorScreen guiRemote) {
        super(new TranslatableComponent("pneumaticcraft.gui.remote.tray." + actionWidget.getId() + ".name"));

        this.actionWidget = actionWidget;
        this.guiRemote = guiRemote;
        xSize = 183;
        ySize = 202;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected ResourceLocation getTexture() {
        return Textures.GUI_WIDGET_OPTIONS;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        super.init();

        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        playerGlobalEnableVar = actionWidget.getEnableVariable().isEmpty() || actionWidget.getEnableVariable().startsWith("#");

        addLabel(xlate("pneumaticcraft.gui.remote.enable"), guiLeft + 10, guiTop + 150);
        addLabel(title, width / 2, guiTop + 5, WidgetLabel.Alignment.CENTRE);
        enableVarTypeButton = new WidgetButtonExtended(guiLeft + 10, guiTop + 158, 12, 14, GlobalVariableHelper.getVarPrefix(playerGlobalEnableVar),
                b -> togglePlayerGlobalEnable()).setTooltipKey("pneumaticcraft.gui.remote.varType.tooltip");
        addRenderableWidget(enableVarTypeButton);

        if (actionWidget instanceof IActionWidgetLabeled) {
            addLabel(xlate("pneumaticcraft.gui.remote.text"), guiLeft + 10, guiTop + 20);
            addLabel(xlate("pneumaticcraft.gui.remote.tooltip"), guiLeft + 10, guiTop + 46);
        }

        addLabel(xlate("pneumaticcraft.gui.remote.enableValue"), guiLeft + 10, guiTop + 175);
        addLabel(new TextComponent("X:"), guiLeft + 10, guiTop + 186);
        addLabel(new TextComponent("Y:"), guiLeft + 67, guiTop + 186);
        addLabel(new TextComponent("Z:"), guiLeft + 124, guiTop + 186);

        enableField = new WidgetComboBox(font, guiLeft + 23, guiTop + 160, 147, 10);
        enableField.setElements(GlobalVariableHelper.extractVarnames(guiRemote.getMenu().variables, playerGlobalEnableVar));
        enableField.setValue(GlobalVariableHelper.stripVarPrefix(actionWidget.getEnableVariable()));
        enableField.setTooltip(xlate("pneumaticcraft.gui.remote.enable.tooltip"));
        addRenderableWidget(enableField);

        Component valueTooltip = xlate("pneumaticcraft.gui.remote.enableValue.tooltip");

        xValueField = new WidgetTextFieldNumber(font, guiLeft + 20, guiTop + 185, 38, 10);
        xValueField.setValue(actionWidget.getEnablingValue().getX());
        xValueField.setTooltip(valueTooltip);
        addRenderableWidget(xValueField);

        yValueField = new WidgetTextFieldNumber(font, guiLeft + 78, guiTop + 185, 38, 10);
        yValueField.setValue(actionWidget.getEnablingValue().getY());
        yValueField.setTooltip(valueTooltip);
        addRenderableWidget(yValueField);

        zValueField = new WidgetTextFieldNumber(font, guiLeft + 136, guiTop + 185, 38, 10);
        zValueField.setValue(actionWidget.getEnablingValue().getZ());
        zValueField.setTooltip(valueTooltip);
        addRenderableWidget(zValueField);

        if (actionWidget instanceof IActionWidgetLabeled) {
            labelField = new WidgetTextField(font, guiLeft + 10, guiTop + 30, 160, 10);
            labelField.setValue(((IActionWidgetLabeled) actionWidget).getText().getString());
            labelField.setTooltip(xlate("pneumaticcraft.gui.remote.label.tooltip"));
            labelField.setMaxLength(1000);
            addRenderableWidget(labelField);

            tooltipField = new WidgetTextField(font, guiLeft + 10, guiTop + 56, 160, 10);

            String joined = ((IActionWidgetLabeled) actionWidget).getTooltip().stream()
                    .map(Component::getString)
                    .collect(Collectors.joining(TOOLTIP_DELIMITER));
            tooltipField.setValue(joined);
            addRenderableWidget(tooltipField);
        }
    }

    @Override
    public void removed() {
        minecraft.keyboardHandler.setSendRepeatsToGui(false);

        actionWidget.setEnableVariable(GlobalVariableHelper.getPrefixedVar(enableField.getValue(), playerGlobalEnableVar));
        actionWidget.setEnablingValue(xValueField.getIntValue(), yValueField.getIntValue(), zValueField.getIntValue());
        if (actionWidget instanceof IActionWidgetLabeled) {
            ((IActionWidgetLabeled) actionWidget).setText(new TextComponent(labelField.getValue()));
            if (tooltipField.getValue().isEmpty()) {
                ((IActionWidgetLabeled) actionWidget).setTooltip(Collections.emptyList());
            } else {
                List<Component> l = Arrays.stream(tooltipField.getValue().split(TOOLTIP_DELIMITER))
                        .map(TextComponent::new)
                        .collect(Collectors.toList());
                ((IActionWidgetLabeled) actionWidget).setTooltip(l);
            }
        }
    }
    private void togglePlayerGlobalEnable() {
        playerGlobalEnableVar = !playerGlobalEnableVar;
        enableVarTypeButton.setMessage(new TextComponent(GlobalVariableHelper.getVarPrefix(playerGlobalEnableVar)));
        enableField.setElements(GlobalVariableHelper.extractVarnames(guiRemote.getMenu().variables, playerGlobalEnableVar));
    }

    @Override
    public void onClose() {
        minecraft.setScreen(guiRemote);
    }
}
