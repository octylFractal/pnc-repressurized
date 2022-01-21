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

package me.desht.pneumaticcraft.common.inventory;

import me.desht.pneumaticcraft.common.core.ModContainers;
import me.desht.pneumaticcraft.common.tileentity.TileEntityElectrostaticCompressor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

public class ContainerElectrostaticCompressor extends Container4UpgradeSlots<TileEntityElectrostaticCompressor> {
    public ContainerElectrostaticCompressor(int i, Inventory playerInventory, FriendlyByteBuf buffer) {
        super(ModContainers.ELECTROSTATIC_COMPRESSOR.get(), i, playerInventory, getTilePos(buffer));
    }

    public ContainerElectrostaticCompressor(int i, Inventory playerInventory, BlockPos pos) {
        super(ModContainers.ELECTROSTATIC_COMPRESSOR.get(), i, playerInventory, pos);
    }
}
