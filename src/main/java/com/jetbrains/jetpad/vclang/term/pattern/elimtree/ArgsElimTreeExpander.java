package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeExpander.Branch;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeExpander.FailedBranch;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeExpander.IncompleteBranch;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeExpander.OKBranch;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;
import static com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeExpander.recalcIndicies;

public class ArgsElimTreeExpander<T> {
  public static abstract class ArgsBranch<T> {
    public final List<T> results;
    public final List<Integer> indices;

    protected ArgsBranch(List<T> results, List<Integer> indices) {
      this.results = results;
      this.indices = indices;
    }
  }

  public static class ArgsOKBranch<T> extends ArgsBranch<T> {
    public final List<Expression> expressions;
    public final List<Binding> context;

    private ArgsOKBranch(List<Expression> expressions, List<Binding> context, List<Integer> indicies, List<T> results) {
      super(results, indicies);
      this.expressions = expressions;
      this.context = context;
    }
  }

  public static class ArgsIncompleteBranch<T> extends ArgsBranch<T> {
    public final List<ArgsOKBranch<T>> maybeBranches = new ArrayList<>();

    private ArgsIncompleteBranch(List<T> results) {
      super(results, Collections.<Integer>emptyList());
    }
  }

  public static class ArgsFailedBranch<T> extends ArgsBranch<T> {
    private ArgsFailedBranch(List<Integer> indicies, List<T> results) {
      super(results, indicies);
    }
  }

  private final List<Binding> myLocalContext;
  private final int myOldContextSize;
  private List<List<Pattern>> myNestedPatterns;

  private final List<OKBranch<T>> currentOKBranches = new ArrayList<>();

  private List<ArgsBranch<T>> result;

  public ArgsElimTreeExpander(List<Binding> localContext) {
    this.myLocalContext = localContext;
    this.myOldContextSize = localContext.size();
  }

  public List<ArgsBranch<T>> expandElimTree(ElimTreeVisitor<T> visitor, List<Expression> types, List<List<Pattern>> patterns, int numPatterns) {
    myNestedPatterns = patterns;
    List<Integer> valid = new ArrayList<>();
    for (int i = 0; i < numPatterns; i++)
      valid.add(i);
    result = new ArrayList<>();

    return expandElimTreeRecurse(visitor, valid, types, null);
  }

  private List<ArgsBranch<T>> expandElimTreeRecurse(ElimTreeVisitor<T> visitor, List<Integer> valid, List<Expression> types, ArgsIncompleteBranch<T> incomplete) {
    if (types.isEmpty()) {
      List<Expression> expressions = new ArrayList<>(currentOKBranches.size());
      for (int lift = 0, i = currentOKBranches.size() - 1; i >= 0; lift += currentOKBranches.get(i).context.size(), --i) {
        expressions.add(currentOKBranches.get(i).expression.liftIndex(0, lift));
      }
      Collections.reverse(expressions);

      ArgsOKBranch<T> okBranch = new ArgsOKBranch<>(expressions, new ArrayList<>(myLocalContext.subList(myOldContextSize, myLocalContext.size())), valid, retrieveResults());
      if (incomplete == null) {
        result.add(okBranch);
      } else {
        if (incomplete.maybeBranches.isEmpty())
          result.add(incomplete);
        incomplete.maybeBranches.add(okBranch);
      }
      return result;
    }

    List<Pattern> patterns = new ArrayList<>();
    for (int i : valid)
      patterns.add(myNestedPatterns.get(myNestedPatterns.size() - types.size()).get(i));
    final boolean isExplicit = myNestedPatterns.get(myNestedPatterns.size() - types.size()).isEmpty() || myNestedPatterns.get(myNestedPatterns.size() - types.size()).get(0).getExplicit();
    List<Branch<T>> nestedBranches = new ElimTreeExpander(myLocalContext).expandElimTree(visitor, patterns, types.get(0), isExplicit);

    for (Branch<T> branch : nestedBranches) {
      if (branch instanceof OKBranch) {
        try (Utils.MultiContextSaver ignore = new Utils.MultiContextSaver(myLocalContext, currentOKBranches)) {
          OKBranch<T> okBranch = (OKBranch<T>) branch;
          myLocalContext.addAll(okBranch.context);
          currentOKBranches.add(okBranch);
          expandElimTreeRecurse(visitor, recalcIndicies(valid, okBranch.indices), substituteInTypes(types, okBranch), null);
        }
      } else if (branch instanceof IncompleteBranch) {
        List<T> results = retrieveResults();
        results.add(branch.result);
        ArgsIncompleteBranch<T> newIncomplete = incomplete != null ? incomplete : new ArgsIncompleteBranch<>(results);

        for (OKBranch<T> maybeBranch : ((IncompleteBranch<T>) branch).maybeBranches) {
          try (Utils.MultiContextSaver ignore = new Utils.MultiContextSaver(myLocalContext, currentOKBranches)) {
            myLocalContext.addAll(maybeBranch.context);
            currentOKBranches.add(maybeBranch);
            expandElimTreeRecurse(visitor, Collections.<Integer>emptyList(), substituteInTypes(types, maybeBranch), newIncomplete);
          }
        }
      } else if (branch instanceof FailedBranch) {
        List<T> results = retrieveResults();
        results.add(branch.result);
        result.add(new ArgsFailedBranch<>(recalcIndicies(valid, branch.indices), results));
      }
    }

    return result;
  }

  private List<T> retrieveResults() {
    List<T> results = new ArrayList<>(currentOKBranches.size());
    for (OKBranch<T> okBranch : currentOKBranches) {
      results.add(okBranch.result);
    }
    return results;
  }

  private List<Expression> substituteInTypes(List<Expression> types, OKBranch okBranch) {
    List<Expression> newTypes = new ArrayList<>();
    for (int i = 1; i < types.size(); i++)
      newTypes.add(expandPatternSubstitute(okBranch.context.size(), i - 1, okBranch.expression, types.get(i)));
    return newTypes;
  }
}
