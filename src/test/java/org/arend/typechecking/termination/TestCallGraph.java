package org.arend.typechecking.termination;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestCallGraph extends BaseCallGraph<TestVertex> {
  public final Map<TestVertex, Set<RecursiveBehavior<TestVertex>>> myErrorInfo = new HashMap<>();

  public TestCallGraph(Set<BaseCallMatrix<TestVertex>> graph) {
    add(graph);
  }

  @Override
  protected String getLabel(TestVertex vertex) {
    return vertex.myName;
  }

  @Override
  protected void formErrorMessage(TestVertex vertex, Set<RecursiveBehavior<TestVertex>> behavior) {
    myErrorInfo.put(vertex, behavior);
  }

  public static Boolean testTermination(Set<BaseCallMatrix<TestVertex>> g) {
    return new TestCallGraph(g).checkTermination();
  }
}
