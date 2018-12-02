package org.arend.typechecking.visitor;

import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.elimtree.*;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.util.Pair;

import java.util.Map;
import java.util.Set;

public class FindDefCallVisitor extends ProcessDefCallsVisitor<Void> {
  private Definition myFoundDefinition;
  private final Set<? extends Definition> myDefinitions;

  public FindDefCallVisitor(Set<? extends Definition> definitions) {
    myDefinitions = definitions;
  }

  public Definition getFoundDefinition() {
    return myFoundDefinition;
  }

  public void clear() {
    myFoundDefinition = null;
  }

  public static Definition findDefinition(Expression expression, Set<? extends Definition> definitions) {
    FindDefCallVisitor visitor = new FindDefCallVisitor(definitions);
    expression.accept(visitor, null);
    return visitor.myFoundDefinition;
  }

  public void findDefinition(Body body) {
    if (body instanceof IntervalElim) {
      for (Pair<Expression, Expression> pair : ((IntervalElim) body).getCases()) {
        if (pair.proj1.accept(this, null) || pair.proj2.accept(this, null)) {
          return;
        }
      }
      findDefinition(((IntervalElim) body).getOtherwise());
    } else if (body instanceof LeafElimTree) {
      ((LeafElimTree) body).getExpression().accept(this, null);
    } else if (body instanceof BranchElimTree) {
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) body).getChildren()) {
        findDefinition(entry.getValue());
        if (myFoundDefinition != null) {
          return;
        }
      }
    } else if (body != null) {
      throw new IllegalStateException();
    }
  }

  @Override
  protected boolean processDefCall(DefCallExpression expression, Void param) {
    if (myDefinitions.contains(expression.getDefinition())) {
      myFoundDefinition = expression.getDefinition();
      return true;
    } else {
      return false;
    }
  }
}
