package org.arend.typechecking.patternmatching;

import org.arend.core.constructor.SingleConstructor;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.ConCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.SigmaExpression;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.pattern.Patterns;
import org.arend.core.sort.Sort;

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

  public interface DataClauseElem extends ClauseElem {
    DependentLink getParameters();
    ConstructorPattern getPattern(Patterns arguments);
  }

  static DataClauseElem makeDataClauseElem(Constructor constructor, ConstructorPattern pattern) {
    return constructor instanceof SingleConstructor ? new TupleClauseElem(pattern) : new ConstructorClauseElem(constructor);
  }

  public static class TupleClauseElem implements DataClauseElem {
    public final ConstructorPattern pattern;

    TupleClauseElem(ConstructorPattern pattern) {
      this.pattern = pattern;
    }

    @Override
    public DependentLink getParameters() {
      return pattern.getParameters();
    }

    @Override
    public ConstructorPattern getPattern(Patterns arguments) {
      return new ConstructorPattern(pattern, arguments);
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
    public ConstructorPattern getPattern(Patterns arguments) {
      return new ConstructorPattern(new ConCallExpression(constructor, Sort.STD, dataArguments, Collections.emptyList()), arguments);
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
          myConsumer.accept(unflattenClauses(new ArrayList<>(myStack)), expression);
        }
      } else {
        BranchElimTree branchElimTree = (BranchElimTree) elimTree;
        for (Map.Entry<Constructor, ElimTree> entry : branchElimTree.getChildren()) {
          if (entry.getKey() != null) {
            myStack.push(entry.getKey() instanceof SingleConstructor ? new TupleClauseElem(new ConstructorPattern(new SigmaExpression(Sort.STD, entry.getValue().getParameters()), new Patterns(Collections.emptyList()))) : new ConstructorClauseElem(entry.getKey()));
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

  public static List<Pattern> unflattenClauses(List<ClauseElem> clauseElems) {
    for (int i = clauseElems.size() - 1; i >= 0; i--) {
      if (clauseElems.get(i) instanceof DataClauseElem) {
        DataClauseElem dataClauseElem = (DataClauseElem) clauseElems.get(i);
        DependentLink parameters = dataClauseElem.getParameters();
        int size = DependentLink.Helper.size(parameters);
        List<Pattern> patterns = new ArrayList<>(size);
        for (int j = i + 1; j < clauseElems.size() && patterns.size() < size; j++) {
          patterns.add(((PatternClauseElem) clauseElems.get(j)).pattern);
        }
        if (patterns.size() < size) {
          for (DependentLink link = DependentLink.Helper.get(parameters, clauseElems.size() - i - 1); link.hasNext(); link = link.getNext()) {
            patterns.add(new BindingPattern(link));
          }
        }
        clauseElems.subList(i, Math.min(i + size + 1, clauseElems.size())).clear();
        clauseElems.add(i, new PatternClauseElem(dataClauseElem.getPattern(new Patterns(patterns))));
      }
    }

    List<Pattern> result = new ArrayList<>(clauseElems.size());
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

  static void addArguments(List<Pattern> patterns, DependentLink parameters) {
    for (DependentLink link = DependentLink.Helper.get(parameters, patterns.size()); link.hasNext(); link = link.getNext()) {
      patterns.add(new BindingPattern(link));
    }
  }
}
