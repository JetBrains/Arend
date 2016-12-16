package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TerminationCheckError;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by user on 12/16/16.
 */
public class TestCallGraph extends BaseCallGraph<TestVertex> {
    public Map<TestVertex, Set<RecursiveBehaviors.RecursiveBehavior<TestVertex>>> myErrorInfo = new HashMap<>();

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
    protected void formErrorMessage(TestVertex vertex, Set<RecursiveBehaviors.RecursiveBehavior<TestVertex>> behavior) {
        myErrorInfo.put(vertex, behavior);
    }

    public static TestCallGraph calculateClosure(Set<BaseCallMatrix<TestVertex>> g) {
        return new TestCallGraph(new TestCallGraph(g));
    }
}
