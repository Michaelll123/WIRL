package Service;

import dev.langchain4j.model.output.structured.Description;


public class VariableResult {

//    public enum Decision{
//        @Description("There is no need to use the tool to obtain extra context.")
//        NO_NEED,
//        @Description("Need to use retrieveFields tool to obtain the fields in the class.")
//        RETRIEVE_ALL_FIELDS,
//        @Description("Need to use retrieveFields tool to obtain the fields in the class.")
//        RETRIEVE_VARIABLES_OF_TYPE,
//        @Description("Need to use retrieveIdenticalFunctionCall tool to obtain the variables invoking identical function calls in the class.")
//        RETRIEVE_VARIABLE_INVOKING_IDENTICAL_FUNCTION_CALL,
//        @Description("Need to use getFunctionParameter tool to obtain the parameters of the function.")
//        GET_METHOD_SIGNATURE,
//    }
    @Description("The index of <infill>")
    int index;
    @Description("The name of the inferred variable")
    String variableName;
    public VariableResult(int index, String variableName){
        this.variableName = variableName;
        this.index = index;
    }
    public String getVariableName() {
        return variableName;
    }

}
