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

package me.desht.pneumaticcraft.common.block.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import me.desht.pneumaticcraft.api.lib.NBTKeys;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.Validate;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

/**
 * Handles redstone behaviour & emission for a block entity
 */
public class RedstoneController<T extends BlockEntity & IRedstoneControl<T>> {
    private static final Pattern RS_TAG_PATTERN = Pattern.compile("^redstone:(\\d+)$");

    private final WeakReference<T> teRef;
    private final List<RedstoneMode<T>> modes;
    @GuiSynced
    private int currentMode;
    @GuiSynced
    private int currentRedstonePower = -1; // current power level for the block entity's block (< 0 means "unknown")

    public RedstoneController(T te) {
        this.teRef = new WeakReference<>(te);
        this.modes = new StandardReceivingModes<T>().modes();
    }

    public RedstoneController(T te, List<RedstoneMode<T>> modes) {
        Validate.isTrue(modes.size() >= 2, "must have at least 2 modes!");
        this.teRef = new WeakReference<>(te);
        this.modes = modes;
    }

    public int getModeCount() {
        return modes.size();
    }

    public RedstoneMode<T> getModeDetails(int idx) {
        return modes.get(idx);
    }

    public int getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(int currentMode) {
        if (currentMode != this.currentMode && currentMode >= 0 && currentMode < modes.size()) {
            this.currentMode = currentMode;
            T te = teRef.get();
            if (te != null) {
                te.onRedstoneModeChanged(this.currentMode);
                te.setChanged();
            }
        }
    }

    public int getCurrentRedstonePower() {
        if (currentRedstonePower < 0) {
            updateRedstonePower();
        }
        return currentRedstonePower;
    }

    public boolean shouldRun() {
        T te = teRef.get();
        return te != null && modes.get(currentMode).runPredicate.test(te);
    }

    public boolean shouldEmit() {
        T te = teRef.get();
        return te != null && modes.get(currentMode).emissionPredicate.test(te);
    }

    public void serialize(CompoundTag tag) {
        // don't write default mode 0; avoid messy NBT
        if (currentMode != 0) {
            tag.putInt(NBTKeys.NBT_REDSTONE_MODE, currentMode);
        }
    }

    public void deserialize(CompoundTag tag) {
        currentMode = tag.getInt(NBTKeys.NBT_REDSTONE_MODE);
    }

    /**
     * Attempt to parse a redstone tag string, as received by {@link IGUIButtonSensitive#handleGUIButtonPress(String, boolean, ServerPlayer)}.
     * If the tag can be parsed, then update the redstone mode for this controller to the integer mode parsed from the tag.
     *
     * @param tag the string tag in the form {@code "redstone:<int>"}
     * @return true if the tag was parsed, false if not
     */
    public boolean parseRedstoneMode(String tag) {
        Matcher m = RS_TAG_PATTERN.matcher(tag);
        if (m.matches() && m.groupCount() == 1) {
            setCurrentMode(Integer.parseInt(m.group(1)));
            return true;
        }
        return false;
    }

    public void updateRedstonePower() {
        T te = teRef.get();
        if (te != null) {
            currentRedstonePower = Objects.requireNonNull(te.getLevel()).getBestNeighborSignal(te.getBlockPos());
        }
    }

    public boolean isEmitter() {
        return !modes.isEmpty() && modes.get(0) instanceof EmittingRedstoneMode;
    }

    public Component getRedstoneTabTitle() {
        T te = teRef.get();
        return te != null ? te.getRedstoneTabTitle() : TextComponent.EMPTY;
    }

    public Component getDescription() {
        T te = teRef.get();
        if (te != null) {
            return te.getRedstoneTabTitle().append(": ").append(xlate(modes.get(currentMode).getTranslationKey()).withStyle(ChatFormatting.YELLOW));
        } else {
            return TextComponent.EMPTY;
        }
    }

    public static abstract class RedstoneMode<T extends BlockEntity & IRedstoneControl<T>> {
        private final String id;
        private final Either<ItemStack,ResourceLocation> texture;
        private final Predicate<T> runPredicate;
        private final Predicate<T> emissionPredicate;

        public RedstoneMode(String id, ItemStack stackIcon, Predicate<T> runPredicate, Predicate<T> emissionPredicate) {
            this(id, Either.left(stackIcon), runPredicate, emissionPredicate);
        }

        public RedstoneMode(String id, ResourceLocation texture, Predicate<T> runPredicate, Predicate<T> emissionPredicate) {
            this(id, Either.right(texture), runPredicate, emissionPredicate);
        }

        private RedstoneMode(String id, Either<ItemStack,ResourceLocation> texture, Predicate<T> runPredicate, Predicate<T> emissionPredicate) {
            this.id = id;
            this.runPredicate = runPredicate;
            this.emissionPredicate = emissionPredicate;
            this.texture = texture;
        }

        public String getId() {
            return id;
        }

        public Either<ItemStack,ResourceLocation> getTexture() {
            return texture;
        }

        public String getTranslationKey() {
            return "pneumaticcraft.gui.tab.redstoneBehaviour." + id;
        }
    }

    public static class EmittingRedstoneMode<T extends BlockEntity & IRedstoneControl<T>> extends RedstoneMode<T> {
        public EmittingRedstoneMode(String id, ResourceLocation texture, Predicate<T> emissionPredicate) {
            super(id, texture, te -> false, emissionPredicate);
        }

        public EmittingRedstoneMode(String id, ItemStack stackIcon, Predicate<T> emissionPredicate) {
            super(id, stackIcon, te -> false, emissionPredicate);
        }
    }

    public static class ReceivingRedstoneMode<T extends BlockEntity & IRedstoneControl<T>> extends RedstoneMode<T> {
        public ReceivingRedstoneMode(String id, ResourceLocation texture, Predicate<T> runPredicate) {
            super(id, texture, runPredicate, t -> false);
        }

        public ReceivingRedstoneMode(String id, ItemStack stackIcon, Predicate<T> runPredicate) {
            super(id, stackIcon, runPredicate, te -> false);
        }
    }

    private static class StandardReceivingModes<T extends BlockEntity & IRedstoneControl<T>> {
        public List<RedstoneController.RedstoneMode<T>> modes() {
            return ImmutableList.of(
                    new ReceivingRedstoneMode<>("standard.always", new ItemStack(Items.GUNPOWDER),
                            te -> true),
                    new ReceivingRedstoneMode<>("standard.high_signal", new ItemStack(Items.REDSTONE),
                            te -> te.getCurrentRedstonePower() > 0),
                    new ReceivingRedstoneMode<>("standard.low_signal",  new ItemStack(Items.REDSTONE_TORCH),
                            te -> te.getCurrentRedstonePower() == 0));
        }
    }
}
