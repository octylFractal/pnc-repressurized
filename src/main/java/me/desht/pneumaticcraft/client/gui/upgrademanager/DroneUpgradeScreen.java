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

package me.desht.pneumaticcraft.client.gui.upgrademanager;

import me.desht.pneumaticcraft.common.inventory.ChargingStationUpgradeManagerMenu;
import me.desht.pneumaticcraft.common.item.DroneItem;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class DroneUpgradeScreen extends AbstractUpgradeManagerScreen {

    public DroneUpgradeScreen(ChargingStationUpgradeManagerMenu container, Inventory inv, Component displayString) {
        super(container, inv, displayString);
    }

    @Override
    public void init() {
        super.init();

        if (!(itemStack.getItem() instanceof DroneItem)) {
            return; // should never happen...
        }

        addAnimatedStat(xlate("pneumaticcraft.gui.tab.info"), Textures.GUI_INFO_LOCATION, 0xFF8888FF, true)
                .setText(xlate("pneumaticcraft.gui.tab.info.item.drone"));
        addUpgradeTabs(itemStack.getItem(), itemStack.getItem().getRegistryName().getPath(), "drone");
    }

    @Override
    protected int getDefaultVolume() {
        return PneumaticValues.DRONE_VOLUME;
    }
}
