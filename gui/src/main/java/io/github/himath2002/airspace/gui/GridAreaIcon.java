package io.github.himath2002.airspace.gui;

import java.io.InputStream;
import java.util.Objects;
import javafx.scene.image.Image;

/**
 * Render state for a positioned, captioned image inside {@link GridArea}.
 */
public final class GridAreaIcon {
    private volatile double x;
    private volatile double y;
    private volatile double rotation;
    private volatile double scale;
    private volatile Image image;
    private volatile String caption;
    private volatile boolean shown = true;

    public GridAreaIcon(
            double x,
            double y,
            double rotation,
            double scale,
            Image image,
            String caption) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.scale = scale;
        this.image = Objects.requireNonNull(image, "image");
        this.caption = Objects.requireNonNull(caption, "caption");
    }

    public GridAreaIcon(
            double x,
            double y,
            double rotation,
            double scale,
            InputStream imageStream,
            String caption) {
        this(x, y, rotation, scale,
                new Image(Objects.requireNonNull(imageStream, "imageStream")), caption);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRotation() {
        return rotation;
    }

    public double getScale() {
        return scale;
    }

    public Image getImage() {
        return image;
    }

    public String getCaption() {
        return caption;
    }

    public boolean isShown() {
        return shown;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setImage(Image image) {
        this.image = Objects.requireNonNull(image, "image");
    }

    public void setCaption(String caption) {
        this.caption = Objects.requireNonNull(caption, "caption");
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }
}
