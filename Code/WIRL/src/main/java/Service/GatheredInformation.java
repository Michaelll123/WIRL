package Service;

import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public class GatheredInformation {
    @Description("The thought process of the assistant for the current context.")
    String thought;
    @Description("The name of tool that should be executed next.")
    String action;
    @Description("The tool input in JSONObject format, e.g., \"arg0\":\"value0\", \"arg1\":\"value1\"")
    JSONObject actionInput;

    public GatheredInformation(String thought, String action, JSONObject actionInput) {
        this.thought = thought;
        this.action = action;
        this.actionInput = actionInput;
    }
    public String getThought() {
        return thought;
    }


    public String getAction() {
        return action;
    }


    public JSONObject getActionInput() {
        return actionInput;
    }

    public String toString() {
        return "thought:'" + thought + '\'' +
                ", action:'" + action + '\'' +
                ", actionInput:" + actionInput + '\n';
    }
}
