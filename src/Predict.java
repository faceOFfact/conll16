
import opennlp.maxent.BasicContextGenerator;
import opennlp.maxent.ContextGenerator;
import opennlp.model.MaxentModel;


public class Predict {
    MaxentModel _model;
    ContextGenerator _cg = new BasicContextGenerator();
    
    public Predict (MaxentModel m) {
	_model = m;
    }
    
    String eval (String predicates) {
      String[] contexts = predicates.split(" ");
      double[] ocs;
      ocs = _model.eval(contexts);
      
      String predict = _model.getBestOutcome(ocs); 
      return predict;
    }
}
