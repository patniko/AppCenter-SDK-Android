/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package demo.tensorflow.org.customvision_sample.env;

/**
 * Utility class for manipulating images.
 **/
public class ImageUtils {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = new Logger();

    static {
        try {
            System.loadLibrary("tensorflow_demo");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.w("Native library not found, native RGB -> YUV conversion may be unavailable.");
        }
    }

    // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
    // are normalized to eight bits.
    private static final int kMaxChannelValue = 262143;

    // Always prefer the native implementation if available.
    private static boolean useNativeConversion = true;

    public static void convertYUV420SPToARGB8888(
            byte[] input,
            int width,
            int height,
            int[] output) {
        if (useNativeConversion) {
            try {
                ImageUtils.convertYUV420SPToARGB8888(input, output, width, height, false);
                return;
            } catch (UnsatisfiedLinkError e) {
                LOGGER.w(
                        "Native YUV420SP -> RGB implementation not found, falling back to Java implementation");
                useNativeConversion = false;
            }
        }

        // Java implementation of YUV420SP to ARGB8888 converting
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width;
            int u = 0;
            int v = 0;

            for (int i = 0; i < width; i++, yp++) {
                int y = 0xff & input[yp];
                if ((i & 1) == 0) {
                    v = 0xff & input[uvp++];
                    u = 0xff & input[uvp++];
                }

                output[yp] = YUV2RGB(y, u, v);
            }
        }
    }

    private static int YUV2RGB(int y, int u, int v) {
        // Adjust and check YUV values
        y = (y - 16) < 0 ? 0 : (y - 16);
        u -= 128;
        v -= 128;

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
        g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
        b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    public static void convertYUV420ToARGB8888(
            byte[] yData,
            byte[] uData,
            byte[] vData,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            int[] out) {
        if (useNativeConversion) {
            try {
                convertYUV420ToARGB8888(
                        yData, uData, vData, out, width, height, yRowStride, uvRowStride, uvPixelStride, false);
                return;
            } catch (UnsatisfiedLinkError e) {
                LOGGER.w(
                        "Native YUV420 -> RGB implementation not found, falling back to Java implementation");
                useNativeConversion = false;
            }
        }

        int yp = 0;
        for (int j = 0; j < height; j++) {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++) {
                int uv_offset = pUV + (i >> 1) * uvPixelStride;

                out[yp++] = YUV2RGB(
                        0xff & yData[pY + i],
                        0xff & uData[uv_offset],
                        0xff & vData[uv_offset]);
            }
        }
    }

    /**
     * Converts YUV420 semi-planar data to ARGB 8888 data using the supplied width and height. The
     * input and output must already be allocated and non-null. For efficiency, no error checking is
     * performed.
     *
     * @param input    The array of YUV 4:2:0 input data.
     * @param output   A pre-allocated array for the ARGB 8:8:8:8 output data.
     * @param width    The width of the input image.
     * @param height   The height of the input image.
     * @param halfSize If true, downsample to 50% in each dimension, otherwise not.
     */
    private static native void convertYUV420SPToARGB8888(
            byte[] input, int[] output, int width, int height, boolean halfSize);

    /**
     * Converts YUV420 semi-planar data to ARGB 8888 data using the supplied width
     * and height. The input and output must already be allocated and non-null.
     * For efficiency, no error checking is performed.
     *
     * @param y
     * @param u
     * @param v
     * @param uvPixelStride
     * @param width         The width of the input image.
     * @param height        The height of the input image.
     * @param halfSize      If true, downsample to 50% in each dimension, otherwise not.
     * @param output        A pre-allocated array for the ARGB 8:8:8:8 output data.
     */
    private static native void convertYUV420ToARGB8888(
            byte[] y,
            byte[] u,
            byte[] v,
            int[] output,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            boolean halfSize);
}