package org.arend.core.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FieldCallExpression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;

public class ReplaceBindingVisitor extends SubstVisitor {
  private final Binding myBinding;
  private final ClassCallExpression myBindingType;
  private boolean myOK = true;

  public ReplaceBindingVisitor(Binding binding, ClassCallExpression bindingType) {
    super(new ExprSubstitution(), LevelSubstitution.EMPTY);
    myBinding = binding;
    myBindingType = bindingType;
  }

  public boolean isOK() {
    return myOK;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (expr.getBinding() == myBinding) {
      myOK = false;
    }
    return super.visitReference(expr, params);
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    ReferenceExpression refExpr = expr.getArgument().checkedCast(ReferenceExpression.class);
    if (refExpr != null && refExpr.getBinding() == myBinding) {
      Expression impl = myBindingType.getImplementation(expr.getDefinition(), refExpr);
      if (impl != null) {
        return impl.accept(this, null);
      }
    }
    return super.visitFieldCall(expr, params);
  }
}
