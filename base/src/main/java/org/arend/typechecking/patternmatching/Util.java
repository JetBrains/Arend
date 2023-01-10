package org.arend.typechecking.patternmatching;

import org.arend.core.constructor.ArrayConstructor;
import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DConstructor;
import org.arend.core.definition.Definition;
import org.arend.core.elimtree.BranchKey;
import org.arend.core.expr.*;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.prelude.Prelude;

import java.util.*;

public class Util {
  public interface ClauseElem {
  }

  public static class PatternClauseElem implements ClauseElem {
    public final ExpressionPattern pattern;

    public PatternClauseElem(ExpressionPattern pattern) {
      this.pattern = pattern;
    }
  }

  public interface DataClauseElem extends ClauseElem {
    DependentLink getParameters();
    ConstructorExpressionPattern getPattern(List<ExpressionPattern> subPatterns);
  }

  public static DataClauseElem makeDataClauseElem(BranchKey branchKey, ConstructorExpressionPattern pattern) {
    if (branchKey instanceof SingleConstructor) {
      return new TupleClauseElem(pattern);
    } else if (branchKey instanceof Constructor) {
      return new ConstructorClauseElem((Constructor) branchKey, pattern.getLevels(), pattern.getDataTypeArguments());
    } else if (branchKey instanceof ArrayConstructor) {
      return new ArrayClauseElem(((ArrayConstructor) branchKey).getConstructor(), pattern.getLevels().toLevelPair(), pattern.getArrayLength(), pattern.getArrayThisBinding(), pattern.getArrayElementsType(), pattern.isArrayEmpty());
    } else {
      throw new IllegalStateException();
    }
  }

  public static class TupleClauseElem implements DataClauseElem {
    public final ConstructorExpressionPattern pattern;

    TupleClauseElem(ConstructorExpressionPattern pattern) {
      this.pattern = pattern;
    }

    @Override
    public DependentLink getParameters() {
      return pattern.getParameters();
    }

    @Override
    public ConstructorExpressionPattern getPattern(List<ExpressionPattern> subPatterns) {
      return new ConstructorExpressionPattern(pattern, subPatterns);
    }
  }

  public static class ConstructorClauseElem implements DataClauseElem {
    final List<Expression> dataArguments;
    final Constructor constructor;
    final Levels levels;

    public ConstructorClauseElem(Constructor constructor, Levels levels, List<? extends Expression> dataArguments) {
      this.dataArguments = new ArrayList<>(dataArguments);
      this.constructor = constructor;
      this.levels = levels;
    }

    @Override
    public DependentLink getParameters() {
      return constructor.getParameters();
    }

    @Override
    public ConstructorExpressionPattern getPattern(List<ExpressionPattern> subPatterns) {
      return new ConstructorExpressionPattern(new ConCallExpression(constructor, levels, dataArguments, Collections.emptyList()), subPatterns);
    }
  }

  public static class ArrayClauseElem implements DataClauseElem {
    private final DConstructor myConstructor;
    private final LevelPair myLevels;
    private final Expression myLength;
    private final Binding myThisBinding;
    private final Expression myElementsType;
    private final Boolean myEmpty;

    public ArrayClauseElem(DConstructor constructor, LevelPair levels, Expression length, Binding thisBinding, Expression elementsType, Boolean isEmpty) {
      myConstructor = constructor;
      myLevels = levels;
      myLength = length;
      myThisBinding = thisBinding;
      myElementsType = elementsType;
      myEmpty = isEmpty;
    }

    @Override
    public DependentLink getParameters() {
      return myConstructor.getArrayParameters(myLevels, myLength, myThisBinding, myElementsType);
    }

    @Override
    public ConstructorExpressionPattern getPattern(List<ExpressionPattern> subPatterns) {
      return new ConstructorExpressionPattern(new FunCallExpression(myConstructor, myConstructor.makeIdLevels(), myLength, myElementsType), myThisBinding, myEmpty, subPatterns);
    }
  }

  @SuppressWarnings("unchecked")
  public static void removeImplicitPatterns(List<Object> result, List<DependentLink> parameters) {
    if (parameters != null) {
      for (int i = 0, j = 0; i < result.size() && j < parameters.size(); i++, j++) {
        if (result.get(i) instanceof BindingPattern && !parameters.get(j).isExplicit()) {
          result.remove(i--);
        }
      }
    }

    for (int i = 0; i < result.size(); i++) {
      if (result.get(i) instanceof ConstructorExpressionPattern pattern) {
        Definition def = pattern.getDefinition();
        if (def == Prelude.EMPTY_ARRAY || def == Prelude.ARRAY_CONS) {
          if (pattern.getDataExpression() instanceof FunCallExpression funCall) {
            List<Expression> args = funCall.getDefCallArguments();
            Binding thisBinding = pattern.getArrayThisBinding();
            if (args.size() <= 4 && (thisBinding != null || args.size() >= 2 && args.get(0) != null && args.get(1) != null)) {
              List<? extends ExpressionPattern> subPatterns = pattern.getSubPatterns();
              boolean keepLength = def == Prelude.ARRAY_CONS && (args.isEmpty() || args.get(0) == null) && subPatterns.size() >= 3 && !(subPatterns.get(0) instanceof BindingPattern);
              Expression newLength = def == Prelude.ARRAY_CONS ? keepLength ? null : args.size() > 0 && args.get(0) != null ? args.get(0) : FieldCallExpression.make(Prelude.ARRAY_LENGTH, new ReferenceExpression(thisBinding)) : null;
              Expression newElementsType = args.size() > 1 && args.get(1) != null ? args.get(1) : FieldCallExpression.make(Prelude.ARRAY_ELEMENTS_TYPE, new ReferenceExpression(thisBinding));
              List<ExpressionPattern> newSubPatterns = new ArrayList<>(3);
              if (def == Prelude.ARRAY_CONS) {
                if (subPatterns.size() <= 2) {
                  newSubPatterns.addAll(subPatterns);
                } else {
                  if (keepLength) {
                    newSubPatterns.add(subPatterns.get(0));
                  }
                  newSubPatterns.addAll(subPatterns.subList(subPatterns.size() - 2, subPatterns.size()));
                }
              }
              removeImplicitPatterns((List<Object>) (List<?>) newSubPatterns, null);
              FunCallExpression newFunCall = new FunCallExpression((DConstructor) def, funCall.getLevels(), newLength, newElementsType);
              result.set(i, thisBinding == null ? new ConstructorExpressionPattern(newFunCall, newSubPatterns) : new ConstructorExpressionPattern(newFunCall, thisBinding, newLength, newSubPatterns));
            }
          }
        } else {
          List<ExpressionPattern> subPatterns = new ArrayList<>(pattern.getSubPatterns());
          removeImplicitPatterns((List<Object>) (List<?>) subPatterns, def == null ? Collections.emptyList() : DependentLink.Helper.toList(pattern.getParameters()));
          result.set(i, new ConstructorExpressionPattern(pattern, subPatterns));
        }
      }
    }
  }

  public static void unflattenClauses(List<ClauseElem> clauseElems, List<? super ExpressionPattern> result) {
    for (int i = clauseElems.size() - 1; i >= 0; i--) {
      if (clauseElems.get(i) instanceof DataClauseElem dataClauseElem) {
        DependentLink parameters = dataClauseElem.getParameters();
        int size = DependentLink.Helper.size(parameters);
        List<ExpressionPattern> patterns = new ArrayList<>(size);
        for (int j = i + 1; j < clauseElems.size() && patterns.size() < size; j++) {
          patterns.add(((PatternClauseElem) clauseElems.get(j)).pattern);
        }
        if (patterns.size() < size) {
          for (DependentLink link = DependentLink.Helper.get(parameters, clauseElems.size() - i - 1); link.hasNext(); link = link.getNext()) {
            patterns.add(new BindingPattern(link));
          }
        }
        clauseElems.subList(i, Math.min(i + size + 1, clauseElems.size())).clear();
        clauseElems.add(i, new PatternClauseElem(dataClauseElem.getPattern(patterns)));
      }
    }

    for (ClauseElem clauseElem : clauseElems) {
      result.add(((PatternClauseElem) clauseElem).pattern);
    }
  }

  public static List<ExpressionPattern> unflattenClauses(List<ClauseElem> clauseElems) {
    List<ExpressionPattern> result = new ArrayList<>(clauseElems.size());
    unflattenClauses(clauseElems, result);
    return result;
  }

  static void removeArguments(List<?> clauseElems, DependentLink parameters, List<DependentLink> elimParams) {
    if (parameters != null && elimParams != null && !elimParams.isEmpty()) {
      DependentLink link = parameters;
      for (int i = 0; i < elimParams.size(); i++, link = link.getNext()) {
        while (link != elimParams.get(i)) {
          clauseElems.remove(i);
          link = link.getNext();
        }
      }
      clauseElems.subList(elimParams.size(), clauseElems.size()).clear();
    }
  }

  static void addArguments(List<ExpressionPattern> patterns, DependentLink parameters) {
    for (DependentLink link = DependentLink.Helper.get(parameters, patterns.size()); link.hasNext(); link = link.getNext()) {
      patterns.add(new BindingPattern(link));
    }
  }
}
