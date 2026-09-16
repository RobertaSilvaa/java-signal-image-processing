import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Convolution2D {

    private static final Path[] DEFAULT_IMAGE_PATH_CANDIDATES = {
            Paths.get("src", "image", "Cat.jpg"),
            Paths.get("image", "Cat.jpg")
    };
    private static final int MAX_PREVIEW_SIZE = 320;
    private static final String[] FILTER_LABELS = {
            "Smoothed",
            "Edges",
            "Enhanced",
            "Sharpened",
            "Gaussian"
    };

    public static double[][] applyConvolution(double[][] image, double[][] kernel) {
        validateMatrix(image, "Image");
        validateMatrix(kernel, "Kernel");

        int imageHeight = image.length;
        int imageWidth = image[0].length;
        int kernelHeight = kernel.length;
        int kernelWidth = kernel[0].length;

        if (kernelHeight > imageHeight || kernelWidth > imageWidth) {
            throw new IllegalArgumentException("Kernel dimensions must not exceed image dimensions.");
        }

        int filteredHeight = imageHeight - kernelHeight + 1;
        int filteredWidth = imageWidth - kernelWidth + 1;
        double[][] filteredImage = new double[filteredHeight][filteredWidth];

        for (int y = 0; y < filteredHeight; y++) {
            for (int x = 0; x < filteredWidth; x++) {
                double sum = 0.0;

                for (int kernelY = 0; kernelY < kernelHeight; kernelY++) {
                    for (int kernelX = 0; kernelX < kernelWidth; kernelX++) {
                        double kernelValue = kernel[kernelHeight - 1 - kernelY][kernelWidth - 1 - kernelX];
                        sum += image[y + kernelY][x + kernelX] * kernelValue;
                    }
                }

                filteredImage[y][x] = sum;
            }
        }

        return filteredImage;
    }

    private static Path resolveDefaultImagePath() throws IOException {
        for (Path candidate : DEFAULT_IMAGE_PATH_CANDIDATES) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        StringBuilder checkedPaths = new StringBuilder();
        for (int i = 0; i < DEFAULT_IMAGE_PATH_CANDIDATES.length; i++) {
            if (i > 0) {
                checkedPaths.append(", ");
            }
            checkedPaths.append(DEFAULT_IMAGE_PATH_CANDIDATES[i].toAbsolutePath());
        }

        throw new IOException("Default image file not found. Checked: " + checkedPaths);
    }

    public static BufferedImage readImage(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Image path must not be null.");
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("Image file not found: " + path.toAbsolutePath());
        }

        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("Unsupported or invalid image format: " + path.toAbsolutePath());
        }

        return image;
    }

    public static double[][][] imageToMatrices(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image must not be null.");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        double[][][] matrices = new double[3][height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                matrices[0][y][x] = (rgb >> 16) & 0xff;
                matrices[1][y][x] = (rgb >> 8) & 0xff;
                matrices[2][y][x] = rgb & 0xff;
            }
        }

        return matrices;
    }

    public static BufferedImage matricesToImage(double[][][] matrices) {
        validateRgbMatrices(matrices);

        int height = matrices[0].length;
        int width = matrices[0][0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = clampAndRound(matrices[0][y][x]);
                int green = clampAndRound(matrices[1][y][x]);
                int blue = clampAndRound(matrices[2][y][x]);
                int rgb = (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    public static void showImages(BufferedImage original, BufferedImage... filteredImages) {
        if (original == null) {
            throw new IllegalArgumentException("Original image must not be null.");
        }
        if (filteredImages == null) {
            throw new IllegalArgumentException("Filtered image array must not be null.");
        }
        if (filteredImages.length > FILTER_LABELS.length) {
            throw new IllegalArgumentException(
                    "At most " + FILTER_LABELS.length + " filtered images can be displayed.");
        }

        for (int i = 0; i < filteredImages.length; i++) {
            if (filteredImages[i] == null) {
                throw new IllegalArgumentException("Filtered image at index " + i + " must not be null.");
            }
        }

        int imageCount = filteredImages.length + 1;
        int columns = Math.min(3, imageCount);
        int rows = (imageCount + columns - 1) / columns;

        JFrame window = new JFrame("2D Convolution Results");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel imagePanel = new JPanel(new GridLayout(rows, columns, 8, 8));
        imagePanel.add(createImageLabel(original, "Original"));

        for (int i = 0; i < filteredImages.length; i++) {
            imagePanel.add(createImageLabel(filteredImages[i], FILTER_LABELS[i]));
        }

        window.add(imagePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private static JLabel createImageLabel(BufferedImage image, String labelText) {
        double widthScale = MAX_PREVIEW_SIZE / (double) image.getWidth();
        double heightScale = MAX_PREVIEW_SIZE / (double) image.getHeight();
        double scale = Math.min(1.0, Math.min(widthScale, heightScale));

        int previewWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int previewHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));

        Image preview = image.getScaledInstance(previewWidth, previewHeight, Image.SCALE_SMOOTH);
        JLabel label = new JLabel(labelText, new ImageIcon(preview), JLabel.CENTER);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.BOTTOM);
        return label;
    }

    private static void validateMatrix(double[][] matrix, String matrixName) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException(matrixName + " must not be null or empty.");
        }
        if (matrix[0] == null || matrix[0].length == 0) {
            throw new IllegalArgumentException(matrixName + " rows must not be null or empty.");
        }

        int expectedWidth = matrix[0].length;
        for (int row = 0; row < matrix.length; row++) {
            if (matrix[row] == null || matrix[row].length != expectedWidth) {
                throw new IllegalArgumentException(matrixName + " must be rectangular.");
            }

            for (int column = 0; column < matrix[row].length; column++) {
                if (!Double.isFinite(matrix[row][column])) {
                    throw new IllegalArgumentException(
                            matrixName + " contains a non-finite value at [" + row + "][" + column + "].");
                }
            }
        }
    }

    private static void validateRgbMatrices(double[][][] matrices) {
        if (matrices == null || matrices.length != 3) {
            throw new IllegalArgumentException("RGB data must contain exactly three color-channel matrices.");
        }

        validateMatrix(matrices[0], "Red channel");
        validateMatrix(matrices[1], "Green channel");
        validateMatrix(matrices[2], "Blue channel");

        int expectedHeight = matrices[0].length;
        int expectedWidth = matrices[0][0].length;

        for (int channel = 1; channel < matrices.length; channel++) {
            if (matrices[channel].length != expectedHeight || matrices[channel][0].length != expectedWidth) {
                throw new IllegalArgumentException("All RGB channel matrices must have identical dimensions.");
            }
        }
    }

    private static int clampAndRound(double value) {
        double clampedValue = Math.max(0.0, Math.min(255.0, value));
        return (int) Math.round(clampedValue);
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java Convolution2D [image-path]");
            System.exit(1);
        }

        try {
            Path imagePath = args.length == 1 ? Paths.get(args[0]) : resolveDefaultImagePath();
            BufferedImage originalImage = readImage(imagePath);
            double[][][] imageMatrices = imageToMatrices(originalImage);

            double[][] smoothingKernel = {
                    { 1 / 9.0, 1 / 9.0, 1 / 9.0 },
                    { 1 / 9.0, 1 / 9.0, 1 / 9.0 },
                    { 1 / 9.0, 1 / 9.0, 1 / 9.0 }
            };

            double[][] edgeDetectionKernel = {
                    { -1, -1, -1 },
                    { -1, 8, -1 },
                    { -1, -1, -1 }
            };

            double[][] enhancementKernel = {
                    { 0, -1, 0 },
                    { -1, 5, -1 },
                    { 0, -1, 0 }
            };

            double[][] sharpenKernel = {
                    { -1, -1, -1 },
                    { -1, 9, -1 },
                    { -1, -1, -1 }
            };

            double[][] gaussianKernel = {
                    { 1 / 16.0, 2 / 16.0, 1 / 16.0 },
                    { 2 / 16.0, 4 / 16.0, 2 / 16.0 },
                    { 1 / 16.0, 2 / 16.0, 1 / 16.0 }
            };

            double[][][] smoothedImage = new double[3][][];
            double[][][] edgeImage = new double[3][][];
            double[][][] enhancedImage = new double[3][][];
            double[][][] sharpenedImage = new double[3][][];
            double[][][] gaussianImage = new double[3][][];

            for (int channel = 0; channel < 3; channel++) {
                smoothedImage[channel] = applyConvolution(imageMatrices[channel], smoothingKernel);
                edgeImage[channel] = applyConvolution(imageMatrices[channel], edgeDetectionKernel);
                enhancedImage[channel] = applyConvolution(imageMatrices[channel], enhancementKernel);
                sharpenedImage[channel] = applyConvolution(imageMatrices[channel], sharpenKernel);
                gaussianImage[channel] = applyConvolution(imageMatrices[channel], gaussianKernel);
            }

            BufferedImage finalSmoothedImage = matricesToImage(smoothedImage);
            BufferedImage finalEdgeImage = matricesToImage(edgeImage);
            BufferedImage finalEnhancedImage = matricesToImage(enhancedImage);
            BufferedImage finalSharpenedImage = matricesToImage(sharpenedImage);
            BufferedImage finalGaussianImage = matricesToImage(gaussianImage);

            SwingUtilities.invokeLater(() -> showImages(
                    originalImage,
                    finalSmoothedImage,
                    finalEdgeImage,
                    finalEnhancedImage,
                    finalSharpenedImage,
                    finalGaussianImage));

        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }
}
