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

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import me.desht.pneumaticcraft.api.crafting.recipe.HeatPropertiesRecipe;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.common.heat.BlockHeatProperties;
import me.desht.pneumaticcraft.common.thirdparty.ModNameCache;
import me.desht.pneumaticcraft.lib.Textures;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.TooltipFlag.Default;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraftforge.fluids.FluidStack;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class JEIBlockHeatPropertiesCategory extends AbstractPNCCategory<HeatPropertiesRecipe> {
    private final IDrawable hotArea;
    private final IDrawable coldArea;
    private final IDrawable air;

    private static final Rect2i INPUT_AREA = new Rect2i(65, 44, 18, 18);
    private static final Rect2i COLD_AREA = new Rect2i(5, 44, 18, 18);
    private static final Rect2i HOT_AREA = new Rect2i(125, 44, 18, 18);
    private static final Rect2i[] OUTPUT_AREAS = new Rect2i[] { COLD_AREA, HOT_AREA };

    public JEIBlockHeatPropertiesCategory() {
        super(ModCategoryUid.HEAT_PROPERTIES, HeatPropertiesRecipe.class,
                xlate("pneumaticcraft.gui.jei.title.heatProperties"),
                guiHelper().createDrawable(Textures.GUI_JEI_HEAT_PROPERTIES, 0, 0, 146, 73),
                guiHelper()
                        .drawableBuilder(Textures.JEI_THERMOMETER, 0, 0, 16, 16)
                        .setTextureSize(16, 16)
                        .build()
        );
        this.hotArea = guiHelper().createDrawable(Textures.GUI_JEI_HEAT_PROPERTIES, 150, 0, 31, 18);
        this.coldArea = guiHelper().createDrawable(Textures.GUI_JEI_HEAT_PROPERTIES, 150, 18, 31, 18);
        this.air = guiHelper().createDrawable(Textures.GUI_JEI_HEAT_PROPERTIES, 150, 36, 16, 16);
    }

    public static Collection<HeatPropertiesRecipe> getAllRecipes() {
        // FIXME filtering out recipes whose input block has no item (e.g. minecraft:fire) is a kludge:
        //  it suppresses JEI errors when loading recipes, but such recipes still aren't shown in JEI
        //  (on the other hand the blocks in the recipe don't appear in JEI's display anyway so ¯\_(ツ)_/¯)
        //noinspection UnstableApiUsage
        return BlockHeatProperties.getInstance().getAllEntries(Minecraft.getInstance().level).stream()
                .filter(r -> r.getBlock() instanceof LiquidBlock || !new ItemStack(r.getBlock()).isEmpty())
                .sorted(Comparator.comparingInt(HeatPropertiesRecipe::getTemperature)
                        .thenComparing(o -> o.getInputDisplayName().getString()))
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public void setIngredients(HeatPropertiesRecipe recipe, IIngredients ingredients) {
        setInputIngredient(recipe.getBlock(), ingredients);

        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();

        collectOutputs(recipe.getTransformCold(), items, fluids);
        collectOutputs(recipe.getTransformHot(), items, fluids);

        ingredients.setOutputLists(VanillaTypes.ITEM, items.stream().map(Collections::singletonList).collect(Collectors.toList()));
        ingredients.setOutputLists(VanillaTypes.FLUID, fluids.stream().map(Collections::singletonList).collect(Collectors.toList()));
    }

    private void collectOutputs(BlockState state, List<ItemStack> items, List<FluidStack> fluids) {
        if (state != null) {
            if (state.getBlock() instanceof LiquidBlock) {
                int level = state.hasProperty(LiquidBlock.LEVEL) ? state.getValue(LiquidBlock.LEVEL) : 15;
                if (level == 0) level = 15;
                FluidStack stack = new FluidStack(((LiquidBlock) state.getBlock()).getFluid(), 1000 * level / 15);
                fluids.add(stack);
                items.add(new ItemStack(Blocks.BARRIER));
            } else {
                ItemStack stack = new ItemStack(state.getBlock());
                items.add(stack.isEmpty() ? new ItemStack(Blocks.BARRIER) : stack);
                fluids.add(FluidStack.EMPTY);
            }
        } else {
            items.add(new ItemStack(Blocks.BARRIER));
            fluids.add(FluidStack.EMPTY);
        }
    }

    private void setInputIngredient(Block block, IIngredients ingredients) {
        if (block instanceof LiquidBlock) {
            FluidStack stack = new FluidStack(((LiquidBlock) block).getFluid(), 1000);
            ingredients.setInput(VanillaTypes.FLUID, stack);
        } else {
            ingredients.setInput(VanillaTypes.ITEM, new ItemStack(block));
        }
    }

    @Override
    public void setRecipe(IRecipeLayout layout, HeatPropertiesRecipe recipe, IIngredients ingredients) {
        List<List<FluidStack>> in = ingredients.getInputs(VanillaTypes.FLUID);
        if (!in.isEmpty()) {
            layout.getFluidStacks().init(0, true, INPUT_AREA.getX() + 2, INPUT_AREA.getY() - 1);
            layout.getFluidStacks().set(0, in.get(0));
        }

        List<List<FluidStack>> out = ingredients.getOutputs(VanillaTypes.FLUID);
        for (int idx = 0; idx < out.size(); idx++) {
            if (!out.get(idx).isEmpty() && !out.get(idx).get(0).isEmpty()) {
                layout.getFluidStacks().init(idx, false, OUTPUT_AREAS[idx].getX() + 2, OUTPUT_AREAS[idx].getY() - 1);
                layout.getFluidStacks().set(idx, out.get(idx).get(0));
            }
        }
    }

    @Override
    public void draw(HeatPropertiesRecipe recipe, PoseStack matrixStack, double mouseX, double mouseY) {
        Font fontRenderer = Minecraft.getInstance().font;

        int h = fontRenderer.lineHeight;

        Component desc = recipe.getDescriptionKey().isEmpty() ?
                TextComponent.EMPTY :
                new TextComponent(" (" + I18n.get(recipe.getDescriptionKey()) + ")");
        fontRenderer.draw(matrixStack, recipe.getInputDisplayName().copy().append(desc), 0, 0, 0x4040a0);

        Component temp = xlate("pneumaticcraft.waila.temperature").append(new TextComponent((recipe.getTemperature() - 273) + "°C"));
        fontRenderer.draw(matrixStack, temp, 0, h * 2, 0x404040);

        String res = NumberFormat.getNumberInstance(Locale.getDefault()).format(recipe.getThermalResistance());
        fontRenderer.draw(matrixStack, I18n.get("pneumaticcraft.gui.jei.thermalResistance") + res, 0, h * 3, 0x404040);

        boolean showCapacity = false;
        if (recipe.getTransformCold() != null) {
            coldArea.draw(matrixStack, INPUT_AREA.getX() - coldArea.getWidth() - 5, 42);
            showCapacity = true;
        }
        if (recipe.getTransformHot() != null) {
            hotArea.draw(matrixStack, HOT_AREA.getX() - hotArea.getWidth() - 5, 42);
            showCapacity = true;
        }

        renderBlock(recipe.getBlockState(), matrixStack, INPUT_AREA.getX() + 9, INPUT_AREA.getY() + 1);
        renderBlock(recipe.getTransformCold(), matrixStack, COLD_AREA.getX() + 9, COLD_AREA.getY() + 1);
        renderBlock(recipe.getTransformHot(), matrixStack, HOT_AREA.getX() + 9, HOT_AREA.getY() + 1);

        if (showCapacity) {
            fontRenderer.draw(matrixStack, xlate("pneumaticcraft.gui.jei.heatCapacity",
                    NumberFormat.getNumberInstance(Locale.getDefault()).format(recipe.getHeatCapacity())),
                    0, getBackground().getHeight() - h, 0x404040
            );
        }
    }

    @Override
    public boolean handleClick(HeatPropertiesRecipe recipe, double mouseX, double mouseY, int mouseButton) {
        IFocus<?> focus = null;
        if (INPUT_AREA.contains((int)mouseX, (int)mouseY)) {
            focus = makeFocus(recipe.getBlock(), mouseButton == 0 ? IFocus.Mode.OUTPUT : IFocus.Mode.INPUT);
        } else if (recipe.getTransformCold() != null && COLD_AREA.contains((int)mouseX, (int)mouseY)) {
            focus = makeFocus(recipe.getTransformCold().getBlock(), mouseButton == 0 ? IFocus.Mode.OUTPUT : IFocus.Mode.INPUT);
        } else if (recipe.getTransformHot() != null && HOT_AREA.contains((int)mouseX, (int)mouseY)) {
            focus = makeFocus(recipe.getTransformHot().getBlock(), mouseButton == 0 ? IFocus.Mode.OUTPUT : IFocus.Mode.INPUT);
        }
        if (focus != null) {
            JEIPlugin.recipesGui.show(focus);
            return true;
        }
        return false;
    }

    @Override
    public List<Component> getTooltipStrings(HeatPropertiesRecipe recipe, double mouseX, double mouseY) {
        List<Component> l = new ArrayList<>();
        if (INPUT_AREA.contains((int)mouseX, (int)mouseY)) {
            addTooltip(recipe.getBlock(), l);
        } else if (recipe.getTransformCold() != null && COLD_AREA.contains((int)mouseX, (int)mouseY)) {
            addTooltip(recipe.getTransformCold().getBlock(), l);
        } else if (recipe.getTransformHot() != null && HOT_AREA.contains((int)mouseX, (int)mouseY)) {
            addTooltip(recipe.getTransformHot().getBlock(), l);
        } else if (mouseY > 20 && mouseY < 30) {
            l.add(xlate("pneumaticcraft.gui.jei.tooltip.thermalResistance"));
        } else if (recipe.getHeatCapacity() != 0 && mouseY > 62) {
            l.add(xlate("pneumaticcraft.gui.jei.tooltip.heatCapacity"));
        }
        return l;
    }

    private IFocus<?> makeFocus(Block block, IFocus.Mode mode) {
        return block == Blocks.AIR || block instanceof LiquidBlock ?
                null :
                JEIPlugin.recipeManager.createFocus(mode, new ItemStack(block));
    }

    private void addTooltip(Block block, List<Component> list) {
        ItemStack stack = new ItemStack(block);
        list.add(stack.getHoverName());
        stack.getItem().appendHoverText(stack, ClientUtils.getClientLevel(), list, ClientUtils.hasShiftDown() ? Default.ADVANCED : Default.NORMAL);
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            list.add(new TextComponent(stack.getItem().getRegistryName().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        list.add(new TextComponent(ModNameCache.getModName(stack.getItem())).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
    }

    private void renderBlock(BlockState state, PoseStack matrixStack, int x, int y) {
        // note: fluid rendering is done by JEI (fluidstacks are registered in the recipe layout)
        if (state != null) {
            if (state.getBlock() == Blocks.AIR) {
                air.draw(matrixStack, x - 8, y - 2);
            } else {
                float rot = Minecraft.getInstance().level.getGameTime() % 360;
                GuiUtils.renderBlockInGui(matrixStack, state, x, y, 100, rot, 15f);
            }
        }
    }
}
