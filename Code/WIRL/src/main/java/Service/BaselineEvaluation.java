package Service;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import edu.lu.uni.serval.utils.FileHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class BaselineEvaluation {

    public static void main(String [] args) throws FileNotFoundException {
        evaluateLLM();
//        getIDEAPerformance();
//        getDeepSeekPerformance();
//        getQwenCoder14BPerformance();
//        getQwenMaxPerformance();
//        evaluateExampleStack();
    }

    private static void evaluateExampleStack() throws FileNotFoundException {

        int recommendedNamesCount = 0;
        int correctCount = 0;
        int totalCount = 223;
        StringBuilder resultRecords = new StringBuilder();
        String datasetFilePath = "E:\\CodeAdaptation\\Baselines\\ExampleStack\\100soUrls.txt";
        String filePath = "E:\\CodeAdaptation\\Baselines\\ExampleStack\\code\\Chrome-extension\\variation-dataset\\";
        ArrayList<String> soUrlsAndCount = FileHelper.readFileByLines(datasetFilePath);
        List<File> allSubDirectories = FileHelper.getAllSubDirectories(filePath);
        List<String> soIndexes = new ArrayList<>();
        for(File subDirectory : allSubDirectories){
            String subDirectoryPath = subDirectory.getAbsolutePath();
            List<File> allFiles = FileHelper.getAllFiles(subDirectoryPath,".java");
            List<String> referenceJavaFileList = new ArrayList<>();
            for(File file : allFiles){
                String fileName = file.getName();
//                System.out.println(fileName);
                if(fileName.startsWith("so")){
                    String [] split = fileName.split("-");
                    String soIndex = split[1];
//                    System.out.println(soIndex);
                    soIndexes.add(subDirectory.getName() + File.separator + soIndex);
                }
//                else{
//                    referenceJavaFileList.add(file.getAbsolutePath());
//                }
            }
        }

        for(String line: soUrlsAndCount){
            String [] split = line.split("\t");
            String soUrl = split[0];
            String count = split[1];
            for(String soIndex : soIndexes){
                if(soIndex.contains(soUrl)){
                    recommendedNamesCount += Integer.parseInt(count);
                    System.out.println(soIndex);
                    break;
                }
            }
//
        }
//        System.out.println(recommendedNamesCount);
    }



    interface Assistant {
//        @UserMessage("Extract all the values of <infill>s into a String List object.")
        AdaptationPair chat(String userMessage);
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
//    private static final String MODEL_NAME = "qwen-turbo";
//    private static final String MODEL_NAME = "qwen-max";
//    private static final String MODEL_NAME = "deepseek-v3";
//    private static final String MODEL_NAME = "qwen2.5-coder-14b-instruct";
//    private static final String MODEL_NAME = "qwen2.5-coder-32b-instruct";
    private static final String MODEL_NAME = "gpt-4o-mini-2024-07-18";
    private ChatLanguageModel model = null;
    public TokenCountEstimator tokenEstimator = null;
    private static final double TEMPERATURE = 0.0f;
    private static Assistant assistant = null;
    private int totalTokenCount;
    private int totalInputTokenCount;
    private int totalOutputTokenCount;

    public BaselineEvaluation() {
        totalTokenCount = 0;
        totalInputTokenCount = 0;
        totalOutputTokenCount = 0;
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
//                .strictTools(true)

                .build();
        if (model != null) {
            tokenEstimator = (TokenCountEstimator) model;
        } else {
            System.out.println("The model does not support direct token estimation.");
        }
        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .systemMessageProvider((message) -> "You are a helpful code assistant.")
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }



    public AdaptationPair getResponse(String prompt) {

//        System.out.println(prompt);
        // 2. 调用生成
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
    private static void getIDEAPerformance() throws FileNotFoundException {
        String filePath = "E:\\CodeAdaptation\\Baselines\\IDEA\\results.txt";
        ArrayList<String> lines = FileHelper.readFileByLines(filePath);
        System.out.println("Total Number of Lines: " + lines.size());
        int recommendedNamesCount = 0;
        int correctCount = 0;
        int totalCount = 0;
        StringBuilder resultRecords = new StringBuilder();
        for(int i =0;i<lines.size();i++){
            String line = lines.get(i);
            String [] split = line.split("\t");
            int cnt = Integer.parseInt(split[0]);
            String recommendedNames = split[1];
            String correctNames = split[2];
            String order = split[3];
            String totalRecommendedNames = split[4];
            if(recommendedNames.contains(",")&& correctNames.contains(",") && order.contains(",") && totalRecommendedNames.contains(",")){
                String [] orderSplit = order.split(",");
                String [] totalNumOfRecommendedNamesSplit = totalRecommendedNames.split(",");
                String [] recommendedNamesSplit = recommendedNames.split(",");
                String [] correctNamesSplit = correctNames.split(",");
                if(orderSplit.length == totalNumOfRecommendedNamesSplit.length && orderSplit.length == cnt){
                    for(int j1 = 0; j1 < recommendedNamesSplit.length; j1++){
                        resultRecords.append("Recommended Name: " + recommendedNamesSplit[j1].trim() + "\tCorrect Name: " + correctNamesSplit[j1].trim()).append("\n");
                        if(!recommendedNamesSplit[j1].trim().equals("null")){
                            recommendedNamesCount++;
                        }
                        if(recommendedNamesSplit[j1].trim().equalsIgnoreCase(correctNamesSplit[j1].trim())){
                            correctCount++;
                        }
                        totalCount++;
                    }
                }
                else{
                    System.out.println("Error in line " + i + ":" + line);
                }
            }
            else{
                resultRecords.append("Recommended Name: " + recommendedNames.trim() + " Correct Name: " + correctNames.trim()).append("\n");
                if(!recommendedNames.trim().equals("null")){
                    recommendedNamesCount++;
                }
                if(recommendedNames.trim().equalsIgnoreCase(correctNames.trim())){
                    correctCount++;
                }
                totalCount++;
            }
        }
        System.out.println("Total Number of Correct Answers: " + correctCount);
        System.out.println("Total Number of Recommended Names: " + recommendedNamesCount);
        System.out.println("Total Number of Answers: " + totalCount);
        System.out.println("Precision: " + (double)correctCount/recommendedNamesCount);
        System.out.println("Recall: " + (double)correctCount/totalCount);
//        FileHelper.outputToFile("E:\\CodeAdaptation\\Baselines\\IDEA\\resultRecords.txt", resultRecords,false);
    }

    private static void getQwenPerformance() throws FileNotFoundException {
        String filePath = "E:\\CodeAdaptation\\Baselines\\Qwen\\results_QwenMax.txt";
        String correctAnswerPath = "E:\\CodeAdaptation\\Baselines\\CorrectAnswer.txt";
        ArrayList<String> lines = FileHelper.readFileByLines(filePath);
        ArrayList<String> answers = FileHelper.readFileByLines(correctAnswerPath);
        System.out.println("Total Number of Lines: " + lines.size());
        System.out.println("Total Number of Answers: " + answers.size());
        int recommendedNamesCount = 0;
        int correctCount = 0;
        int totalCount = 0;
        StringBuilder resultRecords = new StringBuilder();
        for(int i =0;i<lines.size();i++){
            String line = lines.get(i);
            String [] split = line.split("###");
            if(split.length == 2 || split.length == 1){
                System.out.println("Error in line " + i + ":" + line);
                continue;
            }
            int index = Integer.parseInt(split[0]);
            String beforeAdaptation = split[1];
            String afterAdaptation = split[2];
            double timeCost = Double.parseDouble(split[3]);
            int inputTokenCount = Integer.parseInt(split[4]);
            int outputTokenCount = Integer.parseInt(split[5]);
            String answer = answers.get(i);
            String [] answerSplit = answer.split("\t");
            int cnt = Integer.parseInt(answerSplit[0]);
            String correctAnswers = answerSplit[1];
            if(correctAnswers.contains(",") || afterAdaptation.contains(",")){
                String [] afterAdaptationSplit = afterAdaptation.split(",");
                String [] correctAnswersSplit = correctAnswers.split(",");
                List<String> afterAdaptationList = Arrays.asList(afterAdaptationSplit);
                List<String> correctAnswersList = Arrays.asList(correctAnswersSplit);
                List<String> retainList = new ArrayList<>(correctAnswersList);
                retainList.retainAll(afterAdaptationList);
//                recommendedNamesCount+= afterAdaptationList.size();
//                correctCount+= retainList.size();
//                totalCount+= correctAnswersList.size();
//                resultRecords.append("Recommended Name: " + afterAdaptation.trim() + " Correct Name: " + correctAnswers.trim()).append("\n");
            }
            else{
                resultRecords.append("Recommended Name: " + afterAdaptation.trim() + " Correct Name: " + correctAnswers.trim()).append("\n");
                if(!afterAdaptation.trim().equals("null")){
                    recommendedNamesCount++;
                }
                if(afterAdaptation.trim().equalsIgnoreCase(correctAnswers.trim())){
                    correctCount++;
                }
                totalCount++;
            }

        }
        System.out.println("Total Number of Correct Answers: " + correctCount);
        System.out.println("Total Number of Recommended Names: " + recommendedNamesCount);
        System.out.println("Total Number of Answers: " + totalCount);
        System.out.println("Precision: " + (double)correctCount/recommendedNamesCount);
        System.out.println("Recall: " + (double)correctCount/totalCount);
        FileHelper.outputToFile("E:\\CodeAdaptation\\Baselines\\Qwen\\resultRecords_QwenMax.txt", resultRecords,false);

    }

    private static void getQwenCoder14BPerformance() throws FileNotFoundException {
        String filePath = "E:\\CodeAdaptation\\Baselines\\QwenCoder14B\\results_QwenCoder14B.txt";
        String correctAnswerPath = "E:\\CodeAdaptation\\Baselines\\CorrectAnswer.txt";
        ArrayList<String> lines = FileHelper.readFileByLines(filePath);
        ArrayList<String> answers = FileHelper.readFileByLines(correctAnswerPath);
        System.out.println("Total Number of Lines: " + lines.size());
        System.out.println("Total Number of Answers: " + answers.size());
        int recommendedNamesCount = 0;
        int correctCount = 0;
        int totalCount = 0;
        StringBuilder resultRecords = new StringBuilder();
        for(int i =0;i<lines.size();i++){
            String line = lines.get(i);
            String [] split = line.split("###");
            if(split.length == 2 || split.length == 1){
                System.out.println("Error in line " + i + ":" + line);
                continue;
            }
            int index = Integer.parseInt(split[0]);
            String beforeAdaptation = split[1];
            String afterAdaptation = split[2];
            double timeCost = Double.parseDouble(split[3]);
            int inputTokenCount = Integer.parseInt(split[4]);
            int outputTokenCount = Integer.parseInt(split[5]);
            String answer = answers.get(i);
            String [] answerSplit = answer.split("\t");
            int cnt = Integer.parseInt(answerSplit[0]);
            String correctAnswers = answerSplit[1];
            if(correctAnswers.contains(",") || afterAdaptation.contains(",")){
                String [] afterAdaptationSplit = afterAdaptation.split(",");
                String [] correctAnswersSplit = correctAnswers.split(",");
                List<String> afterAdaptationList = Arrays.asList(afterAdaptationSplit);
                List<String> correctAnswersList = Arrays.asList(correctAnswersSplit);
                List<String> retainList = new ArrayList<>(correctAnswersList);
                retainList.retainAll(afterAdaptationList);
//                recommendedNamesCount+= afterAdaptationList.size();
//                correctCount+= retainList.size();
//                totalCount+= correctAnswersList.size();
                resultRecords.append("Recommended Name: " + afterAdaptation.trim() + " Correct Name: " + correctAnswers.trim()).append("\n");
            }
            else{
                resultRecords.append("Recommended Name: " + afterAdaptation.trim() + " Correct Name: " + correctAnswers.trim()).append("\n");
//                if(!afterAdaptation.trim().equals("null")){
//                    recommendedNamesCount++;
//                }
//                if(afterAdaptation.trim().equalsIgnoreCase(correctAnswers.trim())){
//                    correctCount++;
//                }
//                totalCount++;
            }

        }
//        System.out.println("Total Number of Correct Answers: " + correctCount);
//        System.out.println("Total Number of Recommended Names: " + recommendedNamesCount);
//        System.out.println("Total Number of Answers: " + totalCount);
//        System.out.println("Precision: " + (double)correctCount/recommendedNamesCount);
//        System.out.println("Recall: " + (double)correctCount/totalCount);
        FileHelper.outputToFile("E:\\CodeAdaptation\\Baselines\\DeepSeek\\resultRecords_QwenCoder14B.txt", resultRecords,false);
    }

    private static void getQwenMaxPerformance() throws FileNotFoundException {
        String filePath = "E:\\CodeAdaptation\\Baselines\\Qwen-max\\results_QwenMax.txt";
        String correctAnswerPath = "E:\\CodeAdaptation\\Baselines\\CorrectAnswer.txt";
        ArrayList<String> lines = FileHelper.readFileByLines(filePath);
        ArrayList<String> answers = FileHelper.readFileByLines(correctAnswerPath);
        System.out.println("Total Number of Lines: " + lines.size());
        System.out.println("Total Number of Answers: " + answers.size());
        int recommendedNamesCount = 0;
        int correctCount = 0;
        int totalCount = 0;
        StringBuilder resultRecords = new StringBuilder();
        for(int i =0;i<lines.size();i++){
            String line = lines.get(i);
            String [] split = line.split("###");
            if(split.length == 2 || split.length == 1){
                System.out.println("Error in line " + i + ":" + line);
                resultRecords.append("Error\n");
                continue;
            }
            int index = Integer.parseInt(split[0]);
            String beforeAdaptation = split[1];
            String afterAdaptation = split[2];
            double timeCost = Double.parseDouble(split[3]);
            int inputTokenCount = Integer.parseInt(split[4]);
            int outputTokenCount = Integer.parseInt(split[5]);
            String answer = answers.get(i);
            String correctAnswers = answer;
            if(correctAnswers.contains(",") || afterAdaptation.contains(",")){
                String [] afterAdaptationSplit = afterAdaptation.split(",");
                String [] correctAnswersSplit = correctAnswers.split(",");
                List<String> afterAdaptationList = Arrays.asList(afterAdaptationSplit);
                List<String> correctAnswersList = Arrays.asList(correctAnswersSplit);
                List<String> retainList = new ArrayList<>(correctAnswersList);
                retainList.retainAll(afterAdaptationList);
//                recommendedNamesCount+= afterAdaptationList.size();
//                correctCount+= retainList.size();
//                totalCount+= correctAnswersList.size();
                resultRecords.append("Recommended Name: " + afterAdaptation.trim() + " Correct Name: " + correctAnswers.trim()).append("\n");
            }
            else{
                resultRecords.append("Recommended Name: " + afterAdaptation.trim() + " Correct Name: " + correctAnswers.trim()).append("\n");
//                if(!afterAdaptation.trim().equals("null")){
//                    recommendedNamesCount++;
//                }
//                if(afterAdaptation.trim().equalsIgnoreCase(correctAnswers.trim())){
//                    correctCount++;
//                }
//                totalCount++;
            }

        }
//        System.out.println("Total Number of Correct Answers: " + correctCount);
//        System.out.println("Total Number of Recommended Names: " + recommendedNamesCount);
//        System.out.println("Total Number of Answers: " + totalCount);
//        System.out.println("Precision: " + (double)correctCount/recommendedNamesCount);
//        System.out.println("Recall: " + (double)correctCount/totalCount);
        FileHelper.outputToFile("E:\\CodeAdaptation\\Baselines\\Qwen-max\\resultRecords_QwenMax.txt", resultRecords,false);
    }

    private static void getDeepSeekPerformance() throws FileNotFoundException {
        String filePath = "E:\\CodeAdaptation\\Baselines\\DeepSeek\\results_DeepSeekV3.txt";
        String correctAnswerPath = "E:\\CodeAdaptation\\Baselines\\CorrectAnswer.txt";
        ArrayList<String> lines = FileHelper.readFileByLines(filePath);
        ArrayList<String> answers = FileHelper.readFileByLines(correctAnswerPath);
        System.out.println("Total Number of Lines: " + lines.size());
        System.out.println("Total Number of Answers: " + answers.size());
        int recommendedNamesCount = 0;
        int correctCount = 0;
        int totalCount = 0;
        StringBuilder resultRecords = new StringBuilder();
        for(int i =0;i<lines.size();i++){
            String line = lines.get(i);
            String [] split = line.split("###");
            if(split.length == 2 || split.length == 1){
                System.out.println("Error in line " + i + ":" + line);
                resultRecords.append("Error\n");
                continue;
            }
            int index = Integer.parseInt(split[0]);
            String beforeAdaptation = split[1];
            String afterAdaptation = split[2];
            double timeCost = Double.parseDouble(split[3]);
            int inputTokenCount = Integer.parseInt(split[4]);
            int outputTokenCount = Integer.parseInt(split[5]);
            String answer = answers.get(i);
//            String [] answerSplit = answer.split("\t");
//            int cnt = Integer.parseInt(answerSplit[0]);
            String correctAnswers = answer;
            if(correctAnswers.contains(",") || afterAdaptation.contains(",")){
                String [] afterAdaptationSplit = afterAdaptation.split(",");
                String [] correctAnswersSplit = correctAnswers.split(",");
                List<String> afterAdaptationList = Arrays.asList(afterAdaptationSplit);
                List<String> correctAnswersList = Arrays.asList(correctAnswersSplit);
                List<String> retainList = new ArrayList<>(correctAnswersList);
                retainList.retainAll(afterAdaptationList);
//                recommendedNamesCount+= afterAdaptationList.size();
//                correctCount+= retainList.size();
//                totalCount+= correctAnswersList.size();
                resultRecords.append("Recommended Name: " + afterAdaptation.trim() + " Correct Name: " + correctAnswers.trim()).append("\n");
            }
            else{
                resultRecords.append("Recommended Name: " + afterAdaptation.trim() + " Correct Name: " + correctAnswers.trim()).append("\n");
//                if(!afterAdaptation.trim().equals("null")){
//                    recommendedNamesCount++;
//                }
//                if(afterAdaptation.trim().equalsIgnoreCase(correctAnswers.trim())){
//                    correctCount++;
//                }
//                totalCount++;
            }

        }
//        System.out.println("Total Number of Correct Answers: " + correctCount);
//        System.out.println("Total Number of Recommended Names: " + recommendedNamesCount);
//        System.out.println("Total Number of Answers: " + totalCount);
//        System.out.println("Precision: " + (double)correctCount/recommendedNamesCount);
//        System.out.println("Recall: " + (double)correctCount/totalCount);
        FileHelper.outputToFile("E:\\CodeAdaptation\\Baselines\\DeepSeek\\resultRecords_DeepSeekV3.txt", resultRecords,false);

    }
    private static void evaluateLLM() {
        BaselineEvaluation baselineEvaluation = new BaselineEvaluation();
//        String basePath = "E:\\CodeAdaptation\\Baselines\\LLM-AboveContext\\";
        String basePath = "E:\\CodeAdaptation\\Baselines\\LLM-FinalInput\\";
//        String outputPath = "E:\\CodeAdaptation\\Baselines\\Qwen\\";
//        String outputPath = "E:\\CodeAdaptation\\Baselines\\DeepSeek\\";
//        String outputPath = "E:\\CodeAdaptation\\Baselines\\QwenCoder14B\\";
//        String outputPath = "E:\\CodeAdaptation\\Baselines\\QwenCoder32B\\";
//        String outputPath = "E:\\CodeAdaptation\\Baselines\\GPT-4omini\\";
        String outputPath = "E:\\CodeAdaptation\\Baselines\\AblationStudy\\";
        List<File> allFiles = FileHelper.getAllFiles(basePath,".java");
        StringBuilder recordResults = new StringBuilder();
        double startTime= System.currentTimeMillis();
        HashMap<Integer,String> allFilePathsMap = new HashMap<>();
        for(File file : allFiles){
            String filePath = file.getAbsolutePath();
            String fileName = file.getName();
            int index = Integer.parseInt(fileName.split("-")[0]);
            allFilePathsMap.put(index,filePath);
        }
        Set<Integer> indexSett = allFilePathsMap.keySet();
        Arrays.sort(indexSett.toArray());
        for(int i =0;i< indexSett.size();i++){
            if(i>99){
                break;
            }
            int index = (int) indexSett.toArray()[i];
            double eachStartTime= System.currentTimeMillis();
            String filePath = allFilePathsMap.get(index);
            String fileName = filePath.substring(filePath.lastIndexOf("\\")+1);
//            System.out.println(fileName);
            String content = FileHelper.readFile(filePath);
            String question = "Please adapt the code snippets enclosed in \"<start>\" and \"<end>\" into the following given code snippets. " +
                    "Only consider the variables or expressions that are unresolved or inappropriate. Only output the pair of variables or expressions" +
                    "before and after the adaptation. \n [To-be-adapted Code Snippet]\n" + content;
//            System.out.println("Question: " + question);
            try{
                AdaptationPair result = baselineEvaluation.getResponse(question);
                double eachEndTime= System.currentTimeMillis();
                int inputTokenCount = baselineEvaluation.tokenEstimator.estimateTokenCount(question);
                baselineEvaluation.totalInputTokenCount += inputTokenCount;
                int outputTokenCount =0;
                if(result.getVariablesOrExpressionsBeforeAdaptation()!=null && result.getVariablesOrExpressionsAfterAdaptation()!=null){
                    outputTokenCount = baselineEvaluation.tokenEstimator.estimateTokenCount(result.getVariablesOrExpressionsAfterAdaptation())
                            + baselineEvaluation.tokenEstimator.estimateTokenCount(result.getVariablesOrExpressionsBeforeAdaptation());
                }
                baselineEvaluation.totalOutputTokenCount += outputTokenCount;
                System.out.println("Before Adaptation: " + result.getVariablesOrExpressionsBeforeAdaptation());
                System.out.println("After Adaptation: " + result.getVariablesOrExpressionsAfterAdaptation());
                double timeCost= eachEndTime-eachStartTime;
                recordResults.append(i).append("###").append(result.getVariablesOrExpressionsBeforeAdaptation()).append("###").append(result.getVariablesOrExpressionsAfterAdaptation()).append("###").append(timeCost/1000).append("###").append(inputTokenCount).append("###").append(outputTokenCount).append("\n");
            }
            catch (Exception e){
                e.printStackTrace();
                System.out.println("Error in line " + i + ":" + fileName);
                recordResults.append(i).append("###").append("Error").append("\n");
            }

        }
        baselineEvaluation.totalTokenCount = baselineEvaluation.totalInputTokenCount + baselineEvaluation.totalOutputTokenCount;
        double endTime= System.currentTimeMillis();
        System.out.println("Total Time Cost: " + (endTime-startTime) + "ms");
        System.out.println("Total Time Cost: " +(endTime-startTime)/1000 + "s");
        System.out.println("Total Input Token Count: " + baselineEvaluation.totalInputTokenCount);
        System.out.println("Total Output Token Count: " +baselineEvaluation.totalOutputTokenCount);
        System.out.println("Total Token Count: " +baselineEvaluation.totalTokenCount);

        recordResults.append("Total Time Cost: " + (endTime-startTime) + "ms").append("\n");
        recordResults.append("Total Time Cost: " +(endTime-startTime)/1000 + "s").append("\n");
        recordResults.append("Total Input Token Count: " + baselineEvaluation.totalInputTokenCount).append("\n");
        recordResults.append("Total Output Token Count: " +baselineEvaluation.totalOutputTokenCount).append("\n");
        recordResults.append("Total Token Count: " +baselineEvaluation.totalTokenCount).append("\n");
        FileHelper.outputToFile( outputPath + "results_GPT-40mini.txt",recordResults,false);
    }



}
