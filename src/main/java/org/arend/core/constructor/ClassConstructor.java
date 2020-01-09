package org.arend.core.constructor;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.expr.NewExpression;
import org.arend.core.sort.Sort;
import org.arend.ext.core.elimtree.CoreClassBranchKey;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ClassConstructor extends SingleConstructor implements CoreClassBranchKey {
  private final ClassDefinition myClassDef;
  private final Sort mySort;
  private final Set<? extends ClassField> myImplementedFields;

  public ClassConstructor(ClassDefinition classDef, Sort sort, Set<? extends ClassField> implementedFields) {
    myClassDef = classDef;
    mySort = sort;
    myImplementedFields = implementedFields;
  }

  @Nonnull
  @Override
  public ClassDefinition getClassDefinition() {
    return myClassDef;
  }

  public Sort getSort() {
    return mySort;
  }

  public Set<? extends ClassField> getImplementedFields() {
    return myImplementedFields;
  }

  @Override
  public int getNumberOfParameters() {
    return myClassDef.getNumberOfNotImplementedFields() - myImplementedFields.size();
  }

  @Override
  public List<Expression> getMatchedArguments(Expression argument, boolean normalizing) {
    List<Expression> args = new ArrayList<>();
    NewExpression newExpr = argument.cast(NewExpression.class);
    for (ClassField field : myClassDef.getFields()) {
      if (!myClassDef.isImplemented(field) && !myImplementedFields.contains(field)) {
        Expression impl = newExpr == null ? null : newExpr.getImplementationHere(field);
        args.add(impl != null ? impl : FieldCallExpression.make(field, mySort, argument));
      }
    }
    return args;
  }
}
