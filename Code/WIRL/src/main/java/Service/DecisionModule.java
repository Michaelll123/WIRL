package Service;

import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

public class DecisionModule {


    interface Assistant {
//        @UserMessage("Extract all the values of <infill>s into a String List object.")
        GatheredInformation chat(String userMessage);
    }
//    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
//    private static final String ALIYUN_API_KEY = System.getenv("ALIYUN_API_KEY");
    private static final String API_URL = "https://www.DMXapi.com/v1/";
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
    public ChatLanguageModel model = null;
    private static final double TEMPERATURE = 0.0f;
    private static Assistant assistant = null;
    public TokenCountEstimator tokenEstimator = null;
    public ContextProvider contextProvider;
    public DecisionModule() {

        // 1. 初始化模型
        model = OpenAiChatModel.builder()
                .apiKey(ALIYUN_API_KEY)
                .baseUrl(API_URL)
                .modelName(MODEL_NAME)
                .temperature(TEMPERATURE)
                /*
                    make sure them settings are configured if you need structured outputs
                 */
//                .responseFormat("json_schema")
//                .strictJsonSchema(true)
//                .logRequests(true)
//                .logResponses(true)
                /*
                    make sure these settings are configured if you need use tool calls and both structured outputs
                 */
                .strictTools(true)

                .build();
        if (model != null) {
            tokenEstimator = (TokenCountEstimator) model;
        } else {
            System.out.println("The model does not support direct token estimation.");
        }
//        System.out.println(model);
        assistant = AiServices.builder(Assistant.class)
//                .hallucinatedToolNameStrategy()
                .chatLanguageModel(model)
                .tools(new ContextProvider())
                .systemMessageProvider((message) -> "You are a helpful code assistant.")
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
        contextProvider = new ContextProvider();
    }



    public GatheredInformation getResponse(String prompt) {

//        System.out.println(prompt);
        try{
            return assistant.chat(prompt);
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Error in DecisionModule.getResponse");
            return null;
        }

//        return assistant.chat(prompt);
    }
    public static void main(String [] args){
        String question = "What is the square root of the sum of the numbers of letters in the words \"hello\" and \"world\"?";
        DecisionModule decisionModule = new DecisionModule();
        System.out.println(decisionModule.getResponse(question));
    }
}
