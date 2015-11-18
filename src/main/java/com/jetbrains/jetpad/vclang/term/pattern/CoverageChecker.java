package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.match;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class CoverageChecker implements ElimTreeVisitor<CoverageChecker.CoverageCheckingBranch> {
  public CoverageChecker() {}

  @Override
  public CoverageChecker.CoverageCheckingBranch visitElimOK(boolean isExplicit, Constructor constructor, List<CoverageCheckingBranch> children) {
    return new CoverageCheckingOKBranch(new ConstructorPattern(constructor, fillTheRest(constructor, children), isExplicit));
  }

  @Override
  public CoverageCheckingBranch visitElimIncomplete(boolean isExplicit, Constructor constructor, List<CoverageCheckingBranch> children) {
    return new CoverageCheckingIncompleteBranch(new ConstructorPattern(constructor, fillTheRest(constructor, children), isExplicit));
  }

  private List<Pattern> fillTheRest(Constructor constructor, List<CoverageCheckingBranch> children) {
    List<Pattern> args = new ArrayList<>();
    for (CoverageCheckingBranch branch : children) {
      if (branch instanceof CoverageCheckingOKBranch) {
        args.add(((CoverageCheckingOKBranch) branch).branchPattern);
      } else if (branch instanceof CoverageCheckingIncompleteBranch) {
        args.add(((CoverageCheckingIncompleteBranch) branch).noncoveredPattern);
        break;
      } else if (branch instanceof CoverageCheckingFailedBranch){
        args.add(((CoverageCheckingFailedBranch) branch).failedPattern);
        break;
      }
    }
    return fillTheRestPatterns(constructor, args);
  }

  @Override
  public CoverageCheckingBranch visitFailed(boolean isExplicit, Constructor constructor, List<CoverageCheckingBranch> children) {
    return new CoverageCheckingFailedBranch(new ConstructorPattern(constructor, fillTheRest(constructor, children), isExplicit));
  }

  private List<Pattern> fillTheRestPatterns(Constructor constructor, List<Pattern> args) {
    List<TypeArgument> constructorArgs = splitArguments(constructor.getArguments());
    for (int i = args.size(); i < constructorArgs.size(); i++) {
      args.add(match(constructorArgs.get(i).getExplicit(), null));
    }
    return args;
  }

  @Override
  public CoverageCheckingBranch visitIncomplete(boolean isExplicit, Constructor constructor) {
    if (constructor == null)
      return new CoverageCheckingIncompleteBranch(match(isExplicit, null));
    else
      return new CoverageCheckingIncompleteBranch(new ConstructorPattern(constructor, fillTheRestPatterns(constructor, new ArrayList<Pattern>()), isExplicit));
  }

  @Override
  public CoverageCheckingBranch visitFailed(boolean isExplicit) {
    return new CoverageCheckingFailedBranch(match(isExplicit, "*"));
  }

  @Override
  public CoverageCheckingBranch visitName(boolean isExplicit) {
    return new CoverageCheckingOKBranch(match(isExplicit, null));
  }

  public static abstract class CoverageCheckingBranch {}

  public static class CoverageCheckingOKBranch extends CoverageCheckingBranch {
    public final Pattern branchPattern;

    CoverageCheckingOKBranch(Pattern branchPattern) {
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
    public final Pattern failedPattern;

    CoverageCheckingFailedBranch(Pattern failedPattern) {
      this.failedPattern = failedPattern;
    }
  }
}
