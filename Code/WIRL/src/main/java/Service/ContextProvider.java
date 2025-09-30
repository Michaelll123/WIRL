package Service;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ContextProvider {

    public Project project;
    public PsiFile currentFile;
    public PsiMethod currentMethod;
    public int index;
    public String receiver;
    public String argumentType;
    public String parameterName;
    public String variableNameOfType;
    public String fieldStrs;
    public List<CompileErrorCollector.ValidCompileError> validCompileErrors;
    public String unresolvedVariableName;
    public String methodNamesInClass;
    public String finalAnswer;
    public final BiMap<String,String> typeMapping;
    public List<PsiVariable> availableVariables;
    public List<PsiVariable> unusedVariables;
    public String parentFunctionName;
    public String invokedFunctionName;
    public int parameterIndex;

    public ContextProvider() {
        typeMapping = HashBiMap.create();
        typeMapping.put("java.lang.Integer","int");
        typeMapping.put("java.lang.Double","double");
        typeMapping.put("java.lang.Float","float");
        typeMapping.put("java.lang.Boolean","boolean");
        typeMapping.put("java.lang.Byte","byte");
        typeMapping.put("java.lang.Short","short");
        typeMapping.put("java.lang.Long","long");
        typeMapping.put("java.lang.Character","character");
        project = null;
        currentFile = null;
        currentMethod = null;
        index = 0;
        validCompileErrors = null;
        finalAnswer = null;
        unresolvedVariableName = null;
        receiver = null;
        argumentType = null;
        parameterName = null;
        parentFunctionName = null;
        invokedFunctionName = null;
        variableNameOfType = null;
        fieldStrs = null;
        parameterIndex = -1;
        methodNamesInClass = null;
        availableVariables = new ArrayList<>();
        unusedVariables = new ArrayList<>();
    }
    public void reset(){
        receiver = null;
        argumentType = null;
        parameterName = null;
        parentFunctionName = null;
        variableNameOfType = null;
        invokedFunctionName = null;
        fieldStrs = null;
        methodNamesInClass = null;
        availableVariables = new ArrayList<>();
        unusedVariables = new ArrayList<>();
        parameterIndex = -1;
    }
    public BiMap<String, String> getTypeMapping() {
        return typeMapping;
    }

    @Tool("sort the available variables by the literal similarity with the unresolved variable")
    public void sortAvailableVariablesByLiteralSimilarity(){
        System.out.println("Tool Called: sortAvailableVariablesByLiteralSimilarity");
        if (currentFile == null || availableVariables == null || availableVariables.isEmpty()) return;
//        System.out.println(availableVariables);
        availableVariables.sort(new Comparator<PsiVariable>() {
            @Override
            public int compare(PsiVariable o1, PsiVariable o2) {
                String variableName1 = o1.getName();
                String variableName2 = o2.getName();
                double similarity1 = calculateEditDistance(variableName1, unresolvedVariableName);
//                System.out.println(similarity1);
                double similarity2 = calculateEditDistance(variableName2, unresolvedVariableName);
//                System.out.println(similarity2);
                double ceil = 1000 * (similarity2 - similarity1);
                return (int) ceil;
            }
        });
//        System.out.println(availableVariables);
    }

//    @Tool("Retrieve the field names of the current class where the given method name are enclosed")
//    public void retrieveAllFields(){
//        System.out.println("Tool Called: retrieveAllFields");
//        StringBuilder fields = new StringBuilder();
//        if (currentFile == null) return;
//        CompileErrorCollector.ValidCompileError validCompileError = validCompileErrors.get(index);
//        PsiElement element = currentFile.findElementAt(validCompileError.getStartOffset());
//        PsiClass containingClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
//        if (containingClass == null) {
//            return;
//        }
//        PsiField[] classFields = containingClass.getFields();
//        for (PsiField field : classFields) {
//            fields.append(field.getName()).append(" ");
//        }
//        fieldStrs = fields.toString().trim();
//    }

    //    @Tool("Guess the type of the missing variable")
//    public void guessTypeOfMissingVariable(){
//        System.out.println("Tool Called: guessTypeOfMissingVariable ");
//        for(int i=0; i< ContextProvider.validCompileErrors.size(); i++){
//            int startOffset = validCompileErrors.get(i).getStartOffset();
//            PsiElement element = currentFile.findElementAt(startOffset);
//            if(element.getParent())
//        }
//    }
//    @Tool("Get the types of the unused variables")
//    public void getTypeOfUnusedVariables(){
//        System.out.println("Tool Called: getTypeOfUnusedVariables");
//        unusedVariableTypes = "";
//        for(PsiVariable variable: ContextProvider.unusedVariables){
//            unusedVariableTypes = unusedVariableTypes + variable.getType().getCanonicalText() + " ";
//        }
//    }
    @Tool("Since we know the type of unresolved variables, this tool only reserve the type-compatible variables")
    public void reserveTypeCompatibleVariables(@P("The type of the unresolved variables") String variableType){
        System.out.println("Tool Called: reserveTypeCompatibleVariables");
        if(availableVariables.isEmpty() || argumentType == null){
            return;
        }
        if(!unusedVariables.isEmpty()){
            unusedVariables.removeIf( e-> {
                String type = e.getType().getCanonicalText();
                if (type.contains(".")) {
                    type = type.substring(type.lastIndexOf('.') + 1);
                }
                String missingVariableType;
                if(variableType == null){
                    missingVariableType = argumentType;
                }
                else{
                    missingVariableType = variableType;
                }
                if (missingVariableType.contains(".")) {
                    missingVariableType = missingVariableType.substring(type.lastIndexOf('.') + 1);
                }
                return !type.endsWith(missingVariableType) && !type.startsWith(missingVariableType) && !type.contains(missingVariableType) &&!type.equalsIgnoreCase(missingVariableType);
            });
        }
        availableVariables.removeIf( e-> {
            String type = e.getType().getCanonicalText();
            if (type.contains(".")) {
                type = type.substring(type.lastIndexOf('.') + 1);
            }
            String missingVariableType;
            if(variableType == null){
                missingVariableType = argumentType;
            }
            else{
                missingVariableType = variableType;
            }
            if (missingVariableType.contains(".")) {
                missingVariableType = missingVariableType.substring(type.lastIndexOf('.') + 1);
            }
            return !type.endsWith(missingVariableType) && !type.startsWith(missingVariableType) && !type.contains(missingVariableType) &&!type.equalsIgnoreCase(missingVariableType);
        });
    }



    @Tool("Retrieve the variable that invoke identical function calls. Since " +
            "the function call is identical, the receiver of the function call is most likely the same.")
    public void retrieveVariableInvokingIdenticalFunctionCall(){
        System.out.println("Tool Called: retrieveVariableInvokingIdenticalFunctionCall");

        currentMethod.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                if (Objects.equals(expression.getMethodExpression().getReferenceName(), invokedFunctionName)) {
                    PsiExpression qualifierExpression = expression.getMethodExpression().getQualifierExpression();
                    if (qualifierExpression != null) {
                        if(!qualifierExpression.getText().equals(unresolvedVariableName)){
                            receiver = qualifierExpression.getText();
                            System.out.println("receiver: " + receiver);
                            // Stop visiting after finding the first occurrence
                            throw new AbortSearchException();
                        }

                    }

                }
            }
        });
        if(receiver==null){
            currentFile.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                    super.visitMethodCallExpression(expression);
                    if (Objects.equals(expression.getMethodExpression().getReferenceName(), invokedFunctionName)) {
                        PsiExpression qualifierExpression = expression.getMethodExpression().getQualifierExpression();
                        if (qualifierExpression != null) {
                            if(!qualifierExpression.getText().equals(unresolvedVariableName)){
                                receiver = qualifierExpression.getText();
                                System.out.println("receiver: " + receiver);
                                // Stop visiting after finding the first occurrence
                                throw new AbortSearchException();
                            }
                        }
                    }
                }
            });
        }
    }
    //    @Tool("get the specified parameter type and name in the function call." )
//    public void getSpecifiedParameterTypeAndName() {
//        System.out.println("Tool Called: getSpecifiedParameterTypeAndName");
//        if(parameterIndex == -1){
//            return;
//        }
//        CompileErrorCollector.ValidCompileError validCompileError = validCompileErrors.get(index);
//        PsiElement argument = currentFile.findElementAt(validCompileError.getStartOffset());
//        if (argument == null) {
//            return;
//        }
//        PsiElement parent = argument.getParent();
//        while(!(parent instanceof PsiMethodCallExpression) && !(parent instanceof PsiNewExpression)){
//            parent = parent.getParent();
//            if(parent == null) return;
//        }
//        PsiMethod method = null;
//        if(parent instanceof PsiMethodCallExpression){
//            method =  ((PsiMethodCallExpression) parent).resolveMethod();
//        }
//        if(parent instanceof PsiNewExpression){
//            method = ((PsiNewExpression) parent).resolveConstructor();
//        }
//        if (method != null) {
//            contextFunctionName = method.getName();
//            PsiParameterList parameterList = method.getParameterList();
//            PsiParameter [] parameters = parameterList.getParameters();
//            argumentType = parameters[parameterIndex].getType().getCanonicalText();
//            parameterName = parameters[parameterIndex].getName();
//        }
//    }
//    @Tool("Since the context is sufficient, now based on the previous thoughts, actions, and observations, " +
//            "we can directly infer the correct answers by code completion.")
//    public void executeCompletion(@P("The final prompt with all thoughts, actions, and observations reserved.") String finalPrompt) {
//        VariableResult result = CodeCompletion.getResponse(finalPrompt);
//        if (result!=null){
//            finalAnswer = result.getVariableName();
//        }
//        else{
//            DialogUtil.showErrorDialog(project, "Execution Module Failed.");
//            finalAnswer = "initial";
//        }
//    }
    @Tool("get the method names of the class indicated by the given class name." +
            "If no available variables are found, this tool should be called to try to find " +
            "method names that return the same type of data in the given class")
    public void getMethodNames(@P("The name of the class") String className, @P("The data type of the target variable") String dataType) {
        System.out.println("Tool Called: getMethodNames");
        StringBuilder methodNames = new StringBuilder();

        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        PsiClass psiClass = psiFacade.findClass(className, scope);

        if (psiClass != null) {
            collectMethodNames(dataType, psiClass, methodNames);
            for (PsiClassType extendsListType : psiClass.getExtendsListTypes()) {
                PsiClass classObject = extendsListType.resolve();
                if(classObject != null){
                    collectMethodNames(dataType, classObject, methodNames);
                }
            }
            for(PsiClassType implementsListType : psiClass.getImplementsListTypes()){
                PsiClass classObject = implementsListType.resolve();
                if(classObject != null){
                    collectMethodNames(dataType, classObject, methodNames);
                }
            }

        }
        if(methodNames.length()!=0){
            methodNamesInClass = methodNames.toString();
        }
        System.out.println("Method names in class: " + methodNamesInClass);
    }

    private void collectMethodNames(String dataType, PsiClass psiClass, StringBuilder methodNames) {
        BiMap<String,String> typeMapping = getTypeMapping();
        PsiMethod[] methods = psiClass.getMethods();

        for (PsiMethod method : methods) {
//            String parameterList = method.getParameterList().toString();
            PsiType returnType = method.getReturnType();
            if(returnType != null ){
                String canonicalText = returnType.getCanonicalText();
                canonicalText = canonicalText.substring(canonicalText.lastIndexOf('.') + 1);
                dataType = dataType.substring(dataType.lastIndexOf('.') + 1);
                if(canonicalText.equals(dataType)){
                    methodNames.append(method.getName()).append("\n");
                }
                else{
                    if(typeMapping.containsKey(canonicalText) && typeMapping.get(canonicalText).equals(dataType)){
                        methodNames.append(method.getName()).append("\n");
                    }
                    else{
                        BiMap<String, String> inverse = typeMapping.inverse();
                        if(inverse.containsKey(canonicalText) && inverse.get(canonicalText).equals(dataType)){
                            methodNames.append(method.getName()).append("\n");
                        }
                    }
                }
            }
        }
    }

    @Tool("get the available variables in the current context")
    public void getAvailableVariables() {
        CompileErrorCollector.ValidCompileError validCompileError = validCompileErrors.get(index);
        int startOffset = validCompileError.getStartOffset();
        PsiElement element = currentFile.findElementAt(startOffset);
        if (element == null) {
            return;
        }


        PsiParameter[] parameters = currentMethod.getParameterList().getParameters();
        availableVariables.addAll(Arrays.asList(parameters));

        final List<PsiLocalVariable> localVariables = new ArrayList<>();
        PsiElement e = currentMethod.findElementAt(validCompileError.getRelativeStartOffset());
        if(e == null) return;
        PsiElement parent = e.getParent();
        while (!(parent instanceof PsiStatement)) {
            parent = parent.getParent();
            if(parent == null){
                return;
            }
        }
        int startOffsetStatement = parent.getTextOffset();

        currentMethod.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitLocalVariable(PsiLocalVariable variable) {
                if (variable.getTextRange().getStartOffset() < (startOffsetStatement )) {
                    localVariables.add(variable);
                }
            }
        });

        availableVariables.addAll(localVariables);
        currentFile.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitField(@NotNull PsiField field) {
                super.visitField(field);
                availableVariables.add(field);
            }
        });
    }

    public boolean isVariableUnused(PsiVariable variable) {
        return ReferencesSearch.search(variable).findAll().isEmpty();
    }

    @Tool("get the available and unused variables in the current context")
    public void getUnusedVariables() {
        if(availableVariables.isEmpty()){return;}
        for (PsiVariable variable : availableVariables) {
            boolean variableUnused = isVariableUnused(variable);
            if(variableUnused){
                unusedVariables.add(variable);
            }
//            PsiReference[] references = ReferencesSearch.search(variable).findAll().toArray(new PsiReference[0]);
//            if(variable instanceof PsiParameter){
//                if (references.length == 1) {
//                    unusedVariables.add(variable);
//                }
//            }
//            else{
//                if (references.length == 0) {
//                    unusedVariables.add(variable);
//                }
//            }

        }
    }

    /*
        judge whether the existing variables are literally similar to the missing variable.
     */
    public static double calculateEditDistance(String variableName, String missingVariableName){
        if(variableName == null || missingVariableName == null) return Integer.MAX_VALUE;
        NormalizedLevenshtein nl = new NormalizedLevenshtein();
        return (1 - nl.distance(variableName, missingVariableName));
    }

    public String isReceiver() {
        CompileErrorCollector.ValidCompileError validCompileError = validCompileErrors.get(index);
        return isReceiver(currentMethod, validCompileError.getRelativeStartOffset());
    }

    public String isReceiver(PsiMethod psiMethod, int startOffset) {
        PsiElement element = psiMethod.findElementAt(startOffset);
        if (element == null) return "false";

        PsiElement parent = element.getParent();
        while(!(parent.getText().equals(unresolvedVariableName))) {
            parent = parent.getParent();
            if (parent == null) return "false";
        }
        parent = parent.getParent();
        while(!(parent instanceof PsiMethodCallExpression methodCallExpression)){
            parent = parent.getParent();
            if(parent == null) return "false";
        }
        PsiExpression qualifierExpression = methodCallExpression.getMethodExpression().getQualifierExpression();
        String functionName = methodCallExpression.getMethodExpression().getReferenceName();
        if (qualifierExpression!=null && qualifierExpression.getText().equals(unresolvedVariableName)) {
            invokedFunctionName = functionName;
            return "true";
        }
        return "false";
    }
    public String isArgument() {
        CompileErrorCollector.ValidCompileError validCompileError = validCompileErrors.get(index);
        return isArgument(currentMethod, validCompileError.getRelativeStartOffset());
    }

    public String isArgument(PsiMethod psiMethod, int startOffset){
        PsiElement element = psiMethod.findElementAt(startOffset);
        if (element == null) return "false";
        PsiElement containingExpr = element.getParent();
        while(!(containingExpr.getText().equals(unresolvedVariableName))) {
            containingExpr = containingExpr.getParent();
            if (containingExpr == null) return "false";
        }
        PsiElement parent = containingExpr.getParent();
        while(!(parent instanceof PsiMethodCallExpression) && !(parent instanceof PsiNewExpression)){
            parent = parent.getParent();
            if (parent == null) return "false";
        }
        PsiExpressionList argumentList = null;
        PsiMethod resolvedMethod = null;
        if (parent instanceof PsiMethodCallExpression methodCall) {
            argumentList = methodCall.getArgumentList();
            resolvedMethod = methodCall.resolveMethod();
        }
        if(parent instanceof PsiNewExpression newExpression) {
            argumentList = newExpression.getArgumentList();
            resolvedMethod = newExpression.resolveConstructor();
        }
        if (argumentList == null) {return "false";}
        PsiExpression[] arguments = argumentList.getExpressions();
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] == containingExpr) {
                parameterIndex = i;
                break;
            }
        }
        if (resolvedMethod != null) {
            parentFunctionName = resolvedMethod.getName();
            PsiParameter[] parameters = resolvedMethod.getParameterList().getParameters();
            parameterName = parameters[parameterIndex].getName();
            argumentType = parameters[parameterIndex].getType().getCanonicalText();
        }
        return "true";
    }

}
