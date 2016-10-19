package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.type.PiTypeOmega;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.term.expr.visitor.*;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.normalization.EvalNormalizer;

import java.util.*;

public abstract class Expression implements PrettyPrintable, Type {
  public abstract <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params);

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory());
    visitor.addFlags(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS).addFlags(ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM);
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

  @Override
  public boolean isLessOrEquals(Sort sort) {
    UniverseExpression expr = normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
    return expr != null && expr.getSort().isLessOrEquals(sort);
  }

  @Override
  public boolean isLessOrEquals(Type type, Equations equations, Abstract.SourceNode sourceNode) {
    if (type instanceof PiTypeOmega) {
      List<DependentLink> params = new ArrayList<>();
      Expression cod = getPiParameters(params, true, false).toUniverse();
      return cod != null && (params.isEmpty() || CompareVisitor.compare(equations, params, DependentLink.Helper.toList(type.getPiParameters()), sourceNode));
    }
    Expression typeExpr = type.toExpression();
    assert typeExpr != null;
    typeExpr = typeExpr.normalize(NormalizeVisitor.Mode.NF);
    return CompareVisitor.compare(equations, Equations.CMP.LE, normalize(NormalizeVisitor.Mode.NF), typeExpr, sourceNode);
  }

  @Override
  public SortMax toSorts() {
    UniverseExpression universe = normalize(NormalizeVisitor.Mode.WHNF).toUniverse();
    return universe == null ? null : new SortMax(universe.getSort());
  }

  public TypeMax getType() {
    return accept(new GetTypeVisitor(), null);
  }

  @Override
  public boolean findBinding(Referable binding) {
    return this.<Void, Boolean>accept(new FindBindingVisitor(Collections.singleton(binding)), null);
  }

  public boolean findBinding(Set<? extends Referable> bindings) {
    return this.<Void, Boolean>accept(new FindBindingVisitor(bindings), null);
  }

  @Override
  public Expression strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return accept(new StripVisitor(bounds, errorReporter), null);
  }

  public final Expression subst(Binding binding, Expression substExpr) {
    return accept(new SubstVisitor(new ExprSubstitution(binding, substExpr), new LevelSubstitution()), null);
  }

  public final Expression subst(ExprSubstitution subst) {
     return subst(subst, new LevelSubstitution());
  }

  public final Expression subst(LevelSubstitution subst) {
    return subst(new ExprSubstitution(), subst);
  }

  @Override
  public final Expression subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    return exprSubst.getDomain().isEmpty() && levelSubst.getDomain().isEmpty() ? this : accept(new SubstVisitor(exprSubst, levelSubst), null);
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
  public Expression getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly) {
    Expression cod = normalize ? normalize(NormalizeVisitor.Mode.WHNF) : this;
    PiExpression piCod = cod.toPi();
    while (piCod != null) {
      if (implicitOnly) {
        if (piCod.getParameters().isExplicit()) {
          break;
        }
        for (DependentLink link = piCod.getParameters(); link.hasNext(); link = link.getNext()) {
          if (link.isExplicit()) {
            return new PiExpression(link, piCod.getCodomain());
          }
          if (params != null) {
            params.add(link);
          }
        }
      } else {
        if (params != null) {
          for (DependentLink link = piCod.getParameters(); link.hasNext(); link = link.getNext()) {
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

  @Override
  public Expression fromPiParameters(List<DependentLink> params) {
    params = DependentLink.Helper.fromList(params);
    Expression result = this;
    for (int i = params.size() - 1; i >= 0; i--) {
      result = new PiExpression(params.get(i), result);
    }
    return result;
  }

  @Override
  public Expression addParameters(DependentLink params, boolean modify) {
    return params.hasNext() ? new PiExpression(params, this) : this;
  }

  @Override
  public DependentLink getPiParameters() {
    PiExpression pi = normalize(NormalizeVisitor.Mode.WHNF).toPi();
    return pi == null ? EmptyDependentLink.getInstance() : pi.getParameters();
  }

  @Override
  public Type getPiCodomain() {
    PiExpression pi = normalize(NormalizeVisitor.Mode.WHNF).toPi();
    return pi == null ? this : pi.getCodomain();
  }

  @Override
  public Expression toExpression() {
    return this;
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
    List<DependentLink> params = new ArrayList<>();
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
      cod = cod.fromPiParameters(params.subList(expressions.size(), params.size()));
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

  public boolean isAnyUniverse() {
    return false;
  }
}
