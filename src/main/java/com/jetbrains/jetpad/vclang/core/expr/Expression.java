package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.*;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.normalization.EvalNormalizer;

import java.util.*;

public abstract class Expression implements Type {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory());
    visitor
      .addFlags(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS)
      .addFlags(ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM)
      .addFlags(ToAbstractVisitor.Flag.SHOW_CON_PARAMS);
    accept(visitor, null).accept(new PrettyPrintVisitor(builder, 0), Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Expression && compare(this, (Expression) obj);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory(), names);
    visitor.addFlags(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS).addFlags(ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM);
    accept(visitor, null).accept(new PrettyPrintVisitor(builder, indent), prec);
  }

  public boolean isLessOrEquals(Type type, Equations equations, Abstract.SourceNode sourceNode) {
    if (type instanceof Expression) {
      return CompareVisitor.compare(equations, Equations.CMP.LE, normalize(NormalizeVisitor.Mode.NF), ((Expression) type).normalize(NormalizeVisitor.Mode.NF), sourceNode);
    } else {
      // TODO: if this expression is stuck, add an equation
      return normalize(NormalizeVisitor.Mode.WHNF).toUniverse() != null;
    }
  }

  public Sort toSort() {
    UniverseExpression universe = normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
    return universe == null ? null : universe.getSort();
  }

  public Expression getType() {
    return accept(new GetTypeVisitor(), null);
  }

  public boolean findBinding(Variable binding) {
    return accept(new FindBindingVisitor(Collections.singleton(binding)), null) != null;
  }

  public Variable findBinding(Set<? extends Variable> bindings) {
    return this.accept(new FindBindingVisitor(bindings), null);
  }

  public Expression strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return accept(new StripVisitor(bounds, errorReporter), null);
  }

  public Expression copy() {
    return accept(new SubstVisitor(new ExprSubstitution(), LevelSubstitution.EMPTY), null);
  }

  public final Expression subst(Binding binding, Expression substExpr) {
    return accept(new SubstVisitor(new ExprSubstitution(binding, substExpr), LevelSubstitution.EMPTY), null);
  }

  public final Expression subst(ExprSubstitution subst) {
     return subst.isEmpty() ? this : subst(subst, LevelSubstitution.EMPTY);
  }

  public final Expression subst(LevelSubstitution subst) {
    return subst.isEmpty() ? this : subst(new ExprSubstitution(), subst);
  }

  public final Expression subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    return exprSubst.isEmpty() && levelSubst.isEmpty() ? this : accept(new SubstVisitor(exprSubst, levelSubst), null);
  }

  @Override
  public final Expression normalize(NormalizeVisitor.Mode mode) {
    return accept(new NormalizeVisitor(new EvalNormalizer()), mode);
  }

  public static boolean compare(Expression expr1, Expression expr2, Equations.CMP cmp) {
    return CompareVisitor.compare(DummyEquations.getInstance(), cmp, expr1, expr2, null);
  }

  public static boolean compare(Expression expr1, Expression expr2) {
    return compare(expr1, expr2, Equations.CMP.EQ);
  }

  @Override
  public Expression getPiParameters(List<SingleDependentLink> params, boolean normalize, boolean implicitOnly) {
    Expression cod = normalize ? normalize(NormalizeVisitor.Mode.WHNF) : this;
    PiExpression piCod = cod.toPi();
    while (piCod != null) {
      if (implicitOnly) {
        if (piCod.getParameters().isExplicit()) {
          break;
        }
        for (SingleDependentLink link = piCod.getParameters(); link.hasNext(); link = link.getNext()) {
          if (link.isExplicit()) {
            return null;
          }
          if (params != null) {
            params.add(link);
          }
        }
      } else {
        if (params != null) {
          for (SingleDependentLink link = piCod.getParameters(); link.hasNext(); link = link.getNext()) {
            params.add(link);
          }
        }
      }

      cod = piCod.getCodomain();
      if (normalize) {
        cod = cod.normalize(NormalizeVisitor.Mode.WHNF);
      }
      piCod = cod.toPi();
    }
    return cod;
  }

  public Expression fromPiParametersSingle(List<SingleDependentLink> params) {
    params = DependentLink.Helper.fromList(params);
    Expression result = this;
    for (int i = params.size() - 1; i >= 0; i--) {
      result = new PiExpression(params.get(i), result);
    }
    return result;
  }

  public Expression fromPiParameters(List<DependentLink> params) {
    List<SingleDependentLink> parameters = new ArrayList<>();
    ExprSubstitution substitution = new ExprSubstitution();
    List<String> names = new ArrayList<>();
    DependentLink link0 = null;
    for (DependentLink link : params) {
      if (link0 == null) {
        link0 = link;
      }

      names.add(link.getName());
      if (link instanceof TypedDependentLink) {
        SingleDependentLink parameter = ExpressionFactory.singleParam(link.isExplicit(), names, link.getType().subst(substitution, LevelSubstitution.EMPTY));
        parameters.add(parameter);
        names.clear();

        for (; parameter.hasNext(); parameter = parameter.getNext(), link0 = link0.getNext()) {
          substitution.add(link0, ExpressionFactory.Reference(parameter));
        }

        link0 = null;
      }
    }

    Expression type = subst(substitution, LevelSubstitution.EMPTY);
    for (int i = parameters.size() - 1; i >= 0; i--) {
      type = ExpressionFactory.Pi(parameters.get(i), type);
    }
    return type;
  }

  public Expression getLamParameters(List<DependentLink> params) {
    Expression body = this;
    LamExpression lamBody = body.toLam();
    while (lamBody != null) {
      if (params != null) {
        for (DependentLink link = lamBody.getParameters(); link.hasNext(); link = link.getNext()) {
          params.add(link);
        }
      }
      body = lamBody.getBody();
      lamBody = body.toLam();
    }
    return body;
  }

  public Expression applyExpressions(List<? extends Expression> expressions) {
    if (expressions.isEmpty()) {
      return this;
    }

    ExprSubstitution subst = new ExprSubstitution();
    List<SingleDependentLink> params = new ArrayList<>();
    Expression cod = getPiParameters(params, true, false);
    if (params.isEmpty()) {
      assert false;
      return null;
    }
    int size = expressions.size() > params.size() ? params.size() : expressions.size();
    for (int i = 0; i < size; i++) {
      subst.add(params.get(i), expressions.get(i));
    }

    if (expressions.size() < params.size()) {
      cod = cod.fromPiParametersSingle(params.subList(expressions.size(), params.size()));
    }
    return cod.subst(subst).applyExpressions(expressions.subList(size, expressions.size()));
  }

  public Expression getFunction() {
    return this;
  }

  public List<? extends Expression> getArguments() {
    return Collections.emptyList();
  }

  public Expression addArgument(Expression argument) {
    return new AppExpression(this, new ArrayList<>(Collections.singletonList(argument)));
  }

  public Expression addArguments(Collection<? extends Expression> arguments) {
    Expression result = this;
    for (Expression argument : arguments) {
      result = result.addArgument(argument);
    }
    return result;
//    return arguments.isEmpty() ? this : new AppExpression(this, arguments);
  }

  public AppExpression toApp() {
    return null;
  }

  public ClassCallExpression toClassCall() {
    return null;
  }

  public ConCallExpression toConCall() {
    return null;
  }

  public DataCallExpression toDataCall() {
    return null;
  }

  public DefCallExpression toDefCall() {
    return null;
  }

  public LetClauseCallExpression toLetClauseCall() {
    return null;
  }

  public ErrorExpression toError() {
    return null;
  }

  public FieldCallExpression toFieldCall() {
    return null;
  }

  public FunCallExpression toFunCall() {
    return null;
  }

  public LamExpression toLam() {
    return null;
  }

  public LetExpression toLet() {
    return null;
  }

  public NewExpression toNew() {
    return null;
  }

  public OfTypeExpression toOfType() {
    return null;
  }

  public PiExpression toPi() {
    return null;
  }

  public ProjExpression toProj() {
    return null;
  }

  public ReferenceExpression toReference() {
    return null;
  }

  public InferenceReferenceExpression toInferenceReference() {
    return null;
  }

  public SigmaExpression toSigma() {
    return null;
  }

  public TupleExpression toTuple() {
    return null;
  }

  public UniverseExpression toUniverse() {
    return null;
  }

  public Expression getStuckExpression() {
    return null;
  }
}
