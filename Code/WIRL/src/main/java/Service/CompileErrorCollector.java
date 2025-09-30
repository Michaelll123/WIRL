package Service;

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.List;

public class CompileErrorCollector {
    private final Project project;
    private final VirtualFile file;
    private final List<CompileError> errors = new ArrayList<>();

    public CompileErrorCollector(Project project, VirtualFile file) {
        this.project = project;
        this.file = file;
    }

    public static boolean isVariableReference(PsiImportList importList, PsiElement element, String symbolName) {
        if(element == null) return false;
        PsiElement parent = element.getParent();
        if(parent == null) return false;
        PsiElement pp = parent.getParent();
        if(pp instanceof PsiTypeElement){
            return false;
        }
        if (element instanceof PsiIdentifier) {
            if (parent instanceof PsiTypeElement) {
                return false;
            }
            if(parent instanceof PsiReferenceExpression psiReferenceExpression) {

                if (isClassImported(psiReferenceExpression, importList)) {
                    return false;
                }
            }

            if (!symbolName.isEmpty()) {
                return Character.isLowerCase(symbolName.charAt(0)) || symbolName.contains("_"); // 首字母小写，可能是变量
            }
        }

        return true;
    }

    public static boolean isClassImported(PsiReferenceExpression referenceExpression, PsiImportList importList) {
        if (referenceExpression == null) {
            return false;
        }

        String referenceName = referenceExpression.getReferenceName();
        if (referenceName == null) {
            return false;
        }

        if (importList == null) {
            return false;
        }

        for (PsiImportStatement importStatement : importList.getImportStatements()) {
            if (!importStatement.isOnDemand()) {
                String importPath = importStatement.getQualifiedName();
                if (importPath != null) {
                    String importedClassName = importPath.substring(importPath.lastIndexOf('.') + 1);
                    if (importedClassName.equals(referenceName)) {
                        return true;
                    }
                }
            }
        }



        return false;
    }

    public List<CompileError> collectErrors() {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) return errors;

        // Get the document for the file
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) return errors;

        // Get highlighting info using DaemonCodeAnalyzer
//        DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
//
//        // Wait for analysis to complete
//        DaemonCodeAnalyzer.getInstance(project).setUpdateByTimerEnabled(false);
//        if(DaemonCodeAnalyzer.getInstance(project).isHighlightingAvailable(psiFile)) {
//            DaemonCodeAnalyzer.getInstance(project).setUpdateByTimerEnabled(true);
//        }

        // Get the highlighting info
        HighlightInfo[] highlights = DaemonCodeAnalyzerImpl.getHighlights(document, null, project).toArray(new HighlightInfo[0]);
        for (HighlightInfo info : highlights) {
            if (info.getSeverity() == HighlightSeverity.ERROR) {
                errors.add(new CompileError(
                        info.getStartOffset(),
                        info.getEndOffset(),
                        info.getDescription()
                ));
//                for (Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker> quickFixActionMarker : info.quickFixActionMarkers) {
//                    System.out.println(quickFixActionMarker);
//                }

            }
        }

        return errors;
    }

    public static class ValidCompileError {
        private final int startOffset;
        private final int relativeStartOffset;
        private final int endOffset;
        private final int relativeEndOffset;
        private final String invalidIdentifier;

        public ValidCompileError(int startOffset, int endOffset, int relativeStartOffset, int relativeEndOffset, String message) {
            this.startOffset = startOffset;
            this.relativeStartOffset = relativeStartOffset;
            this.endOffset = endOffset;
            this.relativeEndOffset = relativeEndOffset;
            this.invalidIdentifier = message;
        }

        public int getRelativeStartOffset() {
            return relativeStartOffset;
        }

        public int getRelativeEndOffset() {
            return relativeEndOffset;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public String getInvalidIdentifier() {
            return invalidIdentifier;
        }
    }

    public static class CompileError {
        private final int startOffset;
        private final int endOffset;
        private final String message;

        public CompileError(int startOffset, int endOffset, String message) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.message = message;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public String getMessage() {
            return message;
        }

    }
}

