package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.typechecking.termination.BaseCallMatrix;
import com.jetbrains.jetpad.vclang.typechecking.termination.CompositeCallMatrix;
import com.jetbrains.jetpad.vclang.typechecking.termination.RecursiveBehavior;

import java.util.*;

/**
 * Created by user on 12/16/16.
 */
public class TerminationCheckError extends GeneralError {
    private Set<RecursiveBehavior<Definition>> myBehaviors = new HashSet<>();
    private Definition myDefinition;

    public TerminationCheckError(Definition def, Set<RecursiveBehavior<Definition>> behaviors) {
        super(formErrorMessage(def, behaviors), def.getAbstractDefinition());
        myBehaviors = behaviors;
        myDefinition = def;
    }

    public static<T> String formErrorMessage(Definition def, Set<RecursiveBehavior<T>> behaviors) {
        String result = "Termination check failed for function "+def.getName()+".\n";
        for (RecursiveBehavior rb : behaviors) result += printBehavior(rb);
        return result;
    }

    private static String printBehavior(RecursiveBehavior rb) {
        String result = "";
        String brace = "";
        if (rb.myInitialCallMatrix instanceof CompositeCallMatrix) {
            result += "Composite recursive call: {\n ";
            brace = "}";
        } else {
            result += "Recursive call: ";
        }
        result += rb.myInitialCallMatrix.getMatrixLabel() + brace;

        String unknown = "";
        int unknownCount = 0;
        String equal = "";
        int equalCount = 0;
        for (int i = 0; i < rb.getLength(); i++) {
            if (rb.myBehavior.get(i) == BaseCallMatrix.R.Unknown) {
                unknown += (unknown.length() > 0 ? ", " : "") + rb.myLabels.get(i);
                unknownCount++;
            } else if (rb.myBehavior.get(i) == BaseCallMatrix.R.Equal) {
                equal += (equal.length() > 0 ? ", " : "") + rb.myLabels.get(i);
                equalCount++;
            }
        }
        if (unknown.length() > 0)
            result += "\nUnknown recursive behavior for argument"+(unknownCount > 1 ? "s" : "")+": "+unknown;
        if (equal.length() > 0)
            result += "\nDoes not strictly decrease on argument"+(equalCount > 1 ? "s" : "")+": "+equal;

        return result;
    }
}
