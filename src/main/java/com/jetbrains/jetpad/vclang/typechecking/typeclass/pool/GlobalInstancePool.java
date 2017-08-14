package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
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

  private static Abstract.ClassView getClassViewFromDefCall(Abstract.Definition definition, int paramIndex) {
    Collection<? extends Abstract.Parameter> parameters = Abstract.getParameters(definition);
    if (parameters == null) {
      return null;
    }

    int i = 0;
    for (Abstract.Parameter parameter : parameters) {
      if (parameter instanceof Abstract.NameParameter) {
        i++;
      } else
      if (parameter instanceof Abstract.TypeParameter) {
        if (parameter instanceof Abstract.TelescopeParameter) {
          i += ((Abstract.TelescopeParameter) parameter).getReferableList().size();
        } else {
          i++;
        }
        if (i > paramIndex) {
          return Abstract.getUnderlyingClassView(((Abstract.TypeParameter) parameter).getType());
        }
      } else {
        throw new IllegalStateException();
      }
    }
    return null;
  }

  private Expression findInstance(Abstract.ReferenceExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.Definition classView) {
    DefCallExpression classifyingDefCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DefCallExpression.class);
    if (classifyingDefCall == null) {
      return null;
    }

    Abstract.Definition def = (Abstract.Definition) defCall.getReferent();
    Abstract.ClassView classView1 = def instanceof Abstract.ClassViewField ? ((Abstract.ClassViewField) def).getOwnView() : getClassViewFromDefCall(def, paramIndex);
    if (classView1 != null) {
      Collection<? extends Abstract.ClassViewInstance> instances = myInstanceProvider.getInstances(classView1);
      for (Abstract.ClassViewInstance instance : instances) {
        if ((classView instanceof Abstract.ClassView && instance.getClassView().getReferent() == classView ||
          classView instanceof Abstract.ClassDefinition &&
            ((Abstract.ClassView) instance.getClassView().getReferent()).getUnderlyingClassReference().getReferent() == classView) &&
          instance.getClassifyingDefinition() == classifyingDefCall.getDefinition().getAbstractDefinition()) {
          Definition definition = myTypecheckerState.getTypechecked(instance);
          if (definition.status().headerIsOK()) {
            return new FunCallExpression((FunctionDefinition) definition, Sort.PROP /* TODO[classes] */, Collections.emptyList());
          }
        }
      }
    }
    return null;
  }

  @Override
  public Expression getInstance(Abstract.ReferenceExpression defCall, Expression classifyingExpression, Abstract.ClassView classView) {
    return findInstance(defCall, 0, classifyingExpression, classView);
  }

  @Override
  public Expression getInstance(Abstract.ReferenceExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.ClassDefinition classDefinition) {
    return findInstance(defCall, paramIndex, classifyingExpression, classDefinition);
  }
}
