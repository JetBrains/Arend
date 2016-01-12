package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LamExpression extends Expression {
  private final DependentLink myLink;
  private final Expression myBody;

  public LamExpression(DependentLink link, Expression body) {
    myLink = link;
    myBody = body;
  }

  public DependentLink getLink() {
    return myLink;
  }

  public Expression getBody() {
    return myBody;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitLam(this, params);
  }

  @Override
  public Expression getType(List<Binding> context) {
    Map<Binding, Expression> substs = new HashMap<>();
    DependentLink link = myLink.copy(substs);
    return new PiExpression(link, myBody.subst(substs).getType(context));
  }
}
