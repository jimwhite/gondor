import org.ifcx.gondor.Command
import org.ifcx.gondor.Command
import org.ifcx.gondor.Command

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
        flag "-K"
        arg "opt", Command.OPTIONAL, { "--opt=$it" }
//        flag "-l400"
        arg "def", 400, {"-l$it"}
        arg "n", Command.REQUIRED, {"-N$it"}
        arg "m", {"-NN$it"}
//        arg "n", { assert it && "n is required for parseIt" ; "-N$it"}
        arg "foo"
        infile "model"
        infile "input"
        outfile "output"
        jobTemplate { softRunDurationLimit = 100 }
    }

    // second-stage/programs/features/best-parses" -l "$MODELDIR/features.gz" "$MODELDIR/$ESTIMATORNICKNAME-weights.gz"
    def rerank_parses = command(path:'second-stage/programs/features/best-parses') {
        flag '-l' ; infile 'features' ; infile 'weights' ; infile 'stdin' ; outfile 'stdout'
    }

//
// parse_nbest(model:PARSER_MODEL, input:charniak_input, stdout:nbest_output)
// rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS, stdin:nbest_output, stdout:reranker_output)

    def modelFile = new File("model.dat")
    def inputFile = new File("input.txt")
    def parsedFile = new File("output1.ptb")
    def p = parse_nbest(n:15, model:modelFile, input:inputFile, output:parsedFile, m:2)

    println p.n

    def RERANKER_FEATURES = new File('RERANKER_FEATURES')
    def RERANKER_WEIGHTS = new File('RERANKER_WEIGHTS')

    def reranker_output = new File("best_parse.ptb")

//    rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS, stdin:parsedFile, stdout:reranker_output)

    rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS) << parsedFile >> reranker_output >>> new File('errs.txt')

//    (parse_nbest(n:5, model: modelFile) << new File("in2.txt")) | rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS) >> new File("out2.tree")
    (parse_nbest(opt:5, m:0, foo:0, model: modelFile) << new File("in2.txt")) | rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS) >> new File("out2.tree")

    parse_nbest(n:5, foo:"-", model: modelFile) << new File("in2.txt") | rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS) >> new File("out2.tree")

    // new File("in2.txt") >> parse_nbest(model: modelFile) | rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS) >> new File("out2.tree")

    // parse_nbest(model: modelFile) | rerank_parses(features: RERANKER_FEATURES, weights: RERANKER_WEIGHTS) << new File("in3.txt") >> new File("out3.tree")

}
