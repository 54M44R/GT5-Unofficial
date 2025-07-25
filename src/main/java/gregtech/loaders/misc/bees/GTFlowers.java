package gregtech.loaders.misc.bees;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;

import forestry.api.apiculture.FlowerManager;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleFlowers;
import forestry.api.genetics.IChromosomeType;
import forestry.api.genetics.IFlower;
import forestry.api.genetics.IFlowerProvider;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.IPollinatable;
import forestry.api.genetics.ISpeciesRoot;
import gregtech.api.enums.GTValues;
import gregtech.api.util.GTLanguageManager;

public enum GTFlowers implements IFlowerProvider, IAlleleFlowers, IChromosomeType {

    FLAMING;

    private final boolean dominant;

    GTFlowers() {
        dominant = true;
    }

    public static void doInit() {
        for (GTFlowers effect : values()) {
            effect.register();
        }
    }

    @Override
    public String getUID() {
        return "for.flowers." + toString().toLowerCase();
    }

    @Override
    public boolean isDominant() {
        return dominant;
    }

    @Override
    public IFlowerProvider getProvider() {
        return this;
    }

    @Override
    public String getDescription() {
        return GTLanguageManager.getTranslation("for.flowers." + name().toLowerCase());
    }

    public void register() {
        for (ItemStack stack : getItemStacks()) {
            FlowerManager.flowerRegistry.registerAcceptableFlower(Block.getBlockFromItem(stack.getItem()), getUID());
        }

        AlleleManager.alleleRegistry.registerAllele(this, this);
    }

    public ItemStack[] getItemStacks() {
        if (this == GTFlowers.FLAMING) {
            return new ItemStack[] { new ItemStack(Blocks.fire) };
        }
        return GTValues.emptyItemStackArray;
    }

    @Override
    public boolean isAcceptedPollinatable(World world, IPollinatable pollinatable) {
        EnumSet<EnumPlantType> types = pollinatable.getPlantType();
        return types.size() > 1 || !types.contains(EnumPlantType.Nether);
    }

    public boolean isAcceptedFlower(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block == null) {
            return false;
        }

        if (this == GTFlowers.FLAMING) {
            return block == Blocks.fire;
        }
        return false;
    }

    @Override
    public boolean growFlower(World world, IIndividual individual, int x, int y, int z) {
        return false;
    }

    @Override
    public ItemStack[] affectProducts(World world, IIndividual individual, int x, int y, int z, ItemStack[] products) {
        return products;
    }

    @Override
    public String getName() {
        return getDescription();
    }

    @Override
    public String getUnlocalizedName() {
        return getUID();
    }

    @Override
    public String getFlowerType() {
        return getUID();
    }

    @Override
    public Set<IFlower> getFlowers() {
        return new HashSet<>();
    }

    @Override
    public Class<? extends IAllele> getAlleleClass() {
        return getClass();
    }

    @Override
    public ISpeciesRoot getSpeciesRoot() {
        return AlleleManager.alleleRegistry.getSpeciesRoot(getUID());
    }
}
