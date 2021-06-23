package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.expr.*;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.TCDefReferable;
import org.arend.prelude.Prelude;

import java.util.HashMap;
import java.util.Map;

public class DConstructor extends FunctionDefinition {
  private int myNumberOfParameters;
  private ExpressionPattern myPattern;

  public DConstructor(TCDefReferable referable) {
    super(referable);
  }

  public int getNumberOfParameters() {
    return myNumberOfParameters;
  }

  public void setNumberOfParameters(int numberOfParameters) {
    myNumberOfParameters = numberOfParameters;
  }

  public ExpressionPattern getPattern() {
    return myPattern;
  }

  public void setPattern(ExpressionPattern pattern) {
    myPattern = pattern;
  }

  public DependentLink getArrayParameters(ClassCallExpression type) {
    ExprSubstitution substitution = new ExprSubstitution();
    Expression arrayElementsType = type.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE);
    if (arrayElementsType != null) {
      substitution.add(getParameters(), arrayElementsType);
    }
    Expression arrayLength = type.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
    DependentLink newParameters = DependentLink.Helper.subst(arrayElementsType == null ? getParameters() : getParameters().getNext(), substitution, type.getLevelSubstitution());
    if (this == Prelude.ARRAY_CONS && (arrayLength instanceof IntegerExpression || arrayLength instanceof ConCallExpression && ((ConCallExpression) arrayLength).getDefinition() == Prelude.SUC)) {
      DependentLink link = newParameters;
      while (link.getNext().hasNext()) {
        link = link.getNext();
      }
      Map<ClassField, Expression> implementations = new HashMap<>();
      implementations.put(Prelude.ARRAY_ELEMENTS_TYPE, arrayElementsType == null ? new ReferenceExpression(getParameters()) : arrayElementsType);
      implementations.put(Prelude.ARRAY_LENGTH, arrayLength instanceof ConCallExpression ? ((ConCallExpression) arrayLength).getDefCallArguments().get(0) : ((IntegerExpression) arrayLength).isZero() ? arrayLength : ((IntegerExpression) arrayLength).pred());
      link.setType(new ClassCallExpression(Prelude.ARRAY, type.getLevels(), implementations, type.getSort(), UniverseKind.NO_UNIVERSES));
    }
    return newParameters;
  }
}
