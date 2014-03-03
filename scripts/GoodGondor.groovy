
@groovy.transform.BaseScript org.ifcx.gondor.WorkflowScript workflowScript

void init(String contact) {
    setWorkflowName(getWorkflowName() + 'Workflow')
    super.init("jim")
}

workflowScript.with {
    println workflowName

    def jt = createJobTemplate()

    jt.remoteCommand = "/bin/ls"
    jt.outputPath = "good_out.txt"
    runJob(jt)

    jt.remoteCommand = "ls_here"
    jt.outputPath = "good_here.txt"
    runJob(jt)

    jt.remoteCommand = "../scripts/ls_here"
    jt.outputPath = "good_up_then_here.txt"
    runJob(jt)

    def parse_nbest = command(path:'first-stage/PARSE/parseIt') {
        flag "-K" ; flag "-l400" ; flag "-N50" ; infile "model" ; infile "input" ; outfile "output"
        jobTemplate { softRunDurationLimit = 100 }
    }

    // second-stage/programs/features/best-parses" -l "$MODELDIR/features.gz" "$MODELDIR/$ESTIMATORNICKNAME-weights.gz"
    def rerank_parses = command(path:'second-stage/programs/features/best-parses') {
        flag '-l'
        infile 'features'
        infile 'weights'
        infile 'stdin'
        outfile 'stdout'
    }

//
// parse_nbest(model:PARSER_MODEL, input:charniak_input, stdout:nbest_output)
// rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS, stdin:nbest_output, stdout:reranker_output)

    def modelFile = new File("model.dat")
    def inputFile = new File("input.txt")
    def parsedFile = new File("output1.ptb")
    parse_nbest(model:modelFile, input:inputFile, output:parsedFile)

    def RERANKER_FEATURES = new File('RERANKER_FEATURES')
    def RERANKER_WEIGHTS = new File('RERANKER_WEIGHTS')

    def reranker_output = new File("best_parse.ptb")

    rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS, stdin:parsedFile, stdout:reranker_output)
}
