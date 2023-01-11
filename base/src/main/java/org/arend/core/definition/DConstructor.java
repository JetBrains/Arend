package org.arend.core.definition;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.expr.*;
import org.arend.core.expr.type.TypeExpression;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.naming.reference.TCDefReferable;
import org.arend.prelude.Prelude;
import org.arend.util.SingletonList;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.arend.core.expr.ExpressionFactory.Suc;
import static org.arend.core.expr.ExpressionFactory.Zero;

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

  public DependentLink getArrayParameters(LevelPair levels, Expression length, Binding thisBinding, Expression elementsType) {
    if (this == Prelude.EMPTY_ARRAY) {
      return elementsType != null ? EmptyDependentLink.getInstance() : DependentLink.Helper.subst(getParameters(), new ExprSubstitution(thisBinding, new NewExpression(null, new ClassCallExpression(Prelude.DEP_ARRAY, levels, Collections.singletonMap(Prelude.ARRAY_LENGTH, Zero()), Sort.STD.succ(), UniverseKind.NO_UNIVERSES))), levels);
    }

    if ((elementsType == null || thisBinding == null) && length == null) {
      return levels.isEmpty() ? getParameters() : DependentLink.Helper.subst(getParameters(), new ExprSubstitution(), levels);
    }

    if (length != null) {
      ExprSubstitution substitution = new ExprSubstitution(getParameters(), length);
      if (elementsType != null) {
        substitution.add(getParameters().getNext(), elementsType);
      }
      return DependentLink.Helper.subst(elementsType == null ? getParameters().getNext() : getParameters().getNext().getNext(), substitution, levels);
    }

    TypedDependentLink nat = new TypedDependentLink(false, "n", DataCallExpression.make(Prelude.NAT, Levels.EMPTY, Collections.emptyList()), EmptyDependentLink.getInstance());
    ReferenceExpression natRef = new ReferenceExpression(nat);
    Map<ClassField, Expression> impls = new LinkedHashMap<>();
    impls.put(Prelude.ARRAY_LENGTH, natRef);
    impls.put(Prelude.ARRAY_ELEMENTS_TYPE, elementsType);
    Expression newElementsType = elementsType.subst(thisBinding, new NewExpression(null, new ClassCallExpression(Prelude.DEP_ARRAY, levels, impls, Sort.STD.succ(), UniverseKind.ONLY_COVARIANT)));
    TypedSingleDependentLink lamParam = new TypedSingleDependentLink(true, "j", DataCallExpression.make(Prelude.FIN, Levels.EMPTY, new SingletonList<>(natRef)));
    Sort sort = levels.toSort();
    impls.put(Prelude.ARRAY_ELEMENTS_TYPE, new LamExpression(sort.max(Sort.SET0), lamParam, AppExpression.make(newElementsType, Suc(new ReferenceExpression(lamParam)), true)));
    nat.setNext(new TypedDependentLink(true, "a", new TypeExpression(AppExpression.make(newElementsType, Zero(), true), sort), new TypedDependentLink(true, "l", new ClassCallExpression(Prelude.DEP_ARRAY, levels, impls, Sort.STD, UniverseKind.NO_UNIVERSES), EmptyDependentLink.getInstance())));
    return nat;
  }

  public DependentLink getArrayParameters(ClassCallExpression type) {
    Expression length = type.getAbsImplementationHere(Prelude.ARRAY_LENGTH);
    length = length == null || this == Prelude.EMPTY_ARRAY ? null : length.normalize(NormalizationMode.WHNF).pred();
    return getArrayParameters(type.getLevels().toLevelPair(), length, type.getThisBinding(), type.getAbsImplementationHere(Prelude.ARRAY_ELEMENTS_TYPE));
  }
}
