# WIRL
"Wired for Reuse: Automating Context-Aware Code Adaptation in IDEs via LLM-Based Agent"
## What is WIRL?
**WIRL** is an LLM-based agent for **code wiring** framed as a Retrieval-Augmented Generation (RAG) infilling task. **WIRL** combines an LLM, a customized toolkit, and an orchestration module to identify unresolved  ariables, retrieve context, and perform context-aware substitutions.

## What is included by this replication package?

*/Code:* The implementation of **WIRL**, including the leveraged libraries.  

*/Data,* contains the evaluation dataset, its metadata, and the foundation dataset.
- */Dataset*, 
  - *CWEvaluation.7z*, contains 100 code wiring instances involved with 221 pairs of unresolved elements and context elements from Stack Overflow to GitHub. 
  - *CWEvaluationMetaData.xlsx*, contains the meta data of each code wiring instance, e.g., repository name, java file name, referenced Stack Overflow URL, and line number. 
  - *FoundationData.7z*, contains our foundation dataset, containing 3628 adaptation cases.
- */LLM's Input*, contains the code snippets, marked adaptation area with \<start\> and \<end\> tags, inputted to the raw LLMs. 
- */GrACE's Input*, contains the preprocessed data format as required by GrACE.
- */Prompts*, contains the prompt template of raw LLMs and **WIRL**. 

*/Evaluation*, contains the evaluation results of the evaluted baselines and **WIRL**.
- */CoEdPilot*, an advanced code editing approach which is implemented as a VSCode plugin.
- */DeepSeek*, raw LLM of DeepSeek.
- */ExampleStack*, a template-based approach for code adaptation which is implemented as a Google plugin.
- */GPT-4omini*, raw LLM of GPT-4omini.
- */GrACE*, an advanced code editing approach leveraging code completion ability of LLMs with zero-shot setting.
- */IDEA*, the renaming support of IntelliJ IDEA.
- */QwenCoder14B*, raw LLM of QwenCoder14B.
- */QwenCoder32B*, raw LLM of QwenCoder32B.
- */Qwen-max*,  raw LLM of Qwen-max.
- */WIRL*, the proposed approach which is implemented as an IntelliJ IDEA plugin.



## Getting Started
- Search and download "WIRL" in the plugin marketplace of IntelliJ IDEA.
- Configure your LLM API key, your favored LLM, and your custom endpoint URL (Optional) in <code> File | Settings | Tools | WIRL AI Settings</code>.
- Copy a code snippet from any source and paste it into your editor.
- Select an unresolved variable, Right-Click it, and select "WIRL: Map variables" to get suggestions.
- All the unresolved variables in the method where the selected one located will be addressed together!
 
## A Demo of WIRL 
The IntelliJ plugin implementation of WIRL with detailed introduction is available [here](https://plugins.jetbrains.com/plugin/27840-wirl).

The source code of WIRL with permissive MIT license is available [here](https://github.com/Michaelll123/WIRL-With-License).

![](demo.gif)