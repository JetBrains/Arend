package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.typechecking.patternmatching.ElimTypechecking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefinitionCallGraph extends BaseCallGraph<Definition> {
  public Map<Definition, Set<RecursiveBehavior<Definition>>> myErrorInfo = new HashMap<>();

  protected String getLabel(Definition vertex) {
    return vertex.getName();
  }

  protected void formErrorMessage(Definition vertex, Set<RecursiveBehavior<Definition>> behavior) {
    myErrorInfo.put(vertex, behavior);
  }

  public DefinitionCallGraph() {
  }

  public DefinitionCallGraph(DefinitionCallGraph cg) {
    super(cg);
  }

  public void add(FunctionDefinition def, Collection<? extends ElimTypechecking.ClauseData> clauses, Set<? extends Definition> cycle) {
    CollectCallVisitor visitor = new CollectCallVisitor(def, cycle);
    for (ElimTypechecking.ClauseData clause : clauses) {
      visitor.collect(clause);
    }
    add(visitor.getResult());
  }
}
