package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.ClassViewInstanceProvider;

import java.util.Collection;
import java.util.Collections;

public class GlobalInstancePool implements ClassViewInstancePool {
  private final TypecheckerState myTypecheckerState;
  private final ClassViewInstanceProvider myInstanceProvider;

  public GlobalInstancePool(TypecheckerState typecheckerState, ClassViewInstanceProvider instanceProvider) {
    myTypecheckerState = typecheckerState;
    myInstanceProvider = instanceProvider;
  }

  private Expression findInstance(Abstract.DefCallExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.Definition classView) {
    DefCallExpression classifyingDefCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).toDefCall();
    if (classifyingDefCall == null) {
      return null;
    }

    Collection<? extends Abstract.ClassViewInstance> instances = myInstanceProvider.getInstances(defCall, paramIndex);
    for (Abstract.ClassViewInstance instance : instances) {
      if ((classView instanceof Abstract.ClassView && instance.getClassView().getReferent() == classView ||
           classView instanceof Abstract.ClassDefinition &&
             ((Abstract.ClassView) instance.getClassView().getReferent()).getUnderlyingClassDefCall().getReferent() == classView) &&
          instance.getClassifyingDefinition() == classifyingDefCall.getDefinition().getAbstractDefinition()) {
        return new FunCallExpression((FunctionDefinition) myTypecheckerState.getTypechecked(instance), new LevelArguments(), Collections.<Expression>emptyList());
      }
    }
    return null;
  }

  @Override
  public Expression getInstance(Abstract.DefCallExpression defCall, Expression classifyingExpression, Abstract.ClassView classView) {
    return findInstance(defCall, 0, classifyingExpression, classView);
  }

  @Override
  public Expression getInstance(Abstract.DefCallExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.ClassDefinition classDefinition) {
    return findInstance(defCall, paramIndex, classifyingExpression, classDefinition);
  }
}
