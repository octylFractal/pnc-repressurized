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

package me.desht.pneumaticcraft.client.event;

import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.client.model.CamoModel;
import me.desht.pneumaticcraft.client.model.custom.CamouflageModel;
import me.desht.pneumaticcraft.client.model.custom.FluidItemModel;
import me.desht.pneumaticcraft.client.model.custom.RenderedItemModel;
import me.desht.pneumaticcraft.common.block.AbstractCamouflageBlock;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;

@Mod.EventBusSubscriber(modid = Names.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientEventHandler {
    @SubscribeEvent
    public static void onModelBaking(ModelBakeEvent event) {
        // set up camo models for camouflageable blocks
        for (RegistryObject<Block> block : ModBlocks.BLOCKS.getEntries()) {
            if (block.get() instanceof AbstractCamouflageBlock) {
                for (BlockState state : block.get().getStateDefinition().getPossibleStates()) {
                    ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(state);
                    BakedModel model = event.getModelRegistry().get(loc);
                    if (model != null) {
                        event.getModelRegistry().put(loc, new CamoModel(model));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(RL("camouflaged"), CamouflageModel.Loader.INSTANCE);
        ModelLoaderRegistry.registerLoader(RL("fluid_container_item"), FluidItemModel.Loader.INSTANCE);
        ModelLoaderRegistry.registerLoader(RL("rendered_item"), RenderedItemModel.Loader.INSTANCE);
    }
}
