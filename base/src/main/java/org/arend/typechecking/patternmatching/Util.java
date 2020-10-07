package org.arend.typechecking.patternmatching;

import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.BranchKey;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Sort;

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
      return new ConstructorClauseElem((Constructor) branchKey);
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

    public ConstructorClauseElem(Constructor constructor) {
      this.dataArguments = constructor.getDataTypeExpression(Sort.STD).getDefCallArguments();
      this.constructor = constructor;
    }

    @Override
    public DependentLink getParameters() {
      return constructor.getParameters();
    }

    @Override
    public ConstructorExpressionPattern getPattern(List<ExpressionPattern> subPatterns) {
      return new ConstructorExpressionPattern(new ConCallExpression(constructor, Sort.STD, dataArguments, Collections.emptyList()), subPatterns);
    }
  }

  public static void unflattenClauses(List<ClauseElem> clauseElems, List<? super ExpressionPattern> result) {
    for (int i = clauseElems.size() - 1; i >= 0; i--) {
      if (clauseElems.get(i) instanceof DataClauseElem) {
        DataClauseElem dataClauseElem = (DataClauseElem) clauseElems.get(i);
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
