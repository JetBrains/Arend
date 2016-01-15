package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;

import java.util.ArrayList;
import java.util.List;

public class Utils {
  public static int getNumArguments(Pattern pattern) {
    if (pattern instanceof NamePattern || pattern instanceof AnyConstructorPattern) {
      return 1;
    } else if (pattern instanceof ConstructorPattern) {
      return getNumArguments(((ConstructorPattern) pattern).getArguments());
    } else {
      throw new IllegalStateException();
    }
  }

  public static int getNumArguments(List<? extends PatternArgument> patternArgs) {
    int result = 0;
    for (PatternArgument patternArg : patternArgs)
      result += getNumArguments(patternArg.getPattern());
    return result;
  }

  public static class ProcessImplicitResult {
    public final List<Abstract.PatternArgument> patterns;
    public final int wrongImplicitPosition;
    public final int numExplicit;
    public final int numExcessive;

    public ProcessImplicitResult(List<Abstract.PatternArgument> patterns, int numExplicit) {
      this.patterns = patterns;
      this.wrongImplicitPosition = -1;
      this.numExplicit = numExplicit;
      this.numExcessive = 0;
    }

    public ProcessImplicitResult(int wrongImplicitPosition, int numExplicit) {
      this.patterns = null;
      this.wrongImplicitPosition = wrongImplicitPosition;
      this.numExplicit = numExplicit;
      this.numExcessive = 0;
    }

    public ProcessImplicitResult(int numExcessive) {
      this.patterns = null;
      this.numExcessive = numExcessive;
      this.numExplicit = -1;
      this.wrongImplicitPosition = -1;
    }
  }

  public static ProcessImplicitResult processImplicit(List<? extends Abstract.PatternArgument> patterns, DependentLink params) {
    int numExplicit = 0;
    for (DependentLink link = params; link != null; link = link.getNext()) {
      if (link.isExplicit()) {
        numExplicit++;
      }
    }

    List<Abstract.PatternArgument> result = new ArrayList<>();
    int indexI = 0;
    for (DependentLink link = params; link != null; link = link.getNext()) {
      Abstract.PatternArgument curPattern = indexI < patterns.size() ? patterns.get(indexI) : new PatternArgument(new NamePattern(link), false, true);
      if (curPattern.isExplicit() && !link.isExplicit()) {
        curPattern = new PatternArgument(new NamePattern(link), false, true);
      } else {
        indexI++;
      }
      if (curPattern.isExplicit() != link.isExplicit()) {
        return new ProcessImplicitResult(indexI, numExplicit);
      }
      result.add(curPattern);
    }
    if (indexI < patterns.size()) {
      return new ProcessImplicitResult(patterns.size() - indexI);
    }
    return new ProcessImplicitResult(result, numExplicit);
  }

  public static List<Pattern> toPatterns(List<PatternArgument> patternArgs)  {
    List<Pattern> result = new ArrayList<>(patternArgs.size());
    for (PatternArgument patternArg : patternArgs) {
      result.add(patternArg.getPattern());
    }
    return result;
  }
}
