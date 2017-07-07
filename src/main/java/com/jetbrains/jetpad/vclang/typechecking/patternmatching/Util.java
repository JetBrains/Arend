package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class Util {
  interface ClauseElem {
  }

  public static class PatternClauseElem implements ClauseElem {
    public Pattern pattern;

    PatternClauseElem(Pattern pattern) {
      this.pattern = pattern;
    }
  }

  public static class ConstructorClauseElem implements ClauseElem {
    Sort sort;
    List<Expression> dataArguments;
    public Constructor constructor;

    ConstructorClauseElem(Sort sort, List<Expression> dataArguments, Constructor constructor) {
      this.sort = sort;
      this.dataArguments = dataArguments;
      this.constructor = constructor;
    }

    ConstructorClauseElem(Constructor constructor) {
      this.sort = Sort.STD;
      this.dataArguments = constructor.getDataTypeExpression(Sort.STD).getDefCallArguments();
      this.constructor = constructor;
    }
  }

  public static class ElimTreeWalker {
    private final Stack<ClauseElem> myStack = new Stack<>();
    private final BiConsumer<List<Expression>, Expression> myConsumer;

    public ElimTreeWalker(BiConsumer<List<Expression>, Expression> consumer) {
      myConsumer = consumer;
    }

    public void walk(ElimTree elimTree) {
      for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
        myStack.push(new PatternClauseElem(new BindingPattern(link)));
      }
      if (elimTree instanceof LeafElimTree) {
        Expression expression = ((LeafElimTree) elimTree).getExpression();
        if (expression != null) {
          myConsumer.accept(unflattenClauses(new ArrayList<>(myStack)), expression);
        }
      } else {
        BranchElimTree branchElimTree = (BranchElimTree) elimTree;
        for (Map.Entry<BranchElimTree.Pattern, ElimTree> entry : branchElimTree.getChildren()) {
          if (entry.getKey() instanceof Constructor) {
            myStack.push(new ConstructorClauseElem(branchElimTree.getSortArgument(), ((Constructor) entry.getKey()).matchDataTypeArguments(branchElimTree.getDataArguments()), (Constructor) entry.getKey()));
          }
          walk(entry.getValue());
          if (entry.getKey() instanceof Constructor) {
            myStack.pop();
          }
        }
      }
      for (DependentLink link = elimTree.getParameters(); link.hasNext(); link = link.getNext()) {
        myStack.pop();
      }
    }
  }

  static List<Expression> unflattenClauses(List<ClauseElem> clauseElems) {
    return unflattenClauses(clauseElems, null, Collections.emptyList());
  }

  static List<Expression> unflattenClauses(List<ClauseElem> clauseElems, DependentLink parameters, List<DependentLink> elimParams) {
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

    if (parameters != null) {
      for (DependentLink link = DependentLink.Helper.get(parameters, clauseElems.size()); link.hasNext(); link = link.getNext()) {
        clauseElems.add(new PatternClauseElem(new BindingPattern(link)));
      }

      DependentLink link = parameters;
      for (int i = 0; i < elimParams.size(); i++, link = link.getNext()) {
        while (link != elimParams.get(i)) {
          clauseElems.remove(i);
          link = link.getNext();
        }
      }
    }

    return clauseElems.stream().map(clauseElem -> ((PatternClauseElem) clauseElem).pattern.toExpression()).collect(Collectors.toList());
  }
}
