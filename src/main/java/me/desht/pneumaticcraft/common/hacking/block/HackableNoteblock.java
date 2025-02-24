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

package me.desht.pneumaticcraft.common.hacking.block;

import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IHackableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;
import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class HackableNoteblock implements IHackableBlock {
    @Override
    public ResourceLocation getHackableId() {
        return RL("note_block");
    }

    @Override
    public void addInfo(BlockGetter world, BlockPos pos, List<Component> curInfo, Player player) {
        curInfo.add(xlate("pneumaticcraft.armor.hacking.result.makeSound"));
    }

    @Override
    public void addPostHackInfo(BlockGetter world, BlockPos pos, List<Component> curInfo, Player player) {
        curInfo.add(xlate("pneumaticcraft.armor.hacking.finished.makingSound"));
    }

    @Override
    public int getHackTime(BlockGetter world, BlockPos pos, Player player) {
        return 20;
    }

    @Override
    public void onHackComplete(Level world, BlockPos pos, Player player) {
        BlockState state = world.getBlockState(pos);
        fakeRayTrace(player, pos).ifPresent(rtr -> state.use(world, player, InteractionHand.MAIN_HAND, rtr));
    }
}
