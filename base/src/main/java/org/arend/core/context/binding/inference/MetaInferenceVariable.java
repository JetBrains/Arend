package org.arend.core.context.binding.inference;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.Expression;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.term.concrete.Concrete;

import java.util.Set;

public class MetaInferenceVariable extends BaseInferenceVariable {
  private final MetaDefinition myDefinition;
  private final Concrete.ReferenceExpression myReference;

  public MetaInferenceVariable(Expression type, MetaDefinition definition, Concrete.ReferenceExpression reference, Set<Binding> bounds) {
    super(reference.getReferent().getRefName(), type, bounds);
    myDefinition = definition;
    myReference = reference;
  }

  public MetaDefinition getDefinition() {
    return myDefinition;
  }

  public Concrete.ReferenceExpression getExpression() {
    return myReference;
  }
}
