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

package me.desht.pneumaticcraft.common.thirdparty.patchouli;

import me.desht.pneumaticcraft.api.crafting.recipe.AmadronRecipe;
import me.desht.pneumaticcraft.common.recipes.PneumaticCraftRecipeType;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

@SuppressWarnings("unused")
public class ProcessorAmadronTrade implements IComponentProcessor {
    private AmadronRecipe recipe = null;
    private String text = null;

    @Override
    public void setup(IVariableProvider iVariableProvider) {
        ResourceLocation recipeId = new ResourceLocation(iVariableProvider.get("recipe").asString());
        recipe = PneumaticCraftRecipeType.amadronOffers.getRecipe(Minecraft.getInstance().level, recipeId);
        if (recipe == null) {
            Log.warning("Missing amadron offer recipe: " + recipeId);
        }

        text = iVariableProvider.has("text") ? iVariableProvider.get("text").asString() : null;
    }

    @Override
    public IVariable process(String key) {
        if (recipe == null) return null;

        return switch (key) {
            case "input" -> IVariable.from(recipe.getInput().apply(itemStack -> itemStack, fluidStack -> fluidStack));
            case "output" -> IVariable.from(recipe.getOutput().apply(itemStack -> itemStack, fluidStack -> fluidStack));
            case "name" -> IVariable.wrap(recipe.getOutput().getName());
            case "text" -> IVariable.wrap(text == null ? "" : I18n.get(text));
            default -> null;
        };

    }
}
