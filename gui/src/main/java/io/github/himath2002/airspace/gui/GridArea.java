package io.github.himath2002.airspace.gui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Automatically scaling grid surface for positioned aircraft and airport icons.
 */
public final class GridArea extends Pane {
    private final double gridWidth;
    private final double gridHeight;
    private final List<GridAreaIcon> icons = new ArrayList<>();
    private boolean gridLines = true;
    private Color captionColour = Color.WHITE;
    private Canvas canvas;

    public GridArea(double gridWidth, double gridHeight) {
        if (gridWidth <= 0 || gridHeight <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive.");
        }
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    public void setGridLines(boolean gridLines) {
        this.gridLines = gridLines;
    }

    /**
     * Returns the mutable icon collection. Call {@link #requestLayout()} after changing it.
     */
    public List<GridAreaIcon> getIcons() {
        return icons;
    }

    public void setCaptionColour(Color captionColour) {
        this.captionColour = captionColour;
    }

    @Override
    public void layoutChildren() {
        super.layoutChildren();
        if (canvas == null) {
            canvas = new Canvas();
            canvas.widthProperty().bind(widthProperty());
            canvas.heightProperty().bind(heightProperty());
            getChildren().add(canvas);
        }

        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());

        double cellSize = Math.min(getWidth() / gridWidth, getHeight() / gridHeight);
        if (gridLines) {
            drawGrid(graphics, cellSize);
        }

        for (GridAreaIcon icon : icons) {
            if (icon.isShown()) {
                drawIcon(graphics, icon, cellSize);
            }
        }
    }

    private void drawGrid(GraphicsContext graphics, double cellSize) {
        graphics.setStroke(Color.DARKGREY);

        for (double column = 0.0; column < gridWidth; column++) {
            double x = (column + 0.5) * cellSize;
            graphics.strokeLine(
                    x, cellSize / 2.0, x, (gridHeight - 0.5) * cellSize);
        }

        for (double row = 0.0; row < gridHeight; row++) {
            double y = (row + 0.5) * cellSize;
            graphics.strokeLine(
                    cellSize / 2.0, y, (gridWidth - 0.5) * cellSize, y);
        }
    }

    private void drawIcon(GraphicsContext graphics, GridAreaIcon icon, double cellSize) {
        double x = (icon.getX() + 0.5) * cellSize;
        double y = (icon.getY() + 0.5) * cellSize;
        Image image = icon.getImage();

        double width;
        double height;
        if (image.getWidth() > image.getHeight()) {
            width = cellSize * icon.getScale();
            height = width * image.getHeight() / image.getWidth();
        } else {
            height = cellSize * icon.getScale();
            width = height * image.getWidth() / image.getHeight();
        }

        graphics.save();
        graphics.translate(x, y);
        graphics.rotate(icon.getRotation());
        graphics.drawImage(image, -width / 2.0, -height / 2.0, width, height);
        graphics.restore();

        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setTextBaseline(VPos.TOP);
        graphics.setStroke(captionColour);
        graphics.strokeText(icon.getCaption(), x, y + (cellSize / 2.0));
    }
}
