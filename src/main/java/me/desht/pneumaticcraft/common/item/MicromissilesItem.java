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

import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.client.gui.MicromissileScreen;
import me.desht.pneumaticcraft.common.config.ConfigHelper;
import me.desht.pneumaticcraft.common.config.subconfig.MicromissileDefaults;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.entity.projectile.MicromissileEntity;
import me.desht.pneumaticcraft.common.util.ITranslatableEnum;
import me.desht.pneumaticcraft.common.util.RayTraceUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Locale;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class MicromissilesItem extends Item {
    public static final String NBT_TOP_SPEED = "topSpeed";
    public static final String NBT_TURN_SPEED = "turnSpeed";
    public static final String NBT_DAMAGE = "damage";
    public static final String NBT_FILTER = "filter";
    public static final String NBT_PX = "px";
    public static final String NBT_PY = "py";
    public static final String NBT_FIRE_MODE = "fireMode";

    public enum FireMode implements ITranslatableEnum {
        SMART, DUMB;

        public static FireMode fromString(String mode) {
            try {
                return FireMode.valueOf(mode);
            } catch (IllegalArgumentException e) {
                return SMART;
            }
        }

        @Override
        public String getTranslationKey() {
            return "pneumaticcraft.gui.micromissile.mode." + this.toString().toLowerCase(Locale.ROOT);
        }
    }

    public MicromissilesItem() {
        super(ModItems.defaultProps().stacksTo(1).defaultDurability(100));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ConfigHelper.common().micromissiles.missilePodSize.get();
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return toRepair.getItem() == this && repair.getItem() == Blocks.TNT.asItem();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);

        if (playerIn.isShiftKeyDown()) {
            if (worldIn.isClientSide) {
                MicromissileScreen.openGui(stack.getHoverName(), handIn);
            }
            return InteractionResultHolder.success(stack);
        }

        MicromissileEntity missile = new MicromissileEntity(worldIn, playerIn, stack);
        Vec3 newPos = missile.position().add(playerIn.getLookAngle().normalize());
        missile.setPos(newPos.x, newPos.y, newPos.z);
        missile.shootFromRotation(playerIn, playerIn.getXRot(), playerIn.getYRot(), 0.0F, getInitialVelocity(stack), 0.0F);

        playerIn.getCooldowns().addCooldown(this, ConfigHelper.common().micromissiles.launchCooldown.get());

        if (!worldIn.isClientSide) {
            HitResult res = RayTraceUtils.getMouseOverServer(playerIn, 100);
            if (res instanceof EntityHitResult) {
                EntityHitResult ertr = (EntityHitResult) res;
                if (missile.isValidTarget(ertr.getEntity())) {
                    missile.setTarget(ertr.getEntity());
                }
            }
            worldIn.addFreshEntity(missile);
        }

        if (!playerIn.isCreative()) {
            stack.hurtAndBreak(1, playerIn, playerEntity -> { });
        }
        return InteractionResultHolder.success(stack);
    }

    private float getInitialVelocity(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            FireMode fireMode = FireMode.fromString(tag.getString(NBT_FIRE_MODE));
            if (fireMode == FireMode.SMART) {
                return Math.max(0.2f, tag.getFloat(NBT_TOP_SPEED) / 2f);
            } else {
                return 1/3f;
            }
        } else {
            return 1/3f;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> curInfo, TooltipFlag extraInfo) {
        super.appendHoverText(stack, worldIn, curInfo, extraInfo);

        curInfo.add(xlate("pneumaticcraft.gui.micromissile.remaining")
                .append(new TextComponent(Integer.toString(stack.getMaxDamage() - stack.getDamageValue())).withStyle(ChatFormatting.AQUA))
        );
        if (stack.hasTag()) {
            FireMode mode = getFireMode(stack);
            if (mode == FireMode.SMART) {
                CompoundTag tag = stack.getTag();
                curInfo.add(xlate("pneumaticcraft.gui.micromissile.topSpeed"));
                curInfo.add(xlate("pneumaticcraft.gui.micromissile.turnSpeed"));
                curInfo.add(xlate("pneumaticcraft.gui.micromissile.damage"));
                String filter = tag.getString(NBT_FILTER);
                if (!filter.isEmpty()) {
                    curInfo.add(xlate("pneumaticcraft.gui.sentryTurret.targetFilter")
                            .append(": ")
                            .append(ChatFormatting.AQUA + filter));
                }
            }
            curInfo.add(xlate("pneumaticcraft.gui.micromissile.firingMode")
                    .append(": ")
                    .append(xlate(mode.getTranslationKey()).withStyle(ChatFormatting.AQUA)));
            if (ConfigHelper.common().micromissiles.damageTerrain.get()) {
                curInfo.add(xlate("pneumaticcraft.gui.tooltip.terrainWarning"));
            } else {
                curInfo.add(xlate("pneumaticcraft.gui.tooltip.terrainSafe"));
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (!stack.hasTag() && entityIn instanceof Player) {
            MicromissileDefaults.Entry def = MicromissileDefaults.INSTANCE.getDefaults((Player) entityIn);
            if (def != null) {
                stack.setTag(def.toNBT());
            }
        }
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
    }

    public static FireMode getFireMode(ItemStack stack) {
        Validate.isTrue(stack.getItem() instanceof MicromissilesItem);
        return stack.hasTag() ? FireMode.fromString(stack.getTag().getString(MicromissilesItem.NBT_FIRE_MODE)) : FireMode.SMART;
    }

    @Mod.EventBusSubscriber(modid = Names.MOD_ID)
    public static class Listener {
        @SubscribeEvent
        public static void onMissilesRepair(AnvilRepairEvent event) {
            // allow repeated repairing without XP costs spiralling
            if (event.getItemResult().getItem() instanceof MicromissilesItem && event.getItemResult().hasTag()) {
                event.getItemResult().setRepairCost(0);
            }
        }
    }

    public record Tooltip(ItemStack stack) implements TooltipComponent {
    }
}
