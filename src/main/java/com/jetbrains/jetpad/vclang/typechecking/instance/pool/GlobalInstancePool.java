package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.instance.provider.InstanceProvider;

import java.util.Collection;
import java.util.Collections;

public class GlobalInstancePool implements InstancePool {
  private final TypecheckerState myTypecheckerState;
  private final InstanceProvider myInstanceProvider;

  public GlobalInstancePool(TypecheckerState typecheckerState, InstanceProvider instanceProvider) {
    myTypecheckerState = typecheckerState;
    myInstanceProvider = instanceProvider;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, boolean isField, Equations equations, Concrete.SourceNode sourceNode) {
    if (myInstanceProvider == null) {
      return null;
    }

    DefCallExpression classifyingDefCall = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF).checkedCast(DefCallExpression.class);
    if (classifyingDefCall == null) {
      return null;
    }

    TCClassReferable typecheckable = classRef.getUnderlyingTypecheckable();
    ClassField classifyingField = ((ClassDefinition) myTypecheckerState.getTypechecked(typecheckable)).getClassifyingField();
    if (classifyingField == null) {
      return null;
    }

    Collection<? extends Concrete.Instance> instances = myInstanceProvider.getInstances(typecheckable);
    for (Concrete.Instance instance : instances) {
      Referable instanceRef = instance.getReferenceInType();
      if (instanceRef instanceof ClassReferable && (isField ? instanceRef == classRef : ((ClassReferable) instanceRef).getUnderlyingTypecheckable() == typecheckable)) {
        FunctionDefinition definition = (FunctionDefinition) myTypecheckerState.getTypechecked(instance.getData());
        if (definition != null && definition.status().headerIsOK() && definition.getResultType() instanceof ClassCallExpression) {
          Expression impl = ((ClassCallExpression) definition.getResultType()).getImplementationHere(classifyingField);
          if (impl instanceof DefCallExpression && ((DefCallExpression) impl).getDefinition() == classifyingDefCall.getDefinition()) {
            return new FunCallExpression(definition, Sort.generateInferVars(equations, sourceNode), Collections.emptyList());
          }
        }
      }
    }

    return null;
  }
}
