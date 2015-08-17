package com.jetbrains.jetpad.vclang.term.pattern;


import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {
  public static void collectPatternNames(Abstract.Pattern pattern, List<String> names) {
    if (pattern instanceof Abstract.NamePattern) {
      if (((Abstract.NamePattern) pattern).getName() != null)
        names.add(((Abstract.NamePattern) pattern).getName());
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      for (Abstract.Pattern nestedPattern : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        collectPatternNames(nestedPattern, names);
      }
    }
  }

  public static void prettyPrintPattern(Abstract.Pattern pattern, StringBuilder builder, List<String> names) {
    // TODO: names!
    if (!pattern.getExplicit())
      builder.append('{');
    if (pattern instanceof Abstract.NamePattern) {
      if (((Abstract.NamePattern) pattern).getName() == null) {
        builder.append('_');
      } else {
        builder.append(((Abstract.NamePattern) pattern).getName());
      }
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      builder.append('(');
      builder.append(((Abstract.ConstructorPattern) pattern).getConstructorName());
      builder.append(' ');
      for (Abstract.Pattern p : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        prettyPrintPattern(p, builder, names);
      }
      builder.append(')');
    }
    if (!pattern.getExplicit())
      builder.append('}');
  }

  public static List<Expression> patternMatch(Pattern pattern, Expression expr, List<Binding> context) {
    if (pattern instanceof NamePattern) {
      return Collections.singletonList(expr);
    } else if (pattern instanceof ConstructorPattern) {
      ConstructorPattern constructorPattern = (ConstructorPattern) pattern;
      List<Expression> constructorArgs = new ArrayList<>();
      expr = expr.normalize(NormalizeVisitor.Mode.WHNF, context).getFunction(constructorArgs);
      assert expr instanceof DefCallExpression && ((DefCallExpression) expr).getDefinition() instanceof Constructor;
      Constructor constructor = (Constructor) ((DefCallExpression) expr).getDefinition();
      assert constructor == constructorPattern.getConstructor();
      assert constructorArgs.size() == constructorPattern.getArguments().size();
      List<Expression> result = new ArrayList<>();
      for (int i  = 0; i < constructorArgs.size(); i++) {
        result.addAll(patternMatch(constructorPattern.getArguments().get(i), constructorArgs.get(i), context));
      }
      return result;
    } else {
      throw new IllegalStateException();
    }
  }

  public static List<Abstract.Pattern> processImplicit(List<? extends Abstract.Pattern> patterns, List<? extends Abstract.TypeArgument> arguments) {
    ArrayList<Boolean> argIsExplicit = new ArrayList<>();
    for (Abstract.TypeArgument arg : arguments) {
      if (arg instanceof Abstract.TelescopeArgument) {
        argIsExplicit.addAll(Collections.nCopies(((Abstract.TelescopeArgument) arg).getNames().size(), arg.getExplicit()));
      } else {
        argIsExplicit.add(arg.getExplicit());
      }
    }
    List<Abstract.Pattern> result = new ArrayList<>();
    for (int indexI = 0, indexJ = 0; indexJ < argIsExplicit.size(); ++indexJ) {
      Abstract.Pattern curPattern = indexI < patterns.size() ? patterns.get(indexI) : new NamePattern(null, false);
      if (curPattern.getExplicit() && !argIsExplicit.get(indexJ)) {
        curPattern = new NamePattern(null, false);
      } else {
        indexI++;
      }
      assert curPattern.getExplicit() == argIsExplicit.get(indexJ);
      result.add(curPattern);
    }
    return result;
  }

  public static List<Expression> patternMatchAll(List<Pattern> patterns, List<Expression> exprs, List<Binding> context) {
    List<Expression> result = new ArrayList<>();
    assert patterns.size() == exprs.size();
    for (int i = 0; i < patterns.size(); i++) {
      result.addAll(patternMatch(patterns.get(i), exprs.get(i), context));
    }
    return result;
  }
}
