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

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerItem;
import me.desht.pneumaticcraft.common.block.entity.CamouflageableBlockEntity;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.core.ModSounds;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CamoApplicatorItem extends PressurizableItem {
    public CamoApplicatorItem() {
        super(ModItems.toolProps(), PneumaticValues.PNEUMATIC_WRENCH_MAX_AIR, PneumaticValues.PNEUMATIC_WRENCH_VOLUME);
    }

    @Override
    public Component getName(ItemStack stack) {
        BlockState camoState = getCamoState(stack);
        Component disp = super.getName(stack);
        if (camoState != null) {
            return disp.copy().append(": ").append(getCamoStateDisplayName(camoState)).withStyle(ChatFormatting.YELLOW);
        } else {
            return disp;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if (playerIn.isShiftKeyDown()) {
            if (!worldIn.isClientSide) {
                setCamoState(playerIn.getItemInHand(handIn), null);
            } else {
                if (getCamoState(playerIn.getItemInHand(handIn)) != null) {
                    playerIn.playSound(ModSounds.CHIRP.get(), 1.0f, 1.0f);
                }
            }
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, playerIn.getItemInHand(handIn));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (player != null && !level.isClientSide) {
            if (player.isCrouching()) {
                // sneak-right-click: clear camo
                setCamoState(stack, null);
                level.playSound(null, ctx.getClickedPos(), ModSounds.CHIRP.get(), SoundSource.PLAYERS, 1f, 1f);
            } else {
                BlockEntity te = level.getBlockEntity(pos);
                BlockState state = level.getBlockState(pos);
                if (!(te instanceof CamouflageableBlockEntity camoTE)) {
                    // right-click non-camo block: copy its state
                    setCamoState(stack, state);
                    level.playSound(null, ctx.getClickedPos(), ModSounds.CHIRP.get(), SoundSource.PLAYERS, 1f, 2f);
                } else {
                    // right-click camo block: try to apply (or remove) camo

                    IAirHandlerItem airHandler = stack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).orElseThrow(RuntimeException::new);
                    if (!player.isCreative() && airHandler.getPressure() < 0.1F) {
                        // not enough pressure
                        return InteractionResult.FAIL;
                    }

                    BlockState newCamo = getCamoState(stack);
                    BlockState existingCamo = camoTE.getCamouflage();

                    if (existingCamo == newCamo) {
                        level.playSound(null, ctx.getClickedPos(), SoundEvents.COMPARATOR_CLICK, SoundSource.PLAYERS, 1f, 2f);
                        return InteractionResult.SUCCESS;
                    }

                    // make sure player has enough of the camo item
                    if (newCamo != null && !player.isCreative()) {
                        ItemStack camoStack = CamouflageableBlockEntity.getStackForState(newCamo);
                        if (!PneumaticCraftUtils.consumeInventoryItem(player.getInventory(), camoStack)) {
                            player.displayClientMessage(new TranslatableComponent("pneumaticcraft.message.camo.notEnoughBlocks")
                                    .append(camoStack.getHoverName())
                                    .withStyle(ChatFormatting.RED), true);
                            NetworkHandler.sendToAllTracking(new PacketPlaySound(ModSounds.MINIGUN_STOP.get(), SoundSource.PLAYERS,
                                    pos, 1.0F, 2.0F, true), level, pos);
                            return InteractionResult.FAIL;
                        }
                    }

                    // return existing camo block, if any
                    if (existingCamo != null && !player.isCreative()) {
                        ItemStack camoStack = CamouflageableBlockEntity.getStackForState(existingCamo);
                        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, camoStack);
                        level.addFreshEntity(entity);
                        entity.playerTouch(player);
                    }

                    // and apply the new camouflage
                    airHandler.addAir(-PneumaticValues.USAGE_CAMO_APPLICATOR);
                    camoTE.setCamouflage(newCamo);
                    BlockState particleState = newCamo == null ? existingCamo : newCamo;
                    if (particleState != null) {
                        player.getCommandSenderWorld().levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(particleState));
                    }
                    NetworkHandler.sendToAllTracking(new PacketPlaySound(ModSounds.SHORT_HISS.get(), SoundSource.PLAYERS, pos, 1.0F, 1.0F, true), level, pos);
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void setCamoState(ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getTag();
        if (tag == null) tag = new CompoundTag();
        if (state == null) {
            tag.remove("CamoState");
        } else {
            tag.put("CamoState", NbtUtils.writeBlockState(state));
        }
        stack.setTag(tag);
    }

    private static BlockState getCamoState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CamoState")) {
            return NbtUtils.readBlockState(tag.getCompound("CamoState"));
        }
        return null;
    }

    public static Component getCamoStateDisplayName(BlockState state) {
        if (state != null) {
            return new ItemStack(state.getBlock().asItem()).getHoverName();
        }
        return new TextComponent("<?>");
    }

}
