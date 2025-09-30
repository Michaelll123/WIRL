package Service;

import dev.langchain4j.model.output.structured.Description;

public class AdaptationPair {
    @Description("The name of the variable or expression before adaptation, split them with commas if there are multiple ones")
    private String variablesOrExpressionsBeforeAdaptation;
    @Description("The name of the variable or expression after adaptation, split them with commas if there are multiple ones")
    private String variablesOrExpressionsAfterAdaptation;

    public String getVariablesOrExpressionsBeforeAdaptation() {
        return variablesOrExpressionsBeforeAdaptation;
    }

    public String getVariablesOrExpressionsAfterAdaptation() {
        return variablesOrExpressionsAfterAdaptation;
    }
}
