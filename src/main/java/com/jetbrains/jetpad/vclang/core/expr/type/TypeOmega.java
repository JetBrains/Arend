package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Referable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.List;
import java.util.Set;

public class TypeOmega implements Type {
  private final static TypeOmega INSTANCE = new TypeOmega();

  public static TypeOmega getInstance() {
    return INSTANCE;
  }

  private TypeOmega() {}

  @Override
  public Type subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    return this;
  }

  @Override
  public Type applyExpressions(List<? extends Expression> expressions) {
    return this;
  }

  @Override
  public boolean isLessOrEquals(Sort sort) {
    return false;
  }

  @Override
  public boolean isLessOrEquals(TypeMax type, Equations equations, Abstract.SourceNode sourceNode) {
    return false;
  }

  @Override
  public SortMax toSorts() {
    return SortMax.OMEGA;
  }

  @Override
  public Type getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly) {
    return this;
  }

  @Override
  public Type fromPiParameters(List<DependentLink> params) {
    return this;
  }

  @Override
  public Type addParameters(DependentLink params, boolean modify) {
    return this;
  }

  @Override
  public DependentLink getPiParameters() {
    return EmptyDependentLink.getInstance();
  }

  @Override
  public Type getPiCodomain() {
    return this;
  }

  @Override
  public Type normalize(NormalizeVisitor.Mode mode) {
    return this;
  }

  @Override
  public Type strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return this;
  }

  @Override
  public Expression toExpression() {
    return null;
  }

  @Override
  public TypeMax getType() {
    throw new IllegalStateException();
  }

  @Override
  public boolean findBinding(Referable binding) {
    return false;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    ConcreteExpressionFactory.cUniverse(null, null).accept(new PrettyPrintVisitor(builder, indent), prec);
  }
}
