package org.arend.typechecking.subexpr;

import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * For GitHub issues
 */
public class SubExprBugsTest extends TypeCheckingTestCase {
  @Test
  public void issue168() {
    var resolved = resolveNamesDef(
      """
        \\func test => f 114 \\where {
          \\func F => \\Pi Nat -> Nat
          \\func f : F => \\lam i => i Nat.+ 514
        }
        """);
    var concreteDef = (Concrete.FunctionDefinition) resolved.getDefinition();
    var concrete = (Concrete.AppExpression) concreteDef.getBody().getTerm();
    assertNotNull(concrete);
    Concrete.Expression subExpr = concrete.getArguments().get(0).getExpression();
    var accept = concreteDef.accept(new CorrespondedSubDefVisitor(subExpr), typeCheckDef(resolved));
    assertNotNull(accept);
    assertEquals("114", accept.proj1.toString());
    assertEquals("114", accept.proj2.toString());
  }

  @Test
  public void issue180() {
    var resolved = resolveNamesDef("\\func test => \\Pi (A : \\Set) -> A -> A");
    var concreteDef = (Concrete.FunctionDefinition) resolved.getDefinition();
    var concrete = (Concrete.PiExpression) concreteDef.getBody().getTerm();
    assertNotNull(concrete);
    Concrete.@NotNull Expression subExpr = ((Concrete.PiExpression) concrete.getCodomain()).getParameters().get(0).getType();
    var accept = concreteDef.accept(new CorrespondedSubDefVisitor(subExpr), typeCheckDef(resolved));
    assertNotNull(accept);
    assertEquals("A", accept.proj1.toString());
    assertEquals("A", accept.proj2.toString());
  }

  @Test
  public void issue195() {
    var resolved = resolveNamesDef(
      """
        \\record Kibou \\extends No
          | hana => 114514 \\where {
            \\record No | hana : Nat
          }
        """);
    var concreteDef = (Concrete.ClassDefinition) resolved.getDefinition();
    var classField = (Concrete.ClassFieldImpl) concreteDef.getElements().get(0);
    assertNotNull(classField);
    Concrete.Expression implementation = classField.implementation;
    var accept = concreteDef.accept(new CorrespondedSubDefVisitor(implementation), typeCheckDef(resolved));
    assertNotNull(accept);
    assertEquals("114514", accept.proj1.toString());
    // It's actually \lam {this : Kibou} => 114514
    // assertEquals("114514", accept.proj2.toString());
  }

  @Test
  public void issue196() {
    var resolved = resolveNamesDef(
      """
        \\func Dorothy : Alice \\cowith
         | rbq {
           | level => 114514
         } \\where {
            \\record Rbq | level : Nat
            \\record Alice (rbq : Rbq)
          }
        """);
    var concreteDef = (Concrete.FunctionDefinition) resolved.getDefinition();
    var classField = (Concrete.ClassFieldImpl) concreteDef.getBody().getCoClauseElements().get(0);
    assertNotNull(classField);
    Concrete.ClassFieldImpl implementation = classField.getSubCoclauseList().get(0);
    var accept = concreteDef.accept(new CorrespondedSubDefVisitor(implementation.implementation), typeCheckDef(resolved));
    assertNotNull(accept);
    assertEquals("114514", accept.proj1.toString());
    assertEquals("114514", accept.proj2.toString());
  }

  @Test
  public void issue252() {
    typeCheckModule(
      "\\record Tony\n" +
        "  | beta (lam : \\Set0) (b : \\Prop) (d : lam) (a : b) : b");
    var concreteDef = (Concrete.ClassDefinition) getConcreteDesugarized("Tony");
    var def = getDefinition("Tony");
    assertTrue(concreteDef.isRecord());
    var field = (Concrete.ClassField) concreteDef.getElements().get(0);
    var parameters = field.getParameters();
    {
      var parameter = parameters.get(1);
      var accept = concreteDef.accept(new CorrespondedSubDefVisitor(parameter.type), def);
      assertNotNull(accept);
      assertEquals("\\Set0", accept.proj1.toString());
      assertEquals("\\Set0", accept.proj2.toString());
    }
    {
      var parameter = parameters.get(2);
      var accept = concreteDef.accept(new CorrespondedSubDefVisitor(parameter.type), def);
      assertNotNull(accept);
      assertEquals("\\Prop", accept.proj1.toString());
      assertEquals("\\Prop", accept.proj2.toString());
    }
    {
      var parameter = parameters.get(3);
      var accept = concreteDef.accept(new CorrespondedSubDefVisitor(parameter.type), def);
      assertNotNull(accept);
      assertEquals("lam", accept.proj1.toString());
      assertEquals("lam", accept.proj2.toString());
    }
    {
      var parameter = parameters.get(4);
      var accept = concreteDef.accept(new CorrespondedSubDefVisitor(parameter.type), def);
      assertNotNull(accept);
      assertEquals("b", accept.proj1.toString());
      assertEquals("b", accept.proj2.toString());
    }
  }
}
