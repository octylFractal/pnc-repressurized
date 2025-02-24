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

package me.desht.pneumaticcraft.common.recipes.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.desht.pneumaticcraft.api.crafting.recipe.ExplosionCraftingRecipe;
import me.desht.pneumaticcraft.common.core.ModRecipes;
import me.desht.pneumaticcraft.common.recipes.PneumaticCraftRecipeType;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ExplosionCraftingRecipeImpl extends ExplosionCraftingRecipe {
    private static final NonNullList<ItemStack> EMPTY_RESULT = NonNullList.create();

    private final Ingredient input;
    private final List<ItemStack> outputs;
    private final int lossRate;

    public ExplosionCraftingRecipeImpl(ResourceLocation id, Ingredient input, int lossRate, ItemStack... outputs) {
        super(id);

        this.input = input;
        this.outputs = Arrays.asList(outputs);
        this.lossRate = lossRate;
    }

    @Override
    public Ingredient getInput() {
        return input;
    }

    @Override
    public int getAmount() {
        return input.getItems().length > 0 ? input.getItems()[0].getCount() : 0;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getLossRate() {
        return lossRate;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return input.test(stack) && stack.getCount() >= getAmount();
    }

    public static NonNullList<ItemStack> tryToCraft(Level world, ItemStack stack) {
        ExplosionCraftingRecipe recipe = PneumaticCraftRecipeType.explosionCrafting.findFirst(world, r -> r.matches(stack));
        return recipe == null || recipe.getAmount() == 0 ? EMPTY_RESULT : createOutput(recipe, stack);
    }

    /**
     * Get the output items for the given recipe and input item.  Note that the quantity of output items will differ
     * on each call due to the application of the randomised loss rate.
     *
     * @param recipe the recipe to check
     * @param stack the input itemstack
     * @return a list of output items
     */
    private static NonNullList<ItemStack> createOutput(ExplosionCraftingRecipe recipe, ItemStack stack) {
        Random rand = ThreadLocalRandom.current();
        int lossRate = recipe.getLossRate();

        NonNullList<ItemStack> res = NonNullList.create();
        int inputCount = Math.round((float)stack.getCount() / recipe.getAmount());
        if (inputCount >= 3 || rand.nextDouble() >= lossRate / 100D) {
            for (ItemStack s : recipe.getOutputs()) {
                ItemStack newStack = s.copy();
                if (inputCount >= 3) {
                    newStack.setCount((int) (inputCount * (rand.nextDouble() * Math.min(lossRate * 0.02D, 0.2D) + (Math.max(0.9D, 1D - lossRate * 0.01D) - lossRate * 0.01D))));
                }
                res.add(newStack);
            }
        }
        return res;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        input.toNetwork(buffer);
        buffer.writeVarInt(outputs.size());
        outputs.forEach(buffer::writeItem);
        buffer.writeVarInt(lossRate);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EXPLOSION_CRAFTING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return PneumaticCraftRecipeType.explosionCrafting;
    }

    @Override
    public String getGroup() {
        return PneumaticCraftRecipeType.explosionCrafting.toString();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.TNT);
    }

    public static class Serializer<T extends ExplosionCraftingRecipe> extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<T> {
        private final IFactory<T> factory;

        public Serializer(IFactory<T> factory) {
            this.factory = factory;
        }

        @Override
        public T fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = Ingredient.fromJson(json.get("input"));
            int loss_rate = GsonHelper.getAsInt(json,"loss_rate", 0);
            JsonArray outputs = json.get("results").getAsJsonArray();
            NonNullList<ItemStack> results = NonNullList.create();
            for (JsonElement e : outputs) {
                results.add(ShapedRecipe.itemStackFromJson(e.getAsJsonObject()));
            }
            return factory.create(recipeId, input, loss_rate, results.toArray(new ItemStack[0]));
        }

        @Nullable
        @Override
        public T fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient input = Ingredient.fromNetwork(buffer);
            int nOutputs = buffer.readVarInt();
            List<ItemStack> l = new ArrayList<>();
            for (int i = 0; i < nOutputs; i++) {
                l.add(buffer.readItem());
            }
            int lossRate = buffer.readVarInt();
            return factory.create(recipeId, input, lossRate, l.toArray(new ItemStack[0]));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, T recipe) {
            recipe.write(buffer);
        }

        public interface IFactory<T extends ExplosionCraftingRecipe> {
            T create(ResourceLocation id, Ingredient input, int lossRate, ItemStack... result);
        }
    }
}
