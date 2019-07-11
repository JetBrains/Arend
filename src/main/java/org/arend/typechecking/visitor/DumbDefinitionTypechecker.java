package org.arend.typechecking.visitor;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.NewExpression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.sort.Sort;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteDefinitionVisitor;
import org.arend.typechecking.error.local.TypeMismatchError;
import org.arend.util.Decision;

public class DumbDefinitionTypechecker extends BaseDefinitionTypechecker implements ConcreteDefinitionVisitor<Void, Void> {
  public DumbDefinitionTypechecker(DumbTypechecker typechecker) {
    super(typechecker);
  }

  public DumbTypechecker getTypechecker() {
    return (DumbTypechecker) typechecker;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
    FunctionDefinition funcDef = new FunctionDefinition(def.getData());
    getTypechecker().setCurrentDefinition(def, funcDef);
    typecheckFunctionHeader(funcDef, def, null, true);

    Concrete.FunctionBody body = def.getBody();
    if (body instanceof Concrete.ElimFunctionBody) {
      // TODO
    } else if (body instanceof Concrete.CoelimFunctionBody) {
      if (def.getResultType() != null) {
        Referable typeRef = def.getResultType().getUnderlyingReferable();
        if (typeRef instanceof ClassReferable) {
          typecheckCoClauses(funcDef, def, def.getResultType(), def.getResultTypeLevel(), body.getClassFieldImpls());
        } else {
          typechecker.finalCheckExpr(def.getResultType(), def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA ? new UniverseExpression(Sort.PROP) : ExpectedType.OMEGA, false);
        }
      }
    } else {
      CheckTypeVisitor.Result result = typechecker.finalCheckExpr(((Concrete.TermFunctionBody) body).getTerm(), funcDef.getResultType(), false);
      if (result != null && def.getResultType() == null && def.getKind() == Concrete.FunctionDefinition.Kind.LEMMA && !(result.expression instanceof NewExpression)) {
        Expression type = result.type.getType(false);
        if (type != null && (type instanceof UniverseExpression && !((UniverseExpression) type).getSort().isProp() || !(type instanceof UniverseExpression) && type.isWHNF() == Decision.YES)) {
          errorReporter.report(new TypeMismatchError(new UniverseExpression(Sort.PROP), type, def));
        }
      }
    }

    checkElimBody(def);
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    getTypechecker().setCurrentDefinition(def, new DataDefinition(def.getData()));
    // TODO
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    getTypechecker().setCurrentDefinition(def, new ClassDefinition(def.getData()));
    // TODO
    return null;
  }
}
