package gtPlusPlus.api.objects.minecraft;

import java.util.HashMap;

import net.minecraft.util.IIcon;

import org.jetbrains.annotations.NotNull;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.util.StringUtils;

/**
 * A Server Side safe object that can hold {@link IIcon}s.
 *
 * @author Alkalus
 *
 */
public class SafeTexture implements Runnable {

    @SideOnly(Side.CLIENT)
    private static final HashMap<Integer, IIcon> mHashToIconCache = new HashMap<>();

    @SideOnly(Side.CLIENT)
    private static final HashMap<String, Integer> mPathToHashCash = new HashMap<>();

    private static final HashMap<String, SafeTexture> mTextureObjectCache = new HashMap<>();

    private final int mHash;

    private final String mTextureName;

    private static @NotNull String getKey(@NotNull String aTexPath) {
        String aNameKey = StringUtils.sanitizeString(aTexPath);
        aNameKey = aNameKey.replace('/', ' ');
        aNameKey = aNameKey.toLowerCase();
        return aNameKey;
    }

    public static @NotNull SafeTexture register(@NotNull String aTexturePath) {
        String aNameKey = getKey(aTexturePath);
        SafeTexture g = mTextureObjectCache.get(aNameKey);
        if (g == null) {
            g = new SafeTexture(aTexturePath);
            mTextureObjectCache.put(aNameKey, g);
            mPathToHashCash.put(aTexturePath, aTexturePath.hashCode());
        }
        return g;
    }

    private SafeTexture(@NotNull String aTexturePath) {
        mTextureName = aTexturePath;
        mHash = getKey(aTexturePath).hashCode();
        GregTechAPI.sGTBlockIconload.add(this);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon() {
        return mHashToIconCache.get(mHash);
    }

    @Override
    public void run() {
        mHashToIconCache.put(getKey(mTextureName).hashCode(), GregTechAPI.sBlockIcons.registerIcon(mTextureName));
    }
}
