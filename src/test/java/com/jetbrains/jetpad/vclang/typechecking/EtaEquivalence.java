package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EtaEquivalence extends TypeCheckingTestCase {
  @Test
  public void classesEq() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Foo { \\abstract foo : Nat \\abstract bar : Nat }\n" +
        "\\function f (l : Foo) => \\new Foo { foo => l.foo | bar => l.bar }");
    assertNotNull(result.typecheckerState.getTypechecked(result.classDefinition));
    assertTrue(result.getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, ((LeafElimTreeNode) f.getElimTree()).getExpression(), Reference(f.getParameters().getNext()), null));
  }

  @Test
  public void classesGe() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Foo { \\abstract foo : Nat \\abstract bar : Nat }\n" +
        "\\function f (l : Foo) => \\new Foo { foo => l.foo | bar => l.bar }");
    assertNotNull(result.typecheckerState.getTypechecked(result.classDefinition));
    assertTrue(result.getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.GE, ((LeafElimTreeNode) f.getElimTree()).getExpression(), Reference(f.getParameters().getNext()), null));
  }

  @Test
  public void classesLe() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Foo { \\abstract foo : Nat \\abstract bar : Nat }\n" +
        "\\function f (l : Foo) => \\new Foo { foo => l.foo | bar => l.bar }");
    assertNotNull(result.typecheckerState.getTypechecked(result.classDefinition));
    assertTrue(result.getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.LE, ((LeafElimTreeNode) f.getElimTree()).getExpression(), Reference(f.getParameters().getNext()), null));
  }
  @Test
  public void pathEtaLeftTest() {
    typeCheckDef("\\function test (p : 0 = 0) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTest() {
    typeCheckDef("\\function test (p : 0 = 0) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void pathEtaLeftTestLevel() {
    typeCheckDef("\\function test (p : Nat = Nat) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTestLevel() {
    typeCheckDef("\\function test (p : Nat = Nat) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void onlyDefCallsExpanded() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function f (x : Nat -> Nat) => x");
    assertTrue(((LeafElimTreeNode)fun.getElimTree()).getExpression().toLam() == null);
  }
}
