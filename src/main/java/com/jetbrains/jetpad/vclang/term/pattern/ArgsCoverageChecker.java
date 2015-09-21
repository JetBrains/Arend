package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.CoverageCheker.CoverageCheckingBranch;
import com.jetbrains.jetpad.vclang.term.pattern.CoverageCheker.CoverageCheckingFailedBranch;
import com.jetbrains.jetpad.vclang.term.pattern.CoverageCheker.CoverageCheckingIncompleteBranch;
import com.jetbrains.jetpad.vclang.term.pattern.CoverageCheker.CoverageCheckingOKBranch;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.match;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;

public class ArgsCoverageChecker {
  public static abstract class ArgsCoverageCheckingBranch {}

  public static class ArgsCoverageCheckingOKBranch extends ArgsCoverageCheckingBranch {
    public final List<Expression> expressions;
    public final List<Binding> context;
    public final List<Integer> good;
    public final List<Pattern> patterns;

    private ArgsCoverageCheckingOKBranch(List<Expression> expressions, List<Binding> context, List<Integer> good, List<Pattern> patterns) {
      this.expressions = expressions;
      this.context = context;
      this.good = good;
      this.patterns = patterns;
    }
  }

  public static class ArgsCoverageCheckingIncompleteBranch extends ArgsCoverageCheckingBranch {
    public final List<Pattern> incompletePatterns;
    public final List<ArgsCoverageCheckingOKBranch> maybeBranches;

    private ArgsCoverageCheckingIncompleteBranch(List<Pattern> incompletePatterns, List<ArgsCoverageCheckingOKBranch> maybeBranches) {
      this.incompletePatterns = incompletePatterns;
      this.maybeBranches = maybeBranches;
    }
  }

  public static class ArgsCoverageCheckingFailedBranch extends ArgsCoverageCheckingBranch {
    public final List<Pattern> failedPatterns;
    public final List<Integer> bad;

    private ArgsCoverageCheckingFailedBranch(List<Pattern> failedPatterns, List<Integer> bad) {
      this.failedPatterns = failedPatterns;
      this.bad = bad;
    }
  }

  private final List<Binding> myLocalContext;
  private final int myOldContextSize;
  private List<List<Pattern>> myNestedPatterns;

  private final List<Pattern> currentPatterns = new ArrayList<>();
  private final List<Expression> currentExpressions = new ArrayList<>();

  private List<ArgsCoverageCheckingBranch> result;

  public ArgsCoverageChecker(List<Binding> localContext) {
    this.myLocalContext = localContext;
    this.myOldContextSize = localContext.size();
  }

  public List<ArgsCoverageCheckingBranch> checkCoverage(List<Expression> types, List<List<Pattern>> patterns, int numPatterns) {
    myNestedPatterns = patterns;
    List<Integer> valid = new ArrayList<>();
    for (int i = 0; i < numPatterns; i++)
      valid.add(i);
    result = new ArrayList<>();

    return checkCoverageRecurse(valid, types, null);
  }

  private List<ArgsCoverageCheckingBranch> checkCoverageRecurse(List<Integer> valid, List<Expression> types, ArgsCoverageCheckingIncompleteBranch incomplete) {
    if (types.isEmpty()) {
      ArgsCoverageCheckingOKBranch okBranch = new ArgsCoverageCheckingOKBranch(new ArrayList<>(currentExpressions),
            new ArrayList<>(myLocalContext.subList(myOldContextSize, myLocalContext.size())),
            valid, new ArrayList<>(currentPatterns));
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
    List<CoverageCheckingBranch> nestedBranches = new CoverageCheker(myLocalContext).checkCoverage(patterns, types.get(0), isExplicit);

    if (nestedBranches == null)
      return null;

    for (CoverageCheckingBranch branch : nestedBranches) {
      if (branch instanceof CoverageCheckingOKBranch) {
        assert incomplete == null;
        try (Utils.ContextSaver ignore = new Utils.ContextSaver(myLocalContext)) {
          CoverageCheckingOKBranch okBranch = (CoverageCheckingOKBranch) branch;
          myLocalContext.addAll(okBranch.context);

          currentPatterns.add(okBranch.branchPattern);
          currentExpressions.add(okBranch.expression);

          List<Integer> newValid = new ArrayList<>();
          for (int i : okBranch.good)
            newValid.add(valid.get(i));

          checkCoverageRecurse(newValid, substituteInTypes(types, okBranch), null);

          currentExpressions.remove(currentExpressions.size() - 1);
          currentPatterns.remove(currentPatterns.size() - 1);
        }
      } else if (branch instanceof CoverageCheckingIncompleteBranch) {
        ArgsCoverageCheckingIncompleteBranch newIncomplete = incomplete != null ? incomplete
            : new ArgsCoverageCheckingIncompleteBranch(fillAnyPatterns(((CoverageCheckingIncompleteBranch) branch).noncoveredPattern), new ArrayList<ArgsCoverageCheckingOKBranch>());

        for (CoverageCheckingOKBranch maybeBranch : ((CoverageCheckingIncompleteBranch) branch).maybeBranches) {
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myLocalContext)) {
            myLocalContext.addAll(maybeBranch.context);
            currentPatterns.add(maybeBranch.branchPattern);
            currentExpressions.add(maybeBranch.expression);
            checkCoverageRecurse(new ArrayList<Integer>(), substituteInTypes(types, maybeBranch), newIncomplete);
            currentExpressions.remove(currentExpressions.size() - 1);
            currentPatterns.remove(currentPatterns.size() - 1);
          }
        }
      } else if (branch instanceof CoverageCheckingFailedBranch) {
        assert incomplete == null;
        ArrayList<Integer> bad = new ArrayList<>();
        for (int i : ((CoverageCheckingFailedBranch) branch).bad)
          bad.add(valid.get(i));
        result.add(new ArgsCoverageCheckingFailedBranch(fillAnyPatterns(((CoverageCheckingFailedBranch) branch).failedPattern), bad));
      }
    }

    return result;
  }

  private List<Pattern> fillAnyPatterns(Pattern lastPattern) {
    List<Pattern> failedPatterns = new ArrayList<>(currentPatterns);
    failedPatterns.add(lastPattern);
    for (int i = currentPatterns.size() + 1; i < myNestedPatterns.size(); i++)
      failedPatterns.add(match(myNestedPatterns.get(i).isEmpty() || myNestedPatterns.get(i).get(0).getExplicit(), null));
    return failedPatterns;
  }

  private List<Expression> substituteInTypes(List<Expression> types, CoverageCheckingOKBranch okBranch) {
    List<Expression> newTypes = new ArrayList<>();
    for (int i = 1; i < types.size(); i++)
      newTypes.add(expandPatternSubstitute(okBranch.context.size(), i - 1, okBranch.expression, types.get(i)));
    return newTypes;
  }
}
