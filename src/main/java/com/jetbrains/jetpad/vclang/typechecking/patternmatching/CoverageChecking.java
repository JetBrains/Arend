package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BindingPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.Pattern;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class CoverageChecking {
  interface ClauseElem {
  }

  static class PatternClauseElem implements ClauseElem {
    public Pattern pattern;

    PatternClauseElem(Pattern pattern) {
      this.pattern = pattern;
    }
  }

  static class ConstructorClauseElem implements ClauseElem {
    public Constructor constructor;

    ConstructorClauseElem(Constructor constructor) {
      this.constructor = constructor;
    }
  }

  static List<Expression> unflattenMissingClause(List<ClauseElem> clauseElems, DependentLink parameters, List<DependentLink> elimParams) {
    for (int i = clauseElems.size() - 1; i >= 0; i--) {
      if (clauseElems.get(i) instanceof ConstructorClauseElem) {
        Constructor constructor = ((ConstructorClauseElem) clauseElems.get(i)).constructor;
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
        clauseElems.add(i, new PatternClauseElem(new ConstructorPattern(new ConCallExpression(constructor, Sort.STD, DependentLink.Helper.toList(constructor.getDataTypeParameters()).stream().map(ReferenceExpression::new).collect(Collectors.toList()), Collections.emptyList()), patterns)));
      }
    }

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

    for (ClauseElem clauseElem : clauseElems) {
      if (!(clauseElem instanceof PatternClauseElem)) {
        throw new IllegalStateException();
      }
    }

    return clauseElems.stream().map(clauseElem -> ((PatternClauseElem) clauseElem).pattern.toExpression()).collect(Collectors.toList());
  }
}
