package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ArgsElimTreeExpander.ArgsBranch;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ArgsElimTreeExpander.ArgsFailedBranch;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ArgsElimTreeExpander.ArgsIncompleteBranch;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ArgsElimTreeExpander.ArgsOKBranch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.getTypes;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.patternMatchAll;

public class ElimTreeExpander {
  private final List<Binding> myLocalContext;

  public ElimTreeExpander(List<Binding> localContext) {
    this.myLocalContext = localContext;
  }

  public static abstract class Branch<T> {
    public final T result;
    public final List<Integer> indices;

    protected Branch(T result, List<Integer> indices) {
      this.result = result;
      this.indices = indices;
    }
  }

  public static class OKBranch<T> extends Branch<T> {
    public final Expression expression;
    public final List<Binding> context;

    OKBranch(Expression expression, List<Binding> context, List<Integer> indicies, T result) {
      super(result, indicies);
      this.expression = expression;
      this.context = context;
    }
  }

  public static class IncompleteBranch<T> extends Branch<T> {
    public final List<OKBranch<T>> maybeBranches;

    IncompleteBranch(List<OKBranch<T>> maybeBranches, T result) {
      super(result, Collections.<Integer>emptyList());
      this.maybeBranches = maybeBranches;
    }
  }

  public static class FailedBranch<T> extends Branch<T> {
    FailedBranch(List<Integer> indicies, T result) {
      super(result, indicies);
    }
  }

  private static class ConstructorInfo {
    private final Constructor constructor;
    private final List<TypeArgument> arguments;
    private final List<Expression> parameters;

    private ConstructorInfo(Constructor constructor, List<TypeArgument> arguments, List<Expression> parameters) {
      this.constructor = constructor;
      this.arguments = arguments;
      this.parameters = parameters;
    }
  }

  public <T> List<Branch<T>> expandElimTree(ElimTreeVisitor<T> visitor, List<Pattern> patterns, Expression type, boolean isExplicit) {
    List<Integer> namePatternIdxs = new ArrayList<>();
    boolean hasConstructorPattern = false;
    for (int i = 0; i < patterns.size(); i++) {
      if (patterns.get(i) instanceof ConstructorPattern) {
        hasConstructorPattern = true;
      } else if (patterns.get(i) instanceof NamePattern) {
        namePatternIdxs.add(i);
      }
    }

    if (!hasConstructorPattern && !patterns.isEmpty()) {
      return Collections.<Branch<T>>singletonList(new OKBranch<>(Index(0),
          Collections.<Binding>singletonList(new TypedBinding((String) null, type)), namePatternIdxs, visitor.visitName(isExplicit)));
    }

    List<Expression> parameters = new ArrayList<>();
    Expression ftype = type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext).getFunction(parameters);
    Collections.reverse(parameters);
    if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
      return Collections.<Branch<T>>singletonList(new IncompleteBranch<>(Collections.singletonList(
          new OKBranch<T>(Index(0), Collections.<Binding>singletonList(new TypedBinding((String) null, type)), null, null)),
          visitor.visitIncomplete(isExplicit, null)));
    }

    DataDefinition dataType = (DataDefinition) ((DefCallExpression) ftype).getDefinition();
    List<ConstructorInfo> validConstructors = new ArrayList<>();
    for (Constructor constructor : dataType.getConstructors()) {
      if (constructor.hasErrors())
        continue;
      List<Expression> matchedParameters = null;
      if (constructor.getPatterns() != null) {
        Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), parameters, myLocalContext);
        if (matchResult instanceof Utils.PatternMatchMaybeResult) {
          if (patterns.isEmpty()) {
            return Collections.<Branch<T>>singletonList(new IncompleteBranch<>(Collections.singletonList(
                new OKBranch<T>(Index(0), Collections.<Binding>singletonList(new TypedBinding((String) null, type)), null, null)),
                visitor.visitIncomplete(isExplicit, null)));
          }
          List<Integer> bad = new ArrayList<>();
          for (int i = 0; i < patterns.size(); i++) {
            if (patterns.get(i) instanceof ConstructorPattern)
              bad.add(i);
          }
          return Collections.<Branch<T>>singletonList(new FailedBranch<>(bad, visitor.visitFailed(isExplicit)));
        } else if (matchResult instanceof Utils.PatternMatchFailedResult) {
          continue;
        } else if (matchResult instanceof Utils.PatternMatchOKResult) {
          matchedParameters = ((Utils.PatternMatchOKResult) matchResult).expressions;
        }
      } else {
        matchedParameters = parameters;
      }

      ArrayList<TypeArgument> arguments = new ArrayList<>();
      validConstructors.add(new ConstructorInfo(constructor, arguments, matchedParameters));
      splitArguments(constructor.getType().subst(matchedParameters, 0), arguments, myLocalContext);
    }

    List<Branch<T>> result = new ArrayList<>();
    for (ConstructorInfo info : validConstructors) {
      MatchingPatterns matching = new MatchingPatterns(patterns, info.constructor, info.arguments);

      if (matching.indices.isEmpty()) {
        List<Binding> ctx = new ArrayList<>();
        Expression expr = ConCall(info.constructor, info.parameters);
        for (TypeArgument arg : info.arguments) {
          ctx.add(new TypedBinding((String) null, arg.getType()));
          expr = Apps(expr.liftIndex(0, 1), Index(0));
        }
        result.add(new IncompleteBranch<>(Collections.singletonList(new OKBranch<T>(expr, ctx, null, null)), visitor.visitIncomplete(isExplicit, info.constructor)));
        continue;
      }

      List<ArgsBranch<T>> nestedBranches = new ArgsElimTreeExpander<T>(myLocalContext).expandElimTree(visitor, getTypes(info.arguments), matching.nestedPatterns, matching.indices.size());
      for (ArgsBranch<T> branch : nestedBranches) {
        if (branch instanceof ArgsIncompleteBranch) {
          List<OKBranch<T>> maybeBranches = new ArrayList<>();
          for (ArgsOKBranch<T> maybeBranch : ((ArgsIncompleteBranch<T>) branch).maybeBranches) {
            Expression expr = ConCall(info.constructor, info.parameters).liftIndex(0, maybeBranch.context.size());
            expr = Apps(expr, maybeBranch.expressions.toArray(new Expression[maybeBranch.expressions.size()]));
            maybeBranches.add(new OKBranch<T>(expr, maybeBranch.context, null, null));
          }
          result.add(new IncompleteBranch<>(maybeBranches, visitor.visitElimIncomplete(isExplicit, info.constructor, branch.results)));
        } else if (branch instanceof ArgsFailedBranch) {
          result.add(new FailedBranch<>(recalcIndicies(matching.indices, branch.indices), visitor.visitElimFailed(isExplicit, info.constructor, branch.results)));
        } else if (branch instanceof ArgsOKBranch){
          ArgsOKBranch<T> okBranch = (ArgsOKBranch<T>) branch;
          Expression expr = ConCall(info.constructor, info.parameters).liftIndex(0, okBranch.context.size());
          expr = Apps(expr, okBranch.expressions.toArray(new Expression[okBranch.expressions.size()]));
          result.add(new OKBranch<>(expr, okBranch.context, recalcIndicies(matching.indices, okBranch.indices), visitor.visitElimOK(isExplicit, info.constructor, branch.results)));
        }
      }
    }

    return result;
  }

  private static class MatchingPatterns {
    private final List<Integer> indices = new ArrayList<>();
    private final List<List<Pattern>> nestedPatterns = new ArrayList<>();

    private MatchingPatterns(List<Pattern> patterns, Constructor constructor, List<TypeArgument> constructorArgs) {
      for (int j = 0; j < constructorArgs.size(); j++) {
        nestedPatterns.add(new ArrayList<Pattern>());
      }

      for (int j = 0; j < patterns.size(); j++) {
        if (patterns.get(j) instanceof NamePattern || patterns.get(j) instanceof AnyConstructorPattern) {
          indices.add(j);
          for (int k = 0; k < nestedPatterns.size(); k++) {
            nestedPatterns.get(k).add(match(constructorArgs.get(k).getExplicit(), null));
          }
        } else if (patterns.get(j) instanceof ConstructorPattern &&
            ((ConstructorPattern) patterns.get(j)).getConstructor() == constructor) {
          indices.add(j);
          for (int k = 0; k < nestedPatterns.size(); k++) {
            nestedPatterns.get(k).add(((ConstructorPattern) patterns.get(j)).getPatterns().get(k));
          }
        }
      }
    }
  }

  public static ArrayList<Integer> recalcIndicies(List<Integer> valid, List<Integer> newValid) {
    ArrayList<Integer> indicies = new ArrayList<>();
    for (int i : newValid)
      indicies.add(valid.get(i));
    return indicies;
  }
}
