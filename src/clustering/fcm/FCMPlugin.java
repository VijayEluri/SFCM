/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package clustering.fcm;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

/**
 *
 * @author valerio
 */
public class FCMPlugin implements PlugIn{

    public static final String RESULTS_WINDOW_TITLE = "Fuzzy C-Means cluster centers";

    private static boolean showCentroidImage;
    private static boolean sendToResultTable;

    private static final boolean APPLY_LUT = false;
    private static final boolean AUTO_BRIGHTNESS = true;


    private static final String TITLE = "Fuzzy C-Means Clustering";
    private static final String ABOUT = "" +
            "Refactoring and Generalization of the k-means Plugin for ImageJ found" +
            " in the ij-plugin-toolkit";

    /* Creating an array of illegal Image types */
    private static final int[] illegalImages = {ImagePlus.COLOR_256};
    private FCMManager FCMM;

    @Override
    public void run(String arg) {

        /* Showing the About Message */
        if ("about".equalsIgnoreCase(arg))
        {
            IJ.showMessage("About " + TITLE, ABOUT);
            return;
        }

        /* Getting the current Image */
        final ImagePlus imp = IJ.getImage();

        /* Validate it against the illegal Image Type List */
        if (!validateImage(imp))
        {
            IJ.error(TITLE, "Image type not supported");
            return;
        }

        /* Create an instance of KMeansManager */
        FCMM = new FCMManager();

        /* Create an instance of Generic Dialog */
        GenericDialog configDialog = new GenericDialog("K-means Configuration");
        /* and configure it according to the defaults in KMeansManager */
        configDialog = configureDialog(FCMM, configDialog);
        /* Show Dialog */
        configDialog.showDialog();
        if (configDialog.wasCanceled())
        {
            return;
        }

        /* Configuring the KMeansManager */
        getConfigurationFromDialog(configDialog, FCMM);

        /* Validate the correct color space conversion */
        if (!validateColorSpace(imp, FCMM.getColorSpace()))
        {
            IJ.error(TITLE, "Invalid Color Space Conversion");
            return;
        }

        /* Calling the clustering algorithm */
        final long startTime = System.currentTimeMillis();
        ImageStack[] imgArray = FCMM.run(imp);
        final long endTime = System.currentTimeMillis();
        final String message = "Clustering and Visualization completed in " +
                                                      (endTime - startTime) +
                                                                     " ms.";
        IJ.showStatus(message);

        /* Show result images */
        String type = null;
        for (int i = 0; i < imgArray.length; i++)
        {
            ImagePlus r = null;

            //System.out.println(i + " len " + imgArray.length + " " + imgArray[i].getSize());
            ImageProcessor IP = imgArray[i].getProcessor(1);
            int slices = imgArray[i].getSize();
            //System.out.println("class " + IP.getClass());
            if (IP.getClass() == ij.process.ByteProcessor.class && slices == 1)
            {
                //System.out.println("Entrato");
                adjustBrightness(IP, FCMM.getNumberOfClusters());
                r = new ImagePlus("Clusters in Gray Scale", IP);
            }
            else if (IP.getClass() == ij.process.FloatProcessor.class)
            {
//                final boolean doScaling = ImageConverter.getDoScaling();
//                try
//                {
//                    ImageConverter.setDoScaling(false);
//                    r = new ImagePlus("Clusters", imgArray[i]);
//                    final StackConverter stackConverter = new StackConverter(r);
//                    stackConverter.convertToGray8();
//                    final ImageConverter imageConverter = new ImageConverter(r);
//                    imageConverter.convertRGBStackToRGB();
//
//                }
//                finally
//                {
//                    ImageConverter.setDoScaling(doScaling);
//                }
                r = encodeRGBImageFromStack(imp.getType(), imgArray[i]);

            }
            else
            {
                r = new ImagePlus("Clusters", imgArray[i]);
            }

            //System.out.println(type);

            r.show();
        }
    }

    private boolean validateImage(ImagePlus img){

        boolean validImage = true;
        for (int i = 0; i < illegalImages.length && validImage; i++)
        {
            if (img.getType() == illegalImages[i])
            {
                validImage = false;
            }
        }

        return validImage;
    }

    private boolean validateColorSpace(ImagePlus img, String colorSpace){

        boolean validColorSpace = true;
        if (img.getStackSize() > 1 && !colorSpace.equals("None"))
        {
            validColorSpace = false;
        }
        else if (img.getStackSize() == 1 && !(img.getType() == ImagePlus.COLOR_RGB) && !colorSpace.equals("None"))
        {
            validColorSpace = false;
        }
        else
        {
            validColorSpace = true;
        }

        return validColorSpace;
    }

    private GenericDialog configureDialog(FCMManager FCMM, GenericDialog dialog){

        dialog.addNumericField("Number_of_clusters", FCMM.getNumberOfClusters(), 0);
        dialog.addNumericField("Cluster_center_tolerance", FCMM.getTolerance(), 8);
        dialog.addChoice("Initialization Mode", FCMM.getInitModes(), FCMM.getInizializationMode());
//        dialog.addCheckbox("Random_clusters_initialization", FCMM.getRandomInitialization());
//        dialog.addCheckbox("KMeans++_clusters_initialization", FCMM.getKMeansPlusPlusInitialization());
        dialog.addNumericField("Randomization_seed", FCMM.getRandomizationSeed(), 0);
        dialog.addNumericField("Fuzzyness_parameter", FCMM.getFuzzyness(), 1);
        dialog.addChoice("Color_Space_Conversion", FCMM.getColorSpaces(), FCMM.getColorSpace());
        dialog.addCheckbox("Show_clusters_as_centroid_value", FCMM.getClusterCenterColorsVisualization());
        dialog.addCheckbox("Show_clusters_as_random_RGB", FCMM.getRandomRGBVisualization());
        dialog.addCheckbox("Show_clusters_as_gray_levels", FCMM.getGrayScaleVisualization());
        dialog.addCheckbox("Show_clusters_as_binary_stack", FCMM.getBinaryStackVisualization());
        return dialog;
    }

    private void getConfigurationFromDialog(GenericDialog dialog, FCMManager FCMM){
        FCMM.setNumberOfClusters((int) Math.round(dialog.getNextNumber()));
        FCMM.setTolerance((float) dialog.getNextNumber());
        FCMM.setInitializationMode(dialog.getNextChoice());
//        FCMM.setRandomInitialization(dialog.getNextBoolean());
//        FCMM.setKMeansPlusPlusInitialization(dialog.getNextBoolean());
        FCMM.setRandomizationSeed((int) Math.round(dialog.getNextNumber()));
        FCMM.setFuzzyness((float)dialog.getNextNumber());
        FCMM.setColorSpace(dialog.getNextChoice());
        FCMM.setClusterCenterColorsVisualization(dialog.getNextBoolean());
        FCMM.setRandomRGBVisualization(dialog.getNextBoolean());
        FCMM.setGrayScaleVisualization(dialog.getNextBoolean());
        FCMM.setBinaryStackVisualization(dialog.getNextBoolean());
    }

    private void adjustBrightness(ImageProcessor IP, int nClusters){
        if (AUTO_BRIGHTNESS)
        {
            IP.setMinAndMax(0, nClusters);
        }
    }

    private ImagePlus encodeRGBImageFromStack(final int originalImageType, final ImageStack centroidValueStack){

        final boolean doScaling = ImageConverter.getDoScaling();
        try
        {
            ImageConverter.setDoScaling(false);
            final ImagePlus cvImp = new ImagePlus("Cluster centroid values", centroidValueStack);

            if (centroidValueStack.getSize() > 1)
            {
                final StackConverter stackConverter = new StackConverter(cvImp);

                switch (originalImageType)
                {
                    case ImagePlus.COLOR_RGB:
                        stackConverter.convertToGray8();
                        final ImageConverter imageConverter = new ImageConverter(cvImp);
                        imageConverter.convertRGBStackToRGB();
                        break;
                    case ImagePlus.GRAY8:
                        stackConverter.convertToGray8();
                        break;
                    case ImagePlus.GRAY16:
                        stackConverter.convertToGray16();
                        break;
                    case ImagePlus.GRAY32:
                        // No action needed
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported input image type: " + originalImageType);
                }
            }
            else
            {
                final ImageConverter converter = new ImageConverter(cvImp);
                // Convert image back to original type
                switch (originalImageType)
                {
                    case ImagePlus.COLOR_RGB:
                        throw new IllegalArgumentException("Internal error: RGB image cannot have a single band.");
                    case ImagePlus.GRAY8:
                        converter.convertToGray8();
                        break;
                    case ImagePlus.GRAY16:
                        converter.convertToGray16();
                        break;
                    case ImagePlus.GRAY32:
                        // No action needed
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported input image type: " + originalImageType);
                }
            }

            return cvImp;
        }
        finally
        {
            ImageConverter.setDoScaling(doScaling);
        }
    }

}