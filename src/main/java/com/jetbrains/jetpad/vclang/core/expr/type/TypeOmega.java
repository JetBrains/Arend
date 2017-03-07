package com.jetbrains.jetpad.vclang.core.expr.type;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;

import java.util.List;

public class TypeOmega implements Type {
  public final static TypeOmega INSTANCE = new TypeOmega();

  private TypeOmega() {}

  public Type normalize(NormalizeVisitor.Mode mode) {
    return this;
  }

  @Override
  public Type getPiParameters(List<DependentLink> params, boolean normalize, boolean implicitOnly) {
    return this;
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec, int indent) {
    ConcreteExpressionFactory.cUniverse(ConcreteExpressionFactory.cInf(), ConcreteExpressionFactory.cInf()).accept(new PrettyPrintVisitor(builder, indent), prec);
  }
}
