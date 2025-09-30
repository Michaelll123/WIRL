package MyUtils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class DialogUtil {
    public static void showInfoDialog(Project project, String message) {
        Messages.showMessageDialog(
                project,
                message,
                "Information",
                Messages.getInformationIcon()
        );
    }

    public static void showErrorDialog(Project project, String message) {
        Messages.showErrorDialog(
                project,
                message,
                "Error"
        );
    }

    public static void showWarningDialog(Project project, String message) {
        Messages.showWarningDialog(
                project,
                message,
                "Warning"
        );
    }

}
