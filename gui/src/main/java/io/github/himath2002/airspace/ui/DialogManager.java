package io.github.himath2002.airspace.ui;

import javafx.scene.control.Alert;

/**
 * Creates consistent user-facing validation and lifecycle dialogs.
 */
public final class DialogManager {
    private DialogManager() {
        // Utility class.
    }

    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
