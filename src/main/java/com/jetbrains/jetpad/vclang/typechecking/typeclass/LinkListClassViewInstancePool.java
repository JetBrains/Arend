package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;

public class LinkListClassViewInstancePool implements ClassViewInstancePool {
  private final LinkList myLinkList;
  private DependentLink myLastLink;
  private final SimpleClassViewInstancePool myPool;

  public LinkListClassViewInstancePool(LinkList linkList) {
    myLinkList = linkList;
    myLastLink = linkList.getLast();
    myPool = new SimpleClassViewInstancePool();
  }

  @Override
  public Expression getLocalInstance(Expression classifyingExpression) {
    update();
    return myPool.getLocalInstance(classifyingExpression);
  }

  private void update() {
    if (myLinkList.getLast() == myLastLink) {
      return;
    }
    for (DependentLink link = myLastLink; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      ClassCallExpression type = link.getType().normalize(NormalizeVisitor.Mode.WHNF).toClassCall();
      if (type != null && type.getClassView().getClassifyingField() != null) {
        ReferenceExpression reference = new ReferenceExpression(link);
        myPool.addLocalInstance(Apps(new FieldCallExpression(type.getClassView().getClassifyingField()), reference).normalize(NormalizeVisitor.Mode.NF), reference);
      }
      if (link == myLinkList.getLast()) {
        break;
      }
    }
    myLastLink = myLinkList.getLast();
  }
}
