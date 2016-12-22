package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelMax;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PiTypeOmega extends PiUniverseType implements Type {
  public PiTypeOmega(DependentLink parameters) {
    super(parameters, new SortMax(LevelMax.INFINITY, LevelMax.INFINITY));
  }

  public PiTypeOmega() {
    this(EmptyDependentLink.getInstance());
  }

  public PiTypeOmega(DependentLink parameters, Level hlevel) {
    super(parameters, new SortMax(LevelMax.INFINITY, new LevelMax(hlevel)));
  }

  public static PiTypeOmega toPiTypeOmega(TypeMax type) {
    List<DependentLink> params = new ArrayList<>();
    type.getPiParameters(params, true, false);
    return new PiTypeOmega(DependentLink.Helper.mergeList(params, new ExprSubstitution()));
  }

  @Override
  public PiTypeOmega fromPiParameters(List<DependentLink> params) {
    return toPiTypeOmega(super.fromPiParameters(params));
  }

  @Override
  public PiTypeOmega getPiCodomain() {
    return toPiTypeOmega(super.getPiCodomain());
  }

  @Override
  public PiTypeOmega getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly) {
    return toPiTypeOmega(super.getPiParameters(params, normalize, implicitOnly));
  }

  @Override
  public PiTypeOmega subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    return toPiTypeOmega(super.subst(exprSubst, levelSubst));
  }

  @Override
  public PiTypeOmega applyExpressions(List<? extends Expression> expressions) {
    return toPiTypeOmega(super.applyExpressions(expressions));
  }

  @Override
  public PiTypeOmega addParameters(DependentLink params, boolean modify) {
    return toPiTypeOmega(super.addParameters(params, modify));
  }

  @Override
  public PiTypeOmega normalize(NormalizeVisitor.Mode mode) {
    return toPiTypeOmega(super.normalize(mode));
  }

  @Override
  public PiTypeOmega strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return toPiTypeOmega(super.strip(bounds, errorReporter));
  }

}
