package gregtech.common.blocks;

import static com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler.translatedText;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.util.AnimatedTooltipHandler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IItemContainer;
import gregtech.api.items.GTGenericBlock;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTLanguageManager;
import gregtech.common.render.GTRendererCasing;

/**
 * The base class for casings. Casings are the blocks that are mainly used to build multiblocks.
 */
public abstract class BlockCasingsAbstract extends GTGenericBlock
    implements gregtech.api.interfaces.IHasIndexedTexture {

    public static final Supplier<String> NO_MOB_SPAWNING = translatedText("gt.casing.no-mob-spawning");
    public static final Supplier<String> NOT_TILE_ENTITY = translatedText("gt.casing.not-tile-entity");
    public static final Supplier<String> BLAST_PROOF = translatedText("gt.casing.blast-proof");

    public BlockCasingsAbstract(Class<? extends ItemBlock> aItemClass, String aName, Material aMaterial) {
        super(aItemClass, aName, aMaterial);
        setStepSound(soundTypeMetal);
        setCreativeTab(GregTechAPI.TAG_GREGTECH_CASINGS);
        GregTechAPI.registerMachineBlock(this, -1);
        GTLanguageManager.addStringLocalization(getUnlocalizedName() + "." + 32767 + ".name", "Any Sub Block of this");
    }

    public BlockCasingsAbstract(Class<? extends ItemBlock> aItemClass, String aName, Material aMaterial, int aMaxMeta) {
        this(aItemClass, aName, aMaterial);
        for (int i = 0; i < aMaxMeta; i++) {
            Textures.BlockIcons.setCasingTextureForId(getTextureIndex(i), TextureFactory.of(this, i));
        }
    }

    @Override
    public int getRenderType() {
        return GTRendererCasing.mRenderID;
    }

    @Override
    public String getHarvestTool(int aMeta) {
        return "wrench";
    }

    @Override
    public int getHarvestLevel(int aMeta) {
        return 2;
    }

    @Override
    public float getBlockHardness(World aWorld, int aX, int aY, int aZ) {
        return Blocks.iron_block.getBlockHardness(aWorld, aX, aY, aZ);
    }

    @Override
    public float getExplosionResistance(Entity aTNT) {
        return Blocks.iron_block.getExplosionResistance(aTNT);
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public void onBlockAdded(World aWorld, int aX, int aY, int aZ) {
        if (GregTechAPI.isMachineBlock(this, aWorld.getBlockMetadata(aX, aY, aZ))) {
            GregTechAPI.causeMachineUpdate(aWorld, aX, aY, aZ);
        }
    }

    @Override
    public String getUnlocalizedName() {
        return this.mUnlocalizedName;
    }

    @Override
    public String getLocalizedName() {
        return StatCollector.translateToLocal(this.mUnlocalizedName + ".name");
    }

    @Override
    public boolean canBeReplacedByLeaves(IBlockAccess aWorld, int aX, int aY, int aZ) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockAccess aWorld, int aX, int aY, int aZ) {
        return true;
    }

    @Override
    public void breakBlock(World aWorld, int aX, int aY, int aZ, Block aBlock, int aMetaData) {
        if (GregTechAPI.isMachineBlock(this, aMetaData)) {
            GregTechAPI.causeMachineUpdate(aWorld, aX, aY, aZ);
        }
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata;
    }

    @Override
    public int getDamageValue(World aWorld, int aX, int aY, int aZ) {
        return aWorld.getBlockMetadata(aX, aY, aZ);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister aIconRegister) {}

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item aItem, CreativeTabs aCreativeTab, List<ItemStack> aList) {
        for (int i = 0; i < 16; i++) {
            ItemStack aStack = new ItemStack(aItem, 1, i);
            if (!aStack.getDisplayName()
                .contains(".name")) aList.add(aStack);
        }
    }

    /**
     * Provide a fallback to subclasses in addons.
     */
    @Override
    public int getTextureIndex(int aMeta) {
        return Textures.BlockIcons.ERROR_TEXTURE_INDEX;
    }

    @Override
    public int colorMultiplier(IBlockAccess aWorld, int aX, int aY, int aZ) {
        return gregtech.api.enums.Dyes.MACHINE_METAL.toInt();
    }

    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advancedTooltips) {
        // add whatever dynamic info you need in the subclass
    }

    protected void register(int meta, @Nullable IItemContainer container, @Nonnull String defaultLocalName) {
        register(meta, container, defaultLocalName, (Supplier<String>) null);
    }

    @SafeVarargs
    protected final void register(int meta, @Nullable IItemContainer container, @Nonnull String defaultLocalName,
        @Nullable Supplier<String>... tooltips) {
        ItemStack stack = new ItemStack(this, 1, meta);

        GTLanguageManager.addStringLocalization(getUnlocalizedName() + "." + meta + ".name", defaultLocalName);

        if (container != null) {
            container.set(stack.copy());
        }

        if (tooltips != null) {
            for (Supplier<String> tooltip : tooltips) {
                AnimatedTooltipHandler.addItemTooltip(stack, tooltip);
            }
        }
    }
}
