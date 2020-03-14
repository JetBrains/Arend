package org.arend.typechecking.subexpr;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * For GitHub issues
 */
public class SubExprBugs extends TypeCheckingTestCase {
  @Test
  public void issue168() {
    ConcreteLocatedReferable resolved = resolveNamesDef(
        "\\func test => f 114 \\where {\n" +
            "  \\func F => \\Pi Nat -> Nat\n" +
            "  \\func f : F => \\lam i => i Nat.+ 514\n" +
            "}");
    Concrete.FunctionDefinition concreteDef = (Concrete.FunctionDefinition) resolved.getDefinition();
    FunctionDefinition coreDef = (FunctionDefinition) typeCheckDef(resolved);
    Concrete.AppExpression concrete = (Concrete.AppExpression) concreteDef.getBody().getTerm();
    assertNotNull(concrete);
    Concrete.Expression subExpr = concrete.getArguments().get(0).getExpression();
    Pair<@NotNull Expression, Concrete.Expression> accept = concrete.accept(
        new CorrespondedSubExprVisitor(subExpr), (Expression) coreDef.getActualBody());
    assertNotNull(accept);
    assertEquals("114", accept.proj1.toString());
    assertEquals("114", accept.proj2.toString());
  }
}
