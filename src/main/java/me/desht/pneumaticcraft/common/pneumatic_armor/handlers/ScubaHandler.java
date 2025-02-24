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

package me.desht.pneumaticcraft.common.pneumatic_armor.handlers;

import me.desht.pneumaticcraft.api.item.PNCUpgrade;
import me.desht.pneumaticcraft.api.pneumatic_armor.BaseArmorUpgradeHandler;
import me.desht.pneumaticcraft.api.pneumatic_armor.IArmorExtensionData;
import me.desht.pneumaticcraft.api.pneumatic_armor.ICommonArmorHandler;
import me.desht.pneumaticcraft.api.pressure.PressureHelper;
import me.desht.pneumaticcraft.common.config.ConfigHelper;
import me.desht.pneumaticcraft.common.core.ModSounds;
import me.desht.pneumaticcraft.common.core.ModUpgrades;
import me.desht.pneumaticcraft.common.item.PneumaticArmorItem;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.common.network.PacketSpawnParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;

public class ScubaHandler extends BaseArmorUpgradeHandler<IArmorExtensionData> {

    private static final ResourceLocation ID = RL("scuba");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public PNCUpgrade[] getRequiredUpgrades() {
        return new PNCUpgrade[] { ModUpgrades.SCUBA.get() };
    }

    @Override
    public float getIdleAirUsage(ICommonArmorHandler armorHandler) {
        return 0;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public void tick(ICommonArmorHandler commonArmorHandler, boolean enabled) {
        Player player = commonArmorHandler.getPlayer();
        if (!player.level.isClientSide && enabled
                && commonArmorHandler.hasMinPressure(EquipmentSlot.HEAD)
                && player.getAirSupply() < 150) {

            ItemStack helmetStack = player.getItemBySlot(EquipmentSlot.HEAD);

            int baseVol = ((PneumaticArmorItem) helmetStack.getItem()).getBaseVolume();
            int vol = PressureHelper.getUpgradedVolume(baseVol, commonArmorHandler.getUpgradeCount(EquipmentSlot.HEAD, ModUpgrades.VOLUME.get()));
            float airInHelmet = commonArmorHandler.getArmorPressure(EquipmentSlot.HEAD) * vol;
            int playerAir = (int) Math.min(300 - player.getAirSupply(), airInHelmet / ConfigHelper.common().armor.scubaMultiplier.get());
            player.setAirSupply(player.getAirSupply() + playerAir);

            int airUsed = playerAir * ConfigHelper.common().armor.scubaMultiplier.get();
            commonArmorHandler.addAir(EquipmentSlot.HEAD, -airUsed);

            NetworkHandler.sendToPlayer(new PacketPlaySound(ModSounds.SCUBA.get(), SoundSource.PLAYERS, player.blockPosition(), 1f, 1.0f, false), (ServerPlayer) player);
            Vec3 eyes = player.getEyePosition(1.0f).add(player.getLookAngle().scale(0.5));
            NetworkHandler.sendToAllTracking(new PacketSpawnParticle(ParticleTypes.BUBBLE, eyes.x - 0.5, eyes.y, eyes.z -0.5, 0.0, 0.2, 0.0, 10, 1.0, 1.0, 1.0), player.level, player.blockPosition());
        }
    }
}
