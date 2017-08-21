package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProvider;

import java.util.Collection;
import java.util.Collections;

public class GlobalInstancePool implements ClassViewInstancePool {
  private final TypecheckerState myTypecheckerState;
  private final InstanceProvider myInstanceProvider;

  public GlobalInstancePool(TypecheckerState typecheckerState, InstanceProvider instanceProvider) {
    myTypecheckerState = typecheckerState;
    myInstanceProvider = instanceProvider;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, Concrete.ClassView classView, boolean isView) {
    DefCallExpression classifyingDefCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DefCallExpression.class);
    if (classifyingDefCall == null) {
      return null;
    }

    Collection<? extends Concrete.ClassViewInstance> instances = myInstanceProvider.getInstances(classView);
    for (Concrete.ClassViewInstance instance : instances) {
      if ((isView && instance.getClassView().getReferent() == classView ||
          !isView && ((Abstract.ClassView) instance.getClassView().getReferent()).getUnderlyingClassReference().getReferent() == classView.getUnderlyingClassReference().getReferent()) &&
        instance.getClassifyingDefinition() == classifyingDefCall.getDefinition().getConcreteDefinition()) {
        Definition definition = myTypecheckerState.getTypechecked(instance);
        if (definition.status().headerIsOK()) {
          return new FunCallExpression((FunctionDefinition) definition, Sort.PROP /* TODO[classes] */, Collections.emptyList());
        }
      }
    }
    return null;
  }
}
