package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ArgsCoverageChecker.ArgsCoverageCheckingBranch;
import com.jetbrains.jetpad.vclang.term.pattern.ArgsCoverageChecker.ArgsCoverageCheckingFailedBranch;
import com.jetbrains.jetpad.vclang.term.pattern.ArgsCoverageChecker.ArgsCoverageCheckingIncompleteBranch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.patternMatchAll;

public class CoverageCheker {
  private final List<Binding> myLocalContext;

  public CoverageCheker(List<Binding> localContext) {
    this.myLocalContext = localContext;
  }

  public static abstract class CoverageCheckingBranch {}

  public static class CoverageCheckingOKBranch extends CoverageCheckingBranch {
    public final Expression expression;
    public final List<Binding> context;
    public final List<Integer> good;
    public final Pattern branchPattern;

    CoverageCheckingOKBranch(Expression expression, List<Binding> context, List<Integer> good, Pattern branchPattern) {
      this.expression = expression;
      this.context = context;
      this.good = good;
      this.branchPattern = branchPattern;
    }
  }

  public static class CoverageCheckingIncompleteBranch extends CoverageCheckingBranch {
    public final Pattern noncoveredPattern;

    CoverageCheckingIncompleteBranch(Pattern noncoveredPattern) {
      this.noncoveredPattern = noncoveredPattern;
    }
  }

  public static class CoverageCheckingFailedBranch extends CoverageCheckingBranch {
    public final List<Integer> bad;
    public final Pattern failedPattern;

    CoverageCheckingFailedBranch(List<Integer> bad, Pattern failedPattern) {
      this.bad = bad;
      this.failedPattern = failedPattern;
    }
  }

  public List<CoverageCheckingBranch> checkCoverage(List<Pattern> patterns, Expression type) {
    return checkCoverage(patterns, type, true);
  }

  public List<CoverageCheckingBranch> checkCoverage(List<Pattern> patterns, Expression type, boolean isExplicit) {
    List<Integer> namePatternIdxs = new ArrayList<>();
    boolean hasConstructorPattern = false;
    for (int i = 0; i < patterns.size(); i++) {
      if (patterns.get(i) instanceof ConstructorPattern) {
        hasConstructorPattern = true;
      } else if (patterns.get(i) instanceof NamePattern) {
        namePatternIdxs.add(i);
      }
    }

    if (!hasConstructorPattern) {
      if (patterns.isEmpty()) {
        return new ArrayList<>();
      } else {
        return Collections.<CoverageCheckingBranch>singletonList(new CoverageCheckingOKBranch(Index(0),
            Collections.<Binding>singletonList(new TypedBinding((String) null, type)), namePatternIdxs, match(isExplicit, null)));
      }
    }

    List<Expression> parameters = new ArrayList<>();
    Expression ftype = type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext).getFunction(parameters);
    Collections.reverse(parameters);
    DataDefinition dataType = (DataDefinition) ((DefCallExpression) ftype).getDefinition();
    List<Constructor> validConstructors = new ArrayList<>();
    List<List<TypeArgument>> validConstructorArgs = new ArrayList<>();
    for (Constructor constructor : dataType.getConstructors()) {
      if (constructor.hasErrors())
        continue;
      if (constructor.hasErrors())
        continue;
      List<Expression> matchedParameters = null;
      if (constructor.getPatterns() != null) {
        Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), parameters, myLocalContext);
        if (matchResult instanceof Utils.PatternMatchMaybeResult) {
          List<Integer> bad = new ArrayList<>();
          for (int i = 0; i < patterns.size(); i++) {
            if (patterns.get(i) instanceof ConstructorPattern)
              bad.add(i);
          }
          return Collections.<CoverageCheckingBranch>singletonList(
              new CoverageCheckingFailedBranch(bad, match(isExplicit, "*")));
        } else if (matchResult instanceof Utils.PatternMatchFailedResult) {
          continue;
        } else if (matchResult instanceof Utils.PatternMatchOKResult) {
          matchedParameters = ((Utils.PatternMatchOKResult) matchResult).expressions;
        }
      } else {
        matchedParameters = parameters;
      }
      validConstructors.add(constructor);
      validConstructorArgs.add(new ArrayList<TypeArgument>());
      splitArguments(constructor.getType().subst(matchedParameters, 0), validConstructorArgs.get(validConstructorArgs.size() - 1));
    }

    List<CoverageCheckingBranch> result = new ArrayList<>();
    for (int i = 0; i < validConstructors.size(); i++) {
      List<Integer> goodPatternIdxs = new ArrayList<>();
      List<List<Pattern>> goodPatternNested = new ArrayList<>();
      for (int j = 0; j < validConstructorArgs.get(i).size(); j++) {
        goodPatternNested.add(new ArrayList<Pattern>());
      }

      for (int j = 0; j < patterns.size(); j++) {
        if (patterns.get(j) instanceof NamePattern) {
          goodPatternIdxs.add(j);
          for (int k = 0; k < goodPatternNested.size(); k++) {
            goodPatternNested.get(k).add(match(validConstructorArgs.get(i).get(k).getExplicit(), null));
          }
        } else if (patterns.get(j) instanceof ConstructorPattern
            && ((ConstructorPattern) patterns.get(j)).getConstructor() == validConstructors.get(i)) {
          goodPatternIdxs.add(j);
          for (int k = 0; k < goodPatternNested.size(); k++) {
            goodPatternNested.get(k).add(((ConstructorPattern) patterns.get(j)).getArguments().get(k));
          }
        }
      }

      if (goodPatternIdxs.isEmpty()) {
        List<Pattern> args = new ArrayList<>();
        for (TypeArgument arg : validConstructors.get(i).getArguments())
          args.add(match(arg.getExplicit(), null));
        result.add(new CoverageCheckingIncompleteBranch(new ConstructorPattern(validConstructors.get(i), args, isExplicit)));
        continue;
      }

      List<ArgsCoverageCheckingBranch> nestedBranches = new ArgsCoverageChecker(myLocalContext).checkCoverage(validConstructorArgs.get(i), goodPatternNested, goodPatternIdxs.size());
      for (ArgsCoverageCheckingBranch branch : nestedBranches) {
        if (branch instanceof ArgsCoverageCheckingIncompleteBranch) {
          ConstructorPattern pattern = new ConstructorPattern(validConstructors.get(i), new ArrayList<Pattern>(), isExplicit);
          for (CoverageCheckingOKBranch okBranch : branch.okBranches)
            pattern.getArguments().add(okBranch.branchPattern);
          pattern.getArguments().add(((ArgsCoverageCheckingIncompleteBranch) branch).incompleteBranch.noncoveredPattern);
          for (int j = pattern.getArguments().size(); j < validConstructorArgs.get(i).size(); j++)
            pattern.getArguments().add(match(validConstructorArgs.get(i).get(j).getExplicit(), null));

          result.add(new CoverageCheckingIncompleteBranch(pattern));
        } else if (branch instanceof ArgsCoverageCheckingFailedBranch) {
          ConstructorPattern pattern = new ConstructorPattern(validConstructors.get(i), new ArrayList<Pattern>(), isExplicit);
          for (CoverageCheckingOKBranch okBranch : branch.okBranches)
            pattern.getArguments().add(okBranch.branchPattern);
          pattern.getArguments().add(((ArgsCoverageCheckingFailedBranch) branch).failedBranch.failedPattern);
          for (int j = pattern.getArguments().size(); j < validConstructorArgs.get(i).size(); j++)
            pattern.getArguments().add(match(validConstructorArgs.get(i).get(j).getExplicit(), null));

          List<Integer> bad = new ArrayList<>();
          for (int j : ((ArgsCoverageCheckingFailedBranch) branch).failedBranch.bad)
            bad.add(j);

          result.add(new CoverageCheckingFailedBranch(bad, pattern));
        } else {
          ConstructorPattern pattern = new ConstructorPattern(validConstructors.get(i), new ArrayList<Pattern>(), isExplicit);
          List<Binding> context = new ArrayList<>();
          Expression expr = DefCall(null, validConstructors.get(i), parameters);
          for (CoverageCheckingOKBranch okBranch : branch.okBranches) {
            context.addAll(okBranch.context);
            pattern.getArguments().add(okBranch.branchPattern);
            expr = Apps(expr.liftIndex(0, okBranch.context.size()), okBranch.expression);
          }
          List<Integer> good = new ArrayList<>();
          if (branch.okBranches.isEmpty()) {
            good.addAll(goodPatternIdxs);
          } else {
            for (int j : branch.okBranches.get(branch.okBranches.size() - 1).good)
              good.add(goodPatternIdxs.get(j));
          }

          result.add(new CoverageCheckingOKBranch(expr, context, good, pattern));
        }
      }
    }

    return result;
  }
}
