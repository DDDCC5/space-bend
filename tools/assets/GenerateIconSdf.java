// Offline asset authoring only. No plugin classes are linked into the APK.
// Uses the same RGBA/32px/Distance/200 settings as PICO App Identity Configurator 6.1.1.
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.byted.idea.spatial.common.utils.SdfGenerator;

class GenerateIconSdf {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("input PNG, output SDF PNG required");
        BufferedImage image = ImageIO.read(new File(args[0]));
        int width = image.getWidth(), height = image.getHeight();
        byte[] rgba = new byte[width * height * 4];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int pixel = image.getRGB(x, y), i = (y * width + x) * 4;
            rgba[i] = (byte) (pixel >>> 16); rgba[i + 1] = (byte) (pixel >>> 8);
            rgba[i + 2] = (byte) pixel; rgba[i + 3] = (byte) (pixel >>> 24);
        }
        byte[] sdf = new byte[rgba.length];
        float spread = width >= 1024 ? 32f : width >= 512 ? 16f : width / 32f;
        SdfGenerator.INSTANCE.generateRaw(rgba, sdf, width, height, spread, 0f, 0f,
                SdfGenerator.RGBFillMode.Distance, 200);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int i = (y * width + x) * 4;
            int pixel = (sdf[i + 3] & 255) << 24 | (sdf[i] & 255) << 16
                    | (sdf[i + 1] & 255) << 8 | (sdf[i + 2] & 255);
            out.setRGB(x, y, pixel);
        }
        if (!ImageIO.write(out, "png", new File(args[1]))) throw new IllegalStateException("PNG export failed");
        System.out.println("SDF generated: " + args[1]);
    }
}
