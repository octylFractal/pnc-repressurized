package me.desht.pneumaticcraft.client.render.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IArmorUpgradeClientHandler;
import me.desht.pneumaticcraft.client.gui.widget.WidgetKeybindCheckBox;
import me.desht.pneumaticcraft.client.pneumatic_armor.ArmorUpgradeClientRegistry;
import me.desht.pneumaticcraft.client.render.ProgressBarRenderer;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.ArmorMessage;
import me.desht.pneumaticcraft.client.render.pneumatic_armor.HUDHandler;
import me.desht.pneumaticcraft.common.core.ModSounds;
import me.desht.pneumaticcraft.common.item.PneumaticArmorItem;
import me.desht.pneumaticcraft.common.pneumatic_armor.ArmorUpgradeRegistry;
import me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static me.desht.pneumaticcraft.common.item.PneumaticArmorItem.isPneumaticArmorPiece;
import static me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler.CRITICAL_PRESSURE;
import static me.desht.pneumaticcraft.common.pneumatic_armor.CommonArmorHandler.LOW_PRESSURE;
import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class PneumaticArmorHUDOverlay implements IIngameOverlay {
    private static final int PROGRESS_BAR_HEIGHT = 17;

    private final boolean[] gaveCriticalWarning = new boolean[4];  // per-slot
    private final boolean[] gaveLowPressureWarning = new boolean[4];  // per-slot

    @Override
    public void render(ForgeIngameGui gui, PoseStack matrixStack, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.options.hideGui || mc.screen != null
                || !PneumaticArmorItem.isPlayerWearingAnyPneumaticArmor(player)) {
            return;
        }

        Window mw = Minecraft.getInstance().getWindow();
        matrixStack.pushPose();
        CommonArmorHandler comHudHandler = CommonArmorHandler.getHandlerForPlayer(player);

        boolean anyArmorInInit = false;
        for (EquipmentSlot slot : ArmorUpgradeRegistry.ARMOR_SLOTS) {
            ItemStack armorStack = player.getItemBySlot(slot);
            if (armorStack.getItem() instanceof PneumaticArmorItem && comHudHandler.getArmorPressure(slot) >= 0.0001f
                    && !comHudHandler.isArmorReady(slot)) {
                anyArmorInInit = true;
                break;
            }
        }

        for (EquipmentSlot slot : ArmorUpgradeRegistry.ARMOR_SLOTS) {
            ItemStack armorStack = player.getItemBySlot(slot);
            if (armorStack.getItem() instanceof PneumaticArmorItem && comHudHandler.getArmorPressure(slot) >= 0.0001F) {
                if (anyArmorInInit) {
                    // draw initialization progress bar(s)
                    renderInitProgressBarForSlot(matrixStack, partialTicks, mw, comHudHandler, slot, armorStack);
                }
                if (comHudHandler.isArmorReady(slot)) {
                    renderHUDForSlot(matrixStack, partialTicks, player, comHudHandler, slot);
                }
            }
        }

        // render every pending message
        HUDHandler.getInstance().renderMessages(matrixStack, partialTicks);

        matrixStack.popPose();

        // show armor initialisation percentages
        if (comHudHandler.isArmorEnabled() && anyArmorInInit) {
            for (EquipmentSlot slot : ArmorUpgradeRegistry.ARMOR_SLOTS) {
                if (isPneumaticArmorPiece(player, slot) && comHudHandler.getArmorPressure(slot) > 0F) {
                    String text = Math.min(100, comHudHandler.getTicksSinceEquipped(slot) * 100 / comHudHandler.getStartupTime(slot)) + "%";
                    mc.font.drawShadow(matrixStack, text, mw.getGuiScaledWidth() * 0.75f - 8, 14 + PROGRESS_BAR_HEIGHT * (3 - slot.getIndex()), 0xFFFF40);
                }
            }
        }
    }

    private void renderInitProgressBarForSlot(PoseStack matrixStack, float partialTicks, Window mw, CommonArmorHandler comHudHandler, EquipmentSlot slot, ItemStack armorStack) {
        if (comHudHandler.isArmorEnabled()) {
            int xLeft = mw.getGuiScaledWidth() / 2;
            int yOffset = 10 + (3 - slot.getIndex()) * PROGRESS_BAR_HEIGHT;
            float progress = comHudHandler.getTicksSinceEquipped(slot) * 100f / comHudHandler.getStartupTime(slot);
            progress = Math.min(100, progress + partialTicks);
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            ProgressBarRenderer.render2d(matrixStack, mw.getGuiScaledWidth() / 2f, yOffset,
                    mw.getGuiScaledWidth() - 10, yOffset + PROGRESS_BAR_HEIGHT - 1, -90F,
                    progress, 0xAAFFC000, 0xAA00FF00);
            RenderSystem.disableBlend();
            RenderSystem.enableTexture();
            Minecraft.getInstance().getItemRenderer().renderGuiItem(armorStack, xLeft + 2, yOffset);
        }
    }

    private void renderHUDForSlot(PoseStack matrixStack, float partialTicks, Player player, CommonArmorHandler comHudHandler, EquipmentSlot slot) {
        float pressure = comHudHandler.getArmorPressure(slot);

        handlePressureWarnings(player, slot, pressure);

        // all enabled upgrades do their 2D rendering here
        if (WidgetKeybindCheckBox.getCoreComponents().checked || Minecraft.getInstance().screen == null) {
            List<IArmorUpgradeClientHandler<?>> renderHandlers = ArmorUpgradeClientRegistry.getInstance().getHandlersForSlot(slot);
            for (int i = 0; i < renderHandlers.size(); i++) {
                IArmorUpgradeClientHandler<?> clientHandler = renderHandlers.get(i);
                if (comHudHandler.isUpgradeInserted(slot, i) && (comHudHandler.isUpgradeEnabled(slot, i) || !clientHandler.isToggleable())) {
                    IGuiAnimatedStat stat = clientHandler.getAnimatedStat();
                    if (stat != null) {
                        stat.renderStat(matrixStack, -1, -1, partialTicks);
                    }
                    clientHandler.render2D(matrixStack, partialTicks, pressure > 0F);
                }
            }
        }
    }


    public void handlePressureWarnings(Player player, EquipmentSlot slot, float pressure) {
        // low/no pressure warnings
        if (pressure <= CRITICAL_PRESSURE && !gaveCriticalWarning[slot.getIndex()]) {
            HUDHandler.getInstance().addMessage(new ArmorMessage(xlate("pneumaticcraft.armor.message.outOfAir", player.getItemBySlot(slot).getHoverName()),
                    100, 0x70FF0000));
            gaveCriticalWarning[slot.getIndex()] = true;
            player.playSound(ModSounds.MINIGUN_STOP.get(), 1f, 2f);
        } else if (pressure > CRITICAL_PRESSURE && pressure <= LOW_PRESSURE && !gaveLowPressureWarning[slot.getIndex()]) {
            HUDHandler.getInstance().addMessage(new ArmorMessage(xlate("pneumaticcraft.armor.message.almostOutOfAir", player.getItemBySlot(slot).getHoverName()),
                    60, 0x70FF8000));
            gaveLowPressureWarning[slot.getIndex()] = true;
        }

        if (pressure > LOW_PRESSURE + 0.1F) gaveLowPressureWarning[slot.getIndex()] = false;
        if (pressure > CRITICAL_PRESSURE + 0.1F) gaveCriticalWarning[slot.getIndex()] = false;
    }
}
