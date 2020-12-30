package org.arend.typechecking.visitor;

import org.arend.core.definition.Definition;
import org.arend.core.elimtree.*;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class FindDefCallVisitor<T extends Definition> extends SearchVisitor<Void> {
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

  @Override
  protected boolean visitElimBody(ElimBody elimBody, Void param) {
    for (var clause : elimBody.getClauses()) {
      if (clause.getExpression() != null && clause.getExpression().accept(this, param)) {
        return true;
      }
    }
    return false;
  }
}
