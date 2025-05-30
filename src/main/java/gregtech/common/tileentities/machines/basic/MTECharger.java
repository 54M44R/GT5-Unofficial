package gregtech.common.tileentities.machines.basic;

import static gregtech.api.enums.GTValues.V;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicBatteryBuffer;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;

public class MTECharger extends MTEBasicBatteryBuffer {

    public MTECharger(int aID, String aName, String aNameRegional, int aTier, String aDescription, int aSlotCount) {
        super(aID, aName, aNameRegional, aTier, aDescription, aSlotCount);
    }

    public MTECharger(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures, int aSlotCount) {
        super(aName, aTier, aDescription, aTextures, aSlotCount);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTECharger(mName, mTier, mDescriptionArray, mTextures, mInventory.length);
    }

    @Override
    public long getMinimumStoredEU() {
        return V[mTier] * 64L * mInventory.length;
    }

    @Override
    public long maxEUStore() {
        return V[mTier] * 256L * mInventory.length;
    }

    @Override
    public long maxAmperesIn() {
        return Math.max(mChargeableCount * 8L, 4L);
    }

    @Override
    public long maxAmperesOut() {
        return Math.max(mBatteryCount * 4L, 2L);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide()) {
            super.onPostTick(aBaseMetaTileEntity, aTick);
            if (this.getBaseMetaTileEntity() instanceof BaseMetaTileEntity) {
                BaseMetaTileEntity mBaseMetaTileEntity = (BaseMetaTileEntity) getBaseMetaTileEntity();
                if (mBaseMetaTileEntity.getMetaTileEntity() instanceof MetaTileEntity mMetaTileEntity) {
                    // for (int t = 0; t < 6; t++) {
                    if (mMetaTileEntity.dechargerSlotCount() > 0
                        && mBaseMetaTileEntity.getStoredEU() < mBaseMetaTileEntity.getEUCapacity()) {
                        for (int i = mMetaTileEntity.dechargerSlotStartIndex(),
                            k = mMetaTileEntity.dechargerSlotCount() + i; i < k; i++) {
                            if (mMetaTileEntity.mInventory[i] != null
                                && mBaseMetaTileEntity.getStoredEU() < mBaseMetaTileEntity.getEUCapacity()) {
                                mBaseMetaTileEntity.increaseStoredEnergyUnits(
                                    GTModHandler.dischargeElectricItem(
                                        mMetaTileEntity.mInventory[i],
                                        GTUtility.safeInt(
                                            Math.min(
                                                V[mTier] * 15,
                                                mBaseMetaTileEntity.getEUCapacity()
                                                    - mBaseMetaTileEntity.getStoredEU())),
                                        (int) Math.min(Integer.MAX_VALUE, mMetaTileEntity.getInputTier()),
                                        true,
                                        false,
                                        false),
                                    true);
                                if (mMetaTileEntity.mInventory[i].stackSize <= 0) mMetaTileEntity.mInventory[i] = null;
                            }
                        }
                    }
                    if (mMetaTileEntity.rechargerSlotCount() > 0 && mBaseMetaTileEntity.getStoredEU() > 0) {
                        for (int i = mMetaTileEntity.rechargerSlotStartIndex(),
                            k = mMetaTileEntity.rechargerSlotCount() + i; i < k; i++) {
                            if (mBaseMetaTileEntity.getStoredEU() > 0 && mMetaTileEntity.mInventory[i] != null) {
                                mBaseMetaTileEntity
                                    .decreaseStoredEU(
                                        GTModHandler.chargeElectricItem(
                                            mMetaTileEntity.mInventory[i],
                                            GTUtility
                                                .safeInt(Math.min(V[mTier] * 15, mBaseMetaTileEntity.getStoredEU())),
                                            (int) Math.min(Integer.MAX_VALUE, mMetaTileEntity.getOutputTier()),
                                            true,
                                            false),
                                        true);
                                if (mMetaTileEntity.mInventory[i].stackSize <= 0) mMetaTileEntity.mInventory[i] = null;
                            }
                        }
                        // }
                    }
                }
            }
        }
    }
}
