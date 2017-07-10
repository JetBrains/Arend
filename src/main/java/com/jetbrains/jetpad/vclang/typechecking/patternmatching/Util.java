package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BindingPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.Pattern;
import com.jetbrains.jetpad.vclang.core.elimtree.Patterns;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    ConstructorClauseElem(Constructor constructor) {
      this.sort = Sort.STD;
      this.dataArguments = constructor.getDataTypeExpression(Sort.STD).getDefCallArguments();
      this.constructor = constructor;
    }
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
