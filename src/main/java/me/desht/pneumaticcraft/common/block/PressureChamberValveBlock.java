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

package me.desht.pneumaticcraft.common.block;

import me.desht.pneumaticcraft.common.advancements.AdvancementTriggers;
import me.desht.pneumaticcraft.common.block.entity.PressureChamberValveBlockEntity;
import me.desht.pneumaticcraft.common.core.ModBlockEntities;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class PressureChamberValveBlock extends AbstractPneumaticCraftBlock implements IBlockPressureChamber, PneumaticCraftEntityBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public PressureChamberValveBlock() {
        super(ModBlocks.defaultProps());
        registerDefaultState(getStateDefinition().any().setValue(FORMED, false));
    }

    @Override
    public void setPlacedBy(Level par1World, BlockPos pos, BlockState state, LivingEntity par5EntityLiving, ItemStack iStack) {
        super.setPlacedBy(par1World, pos, state, par5EntityLiving, iStack);
        if (!par1World.isClientSide && PressureChamberValveBlockEntity.checkIfProperlyFormed(par1World, pos)) {
            AdvancementTriggers.PRESSURE_CHAMBER.trigger((ServerPlayer) par5EntityLiving);
        }
    }

    @Override
    public boolean isRotatable() {
        return true;
    }

    @Override
    protected boolean canRotateToTopOrBottom() {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult brtr) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide) {
            return world.getBlockEntity(pos, ModBlockEntities.PRESSURE_CHAMBER_VALVE.get()).map(te -> {
                if (te.multiBlockSize > 0) {
                    NetworkHooks.openGui((ServerPlayer) player, te, pos);
                } else if (te.accessoryValves.size() > 0) {
                    // when this isn't the core valve, track down the core valve
                    for (PressureChamberValveBlockEntity valve : te.accessoryValves) {
                        if (valve.multiBlockSize > 0) {
                            NetworkHooks.openGui((ServerPlayer) player, valve, valve.getBlockPos());
                            break;
                        }
                    }
                } else {
                    return InteractionResult.PASS;
                }
                return InteractionResult.SUCCESS;
            }).orElse(InteractionResult.SUCCESS);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            invalidateMultiBlock(world, pos);
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    private void invalidateMultiBlock(Level world, BlockPos pos) {
        if (!world.isClientSide) {
            PneumaticCraftUtils.getTileEntityAt(world, pos, PressureChamberValveBlockEntity.class).ifPresent(teValve -> {
                if (teValve.multiBlockSize > 0) {
                    teValve.onMultiBlockBreak();
                } else if (teValve.accessoryValves.size() > 0) {
                    teValve.accessoryValves.stream()
                            .filter(valve -> valve.multiBlockSize > 0)
                            .findFirst()
                            .ifPresent(PressureChamberValveBlockEntity::onMultiBlockBreak);
                }
            });
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new PressureChamberValveBlockEntity(pPos, pState);
    }
}
