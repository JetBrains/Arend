package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.instance.provider.InstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.Collections;
import java.util.List;

public class GlobalInstancePool implements InstancePool {
  private final TypecheckerState myTypecheckerState;
  private final InstanceProvider myInstanceProvider;
  private final CheckTypeVisitor myCheckTypeVisitor;
  private InstancePool myInstancePool;

  public GlobalInstancePool(TypecheckerState typecheckerState, InstanceProvider instanceProvider, CheckTypeVisitor checkTypeVisitor) {
    myTypecheckerState = typecheckerState;
    myInstanceProvider = instanceProvider;
    myCheckTypeVisitor = checkTypeVisitor;
  }

  public void setInstancePool(InstancePool instancePool) {
    myInstancePool = instancePool;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, boolean isField, Equations equations, Concrete.SourceNode sourceNode) {
    if (myInstancePool != null) {
      Expression result = myInstancePool.getInstance(classifyingExpression, classRef, isField, equations, sourceNode);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider == null) {
      return null;
    }

    TCClassReferable typecheckable = classRef.getUnderlyingTypecheckable();
    DefCallExpression classifyingDefCall = null;
    ClassField classifyingField = null;
    if (classifyingExpression != null) {
      classifyingDefCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DefCallExpression.class);
      if (classifyingDefCall == null) {
        return null;
      }

      classifyingField = ((ClassDefinition) myTypecheckerState.getTypechecked(typecheckable)).getClassifyingField();
      if (classifyingField == null) {
        return null;
      }
    }

    List<? extends Concrete.Instance> instances = myInstanceProvider.getInstances(typecheckable);
    for (int i = instances.size() - 1; i >= 0; i--) {
      Concrete.Instance instance = instances.get(i);
      Referable instanceRef = instance.getReferenceInType();
      if (instanceRef instanceof ClassReferable && (isField ? instanceRef == classRef : ((ClassReferable) instanceRef).getUnderlyingTypecheckable() == typecheckable)) {
        FunctionDefinition instanceDef = (FunctionDefinition) myTypecheckerState.getTypechecked(instance.getData());
        if (instanceDef != null && instanceDef.status().headerIsOK() && instanceDef.getResultType() instanceof ClassCallExpression) {
          ClassCallExpression instanceResultType = (ClassCallExpression) instanceDef.getResultType();
          Expression instanceClassifyingExpr = classifyingDefCall != null || instanceDef.getParameters().hasNext() ? instanceResultType.getImplementationHere(classifyingField) : null;
          if (classifyingDefCall != null) {
            if (!(instanceClassifyingExpr instanceof DefCallExpression && ((DefCallExpression) instanceClassifyingExpr).getDefinition() == classifyingDefCall.getDefinition())) {
              continue;
            }
          }

          Concrete.Expression instanceExpr = new Concrete.ReferenceExpression(sourceNode.getData(), instance.getData());
          for (DependentLink link = instanceDef.getParameters(); link.hasNext(); link = link.getNext()) {
            if (link.isExplicit()) {
              instanceExpr = Concrete.AppExpression.make(sourceNode.getData(), instanceExpr, new Concrete.HoleExpression(sourceNode.getData()), true);
            }
          }

          CheckTypeVisitor.Result result = myCheckTypeVisitor.checkExpr(instanceExpr, classifyingDefCall == null ? null : new ClassCallExpression(instanceResultType.getDefinition(), Sort.STD, Collections.singletonMap(classifyingField, classifyingDefCall), Sort.STD));
          return result == null ? new ErrorExpression(null, null) : result.expression;
        }
      }
    }

    return null;
  }
}
