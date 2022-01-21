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

package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.block.BlockPneumaticDoor;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandler;

public class TileEntityPneumaticDoor extends TileEntityTickableBase {
    @DescSynced
    @LazySynced
    public float rotationAngle;
    public float oldRotationAngle;
    @DescSynced
    public boolean rightGoing;  // true = door rotates clockwise when door base arm extends
    @DescSynced
    public int color = DyeColor.WHITE.getId();

    public TileEntityPneumaticDoor(BlockPos pos, BlockState state) {
        super(ModTileEntities.PNEUMATIC_DOOR.get(), pos, state);
    }

    public void setRotationAngle(float rotationAngle) {
        if (oldRotationAngle == rotationAngle) return;

        oldRotationAngle = this.rotationAngle;
        this.rotationAngle = rotationAngle;

        if (oldRotationAngle < 90f && rotationAngle == 90f) {
            nonNullLevel().setBlockAndUpdate(worldPosition, getBlockState().setValue(DoorBlock.OPEN, true));
        } else if (oldRotationAngle == 90f && rotationAngle < 90f) {
            nonNullLevel().setBlockAndUpdate(worldPosition, getBlockState().setValue(DoorBlock.OPEN, false));
        }

        // also rotate the TE for the other half of the door
        BlockPos otherPos = getBlockPos().relative(isTopDoor() ? Direction.DOWN : Direction.UP);
        PneumaticCraftUtils.getTileEntityAt(getLevel(), otherPos, TileEntityPneumaticDoor.class).ifPresent(otherDoorHalf -> {
            otherDoorHalf.rightGoing = rightGoing;
            otherDoorHalf.setChanged();
            if (rotationAngle != otherDoorHalf.rotationAngle) {
                otherDoorHalf.setRotationAngle(rotationAngle);
            }
        });
    }

    public boolean setColor(DyeColor dyeColor) {
        if (color != dyeColor.getId() && !getBlockState().getValue(BlockPneumaticDoor.TOP_DOOR)) {
            color = (byte) dyeColor.getId();
            PneumaticCraftUtils.getTileEntityAt(level, getBlockPos(), TileEntityPneumaticDoor.class).ifPresent(topHalf -> {
                topHalf.color = color;
                if (!nonNullLevel().isClientSide) {
                    setChanged();
                    topHalf.setChanged();
                    sendDescriptionPacket();
                }
            });
            return true;
        }
        return false;
    }

    private boolean isTopDoor() {
        return BlockPneumaticDoor.isTopDoor(nonNullLevel().getBlockState(getBlockPos()));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.save(tag);

        tag.putBoolean("rightGoing", rightGoing);
        tag.putInt("color", color);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        rightGoing = tag.getBoolean("rightGoing");
        color = tag.getInt("color");
        scheduleDescriptionPacket();
    }

    @Override
    public void serializeExtraItemData(CompoundTag blockEntityTag, boolean preserveState) {
        super.serializeExtraItemData(blockEntityTag, preserveState);

        blockEntityTag.putInt("color", color);
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return null;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(),
                getBlockPos().getX() + 1, getBlockPos().getY() + 2, getBlockPos().getZ() + 1);
    }

    @Override
    public boolean shouldPreserveStateOnBreak() {
        // keep color even if pickaxed
        return true;
    }
}
