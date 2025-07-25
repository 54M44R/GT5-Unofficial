package gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base;

import static gregtech.api.util.GTUtility.validMTEList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizons.modularui.api.drawable.ItemDrawable;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.StructureError;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoidingMode;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.MetaGeneratedTool;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.metatileentity.implementations.MTEHatchMuffler;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.IGTHatchAdder;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gtPlusPlus.GTplusplus;
import gtPlusPlus.GTplusplus.INIT_PHASE;
import gtPlusPlus.api.objects.Logger;
import gtPlusPlus.api.objects.minecraft.BlockPos;
import gtPlusPlus.core.config.ASMConfiguration;
import gtPlusPlus.core.util.minecraft.ItemUtils;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.METHatchAirIntake;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchInputBattery;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.MTEHatchOutputBattery;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;

// Glee8e - 11/12/21 - 2:15pm
// Yeah, now I see what's wrong. Someone inherited from GTPPMultiBlockBase instead of
// GTPPMultiBlockBase<MTEIndustrialDehydrator> as it should have been
// so any method in MTEIndustrialDehydrator would see generic field declared in
// GTPPMultiBlockBase without generic parameter

public abstract class GTPPMultiBlockBase<T extends MTEExtendedPowerMultiBlockBase<T>>
    extends MTEExtendedPowerMultiBlockBase<T> {

    public static final boolean DEBUG_DISABLE_CORES_TEMPORARILY = true;

    public GTRecipe mLastRecipe;

    /**
     * Don't use this for recipe input check, otherwise you'll get duplicated fluids
     */
    public ArrayList<METHatchAirIntake> mAirIntakes = new ArrayList<>();

    public ArrayList<MTEHatchInputBattery> mChargeHatches = new ArrayList<>();
    public ArrayList<MTEHatchOutputBattery> mDischargeHatches = new ArrayList<>();
    public ArrayList<MTEHatch> mAllEnergyHatches = new ArrayList<>();
    public ArrayList<MTEHatch> mAllDynamoHatches = new ArrayList<>();

    public GTPPMultiBlockBase(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GTPPMultiBlockBase(final String aName) {
        super(aName);
    }

    private static int toStackCount(Entry<ItemStack, Integer> e) {
        int tMaxStackSize = e.getKey()
            .getMaxStackSize();
        int tStackSize = e.getValue();
        return (tStackSize + tMaxStackSize - 1) / tMaxStackSize;
    }

    public abstract String getMachineType();

    public String getMachineTooltip() {
        return "Machine Type: " + EnumChatFormatting.YELLOW + getMachineType() + EnumChatFormatting.RESET;
    }

    public String[] getExtraInfoData() {
        return GTValues.emptyStringArray;
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> mInfo = new ArrayList<>();
        if (!this.getMetaName()
            .isEmpty()) {
            mInfo.add(this.getMetaName());
        }

        String[] extra = getExtraInfoData();

        if (extra == null) {
            extra = GTValues.emptyStringArray;
        }
        mInfo.addAll(Arrays.asList(extra));

        long seconds = (this.mTotalRunTime / 20);
        int weeks = (int) (TimeUnit.SECONDS.toDays(seconds) / 7);
        int days = (int) (TimeUnit.SECONDS.toDays(seconds) - 7 * weeks);
        long hours = TimeUnit.SECONDS.toHours(seconds) - TimeUnit.DAYS.toHours(days)
            - TimeUnit.DAYS.toHours(7L * weeks);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        mInfo.add(getMachineTooltip());

        // Lets borrow the GTNH handling

        mInfo.add(
            StatCollector.translateToLocal("GTPP.multiblock.progress") + ": "
                + EnumChatFormatting.GREEN
                + mProgresstime / 20
                + EnumChatFormatting.RESET
                + " s / "
                + EnumChatFormatting.YELLOW
                + mMaxProgresstime / 20
                + EnumChatFormatting.RESET
                + " s");

        if (!this.mAllEnergyHatches.isEmpty()) {
            long storedEnergy = getStoredEnergyInAllEnergyHatches();
            long maxEnergy = getMaxEnergyStorageOfAllEnergyHatches();
            mInfo.add(StatCollector.translateToLocal("GTPP.multiblock.energy") + ":");
            mInfo.add(
                StatCollector.translateToLocal(
                    EnumChatFormatting.GREEN.toString() + storedEnergy
                        + EnumChatFormatting.RESET
                        + " EU / "
                        + EnumChatFormatting.YELLOW
                        + maxEnergy
                        + EnumChatFormatting.RESET
                        + " EU"));

            mInfo.add(StatCollector.translateToLocal("GTPP.multiblock.mei") + ":");
            mInfo.add(
                StatCollector.translateToLocal(
                    EnumChatFormatting.YELLOW.toString() + getMaxInputVoltage()
                        + EnumChatFormatting.RESET
                        + " EU/t(*2A) "
                        + StatCollector.translateToLocal("GTPP.machines.tier")
                        + ": "
                        + EnumChatFormatting.YELLOW
                        + GTValues.VN[GTUtility.getTier(getMaxInputVoltage())]
                        + EnumChatFormatting.RESET));
        }
        if (!this.mAllDynamoHatches.isEmpty()) {
            long storedEnergy = getStoredEnergyInAllDynamoHatches();
            long maxEnergy = getMaxEnergyStorageOfAllDynamoHatches();
            mInfo.add(StatCollector.translateToLocal("GTPP.multiblock.energy") + " In Dynamos:");
            mInfo.add(
                StatCollector.translateToLocal(
                    EnumChatFormatting.GREEN.toString() + storedEnergy
                        + EnumChatFormatting.RESET
                        + " EU / "
                        + EnumChatFormatting.YELLOW
                        + maxEnergy
                        + EnumChatFormatting.RESET
                        + " EU"));
        }

        if (-lEUt > 0) {
            mInfo.add(StatCollector.translateToLocal("GTPP.multiblock.usage") + ":");
            mInfo.add(
                StatCollector
                    .translateToLocal("" + EnumChatFormatting.RED + (-lEUt) + EnumChatFormatting.RESET + " EU/t"));
        } else {
            mInfo.add(StatCollector.translateToLocal("GTPP.multiblock.generation") + ":");
            mInfo.add(
                StatCollector
                    .translateToLocal("" + EnumChatFormatting.GREEN + lEUt + EnumChatFormatting.RESET + " EU/t"));
        }

        mInfo.add(
            StatCollector.translateToLocal("GTPP.multiblock.problems") + ": "
                + EnumChatFormatting.RED
                + (getIdealStatus() - getRepairStatus())
                + EnumChatFormatting.RESET
                + " "
                + StatCollector.translateToLocal("GTPP.multiblock.efficiency")
                + ": "
                + EnumChatFormatting.YELLOW
                + mEfficiency / 100.0F
                + EnumChatFormatting.RESET
                + " %");

        if (this.getPollutionPerSecond(null) > 0) {
            mInfo.add(
                StatCollector.translateToLocal("GTPP.multiblock.pollution") + ": "
                    + EnumChatFormatting.RED
                    + this.getPollutionPerSecond(null)
                    + EnumChatFormatting.RESET
                    + "/sec");
            mInfo.add(
                StatCollector.translateToLocal("GTPP.multiblock.pollutionreduced") + ": "
                    + EnumChatFormatting.GREEN
                    + getAveragePollutionPercentage()
                    + EnumChatFormatting.RESET
                    + " %");
        }

        mInfo.add(
            StatCollector.translateToLocal("GTPP.CC.parallel") + ": "
                + EnumChatFormatting.GREEN
                + (getMaxParallelRecipes())
                + EnumChatFormatting.RESET);

        mInfo.add(
            StatCollector.translateToLocalFormatted(
                "gtpp.infodata.multi_block.total_time",
                "" + EnumChatFormatting.DARK_GREEN + weeks + EnumChatFormatting.RESET,
                "" + EnumChatFormatting.DARK_GREEN + days + EnumChatFormatting.RESET));
        mInfo.add(
            StatCollector.translateToLocalFormatted(
                "gtpp.infodata.multi_block.total_time.0",
                EnumChatFormatting.DARK_GREEN + Long.toString(hours) + EnumChatFormatting.RESET,
                EnumChatFormatting.DARK_GREEN + Long.toString(minutes) + EnumChatFormatting.RESET,
                EnumChatFormatting.DARK_GREEN + Long.toString(second) + EnumChatFormatting.RESET));
        mInfo.add(
            StatCollector.translateToLocalFormatted(
                "gtpp.infodata.multi_block.total_time.in_ticks",
                "" + EnumChatFormatting.DARK_GREEN + this.mTotalRunTime));

        return mInfo.toArray(new String[0]);
    }

    public long getStoredEnergyInAllEnergyHatches() {
        long storedEnergy = 0;
        for (MTEHatch tHatch : validMTEList(mAllEnergyHatches)) {
            storedEnergy += tHatch.getBaseMetaTileEntity()
                .getStoredEU();
        }
        return storedEnergy;
    }

    public long getMaxEnergyStorageOfAllEnergyHatches() {
        long maxEnergy = 0;
        for (MTEHatch tHatch : validMTEList(mAllEnergyHatches)) {
            maxEnergy += tHatch.getBaseMetaTileEntity()
                .getEUCapacity();
        }
        return maxEnergy;
    }

    public long getStoredEnergyInAllDynamoHatches() {
        long storedEnergy = 0;
        for (MTEHatch tHatch : validMTEList(mAllDynamoHatches)) {
            storedEnergy += tHatch.getBaseMetaTileEntity()
                .getStoredEU();
        }
        return storedEnergy;
    }

    public long getMaxEnergyStorageOfAllDynamoHatches() {
        long maxEnergy = 0;
        for (MTEHatch tHatch : validMTEList(mAllDynamoHatches)) {
            maxEnergy += tHatch.getBaseMetaTileEntity()
                .getEUCapacity();
        }
        return maxEnergy;
    }

    private String[] aCachedToolTip;

    /*
     * private final String aRequiresMuffler = "1x Muffler Hatch"; private final String aRequiresCoreModule =
     * "1x Core Module"; private final String aRequiresMaint = "1x Maintanence Hatch";
     */

    public static final String TAG_HIDE_HATCHES = "TAG_HIDE_HATCHES";
    public static final String TAG_HIDE_MAINT = "TAG_HIDE_MAINT";
    public static final String TAG_HIDE_POLLUTION = "TAG_HIDE_POLLUTION";
    public static final String TAG_HIDE_MACHINE_TYPE = "TAG_HIDE_MACHINE_TYPE";

    /**
     * A Static {@link Method} object which holds the current status of logging.
     */
    public static Method aLogger = null;

    public void log(String s) {
        if (!ASMConfiguration.debug.disableAllLogging) {
            if (ASMConfiguration.debug.debugMode) {
                Logger.INFO(s);
            } else {
                Logger.MACHINE_INFO(s);
            }
        }
    }

    public long getMaxInputEnergy() {
        long rEnergy = 0;
        if (mEnergyHatches.size() == 1) // so it only takes 1 amp is only 1 hatch is present so it works like most gt
                                        // multies
            return mEnergyHatches.get(0)
                .getBaseMetaTileEntity()
                .getInputVoltage();
        for (MTEHatchEnergy tHatch : validMTEList(mEnergyHatches)) rEnergy += tHatch.getBaseMetaTileEntity()
            .getInputVoltage()
            * tHatch.getBaseMetaTileEntity()
                .getInputAmperage();
        return rEnergy;
    }

    public boolean isMachineRunning() {
        return this.getBaseMetaTileEntity()
            .isActive();
    }

    @Override
    public void explodeMultiblock() {
        MetaTileEntity tTileEntity;
        for (final Iterator<MTEHatchInputBattery> localIterator = this.mChargeHatches.iterator(); localIterator
            .hasNext(); tTileEntity.getBaseMetaTileEntity()
                .doExplosion(GTValues.V[8])) {
            tTileEntity = localIterator.next();
        }
        for (final Iterator<MTEHatchOutputBattery> localIterator = this.mDischargeHatches.iterator(); localIterator
            .hasNext(); tTileEntity.getBaseMetaTileEntity()
                .doExplosion(GTValues.V[8])) {
            tTileEntity = localIterator.next();
        }
        for (final Iterator<MTEHatch> localIterator = this.mTecTechDynamoHatches.iterator(); localIterator
            .hasNext(); tTileEntity.getBaseMetaTileEntity()
                .doExplosion(GTValues.V[8])) {
            tTileEntity = localIterator.next();
        }
        for (final Iterator<MTEHatch> localIterator = this.mTecTechEnergyHatches.iterator(); localIterator
            .hasNext(); tTileEntity.getBaseMetaTileEntity()
                .doExplosion(GTValues.V[8])) {
            tTileEntity = localIterator.next();
        }

        super.explodeMultiblock();
    }

    protected boolean setGUIItemStack(ItemStack aNewGuiSlotContents) {
        boolean result = false;
        if (this.mInventory[1] == null) {
            this.mInventory[1] = aNewGuiSlotContents != null ? aNewGuiSlotContents.copy() : null;
            this.depleteInput(aNewGuiSlotContents);
            this.updateSlots();
            result = true;
        }
        return result;
    }

    /**
     * Deplete fluid input from a set of restricted hatches. This assumes these hatches can store nothing else but your
     * expected fluid
     */
    protected boolean depleteInputFromRestrictedHatches(Collection<MTEHatchCustomFluidBase> aHatches, int aAmount) {
        for (final MTEHatchCustomFluidBase tHatch : validMTEList(aHatches)) {
            FluidStack tLiquid = tHatch.getFluid();
            if (tLiquid == null || tLiquid.amount < aAmount) {
                continue;
            }
            tLiquid = tHatch.drain(aAmount, false);
            if (tLiquid != null && tLiquid.amount >= aAmount) {
                tLiquid = tHatch.drain(aAmount, true);
                return tLiquid != null && tLiquid.amount >= aAmount;
            }
        }
        return false;
    }

    @Override
    public void updateSlots() {
        for (final MTEHatchInputBattery tHatch : validMTEList(this.mChargeHatches)) {
            tHatch.updateSlots();
        }
        for (final MTEHatchOutputBattery tHatch : validMTEList(this.mDischargeHatches)) {
            tHatch.updateSlots();
        }
        super.updateSlots();
    }

    @Override
    protected void localizeStructureErrors(Collection<StructureError> errors, NBTTagCompound context,
        List<String> lines) {
        super.localizeStructureErrors(errors, context, lines);

        if (errors.contains(StructureError.MISSING_MAINTENANCE)) {
            lines.add(StatCollector.translateToLocal("GT5U.gui.text.no_maintenance"));
        }

        if (errors.contains(StructureError.MISSING_MUFFLER)) {
            lines.add(StatCollector.translateToLocal("GT5U.gui.text.no_muffler"));
        }

        if (errors.contains(StructureError.UNNEEDED_MUFFLER)) {
            lines.add(StatCollector.translateToLocal("GT5U.gui.text.unneeded_muffler"));
        }
    }

    @Override
    protected void validateStructure(Collection<StructureError> errors, NBTTagCompound context) {
        super.validateStructure(errors, context);

        if (shouldCheckMaintenance() && mMaintenanceHatches.isEmpty()) {
            errors.add(StructureError.MISSING_MAINTENANCE);
        }

        if (this.getPollutionPerSecond(null) > 0 && mMufflerHatches.isEmpty()) {
            errors.add(StructureError.MISSING_MUFFLER);
        }

        if (this.getPollutionPerSecond(null) == 0 && !mMufflerHatches.isEmpty()) {
            errors.add(StructureError.UNNEEDED_MUFFLER);
        }
    }

    public boolean checkHatch() {
        return true;
    }

    @Override
    public void clearHatches() {
        super.clearHatches();
        this.mChargeHatches.clear();
        this.mDischargeHatches.clear();
        this.mAirIntakes.clear();
        this.mTecTechEnergyHatches.clear();
        this.mTecTechDynamoHatches.clear();
        this.mAllEnergyHatches.clear();
        this.mAllDynamoHatches.clear();
    }

    public <E> boolean addToMachineListInternal(ArrayList<E> aList, final IGregTechTileEntity aTileEntity,
        final int aBaseCasingIndex) {
        return addToMachineListInternal(aList, getMetaTileEntity(aTileEntity), aBaseCasingIndex);
    }

    public <E> boolean addToMachineListInternal(ArrayList<E> aList, final IMetaTileEntity aTileEntity,
        final int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        }

        // Check type
        /*
         * Class <?> aHatchType = ReflectionUtils.getTypeOfGenericObject(aList); if
         * (!aHatchType.isInstance(aTileEntity)) { return false; }
         */

        // Try setRecipeMap

        try {
            if (aTileEntity instanceof MTEHatchInput) {
                resetRecipeMapForHatch((MTEHatch) aTileEntity, getRecipeMap());
            }
            if (aTileEntity instanceof MTEHatchInputBus) {
                resetRecipeMapForHatch((MTEHatch) aTileEntity, getRecipeMap());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (aList.isEmpty()) {
            if (aTileEntity instanceof MTEHatch) {
                if (GTplusplus.CURRENT_LOAD_PHASE == INIT_PHASE.STARTED) {
                    log(
                        "Adding " + aTileEntity.getInventoryName()
                            + " at "
                            + new BlockPos(aTileEntity.getBaseMetaTileEntity()).getLocationString());
                }
                updateTexture(aTileEntity, aBaseCasingIndex);
                return aList.add((E) aTileEntity);
            }
        } else {
            IGregTechTileEntity aCur = aTileEntity.getBaseMetaTileEntity();
            if (aList.contains(aTileEntity)) {
                log(
                    "Found Duplicate " + aTileEntity.getInventoryName()
                        + " @ "
                        + new BlockPos(aCur).getLocationString());
                return false;
            }
            BlockPos aCurPos = new BlockPos(aCur);
            boolean aExists = false;
            for (E m : aList) {
                IGregTechTileEntity b = ((IMetaTileEntity) m).getBaseMetaTileEntity();
                if (b != null) {
                    BlockPos aPos = new BlockPos(b);
                    if (aCurPos.equals(aPos)) {
                        if (GTplusplus.CURRENT_LOAD_PHASE == INIT_PHASE.STARTED) {
                            log("Found Duplicate " + b.getInventoryName() + " at " + aPos.getLocationString());
                        }
                        return false;
                    }
                }
            }
            if (aTileEntity instanceof MTEHatch) {
                if (GTplusplus.CURRENT_LOAD_PHASE == INIT_PHASE.STARTED) {
                    log("Adding " + aCur.getInventoryName() + " at " + aCurPos.getLocationString());
                }
                updateTexture(aTileEntity, aBaseCasingIndex);
                return aList.add((E) aTileEntity);
            }
        }
        return false;
    }

    private IMetaTileEntity getMetaTileEntity(final IGregTechTileEntity aTileEntity) {
        if (aTileEntity == null) {
            return null;
        }
        return aTileEntity.getMetaTileEntity();
    }

    @Override
    public boolean addToMachineList(final IGregTechTileEntity aTileEntity, final int aBaseCasingIndex) {
        return addToMachineList(getMetaTileEntity(aTileEntity), aBaseCasingIndex);
    }

    public boolean addToMachineList(final IMetaTileEntity aMetaTileEntity, final int aBaseCasingIndex) {
        if (aMetaTileEntity == null) {
            return false;
        }

        // Use this to determine the correct value, then update the hatch texture after.
        boolean aDidAdd = false;

        // Handle Custom Hatches
        if (aMetaTileEntity instanceof MTEHatchInputBattery) {
            log("Found MTEHatchInputBattery");
            aDidAdd = addToMachineListInternal(mChargeHatches, aMetaTileEntity, aBaseCasingIndex);
        } else if (aMetaTileEntity instanceof MTEHatchOutputBattery) {
            log("Found MTEHatchOutputBattery");
            aDidAdd = addToMachineListInternal(mDischargeHatches, aMetaTileEntity, aBaseCasingIndex);
        } else if (aMetaTileEntity instanceof METHatchAirIntake) {
            aDidAdd = addToMachineListInternal(mAirIntakes, aMetaTileEntity, aBaseCasingIndex)
                && addToMachineListInternal(mInputHatches, aMetaTileEntity, aBaseCasingIndex);
        }

        // Handle TT Multi-A Energy Hatches
        else if (isThisHatchMultiEnergy(aMetaTileEntity)) {
            log("Found isThisHatchMultiEnergy");
            aDidAdd = addToMachineListInternal(mTecTechEnergyHatches, aMetaTileEntity, aBaseCasingIndex);
            updateMasterEnergyHatchList(aMetaTileEntity);
        }

        // Handle TT Multi-A Dynamos
        else if (isThisHatchMultiDynamo(aMetaTileEntity)) {
            log("Found isThisHatchMultiDynamo");
            aDidAdd = addToMachineListInternal(mTecTechDynamoHatches, aMetaTileEntity, aBaseCasingIndex);
            updateMasterDynamoHatchList(aMetaTileEntity);
        }

        // Handle Fluid Hatches using seperate logic
        else if (aMetaTileEntity instanceof MTEHatchInput)
            aDidAdd = addToMachineListInternal(mInputHatches, aMetaTileEntity, aBaseCasingIndex);
        else if (aMetaTileEntity instanceof MTEHatchOutput)
            aDidAdd = addToMachineListInternal(mOutputHatches, aMetaTileEntity, aBaseCasingIndex);

        // Process Remaining hatches using Vanilla GT Logic
        else if (aMetaTileEntity instanceof IDualInputHatch hatch) {
            hatch.updateCraftingIcon(this.getMachineCraftingIcon());
            aDidAdd = addToMachineListInternal(mDualInputHatches, aMetaTileEntity, aBaseCasingIndex);
        } else if (aMetaTileEntity instanceof MTEHatchInputBus)
            aDidAdd = addToMachineListInternal(mInputBusses, aMetaTileEntity, aBaseCasingIndex);
        else if (aMetaTileEntity instanceof MTEHatchOutputBus)
            aDidAdd = addToMachineListInternal(mOutputBusses, aMetaTileEntity, aBaseCasingIndex);
        else if (aMetaTileEntity instanceof MTEHatchEnergy) {
            aDidAdd = addToMachineListInternal(mEnergyHatches, aMetaTileEntity, aBaseCasingIndex);
            updateMasterEnergyHatchList(aMetaTileEntity);
        } else if (aMetaTileEntity instanceof MTEHatchDynamo) {
            aDidAdd = addToMachineListInternal(mDynamoHatches, aMetaTileEntity, aBaseCasingIndex);
            updateMasterDynamoHatchList(aMetaTileEntity);
        } else if (aMetaTileEntity instanceof MTEHatchMaintenance)
            aDidAdd = addToMachineListInternal(mMaintenanceHatches, aMetaTileEntity, aBaseCasingIndex);
        else if (aMetaTileEntity instanceof MTEHatchMuffler)
            aDidAdd = addToMachineListInternal(mMufflerHatches, aMetaTileEntity, aBaseCasingIndex);

        // return super.addToMachineList(aTileEntity, aBaseCasingIndex);
        return aDidAdd;
    }

    @Override
    public boolean addMaintenanceToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity instanceof MTEHatchMaintenance) {
            return addToMachineList(aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    @Override
    public boolean addMufflerToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity instanceof MTEHatchMuffler) {
            return addToMachineList(aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    @Override
    public boolean addInputToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity instanceof MTEHatchInput || aMetaTileEntity instanceof MTEHatchInputBus) {
            return addToMachineList(aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    @Override
    public boolean addOutputToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity instanceof MTEHatchOutput || aMetaTileEntity instanceof MTEHatchOutputBus) {
            return addToMachineList(aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    public boolean addAirIntakeToMachineList(final IGregTechTileEntity aTileEntity, final int aBaseCasingIndex) {
        IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity instanceof METHatchAirIntake) {
            return addToMachineList(aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    public boolean addFluidInputToMachineList(final IGregTechTileEntity aTileEntity, final int aBaseCasingIndex) {
        return addFluidInputToMachineList(getMetaTileEntity(aTileEntity), aBaseCasingIndex);
    }

    public boolean addFluidInputToMachineList(final IMetaTileEntity aMetaTileEntity, final int aBaseCasingIndex) {
        if (aMetaTileEntity instanceof MTEHatchInput) {
            return addToMachineList(aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    public boolean clearRecipeMapForAllInputHatches() {
        return resetRecipeMapForAllInputHatches(null);
    }

    public boolean resetRecipeMapForAllInputHatches() {
        return resetRecipeMapForAllInputHatches(this.getRecipeMap());
    }

    public boolean resetRecipeMapForAllInputHatches(RecipeMap<?> aMap) {
        int cleared = 0;
        for (MTEHatchInput g : this.mInputHatches) {
            if (resetRecipeMapForHatch(g, aMap)) {
                cleared++;
            }
        }
        for (MTEHatchInputBus g : this.mInputBusses) {
            if (resetRecipeMapForHatch(g, aMap)) {
                cleared++;
            }
        }
        return cleared > 0;
    }

    public boolean resetRecipeMapForHatch(IGregTechTileEntity aTileEntity, RecipeMap<?> aMap) {
        try {
            final IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
            if (aMetaTileEntity == null) {
                return false;
            }
            if (aMetaTileEntity instanceof MTEHatchInput || aMetaTileEntity instanceof MTEHatchInputBus) {
                return resetRecipeMapForHatch((MTEHatch) aMetaTileEntity, aMap);
            } else {
                return false;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public boolean resetRecipeMapForHatch(MTEHatch aTileEntity, RecipeMap<?> aMap) {
        if (aTileEntity == null) {
            return false;
        }
        if (aTileEntity instanceof MTEHatchInput || aTileEntity instanceof MTEHatchInputBus) {
            if (aTileEntity instanceof MTEHatchInput) {
                ((MTEHatchInput) aTileEntity).mRecipeMap = null;
                ((MTEHatchInput) aTileEntity).mRecipeMap = aMap;
                if (aMap != null) {
                    log("Remapped Input Hatch to " + aMap.unlocalizedName + ".");
                } else {
                    log("Cleared Input Hatch.");
                }
            } else {
                ((MTEHatchInputBus) aTileEntity).mRecipeMap = null;
                ((MTEHatchInputBus) aTileEntity).mRecipeMap = aMap;
                if (aMap != null) {
                    log("Remapped Input Bus to " + aMap.unlocalizedName + ".");
                } else {
                    log("Cleared Input Bus.");
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        super.onScrewdriverRightClick(side, aPlayer, aX, aY, aZ, aTool);
        clearRecipeMapForAllInputHatches();
        onModeChangeByScrewdriver(side, aPlayer, aX, aY, aZ);
        mLastRecipe = null;
        resetRecipeMapForAllInputHatches();
    }

    public void onModeChangeByScrewdriver(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ) {}

    /**
     * Enable Texture Casing Support if found in GT 5.09
     */
    public boolean updateTexture(final IGregTechTileEntity aTileEntity, int aCasingID) {
        return updateTexture(getMetaTileEntity(aTileEntity), aCasingID);
    }

    /**
     * Enable Texture Casing Support if found in GT 5.09
     */
    public boolean updateTexture(final IMetaTileEntity aTileEntity, int aCasingID) {
        if (aTileEntity instanceof MTEHatch mteHatch) {
            mteHatch.updateTexture(aCasingID);
            return true;
        }
        return false;
    }

    /**
     * TecTech Support
     */

    /**
     * This is the array Used to Store the Tectech Multi-Amp Dynamo hatches.
     */
    public ArrayList<MTEHatch> mTecTechDynamoHatches = new ArrayList<>();

    /**
     * This is the array Used to Store the Tectech Multi-Amp Energy hatches.
     */
    public ArrayList<MTEHatch> mTecTechEnergyHatches = new ArrayList<>();

    /**
     * TecTech Multi-Amp Dynamo Support
     *
     * @param aTileEntity      - The Dynamo Hatch
     * @param aBaseCasingIndex - Casing Texture
     * @return
     */
    public boolean addMultiAmpDynamoToMachineList(final IGregTechTileEntity aTileEntity, final int aBaseCasingIndex) {
        final IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity == null) {
            return false;
        }
        if (isThisHatchMultiDynamo(aTileEntity)) {
            return addToMachineListInternal(mTecTechDynamoHatches, aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    public boolean isThisHatchMultiDynamo(IGregTechTileEntity aTileEntity) {
        return isThisHatchMultiDynamo(getMetaTileEntity(aTileEntity));
    }

    public boolean isThisHatchMultiDynamo(IMetaTileEntity aMetaTileEntity) {
        return aMetaTileEntity instanceof MTEHatchDynamoMulti;
    }

    @Override
    public boolean addDynamoToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity instanceof MTEHatchDynamo || isThisHatchMultiDynamo(aMetaTileEntity)) {
            return addToMachineList(aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    private boolean updateMasterDynamoHatchList(IMetaTileEntity aMetaTileEntity) {
        if (aMetaTileEntity == null) {
            return false;
        }
        if (aMetaTileEntity instanceof MTEHatch aHatch) {
            return mAllDynamoHatches.add(aHatch);
        }
        return false;
    }

    /**
     * TecTech Multi-Amp Energy Hatch Support
     *
     * @param aTileEntity      - The Energy Hatch
     * @param aBaseCasingIndex - Casing Texture
     * @return
     */
    public boolean addMultiAmpEnergyToMachineList(final IGregTechTileEntity aTileEntity, final int aBaseCasingIndex) {
        final IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity == null) {
            return false;
        }
        if (isThisHatchMultiEnergy(aMetaTileEntity)) {
            return addToMachineListInternal(mTecTechEnergyHatches, aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    public boolean isThisHatchMultiEnergy(IGregTechTileEntity aTileEntity) {
        return isThisHatchMultiEnergy(getMetaTileEntity(aTileEntity));
    }

    public boolean isThisHatchMultiEnergy(IMetaTileEntity aMetaTileEntity) {
        return aMetaTileEntity instanceof MTEHatchEnergyMulti;
    }

    @Override
    public boolean addEnergyInputToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        IMetaTileEntity aMetaTileEntity = getMetaTileEntity(aTileEntity);
        if (aMetaTileEntity instanceof MTEHatchEnergy || isThisHatchMultiEnergy(aMetaTileEntity)) {
            return addToMachineList(aMetaTileEntity, aBaseCasingIndex);
        }
        return false;
    }

    private boolean updateMasterEnergyHatchList(IMetaTileEntity aMetaTileEntity) {
        if (aMetaTileEntity == null) {
            return false;
        }
        if (aMetaTileEntity instanceof MTEHatch aHatch) {
            return mAllEnergyHatches.add(aHatch);
        }
        return false;
    }

    /**
     * Pollution Management
     */
    public int calculatePollutionReductionForHatch(MTEHatchMuffler hatch, int poll) {
        return hatch.calculatePollutionReduction(poll);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        if (aNBT.hasKey("mVoidExcess")) {
            // backward compatibility
            voidingMode = aNBT.getBoolean("mVoidExcess") ? VoidingMode.VOID_ALL : VoidingMode.VOID_NONE;
        }
        if (aNBT.hasKey("mUseMultiparallelMode")) {
            // backward compatibility
            batchMode = aNBT.getBoolean("mUseMultiparallelMode");
        }
    }

    /**
     * Custom Tool Handling
     */
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer, ForgeDirection side,
        float aX, float aY, float aZ) {
        // Do Things
        if (this.getBaseMetaTileEntity()
            .isServerSide()) {
            // Logger.INFO("Right Clicked Controller.");
            ItemStack tCurrentItem = aPlayer.inventory.getCurrentItem();
            if (tCurrentItem != null) {
                // Logger.INFO("Holding Item.");
                if (tCurrentItem.getItem() instanceof MetaGeneratedTool) {
                    // Logger.INFO("Is MetaGeneratedTool.");
                    int[] aOreID = OreDictionary.getOreIDs(tCurrentItem);
                    for (int id : aOreID) {
                        // Plunger
                        if (OreDictionary.getOreName(id)
                            .equals("craftingToolPlunger")) {
                            // Logger.INFO("Is Plunger.");
                            return onPlungerRightClick(aPlayer, side, aX, aY, aZ);
                        }
                    }
                }
            }
        }
        // Do Super
        return super.onRightclick(aBaseMetaTileEntity, aPlayer, side, aX, aY, aZ);
    }

    public boolean onPlungerRightClick(EntityPlayer aPlayer, ForgeDirection side, float aX, float aY, float aZ) {
        int aHatchIndex = 0;
        GTUtility.sendChatToPlayer(aPlayer, "Trying to clear " + mOutputHatches.size() + " output hatches.");
        for (MTEHatchOutput hatch : this.mOutputHatches) {
            if (hatch.mFluid != null) {
                GTUtility.sendChatToPlayer(
                    aPlayer,
                    "Clearing " + hatch.mFluid.amount
                        + "L of "
                        + hatch.mFluid.getLocalizedName()
                        + " from hatch "
                        + aHatchIndex
                        + ".");
                hatch.mFluid = null;
            }
            aHatchIndex++;
        }
        return aHatchIndex > 0;
    }

    @Override
    public boolean onWireCutterRightClick(ForgeDirection side, ForgeDirection wrenchingSide, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool) {
        if (aPlayer.isSneaking()) {
            batchMode = !batchMode;
            if (batchMode) {
                GTUtility.sendChatToPlayer(aPlayer, StatCollector.translateToLocal("misc.BatchModeTextOn"));
            } else {
                GTUtility.sendChatToPlayer(aPlayer, StatCollector.translateToLocal("misc.BatchModeTextOff"));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onSolderingToolRightClick(ForgeDirection side, ForgeDirection wrenchingSide, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool) {
        if (supportsVoidProtection() && wrenchingSide == getBaseMetaTileEntity().getFrontFacing()) {
            Set<VoidingMode> allowed = getAllowedVoidingModes();
            setVoidingMode(getVoidingMode().nextInCollection(allowed));
            GTUtility.sendChatToPlayer(
                aPlayer,
                StatCollector.translateToLocal("GT5U.gui.button.voiding_mode") + " "
                    + StatCollector.translateToLocal(getVoidingMode().getTransKey()));
            return true;
        } else return super.onSolderingToolRightClick(side, wrenchingSide, aPlayer, aX, aY, aZ, aTool);
    }

    // Only support to use meta to tier

    /**
     * accept meta [0, maxMeta)
     *
     * @param maxMeta exclusive
     */
    public static <T> IStructureElement<T> addTieredBlock(Block aBlock, BiConsumer<T, Integer> aSetTheMeta,
        Function<T, Integer> aGetTheMeta, int maxMeta) {
        return addTieredBlock(aBlock, (t, i) -> {
            aSetTheMeta.accept(t, i);
            return true;
        }, aGetTheMeta, 0, maxMeta);
    }

    /**
     *
     * @param minMeta inclusive
     * @param maxMeta exclusive
     */
    public static <T> IStructureElement<T> addTieredBlock(Block aBlock, BiConsumer<T, Integer> aSetTheMeta,
        Function<T, Integer> aGetTheMeta, int minMeta, int maxMeta) {
        return addTieredBlock(aBlock, (t, i) -> {
            aSetTheMeta.accept(t, i);
            return true;
        }, aGetTheMeta, minMeta, maxMeta);
    }

    /**
     *
     * @param minMeta inclusive
     * @param maxMeta exclusive
     */
    public static <T> IStructureElement<T> addTieredBlock(Block aBlock, BiPredicate<T, Integer> aSetTheMeta,
        Function<T, Integer> aGetTheMeta, int minMeta, int maxMeta) {

        return new IStructureElement<>() {

            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                Block tBlock = world.getBlock(x, y, z);
                if (aBlock == tBlock) {
                    Integer currentMeta = aGetTheMeta.apply(t);
                    int newMeta = tBlock.getDamageValue(world, x, y, z) + 1;
                    if (newMeta > maxMeta || newMeta < minMeta + 1) return false;
                    if (currentMeta == 0) {
                        return aSetTheMeta.test(t, newMeta);
                    } else {
                        return currentMeta == newMeta;
                    }
                }
                return false;
            }

            @Override
            public boolean couldBeValid(T t, World world, int x, int y, int z, ItemStack trigger) {
                Block tBlock = world.getBlock(x, y, z);
                if (aBlock == tBlock) {
                    int expectedMeta = getMeta(trigger);
                    int blockMeta = tBlock.getDamageValue(world, x, y, z) + 1;
                    if (blockMeta > maxMeta || blockMeta < minMeta + 1) return false;
                    return expectedMeta == blockMeta;
                }
                return false;
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, aBlock, getMeta(trigger));
                return true;
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                return world.setBlock(x, y, z, aBlock, getMeta(trigger), 3);
            }

            private int getMeta(ItemStack trigger) {
                int meta = trigger.stackSize;
                if (meta <= 0) meta = minMeta;
                if (meta + minMeta >= maxMeta) meta = maxMeta - 1 - minMeta;
                return meta + minMeta;
            }

            @Nullable
            @Override
            public BlocksToPlace getBlocksToPlace(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                return BlocksToPlace.create(aBlock, getMeta(trigger));
            }

            @Override
            public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                if (world.getBlock(x, y, z) == aBlock) {
                    if (world.getBlockMetadata(x, y, z) == getMeta(trigger)) {
                        return PlaceResult.SKIP;
                    }
                    return PlaceResult.REJECT;
                }
                return StructureUtility.survivalPlaceBlock(
                    aBlock,
                    getMeta(trigger),
                    world,
                    x,
                    y,
                    z,
                    env.getSource(),
                    env.getActor(),
                    env.getChatter());
            }
        };
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int aColorIndex, boolean aActive, boolean aRedstone) {
        ITexture casingTexture = getCasingTexture();
        if (side != facing) {
            return new ITexture[] { casingTexture };
        }

        int textures = 1;
        IIconContainer container = aActive ? getActiveOverlay() : getInactiveOverlay();
        ITexture overlay = null;
        if (container != null) {
            textures++;
            overlay = TextureFactory.builder()
                .addIcon(container)
                .extFacing()
                .build();
        }

        IIconContainer glowContainer = aActive ? getActiveGlowOverlay() : getInactiveGlowOverlay();
        ITexture glowOverlay = null;
        if (glowContainer != null) {
            textures++;
            glowOverlay = TextureFactory.builder()
                .addIcon(glowContainer)
                .extFacing()
                .glow()
                .build();
        }

        ITexture[] retVal = new ITexture[textures];
        retVal[0] = getCasingTexture();
        if (overlay != null) retVal[1] = overlay;
        if (glowOverlay != null) retVal[2] = glowOverlay;
        return retVal;
    }

    protected IIconContainer getActiveOverlay() {
        return null;
    }

    protected IIconContainer getActiveGlowOverlay() {
        return null;
    }

    protected IIconContainer getInactiveOverlay() {
        return null;
    }

    protected IIconContainer getInactiveGlowOverlay() {
        return null;
    }

    protected ITexture getCasingTexture() {
        return Textures.BlockIcons.getCasingTextureForId(getCasingTextureId());
    }

    protected int getCasingTextureId() {
        return 0;
    }

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        if (doesBindPlayerInventory()) {
            super.addUIWidgets(builder, buildContext);
        } else {
            addNoPlayerInventoryUI(builder, buildContext);
        }
    }

    private static final Materials GOOD = Materials.Uranium;
    private static final Materials BAD = Materials.Plutonium;
    private static final ConcurrentHashMap<String, ItemStack> mToolStacks = new ConcurrentHashMap<>();

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    protected void addNoPlayerInventoryUI(ModularWindow.Builder builder, UIBuildContext buildContext) {
        builder.widget(
            new DrawableWidget().setDrawable(GTUITextures.PICTURE_SCREEN_BLACK)
                .setPos(3, 4)
                .setSize(152, 159));
        for (int i = 0; i < 9; i++) {
            builder.widget(
                new DrawableWidget().setDrawable(GTUITextures.BUTTON_STANDARD)
                    .setPos(155, 3 + i * 18)
                    .setSize(18, 18));
        }

        DynamicPositionedColumn screenElements = new DynamicPositionedColumn();
        drawTextsNoPlayerInventory(screenElements);
        builder.widget(screenElements);

        setupToolDisplay();

        builder.widget(
            new ItemDrawable(() -> mToolStacks.get(mWrench + "WRENCH")).asWidget()
                .setPos(156, 58))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mWrench, val -> mWrench = val));
        builder.widget(
            new ItemDrawable(() -> mToolStacks.get(mCrowbar + "CROWBAR")).asWidget()
                .setPos(156, 76))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mCrowbar, val -> mCrowbar = val));
        builder.widget(
            new ItemDrawable(() -> mToolStacks.get(mHardHammer + "HARDHAMMER")).asWidget()
                .setPos(156, 94))
            .widget(
                new TextWidget("H").setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setPos(167, 103))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mHardHammer, val -> mHardHammer = val));
        builder.widget(
            new ItemDrawable(() -> mToolStacks.get(mSoftMallet + "SOFTMALLET")).asWidget()
                .setPos(156, 112))
            .widget(
                new TextWidget("M").setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setPos(167, 121))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mSoftMallet, val -> mSoftMallet = val));
        builder.widget(
            new ItemDrawable(() -> mToolStacks.get(mScrewdriver + "SCREWDRIVER")).asWidget()
                .setPos(156, 130))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mScrewdriver, val -> mScrewdriver = val));
        builder.widget(
            new ItemDrawable(() -> mToolStacks.get(mSolderingTool + "SOLDERING_IRON_LV")).asWidget()
                .setPos(156, 148))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mSolderingTool, val -> mSolderingTool = val));
        builder.widget(
            new ItemDrawable(() -> mToolStacks.get(getBaseMetaTileEntity().isActive() + "GLASS")).asWidget()
                .setPos(156, 22))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> getBaseMetaTileEntity().isActive() ? StatCollector.translateToLocal("gtpp.gui.text.on")
                            : StatCollector.translateToLocal("gtpp.gui.text.off"))
                    .setSynced(false)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setPos(157, 31))
            .widget(
                new FakeSyncWidget.BooleanSyncer(
                    () -> getBaseMetaTileEntity().isActive(),
                    val -> getBaseMetaTileEntity().setActive(val)));
    }

    protected void drawTextsNoPlayerInventory(DynamicPositionedColumn screenElements) {
        screenElements.setSynced(false)
            .setSpace(0)
            .setPos(6, 7);

        screenElements
            .widget(
                new TextWidget(GTUtility.trans("138", "Incomplete Structure.")).setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> !mMachine))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mMachine, val -> mMachine = val))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocal("GTPP.machines.input") + " "
                            + StatCollector.translateToLocal("GTPP.machines.tier")
                            + ": "
                            + EnumChatFormatting.GREEN
                            + GTValues.VOLTAGE_NAMES[(int) getInputTier()])
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine && getInputTier() > 0))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocal("GTPP.machines.output") + " "
                            + StatCollector.translateToLocal("GTPP.machines.tier")
                            + ": "
                            + EnumChatFormatting.GREEN
                            + GTValues.VOLTAGE_NAMES[(int) getOutputTier()])
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine && getOutputTier() > 0))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.multiblock.progress.text",
                            "" + EnumChatFormatting.GREEN
                                + getBaseMetaTileEntity().getProgress() / 20
                                + EnumChatFormatting.RESET,
                            "" + EnumChatFormatting.YELLOW
                                + getBaseMetaTileEntity().getMaxProgress() / 20
                                + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                new TextWidget(StatCollector.translateToLocal("GTPP.multiblock.energy") + ":")
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> "" + EnumChatFormatting.GREEN
                            + getStoredEnergyInAllEnergyHatches()
                            + EnumChatFormatting.RESET
                            + " EU / "
                            + EnumChatFormatting.YELLOW
                            + getMaxEnergyStorageOfAllEnergyHatches()
                            + EnumChatFormatting.RESET
                            + " EU")
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                new TextWidget(StatCollector.translateToLocal("GTPP.multiblock.usage") + ":")
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine && getLastRecipeEU() > 0 && getLastRecipeDuration() > 0))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.multiblock.eu_t_p.text",
                            "" + EnumChatFormatting.RED + -getLastRecipeEU() + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine && getLastRecipeEU() > 0 && getLastRecipeDuration() > 0))
            .widget(
                TextWidget.dynamicString(() -> StatCollector.translateToLocal("GTPP.multiblock.generation") + ":")
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine && getLastRecipeEU() < 0 && getLastRecipeDuration() > 0))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.multiblock.eu_t_p.text",
                            "" + EnumChatFormatting.GREEN + getLastRecipeEU() + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine && getLastRecipeEU() < 0 && getLastRecipeDuration() > 0))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.multiblock.duration.text",
                            "" + EnumChatFormatting.RED + getLastRecipeDuration() + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine && getLastRecipeEU() != 0 && getLastRecipeDuration() > 0))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocal("GTPP.multiblock.specialvalue") + ": "
                            + EnumChatFormatting.RED
                            + getLastRecipeEU()
                            + EnumChatFormatting.RESET)
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(
                        widget -> mMachine && getLastRecipeEU() != 0
                            && getLastRecipeDuration() > 0
                            && (mLastRecipe != null ? mLastRecipe.mSpecialValue : 0) > 0))
            .widget(
                new TextWidget(StatCollector.translateToLocal("GTPP.multiblock.mei") + ":")
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.multiblock.max_voltage.txt",
                            "" + EnumChatFormatting.YELLOW + getMaxInputVoltage() + EnumChatFormatting.RESET,
                            EnumChatFormatting.YELLOW + GTValues.VN[GTUtility.getTier(getMaxInputVoltage())]
                                + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocal("GTPP.multiblock.efficiency") + ": "
                            + EnumChatFormatting.YELLOW
                            + (mEfficiency / 100.0F)
                            + EnumChatFormatting.RESET
                            + " %")
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.multiblock.pollution.txt",
                            "" + EnumChatFormatting.RED + getPollutionPerSecond(null) + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocal("GTPP.multiblock.pollutionreduced") + ": "
                            + EnumChatFormatting.GREEN
                            + getAveragePollutionPercentage()
                            + EnumChatFormatting.RESET
                            + " %")
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                new TextWidget(StatCollector.translateToLocal("gtpp.gui.text.time_since_built") + ": ")
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.text.time.week",
                            "" + EnumChatFormatting.DARK_GREEN + getRuntimeWeeksDisplay() + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.text.time.days",
                            "" + EnumChatFormatting.DARK_GREEN + getRuntimeDaysDisplay() + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.text.time.hours",
                            "" + EnumChatFormatting.DARK_GREEN + getRuntimeHoursDisplay() + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.text.time.minutes",
                            "" + EnumChatFormatting.DARK_GREEN + getRuntimeMinutesDisplay() + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine))
            .widget(
                TextWidget
                    .dynamicString(
                        () -> StatCollector.translateToLocalFormatted(
                            "gtpp.gui.text.time.seconds",
                            "" + EnumChatFormatting.DARK_GREEN + getRuntimeSecondsDisplay() + EnumChatFormatting.RESET))
                    .setTextAlignment(Alignment.CenterLeft)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> mMachine));
    }

    protected int getLastRecipeEU() {
        return mLastRecipe != null ? mLastRecipe.mEUt : 0;
    }

    protected int getLastRecipeDuration() {
        return mLastRecipe != null ? mLastRecipe.mDuration : 0;
    }

    protected long getRuntimeSeconds() {
        return getTotalRuntimeInTicks() / 20;
    }

    protected long getRuntimeWeeksDisplay() {
        return TimeUnit.SECONDS.toDays(getRuntimeSeconds()) / 7;
    }

    protected long getRuntimeDaysDisplay() {
        return TimeUnit.SECONDS.toDays(getRuntimeSeconds()) - 7 * getRuntimeWeeksDisplay();
    }

    protected long getRuntimeHoursDisplay() {
        return TimeUnit.SECONDS.toHours(getRuntimeSeconds()) - TimeUnit.DAYS.toHours(getRuntimeDaysDisplay())
            - TimeUnit.DAYS.toHours(7 * getRuntimeWeeksDisplay());
    }

    protected long getRuntimeMinutesDisplay() {
        return TimeUnit.SECONDS.toMinutes(getRuntimeSeconds()) - (TimeUnit.SECONDS.toHours(getRuntimeSeconds()) * 60);
    }

    protected long getRuntimeSecondsDisplay() {
        return TimeUnit.SECONDS.toSeconds(getRuntimeSeconds()) - (TimeUnit.SECONDS.toMinutes(getRuntimeSeconds()) * 60);
    }

    protected void setupToolDisplay() {
        if (!mToolStacks.isEmpty()) return;

        mToolStacks.put(
            true + "WRENCH",
            MetaGeneratedTool01.INSTANCE.getToolWithStats(IDMetaTool01.WRENCH.ID, 1, GOOD, Materials.Tungsten, null));
        mToolStacks.put(
            true + "CROWBAR",
            MetaGeneratedTool01.INSTANCE.getToolWithStats(IDMetaTool01.CROWBAR.ID, 1, GOOD, Materials.Tungsten, null));
        mToolStacks.put(
            true + "HARDHAMMER",
            MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.HARDHAMMER.ID, 1, GOOD, Materials.Tungsten, null));
        mToolStacks.put(
            true + "SOFTMALLET",
            MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.SOFTMALLET.ID, 1, GOOD, Materials.Tungsten, null));
        mToolStacks.put(
            true + "SCREWDRIVER",
            MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.SCREWDRIVER.ID, 1, GOOD, Materials.Tungsten, null));
        mToolStacks.put(
            true + "SOLDERING_IRON_LV",
            MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.SOLDERING_IRON_LV.ID, 1, GOOD, Materials.Tungsten, null));

        mToolStacks.put(
            false + "WRENCH",
            MetaGeneratedTool01.INSTANCE.getToolWithStats(IDMetaTool01.WRENCH.ID, 1, BAD, Materials.Tungsten, null));
        mToolStacks.put(
            false + "CROWBAR",
            MetaGeneratedTool01.INSTANCE.getToolWithStats(IDMetaTool01.CROWBAR.ID, 1, BAD, Materials.Tungsten, null));
        mToolStacks.put(
            false + "HARDHAMMER",
            MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.HARDHAMMER.ID, 1, BAD, Materials.Tungsten, null));
        mToolStacks.put(
            false + "SOFTMALLET",
            MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.SOFTMALLET.ID, 1, BAD, Materials.Tungsten, null));
        mToolStacks.put(
            false + "SCREWDRIVER",
            MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.SCREWDRIVER.ID, 1, BAD, Materials.Tungsten, null));
        mToolStacks.put(
            false + "SOLDERING_IRON_LV",
            MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.SOLDERING_IRON_LV.ID, 1, BAD, Materials.Tungsten, null));

        ItemStack aGlassPane1 = ItemUtils.getItemStackOfAmountFromOreDict("paneGlassRed", 1);
        ItemStack aGlassPane2 = ItemUtils.getItemStackOfAmountFromOreDict("paneGlassLime", 1);
        mToolStacks.put("falseGLASS", aGlassPane1);
        mToolStacks.put("trueGLASS", aGlassPane2);
    }

    public enum GTPPHatchElement implements IHatchElement<GTPPMultiBlockBase<?>> {

        AirIntake(GTPPMultiBlockBase::addAirIntakeToMachineList, METHatchAirIntake.class) {

            @Override
            public long count(GTPPMultiBlockBase<?> t) {
                return t.mAirIntakes.size();
            }
        },
        TTDynamo(GTPPMultiBlockBase::addMultiAmpDynamoToMachineList, MTEHatchDynamoMulti.class) {

            @Override
            public long count(GTPPMultiBlockBase<?> t) {
                return t.mTecTechDynamoHatches.size();
            }
        },
        TTEnergy(GTPPMultiBlockBase::addMultiAmpEnergyToMachineList, MTEHatchEnergyMulti.class) {

            @Override
            public long count(GTPPMultiBlockBase<?> t) {
                return t.mTecTechEnergyHatches.size();
            }
        };

        private final List<? extends Class<? extends IMetaTileEntity>> mMteClasses;
        private final IGTHatchAdder<? super GTPPMultiBlockBase<?>> mAdder;

        @SafeVarargs
        GTPPHatchElement(IGTHatchAdder<? super GTPPMultiBlockBase<?>> aAdder,
            Class<? extends IMetaTileEntity>... aMteClasses) {
            this.mMteClasses = Arrays.asList(aMteClasses);
            this.mAdder = aAdder;
        }

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return mMteClasses;
        }

        @Override
        public IGTHatchAdder<? super GTPPMultiBlockBase<?>> adder() {
            return mAdder;
        }
    }
}
