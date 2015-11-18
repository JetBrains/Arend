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
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;

public class ArgsElimTreeExpander<T> {
  public static abstract class ArgsBranch<T> {
    public final List<T> results;

    protected ArgsBranch(List<T> results) {
      this.results = results;
    }
  }

  public static class ArgsOKBranch<T> extends ArgsBranch<T> {
    public final List<Expression> expressions;
    public final List<Binding> context;
    public final List<Integer> good;

    private ArgsOKBranch(List<Expression> expressions, List<Binding> context, List<Integer> good, List<T> results) {
      super(results);
      this.expressions = expressions;
      this.context = context;
      this.good = good;
    }
  }

  public static class ArgsIncompleteBranch<T> extends ArgsBranch<T> {
    public final List<ArgsOKBranch<T>> maybeBranches;

    private ArgsIncompleteBranch(List<ArgsOKBranch<T>> maybeBranches, List<T> results) {
      super(results);
      this.maybeBranches = maybeBranches;
    }
  }

  public static class ArgsFailedBranch<T> extends ArgsBranch<T> {
    public final List<Integer> bad;

    private ArgsFailedBranch(List<Integer> bad, List<T> results) {
      super(results);
      this.bad = bad;
    }
  }

  private final List<Binding> myLocalContext;
  private final int myOldContextSize;
  private List<List<Pattern>> myNestedPatterns;

  private final List<T> currentResults = new ArrayList<>();
  private final List<Expression> currentExpressions = new ArrayList<>();
  private final List<Integer> expressionLifting = new ArrayList<>();

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
      List<Expression> expressions = new ArrayList<>(currentExpressions);
      for (int lift = 0, i = expressions.size() - 1; i >= 0; lift += expressionLifting.get(i), --i) {
        expressions.set(i, expressions.get(i).liftIndex(0, lift));
      }

      ArgsOKBranch<T> okBranch = new ArgsOKBranch<>(expressions,
          new ArrayList<>(myLocalContext.subList(myOldContextSize, myLocalContext.size())), valid, new ArrayList<>(currentResults));
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

    if (nestedBranches == null)
      return null;

    for (Branch<T> branch : nestedBranches) {
      if (branch instanceof OKBranch) {
        assert incomplete == null;
        try (Utils.ContextSaver ignore = new Utils.ContextSaver(myLocalContext)) {
          OKBranch<T> okBranch = (OKBranch<T>) branch;
          myLocalContext.addAll(okBranch.context);

          currentResults.add(okBranch.result);
          currentExpressions.add(okBranch.expression);
          expressionLifting.add(okBranch.context.size());

          List<Integer> newValid = new ArrayList<>();
          for (int i : okBranch.good)
            newValid.add(valid.get(i));

          expandElimTreeRecurse(visitor, newValid, substituteInTypes(types, okBranch), null);

          expressionLifting.remove(expressionLifting.size() - 1);
          currentExpressions.remove(currentExpressions.size() - 1);
          currentResults.remove(currentResults.size() - 1);
        }
      } else if (branch instanceof IncompleteBranch) {
        List<T> results = new ArrayList<>(currentResults);
        results.add(((IncompleteBranch<T>) branch).result);
        ArgsIncompleteBranch<T> newIncomplete = incomplete != null ? incomplete
            : new ArgsIncompleteBranch<>(new ArrayList<ArgsOKBranch<T>>(), results);

        for (OKBranch<T> maybeBranch : ((IncompleteBranch<T>) branch).maybeBranches) {
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myLocalContext)) {
            myLocalContext.addAll(maybeBranch.context);
            currentResults.add(maybeBranch.result);
            currentExpressions.add(maybeBranch.expression);
            expressionLifting.add(maybeBranch.context.size());
            expandElimTreeRecurse(visitor, new ArrayList<Integer>(), substituteInTypes(types, maybeBranch), newIncomplete);
            expressionLifting.remove(expressionLifting.size() - 1);
            currentExpressions.remove(currentExpressions.size() - 1);
            currentResults.remove(currentResults.size() - 1);
          }
        }
      } else if (branch instanceof FailedBranch) {
        assert incomplete == null;
        ArrayList<Integer> bad = new ArrayList<>();
        for (int i : ((FailedBranch<T>) branch).bad)
          bad.add(valid.get(i));
        List<T> results = new ArrayList<>(currentResults);
        results.add(((FailedBranch<T>) branch).result);
        result.add(new ArgsFailedBranch<>(bad, results));
      }
    }

    return result;
  }

  private List<Expression> substituteInTypes(List<Expression> types, OKBranch okBranch) {
    List<Expression> newTypes = new ArrayList<>();
    for (int i = 1; i < types.size(); i++)
      newTypes.add(expandPatternSubstitute(okBranch.context.size(), i - 1, okBranch.expression, types.get(i)));
    return newTypes;
  }
}
