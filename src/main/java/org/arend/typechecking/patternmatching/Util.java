package org.arend.typechecking.patternmatching;

import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Sort;

import java.util.*;

public class Util {
  interface ClauseElem {
  }

  public static class PatternClauseElem implements ClauseElem {
    public final ExpressionPattern pattern;

    PatternClauseElem(ExpressionPattern pattern) {
      this.pattern = pattern;
    }
  }

  public interface DataClauseElem extends ClauseElem {
    DependentLink getParameters();
    ConstructorExpressionPattern getPattern(List<ExpressionPattern> subPatterns);
  }

  static DataClauseElem makeDataClauseElem(Constructor constructor, ConstructorExpressionPattern pattern) {
    return constructor instanceof SingleConstructor ? new TupleClauseElem(pattern) : new ConstructorClauseElem(constructor);
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

    private ConstructorClauseElem(Constructor constructor) {
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

  public static List<ExpressionPattern> unflattenClauses(List<ClauseElem> clauseElems) {
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

    List<ExpressionPattern> result = new ArrayList<>(clauseElems.size());
    for (ClauseElem clauseElem : clauseElems) {
      result.add(((PatternClauseElem) clauseElem).pattern);
    }
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
