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

import me.desht.pneumaticcraft.common.block.BlockElevatorCaller;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandler;

public class TileEntityElevatorCaller extends TileEntityTickableBase implements ICamouflageableTE, IRedstoneControl<TileEntityElevatorCaller> {
    private ElevatorButton[] floors = new ElevatorButton[0];
    private int thisFloor;
    private boolean emittingRedstone;
    private boolean shouldUpdateNeighbors;
    private BlockState camoState;
    private final RedstoneController<TileEntityElevatorCaller> rsController = new RedstoneController<>(this);

    public TileEntityElevatorCaller(BlockPos pos, BlockState state) {
        super(ModTileEntities.ELEVATOR_CALLER.get(), pos, state);
    }

    public void setEmittingRedstone(boolean emittingRedstone) {
        if (emittingRedstone != this.emittingRedstone) {
            this.emittingRedstone = emittingRedstone;
            shouldUpdateNeighbors = true;
        }
    }

    @Override
    public void tickCommonPre() {
        super.tickCommonPre();

        if (shouldUpdateNeighbors) {
            updateNeighbours();
            shouldUpdateNeighbors = false;
        }
    }

    public boolean getEmittingRedstone() {
        return emittingRedstone;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        emittingRedstone = tag.getBoolean("emittingRedstone");
        thisFloor = tag.getInt("thisFloor");
        shouldUpdateNeighbors = tag.getBoolean("shouldUpdateNeighbors");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("emittingRedstone", emittingRedstone);
        tag.putInt("thisFloor", thisFloor);
        tag.putBoolean("shouldUpdateNeighbors", shouldUpdateNeighbors);
    }

    @Override
    public void readFromPacket(CompoundTag tag) {
        super.readFromPacket(tag);
        int floorAmount = tag.getInt("floors");
        floors = new ElevatorButton[floorAmount];
        for (int i = 0; i < floorAmount; i++) {
            floors[i] = new ElevatorButton(tag.getCompound("floor" + i));
        }
        camoState = ICamouflageableTE.readCamo(tag);
    }

    @Override
    public void writeToPacket(CompoundTag tag) {
        super.writeToPacket(tag);
        tag.putInt("floors", floors.length);
        for (ElevatorButton floor : floors) {
            tag.put("floor" + floor.floorNumber, floor.writeToNBT(new CompoundTag()));
        }
        ICamouflageableTE.writeCamo(tag, camoState);
    }

    @Override
    public void onNeighborBlockUpdate(BlockPos fromPos) {
        boolean wasPowered = getRedstoneController().getCurrentRedstonePower() > 0;
        super.onNeighborBlockUpdate(fromPos);
        if (getRedstoneController().getCurrentRedstonePower() > 0 && !wasPowered) {
            BlockElevatorCaller.setSurroundingElevators(getLevel(), getBlockPos(), thisFloor);
        }
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return null;
    }

    void setFloors(ElevatorButton[] floors, int thisFloorLevel) {
        this.floors = floors;
        thisFloor = thisFloorLevel;
        sendDescriptionPacket();
    }

    public ElevatorButton[] getFloors() {
        return floors;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), getBlockPos().getX() + 1, getBlockPos().getY() + 1, getBlockPos().getZ() + 1);
    }

    @Override
    public BlockState getCamouflage() {
        return camoState;
    }

    @Override
    public void setCamouflage(BlockState state) {
        camoState = state;
        ICamouflageableTE.syncToClient(this);
    }

    @Override
    public RedstoneController<TileEntityElevatorCaller> getRedstoneController() {
        return rsController;
    }

    public static class ElevatorButton {
        public final float posX, posY, width, height;
        public final int floorNumber;
        public final int floorHeight;
        public float red, green, blue;
        public String buttonText;

        ElevatorButton(float posX, float posY, float width, float height, int floorNumber, int floorHeight) {
            this.posX = posX;
            this.posY = posY;
            this.width = width;
            this.height = height;
            this.floorNumber = floorNumber;
            this.floorHeight = floorHeight;
            this.buttonText = floorNumber + 1 + "";
        }

        ElevatorButton(CompoundTag tag) {
            this.posX = tag.getFloat("posX");
            this.posY = tag.getFloat("posY");
            this.width = tag.getFloat("width");
            this.height = tag.getFloat("height");
            this.buttonText = tag.getString("buttonText");
            this.floorNumber = tag.getInt("floorNumber");
            this.floorHeight = tag.getInt("floorHeight");
            this.red = tag.getFloat("red");
            this.green = tag.getFloat("green");
            this.blue = tag.getFloat("blue");
        }

        void setColor(float red, float green, float blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public CompoundTag writeToNBT(CompoundTag tag) {
            tag.putFloat("posX", posX);
            tag.putFloat("posY", posY);
            tag.putFloat("width", width);
            tag.putFloat("height", height);
            tag.putString("buttonText", buttonText);
            tag.putInt("floorNumber", floorNumber);
            tag.putInt("floorHeight", floorHeight);
            tag.putFloat("red", red);
            tag.putFloat("green", green);
            tag.putFloat("blue", blue);
            return tag;
        }
    }
}
