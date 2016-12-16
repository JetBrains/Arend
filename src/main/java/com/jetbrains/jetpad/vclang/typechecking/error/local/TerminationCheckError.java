package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.termination.BaseCallGraph;

import java.util.*;

/**
 * Created by user on 12/16/16.
 */
public class TerminationCheckError extends GeneralError {
    private Set<BaseCallGraph.RecursiveBehaviors.RecursiveBehavior<Definition>> myBehaviors = new HashSet<>();
    private Definition myDefinition;

    public TerminationCheckError(Definition def, Set<BaseCallGraph.RecursiveBehaviors.RecursiveBehavior<Definition>> behaviors) {
        super(formErrorMessage(def, behaviors), def.getAbstractDefinition());
        myBehaviors = behaviors;
        myDefinition = def;
    }

    public static<T> String formErrorMessage(Definition def, Set<BaseCallGraph.RecursiveBehaviors.RecursiveBehavior<T>> behaviors) {
        String result = "Termination check failed for function "+def.getName()+". Recursive calls that cause the problem:\n";
        for (BaseCallGraph.RecursiveBehaviors.RecursiveBehavior rb : behaviors) result += rb.toString() + "\n";
        System.out.println(result);
        return result;
    }
}
