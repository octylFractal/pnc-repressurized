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

package me.desht.pneumaticcraft.common.inventory.handler;

import me.desht.pneumaticcraft.api.item.PNCUpgrade;
import me.desht.pneumaticcraft.common.block.entity.ChargingStationBlockEntity;
import me.desht.pneumaticcraft.common.util.NBTUtils;
import me.desht.pneumaticcraft.common.util.UpgradableItemUtils;
import me.desht.pneumaticcraft.common.util.upgrade.ApplicableUpgradesDB;
import net.minecraft.world.item.ItemStack;

public class ChargeableItemHandler extends BaseItemStackHandler {
    public ChargeableItemHandler(ChargingStationBlockEntity te) {
        super(te, UpgradableItemUtils.UPGRADE_INV_SIZE);

        if (!NBTUtils.hasTag(getChargingStack(), UpgradableItemUtils.NBT_UPGRADE_TAG)) {
            writeToNBT();
        }
        readFromNBT();
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        writeToNBT();
    }

    private ItemStack getChargingStack() {
        return ((ChargingStationBlockEntity) te).getChargingStack();
    }

    public void writeToNBT() {
        UpgradableItemUtils.setUpgrades(getChargingStack(), this);
    }

    private void readFromNBT() {
        deserializeNBT(NBTUtils.getCompoundTag(getChargingStack(), UpgradableItemUtils.NBT_UPGRADE_TAG));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack itemStack) {
        return itemStack.isEmpty() || isApplicable(itemStack) && isUnique(slot, itemStack);
    }

    private boolean isUnique(int slot, ItemStack stack) {
        for (int i = 0; i < getSlots(); i++) {
            if (i != slot && PNCUpgrade.from(stack) == PNCUpgrade.from(getStackInSlot(i))) return false;
        }
        return true;
    }

    private boolean isApplicable(ItemStack stack) {
        return ApplicableUpgradesDB.getInstance().getMaxUpgrades(getChargingStack().getItem(), PNCUpgrade.from(stack)) > 0;
    }
}
