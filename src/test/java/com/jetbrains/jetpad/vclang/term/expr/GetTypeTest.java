package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static org.junit.Assert.assertEquals;

public class GetTypeTest {
  @Test
  public void constructorTest() {
    ClassDefinition def = typeCheckClass("\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => cons 0 nil");
    Namespace namespace = def.getParentNamespace().findChild(def.getName());
    assertEquals(Apps(namespace.getDefinition("List").getDefCall(), Nat()), namespace.getDefinition("test").getType());
    assertEquals(Apps(namespace.getDefinition("List").getDefCall(), Nat()), ((LeafElimTreeNode) ((FunctionDefinition) namespace.getDefinition("test")).getElimTree()).getExpression().getType());
  }

  @Test
  public void nilConstructorTest() {
    ClassDefinition def = typeCheckClass("\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => (List Nat).nil");
    Namespace namespace = def.getParentNamespace().findChild(def.getName());
    assertEquals(Apps(namespace.getDefinition("List").getDefCall(), Nat()), namespace.getDefinition("test").getType());
    assertEquals(Apps(namespace.getDefinition("List").getDefCall(), Nat()), ((LeafElimTreeNode) ((FunctionDefinition) namespace.getDefinition("test")).getElimTree()).getExpression().getType());
  }

  @Test
  public void classExtTest() {
    ClassDefinition def = typeCheckClass("\\static \\class Test { \\abstract A : \\Type0 \\abstract a : A } \\static \\function test => Test { A => Nat }");
    Namespace namespace = def.getParentNamespace().findChild(def.getName());
    assertEquals(Universe(1), namespace.getDefinition("Test").getType());
    assertEquals(Universe(0, Universe.Type.SET), namespace.getDefinition("test").getType());
    assertEquals(Universe(0, Universe.Type.SET), ((LeafElimTreeNode) ((FunctionDefinition) namespace.getDefinition("test")).getElimTree()).getExpression().getType());
  }

  @Test
  public void lambdaTest() {
    Definition def = typeCheckDef("\\function test => \\lam (f : Nat -> Nat) => f 0");
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), def.getType());
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), ((LeafElimTreeNode) ((FunctionDefinition) def).getElimTree()).getExpression().getType());
  }

  @Test
  public void lambdaTest2() {
    Definition def = typeCheckDef("\\function test => \\lam (A : \\Type0) (x : A) => x");
    DependentLink A = param("A", Universe(0));
    Expression expectedType = Pi(params(A, param("x", Reference(A))), Reference(A));
    assertEquals(expectedType, def.getType());
    assertEquals(expectedType, ((LeafElimTreeNode) ((FunctionDefinition) def).getElimTree()).getExpression().getType());
  }

  @Test
  public void fieldAccTest() {
    ClassDefinition def = typeCheckClass("\\static \\class C { \\abstract x : Nat \\function f (p : 0 = x) => p } \\static \\function test (p : Nat -> C) => (p 0).f");
    Namespace namespace = def.getParentNamespace().findChild(def.getName());
    DependentLink p = param("p", Pi(Nat(), namespace.getDefinition("C").getDefCall()));
    Expression type = Apps(Apps(FunCall(Prelude.PATH_INFIX), new ArgumentExpression(Nat(), false, true)), Zero(), Apps(namespace.getMember("C").namespace.getDefinition("x").getDefCall(), Apps(Reference(p), Zero())));
    assertEquals(Pi(p, Pi(type, type)).normalize(NormalizeVisitor.Mode.NF), namespace.getDefinition("test").getType().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void tupleTest() {
    Definition def = typeCheckDef("\\function test : \\Sigma (x y : Nat) (x = y) => (0, 0, path (\\lam _ => 0))");
    DependentLink xy = param(true, vars("x", "y"), Nat());
    assertEquals(Sigma(params(xy, param(Apps(FunCall(Prelude.PATH_INFIX), Nat(), Reference(xy), Reference(xy.getNext()))))), ((LeafElimTreeNode)((FunctionDefinition) def).getElimTree()).getExpression().getType());
  }

  @Test
  public void letTest() {
    Definition def = typeCheckDef("\\function test => \\lam (F : Nat -> \\Type0) (f : \\Pi (x : Nat) -> F x) => \\let | x => 0 \\in f x");
    DependentLink F = param("F", Pi(Nat(), Universe(0)));
    DependentLink x = param("x", Nat());
    DependentLink f = param("f", Pi(x, Apps(Reference(F), Reference(x))));
    assertEquals(Pi(params(F, f), Apps(Reference(F), Zero())), ((LeafElimTreeNode) ((FunctionDefinition) def).getElimTree()).getExpression().getType().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void patternConstructor1() {
    ClassDefinition def = typeCheckClass(
        "\\static \\data C (n : Nat) | C (zero) => c1 | C (suc n) => c2 Nat");
    Namespace namespace = def.getParentNamespace().findChild(def.getName());
    assertEquals(Apps(namespace.getMember("C").definition.getDefCall(), Zero()), ((DataDefinition) namespace.getMember("C").definition).getConstructor("c1").getType());
    DependentLink n = param("n", Nat());
    assertEquals(Pi(n, Apps(namespace.getMember("C").definition.getDefCall(), Suc(Reference(n)))), ((DataDefinition) namespace.getMember("C").definition).getConstructor("c2").getType());
  }

  @Test
  public void patternConstructor2() {
    ClassDefinition def = typeCheckClass(
        "\\static \\data Vec \\Type0 Nat | Vec A zero => Nil | Vec A (suc n) => Cons A (Vec A n)" +
        "\\static \\data D (n : Nat) (Vec Nat n) | D zero _ => dzero | D (suc n) _ => done");
    Namespace namespace = def.getParentNamespace().findChild(def.getName());
    DataDefinition vec = (DataDefinition) namespace.getMember("Vec").definition;
    DataDefinition d = (DataDefinition) namespace.getMember("D").definition;
    assertEquals(Apps(DataCall(d), Zero(), Reference(d.getConstructor("dzero").getDataTypeParameters())), d.getConstructor("dzero").getType());
    DependentLink doneParams = d.getConstructor("done").getDataTypeParameters();
    assertEquals(Apps(DataCall(d), Suc(Reference(doneParams)), Reference(doneParams.getNext())), d.getConstructor("done").getType());
    DependentLink consParams = vec.getConstructor("Cons").getDataTypeParameters();
    assertEquals(Pi(Reference(consParams), Pi(Apps(DataCall(vec), Reference(consParams), Reference(consParams.getNext())), Apps(DataCall(vec), Reference(consParams), Suc(Reference(consParams.getNext()))))), vec.getConstructor("Cons").getType());
  }

  @Test
  public void patternConstructor3() {
    ClassDefinition def = typeCheckClass(
        "\\static \\data D | d \\Type0\n" +
        "\\static \\data C D | C (d A) => c A");
    Namespace namespace = def.getParentNamespace().findChild(def.getName());
    DataDefinition d = (DataDefinition) namespace.getMember("D").definition;
    DataDefinition c = (DataDefinition) namespace.getMember("C").definition;
    DependentLink A = c.getConstructor("c").getDataTypeParameters();
    assertEquals(Pi(Reference(A), Apps(DataCall(c), Apps(ConCall(d.getConstructor("d")), Reference(A)))), c.getConstructor("c").getType());
  }

  @Test
  public void patternConstructorDep() {
    ClassDefinition def = typeCheckClass(
        "\\static \\data Box (n : Nat) | box\n" +
        "\\static \\data D (n : Nat) (Box n) | D (zero) _ => d");
    Namespace namespace = def.getParentNamespace().findChild(def.getName());
    DataDefinition d = (DataDefinition) namespace.getMember("D").definition;
    assertEquals(Apps(DataCall(d), Zero(), Reference(d.getConstructor("d").getDataTypeParameters())), d.getConstructor("d").getType());
  }
}
