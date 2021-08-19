package org.arend.core.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.expr.*;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.naming.reference.TCDefReferable;
import org.arend.prelude.Prelude;
import org.arend.util.SingletonList;

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
      Expression newElementsType = arrayElementsType.removeConstLam();
      if (newElementsType == null) {
        throw new IllegalStateException(); // TODO[arrays]
      }
      substitution.add(getParameters(), newElementsType);
    }
    DependentLink newParameters = DependentLink.Helper.subst(arrayElementsType == null ? getParameters() : getParameters().getNext(), substitution, type.getLevelSubstitution());
    if (this == Prelude.ARRAY_CONS) {
      Expression arrayLength = type.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
      if (arrayLength instanceof IntegerExpression || arrayLength instanceof ConCallExpression && ((ConCallExpression) arrayLength).getDefinition() == Prelude.SUC) {
        DependentLink link = newParameters;
        while (link.getNext().hasNext()) {
          link = link.getNext();
        }
        Map<ClassField, Expression> implementations = new HashMap<>();
        Expression newLength = arrayLength instanceof ConCallExpression ? ((ConCallExpression) arrayLength).getDefCallArguments().get(0) : ((IntegerExpression) arrayLength).isZero() ? arrayLength : ((IntegerExpression) arrayLength).pred();
        TypedSingleDependentLink lamParam = new TypedSingleDependentLink(true, "j", new DataCallExpression(Prelude.FIN, LevelPair.PROP, new SingletonList<>(newLength)));
        implementations.put(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(type.getLevels().toLevelPair().toSort().max(Sort.SET0), lamParam, AppExpression.make(arrayElementsType == null ? new ReferenceExpression(newParameters) : arrayElementsType, new ReferenceExpression(lamParam), true)));
        implementations.put(Prelude.ARRAY_LENGTH, newLength);
        link.setType(new ClassCallExpression(Prelude.DEP_ARRAY, type.getLevels(), implementations, type.getSort(), UniverseKind.NO_UNIVERSES));
      }
    }
    return newParameters;
  }
}
