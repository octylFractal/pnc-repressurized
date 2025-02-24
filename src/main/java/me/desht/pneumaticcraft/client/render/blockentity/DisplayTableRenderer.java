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

package me.desht.pneumaticcraft.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.block.DisplayTableBlock;
import me.desht.pneumaticcraft.common.block.entity.DisplayTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class DisplayTableRenderer implements BlockEntityRenderer<DisplayTableBlockEntity> {
    public DisplayTableRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(DisplayTableBlockEntity te, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if (!te.nonNullLevel().isLoaded(te.getBlockPos())) return;

        matrixStackIn.pushPose();
        matrixStackIn.translate(0.5, 1, 0.5);
        Block b = te.getBlockState().getBlock();
        double yOff = b instanceof DisplayTableBlock.Shelf shelf ? 1d - shelf.getTableHeight() : 0d;
        renderItemAt(matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, te.displayedStack, 0d, yOff, 0d, 0.5f, te.getRotation());
        matrixStackIn.popPose();
    }

    static void renderItemAt(PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, ItemStack stack, double xOffset, double yOffset, double zOffset, float scale, Direction rot) {
        if (!stack.isEmpty()) {
            matrixStackIn.pushPose();
            matrixStackIn.translate(0, -yOffset, 0);
            RenderUtils.rotateMatrixForDirection(matrixStackIn, rot);
            if (stack.getItem() instanceof BlockItem) {
                matrixStackIn.translate(xOffset, scale / 4d, zOffset);
            } else {
                // lie items flat
                matrixStackIn.translate(xOffset, 0.025, zOffset);
                matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(90));
            }
            matrixStackIn.scale(scale, scale, scale);
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            BakedModel bakedModel = itemRenderer.getModel(stack, Minecraft.getInstance().level, null, 0);
            itemRenderer.render(stack, ItemTransforms.TransformType.FIXED, true, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, bakedModel);
            matrixStackIn.popPose();
        }
    }
}
