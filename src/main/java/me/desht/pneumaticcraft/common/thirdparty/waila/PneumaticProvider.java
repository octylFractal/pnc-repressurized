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
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.heat.TemperatureData;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import java.util.LinkedHashSet;
import java.util.Set;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class PneumaticProvider {
    public static class Data implements IServerDataProvider<BlockEntity> {
        @Override
        public void appendServerData(CompoundTag compoundTag, ServerPlayer serverPlayer, Level level, BlockEntity blockEntity, boolean b) {
            BlockEntity beInfo;
            if (blockEntity instanceof IInfoForwarder forwarder) {
                beInfo = forwarder.getInfoBlockEntity();
                if (beInfo != null) {
                    compoundTag.putInt("infoX", beInfo.getBlockPos().getX());
                    compoundTag.putInt("infoY", beInfo.getBlockPos().getY());
                    compoundTag.putInt("infoZ", beInfo.getBlockPos().getZ());
                }
            } else {
                beInfo = blockEntity;
            }
            if (beInfo != null) {
                Set<IAirHandlerMachine> set = new LinkedHashSet<>();
                beInfo.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY).ifPresent(set::add);
                for (Direction d : DirectionUtil.VALUES) {
                    beInfo.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY, d).ifPresent(set::add);
                }
                ListTag l = new ListTag();
                for (IAirHandlerMachine h : set) {
                    ListTag l2 = new ListTag();
                    l2.add(FloatTag.valueOf(h.getPressure()));
                    l2.add(FloatTag.valueOf(h.getDangerPressure()));
                    l.add(l2);
                }
                compoundTag.put("pressure", l);

                if (beInfo.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY).isPresent()) {
                    compoundTag.put("heatData", new TemperatureData(beInfo).toNBT());
                }

                beInfo.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
                        .ifPresent(h -> {
                            ListTag list = new ListTag();
                            for (int i = 0; i < h.getTanks(); i++) {
                                list.add(h.getFluidInTank(i).writeToNBT(new CompoundTag()));
                            }
                            compoundTag.put("tanks", list);
                        });
            }
        }
    }

    public static class Component implements IComponentProvider {
        @Override
        public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
            CompoundTag tag = blockAccessor.getServerData();
            BlockEntity te = blockAccessor.getBlockEntity();
            if (te instanceof IInfoForwarder) {
                BlockPos infoPos = new BlockPos(tag.getInt("infoX"), tag.getInt("infoY"), tag.getInt("infoZ"));
                te = blockAccessor.getLevel().getBlockEntity(infoPos);
            }
            if (te != null) {
                ListTag l = tag.getList("pressure", Tag.TAG_LIST);
                for (int i = 0; i < l.size(); i++) {
                    ListTag l2 = l.getList(i);
                    String pressureStr = PneumaticCraftUtils.roundNumberTo(l2.getFloat(0), 2);
                    String dangerPressureStr = PneumaticCraftUtils.roundNumberTo(l2.getFloat(1), 1);
                    iTooltip.add(xlate("pneumaticcraft.gui.tooltip.pressureMax", pressureStr, dangerPressureStr));
                }
                handleHeatData(iTooltip, tag);
                if (blockAccessor.getPlayer().isCrouching()) {
                    handleFluidData(iTooltip, tag);
                }
            }
        }

        private void handleFluidData(ITooltip tooltip, CompoundTag tag) {
            ListTag list = tag.getList("tanks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag subtag = list.getCompound(i);
                FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(subtag);
                MutableComponent fluidDesc = fluidStack.isEmpty() ?
                        xlate("pneumaticcraft.gui.misc.empty") :
                        xlate("pneumaticcraft.message.misc.fluidmB", fluidStack.getAmount()).append(" ").append(xlate(fluidStack.getTranslationKey()));
                tooltip.add(xlate("pneumaticcraft.waila.tank", i + 1, fluidDesc.copy().withStyle(ChatFormatting.AQUA)));
            }
        }

        private void handleHeatData(ITooltip tooltip, CompoundTag tag) {
            if (tag.contains("heatData")) {
                TemperatureData tempData = TemperatureData.fromNBT(tag.getCompound("heatData"));
                if (tempData.isMultisided()) {
                    for (Direction face : DirectionUtil.VALUES) {
                        if (tempData.hasData(face)) {
                            tooltip.add(HeatUtil.formatHeatString(face, (int) tempData.getTemperature(face)));
                        }
                    }
                } else if (tempData.hasData(null)) {
                    tooltip.add(HeatUtil.formatHeatString((int) tempData.getTemperature(null)));
                }
            }
        }
    }
}
