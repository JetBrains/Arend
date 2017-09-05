package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProvider;

public class GlobalInstancePool implements InstancePool {
  private final TypecheckerState myTypecheckerState;
  private final InstanceProvider myInstanceProvider;

  public GlobalInstancePool(TypecheckerState typecheckerState, InstanceProvider instanceProvider) {
    myTypecheckerState = typecheckerState;
    myInstanceProvider = instanceProvider;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, Concrete.ClassView classView, boolean isView) {
    /* TODO[abstract]
    DefCallExpression classifyingDefCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DefCallExpression.class);
    if (classifyingDefCall == null) {
      return null;
    }

    Collection<? extends Concrete.Instance> instances = myInstanceProvider.getInstances(classView);
    for (Concrete.Instance instance : instances) {
      if ((isView && instance.getClassView().getReferent() == classView ||
          !isView && ((Concrete.ClassView) instance.getClassView().getReferent()).getUnderlyingClass().getReferent() == classView.getUnderlyingClass().getReferent()) &&
        instance.getClassifyingDefinition() == classifyingDefCall.getDefinition().getReferable()) {
        Definition definition = myTypecheckerState.getTypechecked(instance.getReferable());
        if (definition.status().headerIsOK()) {
          return new FunCallExpression((FunctionDefinition) definition, Sort.PROP TODO[classes], Collections.emptyList());
        }
      }
    }
    */
    return null;
  }
}
