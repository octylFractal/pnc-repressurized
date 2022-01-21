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

package me.desht.pneumaticcraft.common.thirdparty.waila;

import mcp.mobius.waila.api.BlockAccessor;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IServerDataProvider;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.config.IPluginConfig;
import me.desht.pneumaticcraft.common.tileentity.IRedstoneControl;
import me.desht.pneumaticcraft.common.tileentity.RedstoneController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public class RedstoneControlProvider {
    public static class Data implements IServerDataProvider<BlockEntity> {
        @Override
        public void appendServerData(CompoundTag compoundTag, ServerPlayer serverPlayer, Level level, BlockEntity blockEntity, boolean b) {
            if (blockEntity instanceof IRedstoneControl rc) {
                compoundTag.putInt("redstoneMode", rc.getRedstoneMode());
            }
        }
    }

    public static class Component implements IComponentProvider {
        @Override
        public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
            CompoundTag tag = blockAccessor.getServerData();
            // This is used so that we can split values later easier and have them all in the same layout.
            Map<Component, Component> values = new HashMap<>();

            if (tag.contains("redstoneMode")) {
                BlockEntity te = blockAccessor.getBlockEntity();
                if (te instanceof IRedstoneControl) {
                    RedstoneController<?> rsController = ((IRedstoneControl<?>) te).getRedstoneController();
                    iTooltip.add(rsController.getDescription());
                }
            }
        }
    }
}
