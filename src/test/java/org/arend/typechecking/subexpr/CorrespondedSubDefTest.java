package org.arend.typechecking.subexpr;

import org.arend.core.expr.Expression;
import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.util.Pair;
import org.junit.Test;

import static org.junit.Assert.*;

public class CorrespondedSubDefTest extends TypeCheckingTestCase {
  @Test
  public void funTermBody() {
    ConcreteLocatedReferable referable = resolveNamesDef("\\func f => 0");
    Concrete.FunctionDefinition def = (Concrete.FunctionDefinition) referable.getDefinition();
    assertNotNull(def.getBody());
    Pair<Expression, Concrete.Expression> accept = def.accept(
        new CorrespondedSubDefVisitor(def.getBody().getTerm()), typeCheckDef(referable));
    assertEquals("0", accept.proj1.toString());
  }

  @Test
  public void funResultType() {
    ConcreteLocatedReferable referable = resolveNamesDef("\\func f : Nat => 0");
    Concrete.FunctionDefinition def = (Concrete.FunctionDefinition) referable.getDefinition();
    assertNotNull(def.getResultType());
    Pair<Expression, Concrete.Expression> accept = def.accept(
        new CorrespondedSubDefVisitor(def.getResultType()), typeCheckDef(referable));
    assertEquals("Nat", accept.proj1.toString());
  }

  @Test
  public void funParamType() {
    ConcreteLocatedReferable referable = resolveNamesDef("\\func f (a : Nat) => a");
    Concrete.FunctionDefinition def = (Concrete.FunctionDefinition) referable.getDefinition();
    assertFalse(def.getParameters().isEmpty());
    Pair<Expression, Concrete.Expression> accept = def.accept(
        new CorrespondedSubDefVisitor(def.getParameters().get(0).getType()), typeCheckDef(referable));
    assertEquals("Nat", accept.proj1.toString());
  }

  @Test
  public void dataParam() {
    ConcreteLocatedReferable referable = resolveNamesDef("\\data D (a : Nat)");
    Concrete.DataDefinition def = (Concrete.DataDefinition) referable.getDefinition();
    assertFalse(def.getParameters().isEmpty());
    Pair<Expression, Concrete.Expression> accept = def.accept(
        new CorrespondedSubDefVisitor(def.getParameters().get(0).getType()), typeCheckDef(referable));
    assertEquals("Nat", accept.proj1.toString());
  }

  @Test
  public void cons() {
    ConcreteLocatedReferable referable = resolveNamesDef("\\data D | c Nat");
    Concrete.DataDefinition def = (Concrete.DataDefinition) referable.getDefinition();
    assertFalse(def.getConstructorClauses().isEmpty());
    Pair<Expression, Concrete.Expression> accept = def.accept(
        new CorrespondedSubDefVisitor(def
            .getConstructorClauses().get(0)
            .getConstructors().get(0)
            .getParameters().get(0)
            .getType()
        ), typeCheckDef(referable));
    assertEquals("Nat", accept.proj1.toString());
  }
}