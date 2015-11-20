package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.match;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class CoverageChecker implements ElimTreeVisitor<CoverageChecker.CoverageCheckingResult> {
  public static abstract class CoverageCheckingResult {}

  public static class CoverageCheckingOKResult extends CoverageCheckingResult {
    public final Pattern branchPattern;

    CoverageCheckingOKResult(Pattern branchPattern) {
      this.branchPattern = branchPattern;
    }
  }

  public static class CoverageCheckingIncompleteResult extends CoverageCheckingResult {
    public final Pattern noncoveredPattern;

    CoverageCheckingIncompleteResult(Pattern noncoveredPattern) {
      this.noncoveredPattern = noncoveredPattern;
    }
  }

  public static class CoverageCheckingFailedResult extends CoverageCheckingResult {
    public final Pattern failedPattern;

    CoverageCheckingFailedResult(Pattern failedPattern) {
      this.failedPattern = failedPattern;
    }
  }

  @Override
  public CoverageCheckingResult visitElimOK(boolean isExplicit, Constructor constructor, List<CoverageCheckingResult> children) {
    return new CoverageCheckingOKResult(new ConstructorPattern(constructor, fillTheRest(constructor, children), isExplicit));
  }

  @Override
  public CoverageCheckingResult visitElimIncomplete(boolean isExplicit, Constructor constructor, List<CoverageCheckingResult> children) {
    return new CoverageCheckingIncompleteResult(new ConstructorPattern(constructor, fillTheRest(constructor, children), isExplicit));
  }

  @Override
  public CoverageCheckingResult visitElimFailed(boolean isExplicit, Constructor constructor, List<CoverageCheckingResult> children) {
    return new CoverageCheckingFailedResult(new ConstructorPattern(constructor, fillTheRest(constructor, children), isExplicit));
  }

  private List<Pattern> fillTheRest(Constructor constructor, List<CoverageCheckingResult> children) {
    List<Pattern> args = new ArrayList<>();
    for (CoverageCheckingResult branch : children) {
      if (branch instanceof CoverageCheckingOKResult) {
        args.add(((CoverageCheckingOKResult) branch).branchPattern);
      } else if (branch instanceof CoverageCheckingIncompleteResult) {
        args.add(((CoverageCheckingIncompleteResult) branch).noncoveredPattern);
        break;
      } else if (branch instanceof CoverageCheckingFailedResult){
        args.add(((CoverageCheckingFailedResult) branch).failedPattern);
        break;
      }
    }
    return fillTheRestPatterns(constructor, args);
  }

  private List<Pattern> fillTheRestPatterns(Constructor constructor, List<Pattern> args) {
    List<TypeArgument> constructorArgs = splitArguments(constructor.getArguments());
    for (int i = args.size(); i < constructorArgs.size(); i++) {
      args.add(match(constructorArgs.get(i).getExplicit(), null));
    }
    return args;
  }

  @Override
  public CoverageCheckingResult visitIncomplete(boolean isExplicit, Constructor constructor) {
    if (constructor == null)
      return new CoverageCheckingIncompleteResult(match(isExplicit, null));
    else
      return new CoverageCheckingIncompleteResult(new ConstructorPattern(constructor, fillTheRestPatterns(constructor, new ArrayList<Pattern>()), isExplicit));
  }

  @Override
  public CoverageCheckingResult visitFailed(boolean isExplicit) {
    return new CoverageCheckingFailedResult(match(isExplicit, "*"));
  }

  @Override
  public CoverageCheckingResult visitName(boolean isExplicit) {
    return new CoverageCheckingOKResult(match(isExplicit, null));
  }

}
