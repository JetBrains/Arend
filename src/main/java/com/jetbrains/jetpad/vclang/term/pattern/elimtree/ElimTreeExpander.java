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

    protected Branch(T result) {
      this.result = result;
    }
  }

  public static class OKBranch<T> extends Branch<T> {
    public final Expression expression;
    public final List<Binding> context;
    public final List<Integer> good;

    OKBranch(Expression expression, List<Binding> context, List<Integer> good, T result) {
      super(result);
      this.expression = expression;
      this.context = context;
      this.good = good;
    }
  }

  public static class IncompleteBranch<T> extends Branch<T> {
    public final List<OKBranch<T>> maybeBranches;

    IncompleteBranch(List<OKBranch<T>> maybeBranches, T result) {
      super(result);
      this.maybeBranches = maybeBranches;
    }
  }

  public static class FailedBranch<T> extends Branch<T> {
    public final List<Integer> bad;

    FailedBranch(List<Integer> bad, T result) {
      super(result);
      this.bad = bad;
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
    List<Constructor> validConstructors = new ArrayList<>();
    List<List<Expression>> constructorMatchedParameters = new ArrayList<>();
    List<List<TypeArgument>> validConstructorArgs = new ArrayList<>();
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
          return Collections.<Branch<T>>singletonList(new FailedBranch<>(bad,
              visitor.visitFailed(isExplicit)));
        } else if (matchResult instanceof Utils.PatternMatchFailedResult) {
          continue;
        } else if (matchResult instanceof Utils.PatternMatchOKResult) {
          matchedParameters = ((Utils.PatternMatchOKResult) matchResult).expressions;
        }
      } else {
        matchedParameters = parameters;
      }
      constructorMatchedParameters.add(matchedParameters);
      validConstructors.add(constructor);
      validConstructorArgs.add(new ArrayList<TypeArgument>());
      splitArguments(constructor.getType().subst(matchedParameters, 0), validConstructorArgs.get(validConstructorArgs.size() - 1), myLocalContext);
    }

    List<Branch<T>> result = new ArrayList<>();
    for (int i = 0; i < validConstructors.size(); i++) {
      List<Integer> goodPatternIdxs = new ArrayList<>();
      List<List<Pattern>> goodPatternNested = new ArrayList<>();
      for (int j = 0; j < validConstructorArgs.get(i).size(); j++) {
        goodPatternNested.add(new ArrayList<Pattern>());
      }

      for (int j = 0; j < patterns.size(); j++) {
        if (patterns.get(j) instanceof NamePattern || patterns.get(j) instanceof AnyConstructorPattern) {
          goodPatternIdxs.add(j);
          for (int k = 0; k < goodPatternNested.size(); k++) {
            goodPatternNested.get(k).add(match(validConstructorArgs.get(i).get(k).getExplicit(), null));
          }
        } else if (patterns.get(j) instanceof ConstructorPattern
            && ((ConstructorPattern) patterns.get(j)).getConstructor() == validConstructors.get(i)) {
          goodPatternIdxs.add(j);
          for (int k = 0; k < goodPatternNested.size(); k++) {
            goodPatternNested.get(k).add(((ConstructorPattern) patterns.get(j)).getPatterns().get(k));
          }
        }
      }

      if (goodPatternIdxs.isEmpty()) {
        List<Binding> ctx = new ArrayList<>();
        Expression expr = ConCall(validConstructors.get(i), constructorMatchedParameters.get(i));
        for (int j = 0; j < validConstructorArgs.get(i).size(); j++) {
          ctx.add(new TypedBinding((String) null, validConstructorArgs.get(i).get(j).getType()));
          expr = Apps(expr.liftIndex(0, 1), Index(0));
        }
        result.add(new IncompleteBranch<>(Collections.singletonList(
            new OKBranch<T>(expr, ctx, null, null)), visitor.visitIncomplete(isExplicit, validConstructors.get(i))));
        continue;
      }

      List<ArgsBranch<T>> nestedBranches = new ArgsElimTreeExpander<T>(myLocalContext).expandElimTree(visitor, getTypes(validConstructorArgs.get(i)), goodPatternNested, goodPatternIdxs.size());
      for (ArgsBranch<T> branch : nestedBranches) {
        if (branch instanceof ArgsIncompleteBranch) {
          List<OKBranch<T>> maybeBranches = new ArrayList<>();
          for (ArgsOKBranch<T> maybeBranch : ((ArgsIncompleteBranch<T>) branch).maybeBranches) {
            Expression expr = ConCall(validConstructors.get(i), constructorMatchedParameters.get(i)).liftIndex(0, maybeBranch.context.size());
            for (Expression subExpr : maybeBranch.expressions) {
              expr = Apps(expr, subExpr);
            }
            maybeBranches.add(new OKBranch<>(expr, maybeBranch.context, null,
                visitor.visitElimIncomplete(isExplicit, validConstructors.get(i), maybeBranch.results)));
          }
          result.add(new IncompleteBranch<>(maybeBranches, visitor.visitElimIncomplete(isExplicit, validConstructors.get(i), ((ArgsIncompleteBranch<T>) branch).results)));
        } else if (branch instanceof ArgsFailedBranch) {
          List<Integer> bad = new ArrayList<>();
          for (int j : ((ArgsFailedBranch<T>) branch).bad)
            bad.add(j);
          result.add(new FailedBranch<>(bad, visitor.visitFailed(isExplicit, validConstructors.get(i), ((ArgsFailedBranch<T>) branch).results)));
        } else if (branch instanceof ArgsOKBranch){
          ArgsOKBranch<T> okBranch = (ArgsOKBranch<T>) branch;
          Expression expr = ConCall(validConstructors.get(i), constructorMatchedParameters.get(i)).liftIndex(0, okBranch.context.size());
          for (Expression subExpr : okBranch.expressions) {
            expr = Apps(expr, subExpr);
          }
          List<Integer> good = new ArrayList<>();
          for (int j : okBranch.good)
            good.add(goodPatternIdxs.get(j));

          result.add(new OKBranch<>(expr, okBranch.context,
              good, visitor.visitElimOK(isExplicit, validConstructors.get(i), okBranch.results)));
        }
      }
    }

    return result;
  }
}
