package MyUtils;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

public class FindEnclosedMethod {
    private final Project project;
    private final Editor editor;

    public FindEnclosedMethod(Project project, Editor editor) {
        this.project = project;
        this.editor = editor;
    }

    public PsiMethod findMethod(PsiFile psiFile) {
        // Get selected text range
        SelectionModel selectionModel = editor.getSelectionModel();
        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();

        // Get PsiFile from editor
        if (psiFile == null) return null;

        // Find PsiElement at selection
        PsiElement element = psiFile.findElementAt(start);
        if (element == null) return null;

        // Walk up parent elements to find containing method
        return PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    }

}
