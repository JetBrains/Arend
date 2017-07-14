package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.termination.BaseCallMatrix;
import com.jetbrains.jetpad.vclang.typechecking.termination.CompositeCallMatrix;
import com.jetbrains.jetpad.vclang.typechecking.termination.RecursiveBehavior;

import java.util.Set;

public class TerminationCheckError extends GeneralError {
  public final Abstract.Definition definition;

  public TerminationCheckError(Definition def, Set<RecursiveBehavior<Definition>> behaviors) {
    super(formErrorMessage(def, behaviors), def.getAbstractDefinition());
    definition = def.getAbstractDefinition();
  }

  private static <T> String formErrorMessage(Definition def, Set<RecursiveBehavior<T>> behaviors) {
    StringBuilder builder = new StringBuilder("Termination check failed for function ");
    builder.append(def.getName());
    if (!behaviors.isEmpty()) {
      builder.append('\n');
      for (RecursiveBehavior rb : behaviors) {
        builder.append(printBehavior(rb));
      }
    }
    return builder.toString();
  }

  private static String printBehavior(RecursiveBehavior rb) {
    StringBuilder result = new StringBuilder();
    String brace = "";
    if (rb.myInitialCallMatrix instanceof CompositeCallMatrix) {
      result.append("Composite recursive call: {\n ");
      brace = "}";
    } else {
      result.append("Recursive call: ");
    }
    result.append(rb.myInitialCallMatrix.getMatrixLabel()).append(brace);

    StringBuilder unknown = new StringBuilder();
    int unknownCount = 0;
    StringBuilder equal = new StringBuilder();
    int equalCount = 0;
    for (int i = 0; i < rb.getLength(); i++) {
      if (rb.myBehavior.get(i) == BaseCallMatrix.R.Unknown) {
        unknown.append(unknown.length() > 0 ? ", " : "").append(rb.myLabels.get(i));
        unknownCount++;
      } else if (rb.myBehavior.get(i) == BaseCallMatrix.R.Equal) {
        equal.append(equal.length() > 0 ? ", " : "").append(rb.myLabels.get(i));
        equalCount++;
      }
    }
    if (unknown.length() > 0) {
      result.append("\nUnknown recursive behavior for argument").append(unknownCount > 1 ? "s" : "").append(": ").append(unknown);
    }
    if (equal.length() > 0) {
      result.append("\nDoes not strictly decrease on argument").append(equalCount > 1 ? "s" : "").append(": ").append(equal);
    }

    return result.toString();
  }
}
