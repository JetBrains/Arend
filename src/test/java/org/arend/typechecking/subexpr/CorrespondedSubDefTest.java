package org.arend.typechecking.subexpr;

import org.arend.core.definition.Definition;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.*;

public class CorrespondedSubDefTest extends TypeCheckingTestCase {
  @Test
  public void funTermBody() {
    var referable = resolveNamesDef("\\func f => 0");
    var def = (Concrete.FunctionDefinition) referable.getDefinition();
    Concrete.Expression term = def.getBody().getTerm();
    assertNotNull(term);
    var accept = def.accept(new CorrespondedSubDefVisitor(term), typeCheckDef(referable));
    assertNotNull(accept);
    assertEquals("0", accept.proj1.toString());
    assertEquals("0", accept.proj2.toString());
  }

  @Test
  public void errorReport() {
    var referable = resolveNamesDef("\\func f => 0");
    var def = (Concrete.FunctionDefinition) referable.getDefinition();
    Concrete.Expression term = def.getBody().getTerm();
    assertNotNull(term);
    CorrespondedSubDefVisitor visitor = new CorrespondedSubDefVisitor(term);
    def.accept(visitor, typeCheckDef(referable));
    // When matching the telescope of `f`, there's an error
    assertEquals(1, visitor.getExprError().size());
    assertEquals(SubExprError.Kind.Telescope, visitor.getExprError().get(0).getKind());
  }

  @Test
  public void funResultType() {
    var referable = resolveNamesDef("\\func f : Nat => 0");
    var def = (Concrete.FunctionDefinition) referable.getDefinition();
    assertNotNull(def.getResultType());
    var accept = def.accept(new CorrespondedSubDefVisitor(def.getResultType()), typeCheckDef(referable));
    assertNotNull(accept);
    assertEquals("Nat", accept.proj1.toString());
  }

  @Test
  public void funParamType() {
    var referable = resolveNamesDef("\\func f (a : Nat) => a");
    var def = (Concrete.FunctionDefinition) referable.getDefinition();
    assertFalse(def.getParameters().isEmpty());
    Concrete.Expression type = def.getParameters().get(0).getType();
    assertNotNull(type);
    var accept = def.accept(new CorrespondedSubDefVisitor(type), typeCheckDef(referable));
    assertNotNull(accept);
    assertEquals("Nat", accept.proj1.toString());
    assertEquals("Nat", accept.proj2.toString());
  }

  @Test
  public void coelimFun() {
    var referable = resolveNamesDef(
      """
        \\instance t : T
          | A => 114
          | B => 514
          | p => idp
          \\where {
            \\class T {
              | A : Nat
              | B : Nat
              | p : A = A
            }
          }
        """);
    var def = (Concrete.FunctionDefinition) referable.getDefinition();
    Definition coreDef = typeCheckDef(referable);
    Concrete.ClassFieldImpl clause = (Concrete.ClassFieldImpl) def.getBody().getCoClauseElements().get(1);
    var accept = def.accept(new CorrespondedSubDefVisitor(clause.implementation), coreDef);
    assertNotNull(accept);
    assertEquals("514", accept.proj1.toString());
    assertEquals("514", accept.proj2.toString());
  }

  @Test
  public void classes() {
    var referable = resolveNamesDef(
      """
        \\class T {
          | A : Nat
          | B : Int
        }
        """);
    var def = (Concrete.ClassDefinition) referable.getDefinition();
    {
      var clause = (Concrete.ClassField) def.getElements().get(0);
      var accept = def.accept(new CorrespondedSubDefVisitor(clause.getResultType()), typeCheckDef(referable));
      assertNotNull(accept);
      assertEquals("Nat", accept.proj1.toString());
      assertEquals("Nat", accept.proj2.toString());
    }
    {
      var clause = (Concrete.ClassField) def.getElements().get(1);
      var accept = def.accept(new CorrespondedSubDefVisitor(clause.getResultType()), typeCheckDef(referable));
      assertNotNull(accept);
      assertEquals("Int", accept.proj1.toString());
      assertEquals("Int", accept.proj2.toString());
    }
  }

  @Test
  public void classParam() {
    var referable = resolveNamesDef("\\record T (A B : Nat) {}");
    var def = (Concrete.ClassDefinition) referable.getDefinition();
    var clause = (Concrete.ClassField) def.getElements().get(1);
    var accept = def.accept(new CorrespondedSubDefVisitor(clause.getResultType()), typeCheckDef(referable));
    assertNotNull(accept);
    assertEquals("Nat", accept.proj1.toString());
    assertEquals("Nat", accept.proj2.toString());
  }

  @Test
  public void fieldParam() {
    var referable = resolveNamesDef(
      """
        \\record T {
          | A : Nat -> Int
          | B (a : \\Sigma) : Nat
        }
        """);
    var def = (Concrete.ClassDefinition) referable.getDefinition();
    {
      var clauseTy = (Concrete.PiExpression) ((Concrete.ClassField) def.getElements().get(0)).getResultType();
      var accept = def.accept(new CorrespondedSubDefVisitor(clauseTy.getCodomain()), typeCheckDef(referable));
      assertNotNull(accept);
      assertEquals("Int", accept.proj1.toString());
      assertEquals("Int", accept.proj2.toString());
    }
    {
      Concrete.TypeParameter typeParam = ((Concrete.ClassField) def.getElements().get(1)).getParameters().get(0);
      var accept = def.accept(new CorrespondedSubDefVisitor(typeParam.getType()), typeCheckDef(referable));
      assertNotNull(accept);
      assertEquals("\\Sigma", accept.proj1.toString());
      assertEquals("\\Sigma", accept.proj2.toString());
    }
  }

  @Test
  public void cowithFun() {
    var referable = resolveNamesDef(
      """
        \\func t : R \\cowith
          | pre  => 114
          | post => 514
          \\where {
            \\record R {
              | pre  : Nat
              | post : Nat
            }
          }
        """);
    var def = (Concrete.FunctionDefinition) referable.getDefinition();
    Definition coreDef = typeCheckDef(referable);
    var clause = (Concrete.ClassFieldImpl) def.getBody().getCoClauseElements().get(1);
    var accept = def.accept(new CorrespondedSubDefVisitor(clause.implementation), coreDef);
    assertNotNull(accept);
    assertEquals("514", accept.proj1.toString());
    assertEquals("514", accept.proj2.toString());
  }

  @Test
  public void elimFun() {
    var referable = resolveNamesDef(
      """
        \\func f (a b c : Nat): Nat \\elim b
          | zero => a
          | suc b => c
        """);
    var def = (Concrete.FunctionDefinition) referable.getDefinition();
    Definition coreDef = typeCheckDef(referable);
    var clauses = def.getBody().getClauses();
    assertFalse(clauses.isEmpty());
    {
      Concrete.Expression expression = clauses.get(0).getExpression();
      assertNotNull(expression);
      var accept = def.accept(new CorrespondedSubDefVisitor(expression), coreDef);
      assertNotNull(accept);
      assertEquals("a", accept.proj1.toString());
      assertEquals("a", accept.proj2.toString());
    }
    {
      Concrete.Expression expression = clauses.get(1).getExpression();
      assertNotNull(expression);
      var accept = def.accept(new CorrespondedSubDefVisitor(expression), coreDef);
      assertNotNull(accept);
      assertEquals("c", accept.proj1.toString());
      assertEquals("c", accept.proj2.toString());
    }
  }

  @Test
  public void dataParam() {
    var referable = resolveNamesDef("\\data D (a : Nat)");
    var def = (Concrete.DataDefinition) referable.getDefinition();
    assertFalse(def.getParameters().isEmpty());
    var accept = def.accept(new CorrespondedSubDefVisitor(def.getParameters().get(0).getType()), typeCheckDef(referable));
    assertNotNull(accept);
    assertEquals("Nat", accept.proj1.toString());
    assertEquals("Nat", accept.proj2.toString());
  }

  @Test
  public void cons() {
    var referable = resolveNamesDef("\\data D Int | c Nat");
    var def = (Concrete.DataDefinition) referable.getDefinition();
    assertFalse(def.getConstructorClauses().isEmpty());
    var accept = def.accept(new CorrespondedSubDefVisitor(def.getConstructorClauses().get(0).getConstructors().get(0).getParameters().get(0).getType()), typeCheckDef(referable));
    assertNotNull(accept);
    assertEquals("Nat", accept.proj1.toString());
    assertEquals("Nat", accept.proj2.toString());
  }
}