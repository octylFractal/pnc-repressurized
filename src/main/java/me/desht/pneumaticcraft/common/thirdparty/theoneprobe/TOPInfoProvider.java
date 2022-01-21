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

package me.desht.pneumaticcraft.common.thirdparty.theoneprobe;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.misc.Symbols;
import me.desht.pneumaticcraft.api.semiblock.ISemiBlock;
import me.desht.pneumaticcraft.common.block.BlockPressureTube;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.config.ConfigHelper;
import me.desht.pneumaticcraft.common.heat.HeatUtil;
import me.desht.pneumaticcraft.common.heat.TemperatureData;
import me.desht.pneumaticcraft.common.item.ItemCamoApplicator;
import me.desht.pneumaticcraft.common.thirdparty.waila.IInfoForwarder;
import me.desht.pneumaticcraft.common.tileentity.ICamouflageableTE;
import me.desht.pneumaticcraft.common.tileentity.IRedstoneControl;
import me.desht.pneumaticcraft.common.tileentity.TileEntityPressureTube;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class TOPInfoProvider {
    private static final ChatFormatting COLOR = ChatFormatting.GRAY;

    static void handleBlock(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, IProbeHitData data) {
        BlockEntity te = world.getBlockEntity(data.getPos());
        if (te instanceof IInfoForwarder) {
            te = ((IInfoForwarder)te).getInfoBlockEntity();
        }

        if (te == null) return;

        if (te.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY).isPresent()) {
            handlePneumatic(mode, probeInfo, te);
        }
        if (te.getCapability(PNCCapabilities.HEAT_EXCHANGER_CAPABILITY).isPresent()) {
            handleHeat(mode, probeInfo, te);
        }
        if (ConfigHelper.client().general.topShowsFluids.get()) {
            te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, data.getSideHit())
                    .ifPresent(handler -> handleFluidTanks(mode, probeInfo, handler));
        }
        if (te instanceof IRedstoneControl) {
            handleRedstoneMode(mode, probeInfo, (IRedstoneControl<?>) te);
        }
        if (te instanceof TileEntityPressureTube) {
            handlePressureTube(mode, probeInfo, (TileEntityPressureTube) te, data.getSideHit(), player);
        }
        if (te instanceof ICamouflageableTE) {
            handleCamo(mode, probeInfo, ((ICamouflageableTE) te).getCamouflage());
        }
    }

    static void handleSemiblock(Player player, ProbeMode mode, IProbeInfo probeInfo, ISemiBlock semiBlock) {
        IProbeInfo vert = probeInfo.vertical(probeInfo.defaultLayoutStyle().borderColor(semiBlock.getColor()));
        IProbeInfo horiz = vert.horizontal();
        NonNullList<ItemStack> drops = semiBlock.getDrops();
        if (!drops.isEmpty()) {
            ItemStack stack = drops.get(0);
            horiz.item(stack);
            horiz.text(stack.getHoverName());
            semiBlock.addTooltip(vert::text, player, stack.getTag(), player.isShiftKeyDown());
        }
    }

    private static void handlePneumatic(ProbeMode mode, IProbeInfo probeInfo, BlockEntity pneumaticMachine) {
        pneumaticMachine.getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY).ifPresent(airHandler -> {
            String pressure = PneumaticCraftUtils.roundNumberTo(airHandler.getPressure(), 2);
            String dangerPressure = PneumaticCraftUtils.roundNumberTo(airHandler.getDangerPressure(), 1);
            probeInfo.text(xlate("pneumaticcraft.gui.tooltip.maxPressure", dangerPressure).withStyle(COLOR));
            if (mode == ProbeMode.EXTENDED) {
                probeInfo.text(new TextComponent("Pressure:").withStyle(COLOR));
                probeInfo.horizontal()
                        .element(new ElementPressure(pneumaticMachine, airHandler))
                        .vertical()
                        .text(TextComponent.EMPTY)
                        .text(new TextComponent(" " + Symbols.ARROW_LEFT_SHORT + " " + pressure + " bar"));
            } else {
                probeInfo.text(xlate("pneumaticcraft.gui.tooltip.pressure", pressure));
            }
        });
    }

    private static void handleHeat(ProbeMode mode, IProbeInfo probeInfo, BlockEntity heatExchanger) {
        TemperatureData tempData = new TemperatureData(heatExchanger);
        if (tempData.isMultisided()) {
            for (Direction face : DirectionUtil.VALUES) {
                if (tempData.hasData(face)) {
                    probeInfo.text(HeatUtil.formatHeatString(face, (int) tempData.getTemperature(face)));
                }
            }
        } else if (tempData.hasData(null)) {
            probeInfo.text(HeatUtil.formatHeatString((int) tempData.getTemperature(null)));
        }
    }

    private static void handleRedstoneMode(ProbeMode mode, IProbeInfo probeInfo, IRedstoneControl<?> redstoneControl) {
        probeInfo.text(redstoneControl.getRedstoneController().getDescription());
    }

    private static void handlePressureTube(ProbeMode mode, IProbeInfo probeInfo, TileEntityPressureTube te, Direction face, Player player) {
        TubeModule module = BlockPressureTube.getFocusedModule(te.nonNullLevel(), te.getBlockPos(), player);
        if (module != null) {
            List<Component> currenttip = new ArrayList<>();
            module.addInfo(currenttip);
            if (!currenttip.isEmpty()) {
                IProbeInfo vert = probeInfo.vertical(probeInfo.defaultLayoutStyle().borderColor(0xFF4040FF));
                currenttip.forEach(vert::text);
            }
        }
    }

    static void handleFluidTanks(ProbeMode mode, IProbeInfo probeInfo, IFluidHandler handler) {
        if (mode == ProbeMode.EXTENDED) {
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluidStack = handler.getFluidInTank(i);
                Component fluidDesc = fluidStack.isEmpty() ?
                        xlate("pneumaticcraft.gui.misc.empty") :
                        new TextComponent(fluidStack.getAmount() + "mB ").append(xlate(fluidStack.getTranslationKey()));
                probeInfo.text(xlate("pneumaticcraft.waila.tank", i + 1, fluidDesc.copy().withStyle(ChatFormatting.AQUA)));
            }
        }
    }

    private static void handleCamo(ProbeMode mode, IProbeInfo probeInfo, BlockState camo) {
        if (camo != null) {
            probeInfo.text(xlate("pneumaticcraft.waila.camo", ItemCamoApplicator.getCamoStateDisplayName(camo)));
        }
    }

}
