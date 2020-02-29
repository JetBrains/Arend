package org.arend.typechecking.subexpr;

import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Body;
import org.arend.core.expr.Expression;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.util.Pair;

public class CorrespondedSubDefVisitor implements
    ConcreteDefinitionVisitor<Definition, Pair<Expression, Concrete.Expression>> {
  private final CorrespondedSubExprVisitor visitor;

  public CorrespondedSubDefVisitor(CorrespondedSubExprVisitor visitor) {
    this.visitor = visitor;
  }

  public CorrespondedSubDefVisitor(Concrete.Expression subExpr) {
    this(new CorrespondedSubExprVisitor(subExpr));
  }

  private Pair<Expression, Concrete.Expression> visitBody(Concrete.FunctionBody body, Body coreBody) {
    if (body instanceof Concrete.TermFunctionBody && coreBody instanceof Expression) {
      Concrete.Expression term = body.getTerm();
      if (term != null) return term.accept(visitor, (Expression) coreBody);
    }
    // TODO: other function bodies :)
    return null;
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitFunction(Concrete.BaseFunctionDefinition def, Definition params) {
    FunctionDefinition coreDef;
    if (params instanceof FunctionDefinition) {
      coreDef = (FunctionDefinition) params;
    } else return null;
    Concrete.Expression resultType = def.getResultType();
    Expression coreResultType = coreDef.getResultType();
    if (resultType != null && coreResultType != null) {
      Pair<Expression, Concrete.Expression> typeResult = resultType.accept(visitor, coreResultType);
      if (typeResult != null) return typeResult;
    }
    Pair<Expression, Concrete.Expression> parametersResult = visitor
        .visitSigmaParameters(def.getParameters(), coreDef.getParameters());
    if (parametersResult != null) return parametersResult;
    return visitBody(def.getBody(), coreDef.getActualBody());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitData(Concrete.DataDefinition def, Definition params) {
    DataDefinition coreDef;
    if (params instanceof DataDefinition) {
      coreDef = (DataDefinition) params;
    } else return null;
    // TODO: constructors :)
    return visitor.visitSigmaParameters(def.getParameters(), coreDef.getParameters());
  }

  @Override
  public Pair<Expression, Concrete.Expression> visitClass(Concrete.ClassDefinition def, Definition params) {
    // TODO :)
    return null;
  }
}
