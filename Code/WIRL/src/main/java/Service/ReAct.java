package Service;

import MyUtils.DialogUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.intellij.psi.PsiVariable;
import groovy.lang.Tuple2;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReAct {
    public static final String initialState = "[Initial State] Starting from this state. \n";
    public static final String insufficientState = "[Insufficient Context State] You need to collect more context by invoking tools\n";
    public static final String sufficientState = "[Sufficient Context State] Context is sufficient, and it is ready to execute completion.\n";
    public static final String initialStateTools = "[Available tools in initial state]\nGET_AVAILABLE_VARIABLES\nGET_UNUSED_VARIABLES\n";
    public static final String insufficientStateTools = """
            [Available tools in insufficient state]\
            RESERVE_TYPE_COMPATIBLE_VARIABLES
            RETRIEVE_VARIABLE_INVOKING_IDENTICAL_FUNCTION_CALL
            SORT_AVAILABLE_VARIABLES_BY_LITERAL_SIMILARITY
            GET_METHOD_NAMES
            """;
    public static final String sufficientStateTools = "[Available tools in sufficient state]\nEXECUTE_COMPLETION\n";
    public int totalInputTokens;
    public int totalOutputTokens;
    public int totalTokens;
    public ContextProvider contextProvider;
    public ReAct(ContextProvider contextProvider) {
        totalInputTokens=0;
        totalOutputTokens = 0;
        totalTokens = 0;
        this.contextProvider = contextProvider;
        this.contextProvider.reset();
    }

    public String getResponse(String prompt, HashMap<Tuple2<String,String>, String> historyTracker) {
        String finalAnswer = "initial";
        // load the prompt
//        String filePath1 = "E:\\CodeAdaptation\\Prompts\\Decision_Completion.txt";
//        String filePath1 = "E:\\CodeAdaptation\\Prompts\\ReAct.txt";
//        String filePath1 = "E:\\CodeAdaptation\\Prompts\\ReAct_1.txt";
//        String filePath1 = "E:\\CodeAdaptation\\Prompts\\AutoGPT.txt";
        ClassLoader classLoader = ReAct.class.getClassLoader();
        InputStream stream = classLoader.getResourceAsStream("AutoGPT.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String command;
        Stream<String> lines = reader.lines();
        command = lines.collect(Collectors.toSet()).stream().collect(Collectors.joining());
//        String filePath2 = "E:\\CodeAdaptation\\Prompts\\Few-shot-Example.txt";
//        String filePath = "E:\\CodeAdaptation\\Prompts\\Decision.txt";
//        String command = readFileToString(filePath1);
//        String example = readFileToString(filePath2);
//        String finalPrompt = command + example + prompt;
        String finalPrompt = command + "\n" + prompt;
        finalPrompt  = finalPrompt + "\nState Description:\n" + initialState + initialStateTools + insufficientState + insufficientStateTools + sufficientState + sufficientStateTools;
        int maxSteps = 1;
        DecisionModule decisionModule = new DecisionModule();
        finalPrompt += "\n[Initial State]\n";
        GatheredInformation availableInfo = handleAvailableVariables();
        if(availableInfo !=null){
            String thought = availableInfo.getThought();
            String action = availableInfo.getAction();
            JSONObject actionInput = availableInfo.getActionInput();
            String observation = executeTool(action,actionInput);
            if(!observation.startsWith("No") && !observation.startsWith("Invalid") && !observation.startsWith("Error")){
                finalPrompt += "\nThought: " + thought + "\nAction: " + action + "\nActionInput: " + actionInput + "\nObservation: " + observation + "\n";
                contextProvider.sortAvailableVariablesByLiteralSimilarity();
                String thought1 = "The available variables should be sorted by their literal similarity to the unresolved variable.";
                String action1 = "SORT_AVAILABLE_VARIABLES_BY_LITERAL_SIMILARITY";
                String jsonString = "{\"arg0\":\"null\"}";
                String observation1 = executeTool(action1, JSONObject.parseObject(jsonString));
                finalPrompt += "\nThought: " + thought1 + "\nAction: " + action1 + "\nActionInput: " + jsonString + "\nObservation: " + observation1 + "\n";
            }
        }
        GatheredInformation unusedInfo = handleUnusedVariables();
        if(unusedInfo !=null){
            String thought = unusedInfo.getThought();
            String action = unusedInfo.getAction();
            JSONObject actionInput = unusedInfo.getActionInput();
            String observation = executeTool(action,actionInput);
            if(!observation.startsWith("No") && !observation.startsWith("Invalid") && !observation.startsWith("Error")){
                finalPrompt += "\nThought: " + thought + "\nAction: " + action + "\nActionInput: " + actionInput + "\nObservation: " + observation + "\n";
            }
        }
        GatheredInformation receiverResult = handleReceiver();
        if(receiverResult !=null){
            String thought = receiverResult.getThought();
            String action = receiverResult.getAction();
            JSONObject actionInput = receiverResult.getActionInput();
            String observation = executeTool(action,actionInput);
            if(!observation.startsWith("No") && !observation.startsWith("Invalid") && !observation.startsWith("Error")){
                finalPrompt += "\nThought: " + thought + "\nAction: " + action + "\nActionInput: " + actionInput + "\nObservation: " + observation + "\n";
            }
        }
        Boolean result = handleArgument();
        if(result){
            GatheredInformation compatibleVariableInfo = reserveCompatibleVariables();
            if(compatibleVariableInfo !=null){
                String thought = compatibleVariableInfo.getThought();
                String action = compatibleVariableInfo.getAction();
                JSONObject actionInput = compatibleVariableInfo.getActionInput();
                String observation = executeTool(action,actionInput);
                if(!observation.startsWith("No") && !observation.startsWith("Invalid") && !observation.startsWith("Error")){
                    finalPrompt += "\nThought: " + thought + "\nAction: " + action + "\nActionInput: " + actionInput + "\nObservation: " + observation + "\n";
                }
            }

        }
        for (int i = 0; i < maxSteps; i++) {
            System.out.println("Step " + (i+1) + ":\n" );
            GatheredInformation gatheredInformation = null;
            gatheredInformation = decisionModule.getResponse(finalPrompt);
            int inputTokenCount = decisionModule.tokenEstimator.estimateTokenCount(finalPrompt);
            totalInputTokens +=inputTokenCount;
            if(gatheredInformation!=null){
                int outputTokenCount = decisionModule.tokenEstimator.estimateTokenCount(gatheredInformation.toString());
                totalOutputTokens +=outputTokenCount;
            }
            if(gatheredInformation == null){
                DialogUtil.showErrorDialog(contextProvider.project, "ReAct Failed.");
                break;
            }
            // 解析 Action
            String action = gatheredInformation.getAction();
            JSONObject actionInput = gatheredInformation.getActionInput();
            String thought = gatheredInformation.getThought();

            if (action.equals("EXECUTE_COMPLETION") || action.equals("executeCompletion")) {
                finalPrompt += "\n[Sufficient Context State]\n";
                break;
            }
            else{
                finalPrompt += "\n[Insufficient Context State]\n";
            }

            if(historyTracker.containsKey(new Tuple2<>(action,actionInput.toString()))){
                System.out.println("cache hit: " + action + ":" + actionInput);
                String observation = historyTracker.get(new Tuple2<>(action,actionInput.toString()));
                finalPrompt += "\nThought: " + thought + "\nAction: " + action + "\nActionInput: " + actionInput + "\nObservation: " + observation + "\n";
                continue;
            }

            // 执行工具
            String observation = executeTool(action,actionInput);
            historyTracker.put(new Tuple2<>(action,actionInput.toString()),observation);
            System.out.println("\nThought: " + thought + "\nAction: " + action + "\nActionInput: " + actionInput + "\nObservation: " + observation + "\n");
            finalPrompt += "\nThought: " + thought + "\nAction: " + action + "\nActionInput: " + actionInput + "\nObservation: " + observation + "\n";

        }
        CodeCompletion codeCompletion = new CodeCompletion();
        VariableResult response = codeCompletion.getResponse(finalPrompt);
        int inputTokenCount = codeCompletion.tokenEstimator.estimateTokenCount(finalPrompt);
        totalInputTokens +=inputTokenCount;
        if (response!=null){
            finalAnswer = response.getVariableName();
            int outputTokenCount = codeCompletion.tokenEstimator.estimateTokenCount(response.getVariableName());
            totalOutputTokens +=outputTokenCount;
        }
        else{
            DialogUtil.showErrorDialog(contextProvider.project, "Execution Module Failed.");
            finalAnswer = "initial";
        }
        totalTokens = totalInputTokens + totalOutputTokens;
        System.out.println("\nfinal answer: " + finalAnswer);
        return finalAnswer;
//        return assistant.chat(prompt);
    }

    private GatheredInformation reserveCompatibleVariables() {
        if (!contextProvider.availableVariables.isEmpty() && contextProvider.argumentType !=null) {
            String thought2 = "Since I already know the type of the unresolved variable, I can filter out the variables that are not compatible in types.";
            String jsonString = "{\"arg0\":\"null\"}";
            return new GatheredInformation(thought2, "RESERVE_TYPE_COMPATIBLE_VARIABLES", JSONObject.parseObject(jsonString));
        }
        return null;
    }

    public GatheredInformation handleUnusedVariables() {
        if (!contextProvider.availableVariables.isEmpty()) {
            contextProvider.getUnusedVariables();
            if (!contextProvider.unusedVariables.isEmpty()) {
                String thought2 = "I need to figure out whether there are unused variables in the current context. If there are any unused variables, " +
                        "I should analyze the context and decide which one is the correct answer.";
                String jsonString = "{\"arg0\":\"null\"}";
                return new GatheredInformation(thought2, "GET_UNUSED_VARIABLES", JSONObject.parseObject(jsonString));
            }
        }
        return null;
    }
    public GatheredInformation handleAvailableVariables(){
        contextProvider.getAvailableVariables();
        String thought1 = "I need to figure out the available variables in the current context.";
        String jsonString = "{\"arg0\":\"null\"}";
        return new GatheredInformation(thought1, "GET_AVAILABLE_VARIABLES", JSONObject.parseObject(jsonString));
    }
    public Boolean handleArgument(){
        String isArgument = contextProvider.isArgument();
        return !isArgument.equals("false");
    }

    private @Nullable GatheredInformation handleReceiver() {
        String isReceiver = contextProvider.isReceiver();
        if(!isReceiver.equals("false")){
            String thought = "Since the unresolved variable is a receiver in function call, " +
                    "we may need to retrieve the variables that invoke identical function calls." +
                    "If such a variable is found, then this variable may be the correct answer.";
            String jsonString = "{\"arg0\":\"null\"}";
            return new GatheredInformation(thought, "RETRIEVE_VARIABLE_INVOKING_IDENTICAL_FUNCTION_CALL", JSONObject.parseObject(jsonString));
        }
        return null;
    }

    private String executeTool(String action, JSONObject actionInput) {
        try{
            String input1 = null;
            String input2 = null;
            if(actionInput != null){
                if(actionInput.size() == 1){
                    input1 = (String) actionInput.get("arg0");
                }
                else if(actionInput.size() == 2){
                    input1 = (String) actionInput.get("arg0");
                    input2 = (String) actionInput.get("arg1");
                }
            }
            return switch (action) {
//                case "NO_NEED", "No_NEED" -> new ContextProvider().noNeed();、
                case "GET_AVAILABLE_VARIABLES", "getAvailableVariables" ->
                        contextProvider.availableVariables.isEmpty()? "No available variables found." : "The available variables are: " + contextProvider.availableVariables;
                case "GET_UNUSED_VARIABLES", "getUnusedVariables" ->{
                    String unusedStr = "";
                    for (PsiVariable variable: contextProvider.unusedVariables) {
                        unusedStr += variable.getName() + ":" + variable.getType().getCanonicalText() + " ";
                    }
                    yield contextProvider.unusedVariables.isEmpty()? "No unused variables found." : "The unused variables are: " + unusedStr;
                }
                case "RESERVE_TYPE_COMPATIBLE_VARIABLES", "reserveTypeCompatibleVariables" -> {
                    if(contextProvider.argumentType!=null){
                        contextProvider.reserveTypeCompatibleVariables(contextProvider.argumentType);
                    }
                    else{
                        contextProvider.reserveTypeCompatibleVariables(input1);
                    }
                    yield contextProvider.availableVariables.isEmpty()? "No type-compatible variables found." : "The variables of the same type " + contextProvider.argumentType + " with the target variable are: " + contextProvider.availableVariables;
                }
                case "RETRIEVE_VARIABLE_INVOKING_IDENTICAL_FUNCTION_CALL","retrieveVariableInvokingIdenticalFunctionCall" -> {
                    contextProvider.retrieveVariableInvokingIdenticalFunctionCall();
                    yield contextProvider.receiver == null? "No such variable found." : "There is a variable invoking the function call " + contextProvider.invokedFunctionName + ": " + contextProvider.receiver;
                }

                case "SORT_AVAILABLE_VARIABLES_BY_LITERAL_SIMILARITY","sortAvailableVariablesByLiteralSimilarity" -> {
                    contextProvider.sortAvailableVariablesByLiteralSimilarity();
                    yield contextProvider.availableVariables == null || contextProvider.availableVariables.isEmpty() ? "No such variable found." : "The available variable that are the most literally similar to "
                            + contextProvider.unresolvedVariableName + " or " + contextProvider.parameterName + " are: " + contextProvider.availableVariables.get(0);
                }
                case "GET_METHOD_NAMES","getMethodNames" -> {
                    if (contextProvider.argumentType == null) {
                        contextProvider.getMethodNames(input1,contextProvider.argumentType);
                    }
                    contextProvider.getMethodNames(input1,input2);
                    yield contextProvider.methodNamesInClass == null? "No methods found." : "The class " + input1 + " contains the following methods returning the same data type: " + input2 + "\n" + contextProvider.methodNamesInClass;
                }
                default -> throw new IllegalStateException("Unexpected value: " + action);
            };
        }
        catch(AbortSearchException e){
            System.out.println("AbortSearchException");
            return contextProvider.receiver!=null? "There is a variable invoking the function call " + actionInput+ ": " + contextProvider.receiver
                    :"No such variable found.";
        }
        catch (IllegalArgumentException e){
            System.out.println("IllegalArgumentException");
            return "Invalid action input: " + actionInput;
        }
        catch (Exception e){
            e.printStackTrace();
            return "Error in executing tool: " + action;
        }
    }


    public static void main(String [] args){
        String question = "What is the square root of the sum of the numbers of letters in the words \"hello\" and \"world\"?";
//        System.out.println(getResponse(question));
    }
}
