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
import me.desht.pneumaticcraft.client.gui.GPSAreaToolScreen;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.core.ModSounds;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetArea;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.variables.GlobalVariableHelper;
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
import java.util.*;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class GPSAreaToolItem extends Item implements IPositionProvider, IGPSToolSync {
    public GPSAreaToolItem() {
        super(ModItems.defaultProps());
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() != null) {
            setGPSPosAndNotify(ctx.getPlayer(), ctx.getHand(), ctx.getClickedPos(), 0);
            ctx.getPlayer().playSound(ModSounds.CHIRP.get(), 1.0f, 1.5f);
        }
        return InteractionResult.SUCCESS; // we don't want to use the item.
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        if (worldIn.isClientSide) {
            GPSAreaToolScreen.showGUI(stack, handIn, 0);
        }
        return InteractionResultHolder.success(stack);
    }

    public static void setGPSPosAndNotify(Player player, ItemStack stack, BlockPos pos, int index) {
        setGPSLocation(player, stack, pos, null, index, true);
        if (player instanceof ServerPlayer sp) {
            player.displayClientMessage(new TextComponent(ChatFormatting.AQUA + String.format("[%s] ", stack.getDisplayName().getString()))
                    .append(getMessageText(player.level, pos, index)), false);
            sp.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
        }
    }

    public static void setGPSPosAndNotify(Player player, InteractionHand hand, BlockPos pos, int index) {
        setGPSPosAndNotify(player, player.getItemInHand(hand), pos, index);
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
            int n = infoList.size();
            ProgWidgetArea area = getArea(ClientUtils.getClientPlayer(), stack);
            for (int index = 0; index < 2; index++) {
                final int i = index;
                getGPSLocation(ClientUtils.getClientPlayer(), stack, index).ifPresent(pos -> infoList.add(getMessageText(worldIn, pos, i)));
                String varName = area.getVarName(index);
                if (!varName.isEmpty()) {
                    infoList.add(xlate("pneumaticcraft.gui.tooltip.gpsTool.variable", varName));
                }
            }
            if (infoList.size() - n >= 2) area.addAreaTypeTooltip(infoList);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean heldItem) {
        if (!world.isClientSide && entity instanceof Player p) {
            ProgWidgetArea area = getArea(p, stack);
            for (int index = 0; index < 2; index++) {
                String varName = area.getVarName(index);
                if (!varName.isEmpty()) {
                    BlockPos curPos = area.getPos(index).orElse(PneumaticCraftUtils.invalidPos());
                    BlockPos pos = GlobalVariableHelper.getPos(entity.getUUID(), varName, PneumaticCraftUtils.invalidPos());
                    if (!curPos.equals(pos)) {
                        setGPSLocation(p, stack, pos, area, index, false);
                    }
                }
            }
        }
    }

    @Nonnull
    public static ProgWidgetArea getArea(UUID playerId, ItemStack stack) {
        Validate.isTrue(stack.getItem() instanceof GPSAreaToolItem);
        ProgWidgetArea area = new ProgWidgetArea();
        if (stack.hasTag()) {
            area.setVariableProvider(GlobalVariableHelper.getVariableProvider(), playerId);  // allows client to read vars for rendering purposes
            area.readFromNBT(stack.getTag());
        }
        return area;
    }

    public static ProgWidgetArea getArea(Player player, ItemStack stack) {
        return getArea(player.getUUID(), stack);
    }

    public static Optional<BlockPos> getGPSLocation(Player player, ItemStack gpsTool, int index) {
        Validate.isTrue(index == 0 || index == 1, "index must be 0 or 1!");
        ProgWidgetArea area = getArea(player, gpsTool);
        Optional<BlockPos> pos = area.getPos(index);

        // if there's a variable set for this index, use its value instead (and update the stored position)
        String var = area.getVarName(index);
        if (!var.isEmpty() && !player.getLevel().isClientSide) {
            BlockPos newPos = GlobalVariableHelper.getPos(player.getUUID(), var);
            if (pos.isEmpty() || !pos.get().equals(newPos)) {
                area.setPos(index, newPos);
                area.writeToNBT(gpsTool.getOrCreateTag());
            }
            return Optional.of(newPos);
        }

        return pos;
    }

    private static void setGPSLocation(Player player, ItemStack gpsTool, BlockPos pos, ProgWidgetArea area, int index, boolean updateVar) {
        if (area == null) area = getArea(player, gpsTool);
        area.setPos(index, pos);
        area.writeToNBT(gpsTool.getOrCreateTag());

        if (updateVar) {
            String varName = area.getVarName(index);
            if (!varName.isEmpty()) {
                GlobalVariableHelper.setPos(player.getUUID(), varName, pos);
            }
        }
    }

    public static void setVariable(Player player, ItemStack gpsTool, String variable, int index) {
        ProgWidgetArea area = getArea(player, gpsTool);
        area.setVarName(index, variable);
        area.writeToNBT(gpsTool.getOrCreateTag());
    }

    public static String getVariable(Player player, ItemStack gpsTool, int index) {
        return getArea(player, gpsTool).getVarName(index);
    }

    @Override
    public void syncVariables(ServerPlayer player, ItemStack stack) {
        ProgWidgetArea area = getArea(player, stack);
        String v1 = area.getVarName(0);
        String v2 = area.getVarName(1);
        if (!v1.isEmpty()) PneumaticRegistry.getInstance().getMiscHelpers().syncGlobalVariable(player, v1);
        if (!v2.isEmpty()) PneumaticRegistry.getInstance().getMiscHelpers().syncGlobalVariable(player, v2);
    }

    @Override
    public List<BlockPos> getStoredPositions(UUID playerId, @Nonnull ItemStack stack) {
        Set<BlockPos> posSet = new HashSet<>();
        getArea(playerId, stack).getArea(posSet);
        return new ArrayList<>(posSet);
    }

    @Override
    public List<BlockPos> getRawStoredPositions(Player player, ItemStack stack) {
        ProgWidgetArea area = getArea(player, stack);
        return ImmutableList.of(area.getPos(0).orElse(BlockPos.ZERO), area.getPos(1).orElse(BlockPos.ZERO));
    }

    @Override
    public int getRenderColor(int index) {
        return 0x60FFFF00;
    }

    @Override
    public boolean disableDepthTest() {
        return false;
    }

    @Override
    public void syncFromClient(Player player, ItemStack stack, int index, BlockPos pos, String varName) {
        GPSAreaToolItem.setVariable(player, stack, varName, index);
        GPSAreaToolItem.setGPSPosAndNotify(player, stack, pos, index);
        if (!varName.isEmpty()) {
            GlobalVariableHelper.setPos(player.getUUID(), varName, pos);
        }
    }

    public static class EventHandler {
        @SubscribeEvent
        public static void onBlockLeftClick(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getItemStack().getItem() == ModItems.GPS_AREA_TOOL.get()) {
                Optional<BlockPos> optPos = getGPSLocation(event.getPlayer(), event.getItemStack(), 1);
                if (!event.getPos().equals(optPos.orElse(null))) {
                    event.getPlayer().playSound(ModSounds.CHIRP.get(), 1.0f, 1.5f);
                    setGPSPosAndNotify(event.getPlayer(), event.getHand(), event.getPos(), 1);
                }
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLeftClickAir(PlayerInteractEvent.LeftClickEmpty event) {
            if (event.getItemStack().getItem() == ModItems.GPS_AREA_TOOL.get()) {
                GPSAreaToolScreen.showGUI(event.getItemStack(), event.getHand(), 1);
            }
        }
    }
}
