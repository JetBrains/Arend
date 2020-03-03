package org.arend.core.constructor;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.expr.NewExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.elimtree.CoreClassBranchKey;
import org.arend.ext.core.ops.CMP;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ClassConstructor extends SingleConstructor implements CoreClassBranchKey {
  private final ClassDefinition myClassDef;
  private Sort mySort;
  private final Set<? extends ClassField> myImplementedFields;

  public ClassConstructor(ClassDefinition classDef, Sort sort, Set<? extends ClassField> implementedFields) {
    myClassDef = classDef;
    mySort = sort;
    myImplementedFields = implementedFields;
  }

  @NotNull
  @Override
  public ClassDefinition getClassDefinition() {
    return myClassDef;
  }

  public Sort getSort() {
    return mySort;
  }

  public void substSort(LevelSubstitution substitution) {
    mySort = mySort.subst(substitution);
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

  @Override
  public boolean compare(SingleConstructor other, Equations equations, Concrete.SourceNode sourceNode) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassConstructor)) {
      return false;
    }

    ClassConstructor con = (ClassConstructor) other;
    if (myClassDef != con.myClassDef || !myImplementedFields.equals(con.myImplementedFields)) {
      return false;
    }

    if (myClassDef.getUniverseKind() == UniverseKind.NO_UNIVERSES) {
      return true;
    }
    for (ClassField field : myClassDef.getFields()) {
      if (field.getUniverseKind() != UniverseKind.NO_UNIVERSES && !myClassDef.isImplemented(field) && !myImplementedFields.contains(field)) {
        return Sort.compare(mySort, con.mySort, CMP.EQ, equations, sourceNode);
      }
    }
    return true;
  }
}
