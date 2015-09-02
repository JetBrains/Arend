package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.arg.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.CoverageCheker.CoverageCheckingBranch;
import com.jetbrains.jetpad.vclang.term.pattern.CoverageCheker.CoverageCheckingFailedBranch;
import com.jetbrains.jetpad.vclang.term.pattern.CoverageCheker.CoverageCheckingIncompleteBranch;
import com.jetbrains.jetpad.vclang.term.pattern.CoverageCheker.CoverageCheckingOKBranch;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.TypeArg;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;

public class ArgsCoverageChecker {
  public static class ArgsCoverageCheckingBranch {
    public final List<CoverageCheckingOKBranch> okBranches;
    ArgsCoverageCheckingBranch(List<CoverageCheckingOKBranch> okBranches) {
      this.okBranches = okBranches;
    }
  }

  public static class ArgsCoverageCheckingIncompleteBranch extends ArgsCoverageCheckingBranch {
    public final CoverageCheckingIncompleteBranch incompleteBranch;
    ArgsCoverageCheckingIncompleteBranch(List<CoverageCheckingOKBranch> okBranches, CoverageCheckingIncompleteBranch incompleteBranch) {
      super(okBranches);
      this.incompleteBranch = incompleteBranch;
    }
  }

  public static class ArgsCoverageCheckingFailedBranch extends ArgsCoverageCheckingBranch {
    public final CoverageCheckingFailedBranch failedBranch;
    ArgsCoverageCheckingFailedBranch(List<CoverageCheckingOKBranch> okBranches, CoverageCheckingFailedBranch failedBranch) {
      super(okBranches);
      this.failedBranch = failedBranch;
    }
  }

  private final List<Binding> myLocalContext;
  private List<List<Pattern>> myNestedPatterns;
  private List<CoverageCheckingOKBranch> currentBranch = new ArrayList<>();
  private List<ArgsCoverageCheckingBranch> result;

  public ArgsCoverageChecker(List<Binding> localContext) {
    this.myLocalContext = localContext;
  }

  public List<ArgsCoverageCheckingBranch> checkCoverage(List<TypeArgument> args, List<List<Pattern>> patterns, int numPatterns) {
    myNestedPatterns = patterns;
    List<Integer> valid = new ArrayList<>();
    for (int i = 0; i < numPatterns; i++) {
      valid.add(i);
    }
    result = new ArrayList<>();
    return checkCoverageRecurse(valid, args);
  }

  private List<ArgsCoverageCheckingBranch> checkCoverageRecurse(List<Integer> valid, List<TypeArgument> constructorArgs) {
    if (constructorArgs.isEmpty()) {
      result.add(new ArgsCoverageCheckingBranch(new ArrayList<>(currentBranch)));
      return result;
    }

    List<Pattern> patterns = new ArrayList<>();
    for (int i : valid)
      patterns.add(myNestedPatterns.get(myNestedPatterns.size() - constructorArgs.size()).get(i));

    List<CoverageCheckingBranch> nestedBranches = new CoverageCheker(myLocalContext).checkCoverage(patterns, constructorArgs.get(0).getType(), constructorArgs.get(0).getExplicit());
    if (nestedBranches == null)
      return null;

    for (CoverageCheckingBranch branch : nestedBranches) {
      try (Utils.ContextSaver ignore = new Utils.ContextSaver(myLocalContext)) {
        if (branch instanceof CoverageCheckingOKBranch) {
          CoverageCheckingOKBranch okBranch = (CoverageCheckingOKBranch) branch;
          currentBranch.add(okBranch);

          myLocalContext.addAll(okBranch.context);

          List<Integer> newValid = new ArrayList<>();
          for (int i : okBranch.good)
            newValid.add(valid.get(i));

          List<TypeArgument> newConstructorArgs = new ArrayList<>();
          for (int i = 1; i < constructorArgs.size(); i++) {
            newConstructorArgs.add(TypeArg(constructorArgs.get(i).getExplicit(),
                expandPatternSubstitute(okBranch.context.size(), i - 1, okBranch.expression, constructorArgs.get(i).getType())));
          }

          checkCoverageRecurse(newValid, newConstructorArgs);

          currentBranch.remove(currentBranch.size() - 1);
        } else if (branch instanceof CoverageCheckingIncompleteBranch) {
          result.add(new ArgsCoverageCheckingIncompleteBranch(new ArrayList<>(currentBranch), (CoverageCheckingIncompleteBranch) branch));
        } else if (branch instanceof CoverageCheckingFailedBranch) {
          result.add(new ArgsCoverageCheckingFailedBranch(new ArrayList<>(currentBranch), (CoverageCheckingFailedBranch) branch));
        }
      }
    }

    return result;
  }
}
