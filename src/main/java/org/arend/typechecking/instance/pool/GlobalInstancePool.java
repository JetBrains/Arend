package org.arend.typechecking.instance.pool;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCClassReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.visitor.CheckTypeVisitor;

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

  public InstancePool getInstancePool() {
    return myInstancePool;
  }

  public void setInstancePool(InstancePool instancePool) {
    myInstancePool = instancePool;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, TCClassReferable classRef, Equations equations, Concrete.SourceNode sourceNode) {
    if (myInstancePool != null) {
      Expression result = myInstancePool.getInstance(classifyingExpression, classRef, equations, sourceNode);
      if (result != null) {
        return result;
      }
    }

    if (myInstanceProvider == null) {
      return null;
    }

    ClassField classifyingField = null;
    if (classifyingExpression != null) {
      classifyingExpression = classifyingExpression.normalize(NormalizeVisitor.Mode.WHNF);
      while (classifyingExpression.isInstance(LamExpression.class)) {
        classifyingExpression = classifyingExpression.cast(LamExpression.class).getBody();
      }
      if (!classifyingExpression.isInstance(DefCallExpression.class) && !classifyingExpression.isInstance(UniverseExpression.class) && !classifyingExpression.isInstance(IntegerExpression.class)) {
        return null;
      }

      Definition typechecked = myTypecheckerState.getTypechecked(classRef);
      if (!(typechecked instanceof ClassDefinition)) {
        return null;
      }
      classifyingField = ((ClassDefinition) typechecked).getClassifyingField();
      if (classifyingField == null) {
        return null;
      }
    }

    List<? extends Concrete.FunctionDefinition> instances = myInstanceProvider.getInstances();
    for (Concrete.FunctionDefinition instance : instances) {
      Referable instanceRef = instance.getReferenceInType();
      if (!(instanceRef instanceof ClassReferable && ((ClassReferable) instanceRef).isSubClassOf(classRef))) {
        continue;
      }

      FunctionDefinition instanceDef = (FunctionDefinition) myTypecheckerState.getTypechecked(instance.getData());
      if (instanceDef != null && instanceDef.status().headerIsOK() && instanceDef.getResultType() instanceof ClassCallExpression) {
        ClassCallExpression instanceResultType = (ClassCallExpression) instanceDef.getResultType();
        if (classifyingExpression != null) {
          Expression instanceClassifyingExpr = instanceResultType.getImplementationHere(classifyingField);
          if (instanceClassifyingExpr != null) {
            instanceClassifyingExpr = instanceClassifyingExpr.normalize(NormalizeVisitor.Mode.WHNF);
          }
          while (instanceClassifyingExpr instanceof LamExpression) {
            instanceClassifyingExpr = ((LamExpression) instanceClassifyingExpr).getBody();
          }
          if (!(instanceClassifyingExpr instanceof UniverseExpression && classifyingExpression.isInstance(UniverseExpression.class) ||
            instanceClassifyingExpr instanceof IntegerExpression && (classifyingExpression.isInstance(IntegerExpression.class) && ((IntegerExpression) instanceClassifyingExpr).isEqual(classifyingExpression.cast(IntegerExpression.class)) ||
              classifyingExpression.isInstance(ConCallExpression.class) && ((IntegerExpression) instanceClassifyingExpr).match(classifyingExpression.cast(ConCallExpression.class).getDefinition())) ||
            instanceClassifyingExpr instanceof DefCallExpression && classifyingExpression.isInstance(DefCallExpression.class) && ((DefCallExpression) instanceClassifyingExpr).getDefinition() == classifyingExpression.cast(DefCallExpression.class).getDefinition())) {
            continue;
          }
        }

        Concrete.Expression instanceExpr = new Concrete.ReferenceExpression(sourceNode.getData(), instance.getData());
        for (DependentLink link = instanceDef.getParameters(); link.hasNext(); link = link.getNext()) {
          if (link.isExplicit()) {
            instanceExpr = Concrete.AppExpression.make(sourceNode.getData(), instanceExpr, new Concrete.HoleExpression(sourceNode.getData()), true);
          }
        }

        Expression expectedType = classifyingField == null ? null : myCheckTypeVisitor.fixClassExtSort(new ClassCallExpression(instanceResultType.getDefinition(), Sort.generateInferVars(myCheckTypeVisitor.getEquations(), instanceResultType.getDefinition().hasUniverses(), sourceNode)), sourceNode);
        CheckTypeVisitor.Result result = myCheckTypeVisitor.checkExpr(instanceExpr, expectedType);
        return result == null ? new ErrorExpression(null, null) : result.expression;
      }
    }

    return null;
  }

  @Override
  public GlobalInstancePool subst(ExprSubstitution substitution) {
    if (myInstancePool != null) {
      GlobalInstancePool result = new GlobalInstancePool(myTypecheckerState, myInstanceProvider, myCheckTypeVisitor);
      result.setInstancePool(myInstancePool.subst(substitution));
      return result;
    } else {
      return this;
    }
  }
}
