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

package me.desht.pneumaticcraft.common.util.upgrade;

import me.desht.pneumaticcraft.api.item.IUpgradeItem;
import me.desht.pneumaticcraft.api.item.PNCUpgrade;
import me.desht.pneumaticcraft.common.core.ModUpgrades;
import me.desht.pneumaticcraft.common.item.UpgradeItem;
import me.desht.pneumaticcraft.common.util.NBTUtils;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class UpgradeCache {
    private byte[] countCache;  // indexed by upgrade's internal (numeric) registry ID
    private final IUpgradeHolder holder;
    private Direction ejectDirection;

    public UpgradeCache(IUpgradeHolder holder) {
        this.holder = holder;
    }

    /**
     * Mark the upgrade cache as invalid.  The next query for an upgrade will force a cache rebuild from the upgrade
     * inventory.
     */
    public void invalidateCache() {
        countCache = null;
    }

    public int getUpgrades(PNCUpgrade type) {
        validateCache();
        return countCache[type.getCacheId()];
    }

    public Direction getEjectDirection() {
        return ejectDirection;
    }

    public void validateCache() {
        if (countCache != null) return;

        countCache = new byte[largestID()];
        IItemHandler handler = holder.getUpgradeHandler();

        ejectDirection = null;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
                PNCUpgrade upgradeType = upgradeItem.getUpgradeType();
                if (countCache[upgradeType.getCacheId()] != 0) {
                    Log.warning("found upgrade " + upgradeType + " in multiple slots! Ignoring.");
                    continue;
                }
                countCache[upgradeType.getCacheId()] = (byte)(stack.getCount() * upgradeItem.getUpgradeTier());
                handleExtraData(stack, upgradeType);
            } else if (!stack.isEmpty()) {
                throw new IllegalStateException("found non-upgrade item in an upgrade handler! " + stack);
            }
        }
        holder.onUpgradesChanged();
    }

    public CompoundTag toNBT() {
        IItemHandler handler = holder.getUpgradeHandler();
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
                String key = PneumaticCraftUtils.modDefaultedString(upgradeItem.getUpgradeType().getRegistryName());
                int count = upgradeItem.getUpgradeTier() * stack.getCount();
                tag.put(key, IntTag.valueOf(count));
            }
        }
        return tag;
    }

    private int largestID() {
        int max = 0;
        for (PNCUpgrade upgrade : ModUpgrades.UPGRADES.get().getValues()) {
            max = Math.max(max, upgrade.getCacheId());
        }
        return max;
    }

    private void handleExtraData(ItemStack stack, PNCUpgrade type) {
        if (type == ModUpgrades.DISPENSER.get() && stack.hasTag()) {
            ejectDirection = Direction.byName(NBTUtils.getString(stack, UpgradeItem.NBT_DIRECTION));
        }
    }
}
