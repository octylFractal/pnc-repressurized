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

package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.common.core.ModUpgrades;
import me.desht.pneumaticcraft.common.entity.drone.DroneEntity;
import me.desht.pneumaticcraft.common.item.minigun.AbstractGunAmmoItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class DroneAIAttackEntity extends MeleeAttackGoal {
    private final DroneEntity attacker;
    private final boolean isRanged;
    private final double rangedAttackRange;

    public DroneAIAttackEntity(DroneEntity attacker, double speed, boolean useLongMemory) {
        super(attacker, speed, useLongMemory);
        this.attacker = attacker;
        isRanged = attacker.hasMinigun();
        float rangeMult = 1.0f;
        if (isRanged) {
            ItemStack stack = attacker.getMinigun().getAmmoStack();
            if (stack.getItem() instanceof AbstractGunAmmoItem) {
                rangeMult = ((AbstractGunAmmoItem) stack.getItem()).getRangeMultiplier(stack);
            }
        }
        rangedAttackRange = (16 + Math.min(16, attacker.getUpgrades(ModUpgrades.RANGE.get()))) * rangeMult;
    }

    @Override
    public boolean canUse() {
        if (isRanged && attacker.getSlotForAmmo() < 0) {
            attacker.getDebugger().addEntry("pneumaticcraft.gui.progWidget.entityAttack.debug.noAmmo");
            return false;
        }

        LivingEntity target = attacker.getTarget();
        if (target == null || !target.isAlive()) {
            attacker.getDebugger().addEntry("pneumaticcraft.gui.progWidget.entityAttack.debug.noEntityToAttack");
        }

        return super.canUse();
    }

    @Override
    public void start() {
        super.start();

        attacker.incAttackCount();

        // switch to the carried melee weapon with the highest attack damage
        if (attacker.getTarget() != null && attacker.getInv().getSlots() > 1) {
            int bestSlot = 0;
            double bestDmg = 0;
            for (int i = 0; i < attacker.getInv().getSlots(); i++) {
                ItemStack stack = attacker.getInv().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    AttributeInstance damage = new AttributeInstance(Attributes.ATTACK_DAMAGE, c -> {});
                    for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
                        damage.addTransientModifier(modifier);
                    }
                    float f1 = EnchantmentHelper.getDamageBonus(stack, attacker.getTarget().getMobType());
                    if (damage.getValue() + f1 > bestDmg) {
                        bestDmg = damage.getValue() + f1;
                        bestSlot = i;
                    }
                }
            }
            if (bestSlot != 0) {
                ItemStack copy = attacker.getInv().getStackInSlot(0).copy();
                attacker.getInv().setStackInSlot(0, attacker.getInv().getStackInSlot(bestSlot));
                attacker.getInv().setStackInSlot(bestSlot, copy);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (isRanged) {
            LivingEntity target = attacker.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (attacker.getSlotForAmmo() < 0) return false;
            double dist = attacker.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            if (dist < Math.pow(rangedAttackRange, 2) && attacker.getSensing().hasLineOfSight(target))
                return true;
        }
        return super.canContinueToUse();
    }

    @Override
    public void tick() {
        if (isRanged) {
            LivingEntity target = attacker.getTarget();
            if (target != null) {
                double dist = attacker.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
                if (dist < Math.pow(rangedAttackRange, 2) && attacker.getSensing().hasLineOfSight(target)) {
                    attacker.getFakePlayer().setPos(attacker.getX(), attacker.getY(), attacker.getZ());
                    attacker.tryFireMinigun(target);
                    if (dist < Math.pow(rangedAttackRange * 0.75, 2)) {
                        attacker.getNavigation().stop();
                    }
                }
            }
        } else {
            super.tick();
        }
    }
}
