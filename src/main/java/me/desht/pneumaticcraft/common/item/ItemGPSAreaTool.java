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

package me.desht.pneumaticcraft.common.item;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.item.IPositionProvider;
import me.desht.pneumaticcraft.client.gui.areatool.GuiGPSAreaTool;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.core.ModSounds;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetArea;
import me.desht.pneumaticcraft.common.util.NBTUtils;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.variables.GlobalVariableManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class ItemGPSAreaTool extends Item implements IPositionProvider {
    public ItemGPSAreaTool() {
        super(ModItems.defaultProps());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        setGPSPosAndNotify(ctx.getPlayer(), ctx.getClickedPos(), ctx.getHand(), 0);
        ctx.getPlayer().playSound(ModSounds.CHIRP.get(), 1.0f, 1.5f);
        return InteractionResult.SUCCESS; // we don't want to use the item.
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        if (worldIn.isClientSide) {
            GuiGPSAreaTool.showGUI(stack, handIn, 0);
        }
        return InteractionResultHolder.success(stack);
    }


    public static void setGPSPosAndNotify(Player player, BlockPos pos, InteractionHand hand, int index) {
        ItemStack stack = player.getItemInHand(hand);
        setGPSLocation(stack, pos, index);
        if (!player.level.isClientSide) {
            player.displayClientMessage(new TextComponent(ChatFormatting.AQUA + String.format("[%s] ", stack.getHoverName().getString()))
                    .append(getMessageText(player.level, pos, index)), false);
            if (player instanceof ServerPlayer sp)
                sp.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
        }
    }

    private static Component getMessageText(Level worldIn, BlockPos pos, int index) {
        Component translated = PneumaticCraftUtils.getBlockNameAt(worldIn, pos);
        MutableComponent blockName = worldIn.isLoaded(pos) ?
                new TextComponent(" (").append(translated).append(")") :
                TextComponent.EMPTY.plainCopy();
        String str = String.format("P%d%s: [%d, %d, %d]", index + 1, ChatFormatting.YELLOW, pos.getX(), pos.getY(), pos.getZ());
        return new TextComponent(str).withStyle(index == 0 ? ChatFormatting.RED : ChatFormatting.GREEN).append(blockName.withStyle(ChatFormatting.GREEN));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> infoList, TooltipFlag par4) {
        super.appendHoverText(stack, worldIn, infoList, par4);

        if (worldIn != null) {
            ClientUtils.addGuiContextSensitiveTooltip(stack, infoList);
            int nPos = 0;
            for (int index = 0; index < 2; index++) {
                BlockPos pos = getGPSLocation(worldIn, stack, index);
                if (!pos.equals(BlockPos.ZERO)) {
                    infoList.add(getMessageText(worldIn, pos, index));//.mergeStyle(index == 0 ? TextFormatting.RED : TextFormatting.GREEN));
                    nPos++;
                }
                String varName = getVariable(stack, index);
                if (!varName.isEmpty()) {
                    infoList.add(xlate("pneumaticcraft.gui.tooltip.gpsTool.variable", varName));
                }
            }
            if (nPos > 0) getArea(stack).addAreaTypeTooltip(infoList);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean heldItem) {
        if (!world.isClientSide) {
            for (int index = 0; index < 2; index++) {
                String var = getVariable(stack, index);
                if (!var.isEmpty()) {
                    BlockPos pos = GlobalVariableManager.getInstance().getPos(var);
                    setGPSLocation(stack, pos, index);
                }
            }
        }
    }

    @Nonnull
    public static ProgWidgetArea getArea(ItemStack stack) {
        Validate.isTrue(stack.getItem() instanceof ItemGPSAreaTool);
        ProgWidgetArea area = new ProgWidgetArea();
        if (stack.hasTag()) {
            area.setVariableProvider(GlobalVariableManager.getInstance());  // allows client to read vars for rendering purposes
            area.readFromNBT(stack.getTag());
        }
        return area;
    }

    public static BlockPos getGPSLocation(Level world, ItemStack gpsTool, int index) {
        ProgWidgetArea area = getArea(gpsTool);

        String var = getVariable(gpsTool, index);
        if (!var.isEmpty() && !world.isClientSide) {
            BlockPos pos = GlobalVariableManager.getInstance().getPos(var);
            setGPSLocation(gpsTool, pos, index);
        }

        if (index == 0) {
            return new BlockPos(area.x1, area.y1, area.z1);
        } else if (index == 1) {
            return new BlockPos(area.x2, area.y2, area.z2);
        } else {
            throw new IllegalArgumentException("index must be 0 or 1!");
        }
    }

    private static void setGPSLocation(ItemStack gpsTool, BlockPos pos, int index) {
        ProgWidgetArea area = getArea(gpsTool);
        if (index == 0) {
            area.setP1(pos);
        } else if (index == 1) {
            area.setP2(pos);
        }
        NBTUtils.initNBTTagCompound(gpsTool);
        String var = getVariable(gpsTool, index);
        if (!var.equals("")) GlobalVariableManager.getInstance().set(var, pos);
        area.writeToNBT(gpsTool.getTag());
    }

    public static void setVariable(ItemStack gpsTool, String variable, int index) {
        ProgWidgetArea area = getArea(gpsTool);
        if (index == 0) {
            area.setCoord1Variable(variable);
        } else if (index == 1) {
            area.setCoord2Variable(variable);
        }
        NBTUtils.initNBTTagCompound(gpsTool);
        area.writeToNBT(gpsTool.getTag());
    }

    public static String getVariable(ItemStack gpsTool, int index) {
        ProgWidgetArea area = getArea(gpsTool);
        return index == 0 ? area.getCoord1Variable() : area.getCoord2Variable();
    }

    @Override
    public void syncVariables(ServerPlayer player, ItemStack stack) {
        String v1 = getVariable(stack, 0);
        if (!v1.isEmpty()) PneumaticRegistry.getInstance().syncGlobalVariable(player, v1);
        String v2 = getVariable(stack, 1);
        if (!v1.isEmpty()) PneumaticRegistry.getInstance().syncGlobalVariable(player, v2);
    }

    @Override
    public List<BlockPos> getStoredPositions(Level world, @Nonnull ItemStack stack) {
        Set<BlockPos> posSet = new HashSet<>();
        getArea(stack).getArea(posSet);
        return new ArrayList<>(posSet);
    }

    @Override
    public List<BlockPos> getRawStoredPositions(Level world, ItemStack stack) {
        ProgWidgetArea area = getArea(stack);
        return ImmutableList.of(
                new BlockPos(area.x1, area.y1, area.z1),
                new BlockPos(area.x2, area.y2, area.z2)
        );
    }

    @Override
    public int getRenderColor(int index) {
        return 0x60FFFF00;
    }

    @Override
    public boolean disableDepthTest() {
        return false;
    }

    public static class EventHandler {
        @SubscribeEvent
        public static void onBlockLeftClick(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getItemStack().getItem() == ModItems.GPS_AREA_TOOL.get()) {
                if (!event.getPos().equals(getGPSLocation(event.getWorld(), event.getItemStack(), 1))) {
                    event.getPlayer().playSound(ModSounds.CHIRP.get(), 1.0f, 1.5f);
                    setGPSPosAndNotify(event.getPlayer(), event.getPos(), event.getHand(), 1);
                }
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLeftClickAir(PlayerInteractEvent.LeftClickEmpty event) {
            if (event.getItemStack().getItem() == ModItems.GPS_AREA_TOOL.get()) {
                GuiGPSAreaTool.showGUI(event.getItemStack(), event.getHand(), 1);
            }
        }
    }
}
