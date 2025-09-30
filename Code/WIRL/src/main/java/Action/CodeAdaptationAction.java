package Action;

import MyUtils.*;
import Service.*;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import edu.lu.uni.serval.utils.FileHelper;
import groovy.lang.Tuple2;
import org.gradle.internal.time.Time;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CodeAdaptationAction extends AnAction {


    public CodeAdaptationAction(){
        KeymapManager keymapManager = KeymapManager.getInstance();
        Keymap activeKeymap = keymapManager.getActiveKeymap();

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_A,
                InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

        activeKeymap.addShortcut("$CodeAdaptation", new KeyboardShortcut(keyStroke, null));
    }
    private final AtomicInteger commandCounter = new AtomicInteger(0);
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("entered");
        StringBuilder resultRecord = new StringBuilder();
        int totalToken = 0;
        int totalInputToken = 0;
        int totalOutputToken = 0;
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        assert editor != null;
        assert project != null;

        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            return;
        }
        int selectedStart = editor.getSelectionModel().getSelectionStart();
        int selectedEnd = editor.getSelectionModel().getSelectionEnd();
        /*
            locate the enclosing method of the selected text.
         */
        String enclosedMethodBody;
        FindEnclosedMethod findEnclosedMethod = new FindEnclosedMethod(project, editor);
        /*
            set context
         */
        ContextProvider contextProvider = new ContextProvider();

        contextProvider.project = project;
        contextProvider.currentFile = PsiDocumentManager.getInstance(project)
                .getPsiFile(editor.getDocument());
        /*
            get the enclosing method for the selected text
         */
        PsiMethod enclosedMethod = findEnclosedMethod.findMethod(contextProvider.currentFile);
        contextProvider.currentMethod = enclosedMethod;
        int methodStartOffset = Objects.requireNonNull(enclosedMethod.getSourceElement()).getTextRange().getStartOffset();
        int methodEndOffset = methodStartOffset + enclosedMethod.getTextLength();
        enclosedMethodBody = enclosedMethod.getText();
        if (enclosedMethodBody == null) return;
//        System.out.println(enclosedMethodBody);
        /*
            collect the resolved syntax errors by using the CompileErrorCollector class
         */
        CompileErrorCollector compileErrorCollector = new CompileErrorCollector(project, e.getData(CommonDataKeys.VIRTUAL_FILE));
        List<CompileErrorCollector.CompileError> compileErrors = compileErrorCollector.collectErrors();
//        for(CompileErrorCollector.CompileError error : compileErrors) {
//            System.out.println(error.getMessage());
//            System.out.println(error.getStartOffset());
//            System.out.println(error.getEndOffset());
//        }
        List<CompileErrorCollector.ValidCompileError> validCompileErrors = new ArrayList<>();
        StringBuilder prompt = new StringBuilder(enclosedMethodBody);
        /*
            locate the syntax errors that are within the enclosing method.
         */
        Pattern cannotResolvePattern = Pattern.compile("Cannot resolve symbol '(.*?)'");
        PsiJavaFile currentJavaFile = (PsiJavaFile) (contextProvider.currentFile);
        PsiImportList importList = currentJavaFile.getImportList();
        for(CompileErrorCollector.CompileError error : compileErrors){
            String message = error.getMessage();
//            System.out.println(message);
            Matcher matcher = cannotResolvePattern.matcher(message);
            if (matcher.find()) {
                String symbolName = matcher.group(1);
                int startOffset = error.getStartOffset();
                int endOffset = error.getEndOffset();
                if(startOffset >= methodStartOffset && endOffset <= methodEndOffset){
                    PsiElement element = enclosedMethod.findElementAt(startOffset-methodStartOffset);
                    if (element != null) {
                        if (CompileErrorCollector.isVariableReference(importList, element, symbolName)) {
                            String invalidIdentifier = enclosedMethodBody.substring(startOffset - methodStartOffset, endOffset - methodStartOffset);
                            validCompileErrors.add(new CompileErrorCollector.ValidCompileError(startOffset, endOffset, startOffset - methodStartOffset, endOffset - methodStartOffset, invalidIdentifier));
                        }
                    }
                }
            }

        }
        /*
            replace the syntax errors with the infill tag
         */
        resultRecord.append(project.getName()).append("###").append(contextProvider.currentFile.getName()).append("###");
//        HashMap<String,String> resultMemory = new HashMap<>();
        HashMap<Tuple2<String,String>, String> historyTracker = new HashMap<>();
//        String outputFileName = "E:\\CodeAdaptation\\Evaluation\\CCWHelper\\resultRecord_QwenMax.txt";
//        String outputFileName = "E:\\CodeAdaptation\\Evaluation\\CCWHelper\\resultRecord_Qwen2.5Coder32B.txt";
//        String outputFileName = "E:\\CodeAdaptation\\Evaluation\\CCWHelper\\resultRecord_Qwen2.5Coder3B.txt";
        String outputFileName = "E:\\CodeAdaptation\\Evaluation\\CCWHelper\\resultRecord_Qwen2.5Coder14B.txt";
        double startTime= System.currentTimeMillis();
        if(validCompileErrors.isEmpty() || (validCompileErrors.size() ==1 && !selectedText.equals(validCompileErrors.get(0).getInvalidIdentifier()))) {
//            DialogUtil.showErrorDialog(project,"not found valid syntax error");
//            return;
            contextProvider.index = 0;
            if(validCompileErrors.isEmpty())
                validCompileErrors.add(new CompileErrorCollector.ValidCompileError(selectedStart, selectedEnd, selectedStart - methodStartOffset, selectedEnd - methodStartOffset, selectedText));
            else{
                validCompileErrors.clear();
                validCompileErrors.add(new CompileErrorCollector.ValidCompileError(selectedStart, selectedEnd, selectedStart - methodStartOffset, selectedEnd - methodStartOffset, selectedText));
            }
            contextProvider.validCompileErrors = validCompileErrors;
            prompt.replace(selectedStart -methodStartOffset, selectedEnd - methodStartOffset,"<infill>");
            contextProvider.unresolvedVariableName = selectedText;
//            String realScenarioPrompt = retainLeftContext(prompt);
            String resultVariableName = null;
            ReAct reAct = new ReAct(contextProvider);
            resultVariableName = reAct.getResponse(prompt.toString(), historyTracker);
            resultRecord.append(contextProvider.unresolvedVariableName).append(":").append(resultVariableName).append(" ");
            totalToken += reAct.totalTokens;
            totalInputToken += reAct.totalInputTokens;
            totalOutputToken += reAct.totalOutputTokens;
//            resultMemory.put(ContextProvider.unresolvedVariableName,resultVariableName);
            String finalResultVariableName = resultVariableName;
            double endTime= System.currentTimeMillis();
            Runnable runnable = () -> {
                Document document = editor.getDocument();
                document.replaceString(selectedStart, selectedEnd, finalResultVariableName.trim());
            };
            WriteCommandAction.runWriteCommandAction(project, runnable);

            double time = (endTime - startTime);
            resultRecord.append("###").append(time).append("###").append(totalInputToken).append("###").append(totalOutputToken).append("###").append(totalToken).append("\n");
            FileHelper.outputToFile(outputFileName, resultRecord,true);
            return;
        }


        /*
            masked the unresolved variables once for all, and discriminate them by different index
         */
        contextProvider.validCompileErrors = validCompileErrors;
        final int[] outputOffset = {0};
        int offset = 0;
        double time = 0;
        Map<Integer, String> index2IdentifierMap = new HashMap<>();
        Map<String,Integer > identifier2IndexMap = new HashMap<>();
        List<String> invalidIdentifierList = new ArrayList<>();
        for(CompileErrorCollector.ValidCompileError error : validCompileErrors){
            String invalidIdentifier = error.getInvalidIdentifier();
            invalidIdentifierList.add(invalidIdentifier);
        }
        int tempOffset = 0;
        int index = 0;
        for(int i=0;i<validCompileErrors.size();i++){
            CompileErrorCollector.ValidCompileError error = validCompileErrors.get(i);
            int relativeStartOffset = error.getRelativeStartOffset();
            int relativeEndOffset = error.getRelativeEndOffset();
            String invalidIdentifier = error.getInvalidIdentifier();
            if(!index2IdentifierMap.containsValue(invalidIdentifier)){
                index2IdentifierMap.put(index, invalidIdentifier);
                identifier2IndexMap.put(invalidIdentifier,index);
                prompt.replace(relativeStartOffset + tempOffset, relativeEndOffset + tempOffset,"<"+ index +"-infill>");
                tempOffset += ("<"+ index +"-infill>").length() - invalidIdentifier.length();
                index++;
            }
            else{
                int tempIndex = identifier2IndexMap.get(invalidIdentifier);
                prompt.replace(relativeStartOffset + tempOffset, relativeEndOffset + tempOffset,"<"+tempIndex +"-infill>");
                tempOffset += ("<"+tempIndex +"-infill>").length() - invalidIdentifier.length();
            }
        }
        Map<Integer,String> resultMap = new HashMap<>();
        for(int k=0;k<index2IdentifierMap.size();k++){
            String kVariableName = index2IdentifierMap.get(k);
            contextProvider.unresolvedVariableName = kVariableName;
            contextProvider.index =invalidIdentifierList.indexOf(contextProvider.unresolvedVariableName);
            ReAct reAct = new ReAct(contextProvider);
            String resultVariableName = null;
            double eachIterStartTime = System.currentTimeMillis();
            resultVariableName = reAct.getResponse(prompt.toString(), historyTracker);
//            resultMemory.put(ContextProvider.unresolvedVariableName,resultVariableName);
            totalToken += reAct.totalTokens;
            totalInputToken += reAct.totalInputTokens;
            totalOutputToken += reAct.totalOutputTokens;
            double eachIterEndTime = System.currentTimeMillis();

            double eachIterTime = (eachIterEndTime - eachIterStartTime);
            time += eachIterTime;
            resultMap.put(k,resultVariableName);
            if(k<index2IdentifierMap.size()-1){
                prompt = new StringBuilder(prompt.toString().replace("<" + k + "-infill>", resultVariableName));
//                int tempOffset1 = 0;
//                for(int m = 0;m <validCompileErrors.size();m++){
//                    CompileErrorCollector.ValidCompileError validCompileError = validCompileErrors.get(m);
//                    String invalidIdentifier = validCompileError.getInvalidIdentifier();
//                    int relativeStartOffset = validCompileError.getRelativeStartOffset();
//                    if(invalidIdentifier.equals(kVariableName)){
//                        prompt.replace(relativeStartOffset + tempOffset1, relativeStartOffset + tempOffset1+ ("<"+k +"-infill>").length() ,resultVariableName.trim());
//                        tempOffset1 += resultVariableName.length() - invalidIdentifier.length() ;
//                    }
//                }
            }
        }
        for(int i=0;i<validCompileErrors.size();i++){
            int finalI = i;
            String invalidIdentifier = validCompileErrors.get(i).getInvalidIdentifier();
            int id = identifier2IndexMap.get(invalidIdentifier);
            resultRecord.append(invalidIdentifier).append(":").append(resultMap.get(id).trim()).append(" ");
            Runnable runnable = () -> {
                Document document = editor.getDocument();
                document.replaceString(validCompileErrors.get(finalI).getStartOffset()+ outputOffset[0], validCompileErrors.get(finalI).getEndOffset()+ outputOffset[0], resultMap.get(id).trim());
                outputOffset[0] = outputOffset[0] + resultMap.get(id).trim().length() - validCompileErrors.get(finalI).getInvalidIdentifier().length();
            };
            WriteCommandAction.runWriteCommandAction(project, runnable);
        }
        resultRecord.append("###").append(time).append("###").append(totalInputToken).append("###").append(totalOutputToken).append("###").append(totalToken).append("\n");

        FileHelper.outputToFile(outputFileName, resultRecord,true);

    }
}
