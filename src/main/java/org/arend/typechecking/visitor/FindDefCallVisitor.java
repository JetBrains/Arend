package org.arend.typechecking.visitor;

import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.elimtree.*;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.util.Pair;

import java.util.*;

public class FindDefCallVisitor<T extends Definition> extends ProcessDefCallsVisitor<Void> {
  private final Set<T> myFoundDefinitions = new HashSet<>();
  private final Set<? extends T> myDefinitions;
  private final boolean myFindAll;

  public FindDefCallVisitor(Set<? extends T> definitions, boolean findAll) {
    myDefinitions = definitions;
    myFindAll = findAll;
  }

  public T getFoundDefinition() {
    Iterator<T> it = myFoundDefinitions.iterator();
    return it.hasNext() ? it.next() : null;
  }

  public Set<T> getFoundDefinitions() {
    return myFoundDefinitions;
  }

  public void clear() {
    myFoundDefinitions.clear();
  }

  public static <T extends Definition> T findDefinition(Expression expression, Set<? extends T> definitions) {
    FindDefCallVisitor<T> visitor = new FindDefCallVisitor<>(definitions, false);
    expression.accept(visitor, null);
    return visitor.getFoundDefinition();
  }

  public static <T extends Definition> Set<T> findDefinitions(Expression expression, Set<? extends T> definitions) {
    FindDefCallVisitor<T> visitor = new FindDefCallVisitor<>(definitions, true);
    expression.accept(visitor, null);
    return visitor.myFoundDefinitions;
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
        if (!myFindAll && !myFoundDefinitions.isEmpty()) {
          return;
        }
      }
    } else if (body != null) {
      throw new IllegalStateException();
    }
  }

  @Override
  protected boolean processDefCall(DefCallExpression expression, Void param) {
    //noinspection SuspiciousMethodCalls
    if (myDefinitions.contains(expression.getDefinition())) {
      //noinspection unchecked
      myFoundDefinitions.add((T) expression.getDefinition());
      return !myFindAll;
    } else {
      return false;
    }
  }
}
