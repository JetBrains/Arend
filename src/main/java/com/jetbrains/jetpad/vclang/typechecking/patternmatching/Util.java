package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.pattern.BindingPattern;
import com.jetbrains.jetpad.vclang.core.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.*;
import java.util.function.BiConsumer;

public class Util {
  interface ClauseElem {
  }

  public static class PatternClauseElem implements ClauseElem {
    public final Pattern pattern;

    PatternClauseElem(Pattern pattern) {
      this.pattern = pattern;
    }
  }

  public static class ConstructorClauseElem implements ClauseElem {
    public final Sort sort;
    public final List<Expression> dataArguments;
    public final Constructor constructor;

    ConstructorClauseElem(Constructor constructor) {
      this.sort = Sort.STD;
      this.dataArguments = constructor.getDataTypeExpression(Sort.STD).getDefCallArguments();
      this.constructor = constructor;
    }
  }

  public static class ElimTreeWalker {
    private final Stack<ClauseElem> myStack = new Stack<>();
    private final BiConsumer<List<Pattern>, Expression> myConsumer;

    public ElimTreeWalker(BiConsumer<List<Pattern>, Expression> consumer) {
      myConsumer = consumer;
    }

    public void walk(ElimTree elimTree) {
      for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
        myStack.push(new PatternClauseElem(new BindingPattern(link)));
      }
      if (elimTree instanceof LeafElimTree) {
        Expression expression = ((LeafElimTree) elimTree).getExpression();
        if (expression != null) {
          myConsumer.accept(unflattenClausesPatterns(new ArrayList<>(myStack)), expression);
        }
      } else {
        BranchElimTree branchElimTree = (BranchElimTree) elimTree;
        for (Map.Entry<Constructor, ElimTree> entry : branchElimTree.getChildren()) {
          if (entry.getKey() != null) {
            myStack.push(new ConstructorClauseElem(entry.getKey()));
            walk(entry.getValue());
            myStack.pop();
          }
        }
      }
      for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
        myStack.pop();
      }
    }
  }

  private static List<Pattern> unflattenClausesPatterns(List<ClauseElem> clauseElems) {
    for (int i = clauseElems.size() - 1; i >= 0; i--) {
      if (clauseElems.get(i) instanceof ConstructorClauseElem) {
        ConstructorClauseElem conClauseElem = (ConstructorClauseElem) clauseElems.get(i);
        Constructor constructor = conClauseElem.constructor;
        int size = DependentLink.Helper.size(constructor.getParameters());
        List<Pattern> patterns = new ArrayList<>(size);
        for (int j = i + 1; j < clauseElems.size() && patterns.size() < size; j++) {
          patterns.add(((PatternClauseElem) clauseElems.get(j)).pattern);
        }
        if (patterns.size() < size) {
          for (DependentLink link = DependentLink.Helper.get(constructor.getParameters(), clauseElems.size() - i - 1); link.hasNext(); link = link.getNext()) {
            patterns.add(new BindingPattern(link));
          }
        }
        clauseElems.subList(i, Math.min(i + size + 1, clauseElems.size())).clear();
        clauseElems.add(i, new PatternClauseElem(new ConstructorPattern(new ConCallExpression(constructor, conClauseElem.sort, conClauseElem.dataArguments, Collections.emptyList()), new Patterns(patterns))));
      }
    }

    List<Pattern> result = new ArrayList<>(clauseElems.size());
    for (ClauseElem clauseElem : clauseElems) {
      result.add(((PatternClauseElem) clauseElem).pattern);
    }
    return result;
  }

  static List<Expression> unflattenClauses(List<ClauseElem> clauseElems) {
    List<Pattern> patterns = unflattenClausesPatterns(clauseElems);
    List<Expression> result = new ArrayList<>(patterns.size());
    for (Pattern pattern : patterns) {
      result.add(pattern.toExpression());
    }
    return result;
  }

  static void removeArguments(List<?> clauseElems, DependentLink parameters, List<DependentLink> elimParams) {
    if (parameters != null && elimParams != null) {
      DependentLink link = parameters;
      for (int i = 0; i < elimParams.size(); i++, link = link.getNext()) {
        while (link != elimParams.get(i)) {
          clauseElems.remove(i);
          link = link.getNext();
        }
      }
    }
  }

  static void addArguments(List<Expression> expressions, DependentLink parameters) {
    for (DependentLink link = DependentLink.Helper.get(parameters, expressions.size()); link.hasNext(); link = link.getNext()) {
      expressions.add(new ReferenceExpression(link));
    }
  }
}
