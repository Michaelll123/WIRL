package Service;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static MyUtils.FileUtil.readFileToString;

public class CodeCompletion {

    interface Assistant {
        //        @UserMessage("Extract all the values of <infill>s into a String List object.")
        VariableResult chat(String userMessage);
    }
//    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String API_URL = "https://www.DMXapi.com/v1/";
//    private static final String ALIYUN_API_KEY = System.getenv("ALIYUN_API_KEY");
    private static final String ALIYUN_API_KEY = System.getenv("OPENAI_API_KEY");
    //        private static final String MODEL_NAME = "deepseek-r1:70b";
//    private static final String MODEL_NAME = "llama3.1:8b";
//    private static final String MODEL_NAME = "qwen2.5-coder:7b";
//    private static final String MODEL_NAME = "deepseek-v3";
//    private static final String MODEL_NAME = "qwen-coder-turbo";
//    private static final String MODEL_NAME = "qwen2.5-coder-32b-instruct";
//    private static final String MODEL_NAME = "qwen2.5-coder-3b-instruct";
//    private static final String MODEL_NAME = "qwen2.5-coder-14b-instruct";
    private static final String MODEL_NAME = "gpt-4o-mini-2024-07-18";
//    private static final String MODEL_NAME = "qwen-max";
//        private static final String MODEL_NAME = "qwen2.5-coder:32b";
    private static final double TEMPERATURE = 0.0f;
    public ChatLanguageModel model = null;
    private static Assistant assistant = null;
    public TokenCountEstimator tokenEstimator = null;
    public CodeCompletion(){
        // 1. 初始化模型
        model = OpenAiChatModel.builder()
                .apiKey(ALIYUN_API_KEY)
                .baseUrl(API_URL)
                .modelName(MODEL_NAME)
                .temperature(TEMPERATURE)
                /*
                    make sure them settings are configured if you need structured outputs
                 */
//                .responseFormat("json_object")
//                .strictJsonSchema(true)
//                .logRequests(true)
//                .logResponses(true)
                /*
                make sure these settings are configured if you wanna use tool calls
                 */
//                .strictTools(true)
                .build();
        if (model != null) {
            tokenEstimator = (TokenCountEstimator) model;
        } else {
            System.out.println("The model does not support direct token estimation.");
        }
        assistant = AiServices.builder(Assistant.class)
//                .hallucinatedToolNameStrategy()
                .chatLanguageModel(model)
//                .tools(new ContextProvider())
                .systemMessageProvider((message) -> "You are a helpful code assistant.")
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    public VariableResult getResponse(String prompt) {
        System.out.println(prompt);

        // load the prompt
//        String filePath = "E:\\CodeAdaptation\\Prompts\\Execution_Completion.txt";
////        String filePath = "E:\\CodeAdaptation\\Prompts\\Execution.txt";
//        String command = readFileToString(filePath);
//        String command =
////                "You are a helpful assistant." +
//                "Please complete the following code snippets where the to-be-completed places are marked with <infill>.\n" +
//
////                "You can use tools when necessary to complete the code, and the tools are provided via ContextProvider class.\n" +
////                "Only use tools that are listed as follows:\n" +
////                "1. retrieveFields(String methodName): Retrieve the fields of a class where the given method name are enclosed.\n" +
////                "2. retrieveIdenticalFunctionCall(String functionName): Retrieve the variables that have identical function calls.\n" +
//
//                "Only output the values of <infill>s with commas as the separators, e.g., a,b,c\n";
////        Output the value of <infills> into the String List where each " +
////        "element corresponding to each <infill>.
////                "Output the value of <infills> into the String List.\n";
        // 2. 调用生成
        try{
            return assistant.chat(prompt);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
//        return assistant.chat(prompt);
    }
    public static void main(String [] args){
        String question = "What is the square root of the sum of the numbers of letters in the words \"hello\" and \"world\"?";
        CodeCompletion cc = new CodeCompletion();
        System.out.println(cc.model.chat(question));
//        ClassLoader classLoader = CodeCompletion.class.getClassLoader();
//        InputStream stream = classLoader.getResourceAsStream("AutoGPT.txt");
//        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//        String command;
//        Stream<String> lines = reader.lines();
//        command = lines.collect(Collectors.toSet()).stream().collect(Collectors.joining());
//        CodeCompletion codeCompletion = new CodeCompletion();
//        int tokenCount = codeCompletion.tokenEstimator.estimateTokenCount(command);
//        System.out.println(tokenCount);

    }
}
