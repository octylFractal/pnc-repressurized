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

package me.desht.pneumaticcraft.common.thirdparty.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import me.desht.pneumaticcraft.api.crafting.recipe.FluidMixerRecipe;
import me.desht.pneumaticcraft.client.render.pressure_gauge.PressureGaugeRenderer2D;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class JEIFluidMixerCategory extends AbstractPNCCategory<FluidMixerRecipe> {
    private final ITickTimer tickTimer;
    private final IDrawableAnimated progressBar;

    public JEIFluidMixerCategory() {
        super(RecipeTypes.FLUID_MIXER,
                xlate(ModBlocks.FLUID_MIXER.get().getDescriptionId()),
                guiHelper().createDrawable(Textures.GUI_JEI_FLUID_MIXER, 0, 0, 166, 70),
                guiHelper().createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.FLUID_MIXER.get()))
        );
        tickTimer = guiHelper().createTickTimer(60, 60, false);
        IDrawableStatic d = guiHelper().createDrawable(Textures.GUI_FLUID_MIXER, 180, 0, 44, 30);
        progressBar = guiHelper().createAnimatedDrawable(d, 60, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FluidMixerRecipe recipe, IFocusGroup focuses) {
        FluidStack in1 = recipe.getInput1().getFluidStacks().get(0);
        FluidStack in2 = recipe.getInput2().getFluidStacks().get(0);
        FluidStack outF = recipe.getOutputFluid();
        int[] amounts = new int[] { in1.getAmount(), in2.getAmount(), outF.getAmount() };
        int max = Arrays.stream(amounts).max().getAsInt();

        int inH1 = Math.min(64, in1.getAmount() * 64 / max);
        int inH2 = Math.min(64, in2.getAmount() * 64 / max);
        int outH = Math.min(64, outF.getAmount() * 64 / max);

        builder.addSlot(RecipeIngredientRole.INPUT, 5, 3 + (64 - inH1))
                .addIngredients(ForgeTypes.FLUID_STACK, recipe.getInput1().getFluidStacks())
                .setFluidRenderer(in1.getAmount(), false, 16, inH1)
                .setOverlay(Helpers.makeTankOverlay(inH1), 0, 0);
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 3 + (64 - inH2))
                .addIngredients(ForgeTypes.FLUID_STACK, recipe.getInput2().getFluidStacks())
                .setFluidRenderer(in2.getAmount(), false, 16, inH2)
                .setOverlay(Helpers.makeTankOverlay(inH2), 0, 0);

        if (!recipe.getOutputFluid().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 3 + (64 - outH))
                    .addIngredients(ForgeTypes.FLUID_STACK, Collections.singletonList(outF))
                    .setFluidRenderer(outF.getAmount(), false, 16, outH)
                    .setOverlay(Helpers.makeTankOverlay(outH), 0, 0);
        }
        if (!recipe.getOutputItem().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 64, 51)
                    .addItemStack(recipe.getOutputItem());
        }
    }

    @Override
    public void draw(FluidMixerRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack matrixStack, double mouseX, double mouseY) {
        float pressure = recipe.getRequiredPressure() * ((float) tickTimer.getValue() / tickTimer.getMaxValue());
        PressureGaugeRenderer2D.drawPressureGauge(matrixStack, Minecraft.getInstance().font, -1, PneumaticValues.MAX_PRESSURE_TIER_ONE, PneumaticValues.DANGER_PRESSURE_TIER_ONE, recipe.getRequiredPressure(), pressure, 138, 35);

        progressBar.draw(matrixStack, 45, 20);
    }

    @Override
    public List<Component> getTooltipStrings(FluidMixerRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        List<Component> res = new ArrayList<>();
        if (recipe.getRequiredPressure() > 0 && mouseX >= 117 && mouseY >= 15 && mouseX <= 157 && mouseY <= 55) {
            res.add(xlate("pneumaticcraft.gui.tooltip.pressure", recipe.getRequiredPressure()));
        } else if (mouseX >= 45 && mouseY >= 20 && mouseX <= 89 && mouseY <= 50) {
            res.add(new TextComponent((recipe.getProcessingTime()) / 20f + "s"));
            res.add(xlate("pneumaticcraft.gui.jei.tooltip.processingTime").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        return res;
    }
}
