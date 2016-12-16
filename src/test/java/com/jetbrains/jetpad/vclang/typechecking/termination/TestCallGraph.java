package com.jetbrains.jetpad.vclang.typechecking.termination;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by user on 12/16/16.
 */
public class TestCallGraph extends BaseCallGraph<TestVertex> {
    public Map<TestVertex, Set<RecursiveBehavior<TestVertex>>> myErrorInfo = new HashMap<>();

    public TestCallGraph(Set<BaseCallMatrix<TestVertex>> graph) {
        add(graph);
    }

    public TestCallGraph(TestCallGraph tcg) {
        super(tcg);
    }

    @Override
    protected String getLabel(TestVertex vertex) {
        return vertex.myName;
    }

    @Override
    protected void formErrorMessage(TestVertex vertex, Set<RecursiveBehavior<TestVertex>> behavior) {
        myErrorInfo.put(vertex, behavior);
    }

    public static TestCallGraph calculateClosure(Set<BaseCallMatrix<TestVertex>> g) {
        return new TestCallGraph(new TestCallGraph(g));
    }
}
