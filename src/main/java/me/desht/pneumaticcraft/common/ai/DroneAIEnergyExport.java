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

package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetInventoryBase;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public class DroneAIEnergyExport extends DroneAIImExBase<ProgWidgetInventoryBase> {
    public DroneAIEnergyExport(IDroneBase drone, ProgWidgetInventoryBase widget) {
        super(drone, widget);
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double squareDistToBlock) {
        return exportEnergy(pos, false) && super.doBlockInteraction(pos, squareDistToBlock);
    }

    @Override
    protected boolean isValidPosition(BlockPos pos) {
        return exportEnergy(pos, true);
    }

    private boolean exportEnergy(BlockPos pos, boolean simulate) {
        boolean didWork = false;
        int energy = drone.getCapability(CapabilityEnergy.ENERGY).map(IEnergyStorage::getEnergyStored).orElseThrow(RuntimeException::new);
        if (energy == 0) {
            abort();
        } else {
            BlockEntity te = drone.world().getBlockEntity(pos);
            if (te != null) {
                for (Direction face : DirectionUtil.VALUES) {
                    if (progWidget.isSideSelected(face)) {
                        didWork = tryExportToSide(te, face, simulate);
                        if (didWork) break;
                    }
                }
            }
        }
        return didWork;
    }

    private boolean tryExportToSide(BlockEntity te, Direction face, boolean simulate) {
        return te.getCapability(CapabilityEnergy.ENERGY, face).map(tileHandler -> {
            int receivable = tileHandler.receiveEnergy(progWidget.useCount() ? getRemainingCount() : Integer.MAX_VALUE, true);
            int toTransfer = extractFromDrone(receivable, true);
            if (toTransfer > 0) {
                if (!simulate) {
                    decreaseCount(toTransfer);
                    extractFromDrone(toTransfer, false);
                    tileHandler.receiveEnergy(toTransfer, false);
                }
                return true;
            }
            return false;
        }).orElse(false);
    }

    private int extractFromDrone(int maxEnergy, boolean simulate) {
        return drone.getCapability(CapabilityEnergy.ENERGY)
                .map(h -> h.extractEnergy(maxEnergy, simulate))
                .orElseThrow(RuntimeException::new);
    }
}
