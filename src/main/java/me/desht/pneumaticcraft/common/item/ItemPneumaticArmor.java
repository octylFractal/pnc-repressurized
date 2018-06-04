package me.desht.pneumaticcraft.common.item;

import me.desht.pneumaticcraft.PneumaticCraftRepressurized;
import me.desht.pneumaticcraft.api.client.pneumaticHelmet.IUpgradeRenderHandler;
import me.desht.pneumaticcraft.api.item.IItemRegistry.EnumUpgrade;
import me.desht.pneumaticcraft.api.item.IPressurizable;
import me.desht.pneumaticcraft.api.item.IUpgradeAcceptor;
import me.desht.pneumaticcraft.client.render.pneumaticArmor.RenderCoordWireframe;
import me.desht.pneumaticcraft.client.render.pneumaticArmor.UpgradeRenderHandlerList;
import me.desht.pneumaticcraft.common.DateEventHandler;
import me.desht.pneumaticcraft.common.NBTUtil;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.recipes.CraftingRegistrator;
import me.desht.pneumaticcraft.common.recipes.RecipeOneProbe;
import me.desht.pneumaticcraft.common.util.UpgradableItemUtils;
import me.desht.pneumaticcraft.lib.ModIds;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import me.desht.pneumaticcraft.lib.Textures;
import me.desht.pneumaticcraft.proxy.CommonProxy.EnumGuiId;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Optional.InterfaceList({
        @Optional.Interface(iface = "thaumcraft.api.items.IGoggles", modid = ModIds.THAUMCRAFT),
        @Optional.Interface(iface = "thaumcraft.api.items.IVisDiscountGear", modid = ModIds.THAUMCRAFT),
        @Optional.Interface(iface = "thaumcraft.api.items.IRevealer", modid = ModIds.THAUMCRAFT)
})
public class ItemPneumaticArmor extends ItemArmor implements IPressurizable, IChargingStationGUIHolderItem, IUpgradeAcceptor,
        IRevealer, IGoggles, IVisDiscountGear {

    public ItemPneumaticArmor(ItemArmor.ArmorMaterial material, int renderIndex, EntityEquipmentSlot armorType, int maxAir) {
        super(material, renderIndex, armorType);
        // TODO other armor types?
        setRegistryName("pneumatic_helmet");
        setUnlocalizedName("pneumatic_helmet");
        setMaxDamage(maxAir);
        setCreativeTab(PneumaticCraftRepressurized.tabPneumaticCraft);
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        return Textures.ARMOR_PNEUMATIC + "_1.png";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (isInCreativeTab(tab)) {
            subItems.add(new ItemStack(this));
            ItemStack chargedStack = new ItemStack(this);
            addAir(chargedStack, PneumaticValues.PNEUMATIC_HELMET_VOLUME * 10);
            subItems.add(chargedStack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack iStack, World world, List<String> textList, ITooltipFlag flag) {
        if (iStack.hasTagCompound() && iStack.getTagCompound().getInteger(RecipeOneProbe.ONE_PROBE_TAG) == 1) {
            textList.add(TextFormatting.BLUE + "The One Probe installed");
        }
        float pressure = getPressure(iStack);
        textList.add((pressure < 0.5F ? TextFormatting.RED : TextFormatting.DARK_GREEN) + "Pressure: " + Math.round(pressure * 10D) / 10D + " bar");
        if (UpgradableItemUtils.addUpgradeInformation(iStack, world, textList, flag) > 0) {
            // supplementary search & tracker information
            ItemStack searchedStack = getSearchedStack(iStack);
            if (!searchedStack.isEmpty()) {
                for (int i = 0; i < textList.size(); i++) {
                    if (textList.get(i).contains("Item Search")) {
                        textList.set(i, textList.get(i) + " (searching " + searchedStack.getDisplayName() + ")");
                        break;
                    }
                }
            }
            RenderCoordWireframe coordHandler = getCoordTrackLocation(iStack);
            if (coordHandler != null) {
                for (int i = 0; i < textList.size(); i++) {
                    if (textList.get(i).contains("Coordinate Tracker")) {
                        textList.set(i, textList.get(i) + " (tracking " + coordHandler.pos.getX() + ", " + coordHandler.pos.getY() + ", " + coordHandler.pos.getZ() + " in " + coordHandler.world.provider.getDimensionType() + ")");
                        break;
                    }
                }
            }
        }

        ItemPneumatic.addTooltip(iStack, world, textList);
    }

    @SideOnly(Side.CLIENT)
    @Nonnull
    public static ItemStack getSearchedStack() {
        return getSearchedStack(PneumaticCraftRepressurized.proxy.getPlayer().getItemStackFromSlot(EntityEquipmentSlot.HEAD));
    }

    @Nonnull
    public static ItemStack getSearchedStack(ItemStack helmetStack) {
        if (helmetStack.isEmpty() || !NBTUtil.hasTag(helmetStack, "SearchStack")) return ItemStack.EMPTY;
        NBTTagCompound tag = NBTUtil.getCompoundTag(helmetStack, "SearchStack");
        if (tag.getInteger("itemID") == -1) return ItemStack.EMPTY;
        return new ItemStack(Item.getItemById(tag.getInteger("itemID")), 1, tag.getInteger("itemDamage"));
    }

    @SideOnly(Side.CLIENT)
    public static RenderCoordWireframe getCoordTrackLocation(ItemStack helmetStack) {
        if (helmetStack.isEmpty() || !NBTUtil.hasTag(helmetStack, "CoordTracker")) return null;
        NBTTagCompound tag = NBTUtil.getCompoundTag(helmetStack, "CoordTracker");
        if (tag.getInteger("y") == -1 || FMLClientHandler.instance().getClient().world.provider.getDimension() != tag.getInteger("dimID"))
            return null;
        return new RenderCoordWireframe(FMLClientHandler.instance().getClient().world, NBTUtil.getPos(tag));
    }

    @SideOnly(Side.CLIENT)
    public static String getEntityFilter(ItemStack helmetStack) {
        if (helmetStack.isEmpty() || !NBTUtil.hasTag(helmetStack, "entityFilter")) return "";
        return NBTUtil.getString(helmetStack, "entityFilter");
    }

    public static void setEntityFilter(ItemStack helmetStack, String filter) {
        if (!helmetStack.isEmpty()) {
            NBTUtil.setString(helmetStack, "entityFilter", filter);
        }
    }

    @Override
    public boolean getIsRepairable(ItemStack par1ItemStack, ItemStack par2ItemStack) {
        return false;
    }

    @Override
    public float getPressure(ItemStack iStack) {
        int volume = UpgradableItemUtils.getUpgrades(EnumUpgrade.VOLUME, iStack) * PneumaticValues.VOLUME_VOLUME_UPGRADE + PneumaticValues.PNEUMATIC_HELMET_VOLUME;
        int oldVolume = NBTUtil.getInteger(iStack, "volume");
        int currentAir = NBTUtil.getInteger(iStack, "air");
        if (volume < oldVolume) {
            currentAir = currentAir * volume / oldVolume;
            NBTUtil.setInteger(iStack, "air", currentAir);
        }
        if (volume != oldVolume) {
            NBTUtil.setInteger(iStack, "volume", volume);
        }
        return (float) currentAir / volume;
    }

    public boolean hasSufficientPressure(ItemStack iStack) {
        return getPressure(iStack) > 0F;
    }

    @Override
    public float maxPressure(ItemStack iStack) {
        return 10F;
    }

    @Override
    public void addAir(ItemStack iStack, int amount) {
        int oldAir = NBTUtil.getInteger(iStack, "air");
        NBTUtil.setInteger(iStack, "air", Math.max(oldAir + amount, 0));
    }

    @Override
    public EnumGuiId getGuiID() {
        return EnumGuiId.PNEUMATIC_HELMET;
    }

    /**
     * Override this method to have an item handle its own armor rendering.
     *
     * @param entityLiving The entity wearing the armor
     * @param itemStack    The itemStack to render the model of
     * @param armorSlot    0=head, 1=torso, 2=legs, 3=feet
     * @return A ModelBiped to render instead of the default
     */

    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped _default) {
        if (armorSlot == EntityEquipmentSlot.HEAD && (ConfigHandler.client.useHelmetModel || DateEventHandler.isIronManEvent())) {
            /*RenderItemPneumaticHelmet.INSTANCE.render(entityLiving);

            RenderPlayer render = (RenderPlayer)Minecraft.getMinecraft().getRenderManager().entityRenderMap.get(EntityPlayer.class);
            ModelBiped model = armorSlot == 2 ? render.modelArmor : render.modelArmorChestplate;
            model.bipedHead.showModel = false;
            return model;*///TODO 1.8 fix
        }
        return null;
    }

    @Override
    public Set<Item> getApplicableUpgrades() {
        Set<Item> items = new HashSet<>();
        for (IUpgradeRenderHandler handler : UpgradeRenderHandlerList.instance().upgradeRenderers) {
            Collections.addAll(items, handler.getRequiredUpgrades());
        }
        items.add(CraftingRegistrator.getUpgrade(EnumUpgrade.SPEED).getItem());
        items.add(CraftingRegistrator.getUpgrade(EnumUpgrade.VOLUME).getItem());
        items.add(CraftingRegistrator.getUpgrade(EnumUpgrade.RANGE).getItem());
        items.add(CraftingRegistrator.getUpgrade(EnumUpgrade.SECURITY).getItem());
        if (Loader.isModLoaded(ModIds.THAUMCRAFT)) {
            items.add(CraftingRegistrator.getUpgrade(EnumUpgrade.THAUMCRAFT).getItem());
        }
        return items;
    }

    @Override
    public String getName() {
        return getUnlocalizedName() + ".name";
    }

    private boolean hasThaumcraftUpgradeAndPressure(ItemStack stack) {
        return hasSufficientPressure(stack) && UpgradableItemUtils.getUpgrades(EnumUpgrade.THAUMCRAFT, stack) > 0;
    }

    @Override
    @Optional.Method(modid = ModIds.THAUMCRAFT)
    public int getVisDiscount(ItemStack stack, EntityPlayer player) {
        return hasThaumcraftUpgradeAndPressure(stack) ? 5 : 0;
    }

    @Override
    @Optional.Method(modid = ModIds.THAUMCRAFT)
    public boolean showIngamePopups(ItemStack itemstack, EntityLivingBase player) {
        return hasThaumcraftUpgradeAndPressure(itemstack);
    }

    @Override
    @Optional.Method(modid = ModIds.THAUMCRAFT)
    public boolean showNodes(ItemStack itemstack, EntityLivingBase player) {
        return hasThaumcraftUpgradeAndPressure(itemstack);
    }

}
