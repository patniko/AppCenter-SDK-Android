package com.microsoft.appcenter.assets;

import android.content.Context;
import android.content.res.Resources;

import com.microsoft.appcenter.assets.exceptions.AssetsInvalidPublicKeyException;
import com.microsoft.appcenter.assets.interfaces.AssetsPublicKeyProvider;

/**
 * Android-specific instance of {@link AssetsPublicKeyProvider}.
 */
public class AssetsAndroidPublicKeyProvider implements AssetsPublicKeyProvider {

    /**
     * Public-key related resource descriptor.
     */
    private Integer mPublicKeyResourceDescriptor;

    /**
     * Instance of application context.
     */
    private Context mContext;

    /**
     * Creates an instance of {@link AssetsAndroidPublicKeyProvider}.
     *
     * @param publicKeyResourceDescriptor public-key related resource descriptor.
     * @param context                     application context.
     */
    public AssetsAndroidPublicKeyProvider(final Integer publicKeyResourceDescriptor, final Context context) {
        mPublicKeyResourceDescriptor = publicKeyResourceDescriptor;
        mContext = context;
    }

    @Override
    public String getPublicKey() throws AssetsInvalidPublicKeyException {
        if (mPublicKeyResourceDescriptor == null) {
            return null;
        }
        String publicKey;
        try {
            publicKey = mContext.getString(mPublicKeyResourceDescriptor);
        } catch (Resources.NotFoundException e) {
            throw new AssetsInvalidPublicKeyException(
                    "Unable to get public key, related resource descriptor " +
                            mPublicKeyResourceDescriptor +
                            " can not be found", e
            );
        }
        if (publicKey.isEmpty()) {
            throw new AssetsInvalidPublicKeyException("Specified public key is empty");
        }
        return publicKey;
    }
}
