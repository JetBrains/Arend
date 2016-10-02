package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class GetTypeTest extends TypeCheckingTestCase {
  private static void testType(Expression expected, TypeCheckClassResult result) {
    assertEquals(expected, ((FunctionDefinition) result.getDefinition("test")).getResultType().toExpression());
    assertEquals(expected, ((LeafElimTreeNode) ((FunctionDefinition) result.getDefinition("test")).getElimTree()).getExpression().getType().toExpression());
  }

  @Test
  public void constructorTest() {
    TypeCheckClassResult result = typeCheckClass("\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => cons 0 nil");
    testType(DataCall((DataDefinition) result.getDefinition("List"), Nat()), result);
  }

  @Test
  public void nilConstructorTest() {
    TypeCheckClassResult result = typeCheckClass("\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => (List Nat).nil");
    testType(DataCall((DataDefinition) result.getDefinition("List"), Nat()), result);
  }

  @Test
  public void classExtTest() {
    TypeCheckClassResult result = typeCheckClass("\\static \\class Test { \\abstract A : \\Type0 \\abstract a : A } \\static \\function test => Test { A => Nat }");
    assertEquals(Universe(1), result.getDefinition("Test").getType(new LevelSubstitution()).toExpression());
    assertEquals(Universe(Sort.SetOfLevel(0)), result.getDefinition("test").getType(new LevelSubstitution()).toExpression());
    testType(Universe(Sort.SetOfLevel(0)), result);
  }

  @Test
  public void lambdaTest() {
    TypeCheckClassResult result = typeCheckClass("\\static \\function test => \\lam (f : Nat -> Nat) => f 0");
    testType(Pi(Pi(Nat(), Nat()), Nat()), result);
  }

  @Test
  public void lambdaTest2() {
    TypeCheckClassResult result = typeCheckClass("\\function test => \\lam (A : \\Type0) (x : A) => x");
    DependentLink A = param("A", Universe(0));
    Expression expectedType = Pi(params(A, param("x", Reference(A))), Reference(A));
    testType(expectedType, result);
  }

  @Test
  public void fieldAccTest() {
    TypeCheckClassResult result = typeCheckClass("\\static \\class C { \\abstract x : Nat \\function f (p : 0 = x) => p } \\static \\function test (p : Nat -> C) => (p 0).f");
    DependentLink p = param("p", Pi(Nat(), ClassCall((ClassDefinition) result.getDefinition("C"))));
    Expression type = FunCall(Prelude.PATH_INFIX, new Level(0), new Level(1),
        Nat(),
        Zero(),
        FieldCall((ClassField) result.getDefinition("C.x"), Apps(Reference(p), Zero())));
    assertEquals(Pi(p, Pi(type, type)).normalize(NormalizeVisitor.Mode.NF), result.getDefinition("test").getType(new LevelSubstitution()).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void tupleTest() {
    TypeCheckClassResult result = typeCheckClass("\\function test : \\Sigma (x y : Nat) (x = y) => (0, 0, path (\\lam _ => 0))");
    DependentLink xy = param(true, vars("x", "y"), Nat());
    testType(Sigma(params(xy, param(FunCall(Prelude.PATH_INFIX, new Level(0), new Level(1), Nat(), Reference(xy), Reference(xy.getNext()))))), result);
  }

  @Test
  public void letTest() {
    Definition def = typeCheckDef("\\function test => \\lam (F : Nat -> \\Type0) (f : \\Pi (x : Nat) -> F x) => \\let | x => 0 \\in f x");
    DependentLink F = param("F", Pi(Nat(), Universe(0)));
    DependentLink x = param("x", Nat());
    DependentLink f = param("f", Pi(x, Apps(Reference(F), Reference(x))));
    assertEquals(Pi(params(F, f), Apps(Reference(F), Zero())), ((Expression) ((LeafElimTreeNode) ((FunctionDefinition) def).getElimTree()).getExpression().getType()).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void patternConstructor1() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data C (n : Nat) | C (zero) => c1 | C (suc n) => c2 Nat");
    DataDefinition data = (DataDefinition) result.getDefinition("C");
    assertEquals(DataCall(data, Zero()), data.getConstructor("c1").getType(new LevelSubstitution()));
    DependentLink params = data.getConstructor("c2").getDataTypeParameters();
    assertEquals(
        Pi(params, Pi(param(Nat()), DataCall(data, Suc(Reference(params))))),
        data.getConstructor("c2").getType(new LevelSubstitution())
    );
  }

  @Test
  public void patternConstructor2() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data Vec \\Type0 Nat | Vec A zero => Nil | Vec A (suc n) => Cons A (Vec A n)" +
        "\\static \\data D (n : Nat) (Vec Nat n) | D zero _ => dzero | D (suc n) _ => done");
    DataDefinition vec = (DataDefinition) result.getDefinition("Vec");
    DataDefinition d = (DataDefinition) result.getDefinition("D");
    assertEquals(
        Pi(d.getConstructor("dzero").getDataTypeParameters(), DataCall(d, Zero(), Reference(d.getConstructor("dzero").getDataTypeParameters()))),
        d.getConstructor("dzero").getType(new LevelSubstitution())
    );
    DependentLink doneParams = d.getConstructor("done").getDataTypeParameters();
    assertEquals(
        Pi(d.getConstructor("done").getDataTypeParameters(), DataCall(d, Suc(Reference(doneParams)), Reference(doneParams.getNext()))),
        d.getConstructor("done").getType(new LevelSubstitution())
    );
    DependentLink consParams = vec.getConstructor("Cons").getDataTypeParameters();
    assertEquals(
        Pi(consParams, Pi(Reference(consParams), Pi(DataCall(vec, Reference(consParams), Reference(consParams.getNext())), DataCall(vec, Reference(consParams), Suc(Reference(consParams.getNext())))))),
        vec.getConstructor("Cons").getType(new LevelSubstitution())
    );
  }

  @Test
  public void patternConstructor3() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data D | d \\Type0\n" +
        "\\static \\data C D | C (d A) => c A");
    DataDefinition d = (DataDefinition) result.getDefinition("D");
    DataDefinition c = (DataDefinition) result.getDefinition("C");
    DependentLink A = c.getConstructor("c").getDataTypeParameters();
    assertEquals(
        Pi(c.getConstructor("c").getDataTypeParameters(), Pi(Reference(A), DataCall(c, ConCall(d.getConstructor("d"), Collections.<Expression>emptyList(), Reference(A))))),
        c.getConstructor("c").getType(new LevelSubstitution())
    );
  }

  @Test
  public void patternConstructorDep() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data Box (n : Nat) | box\n" +
        "\\static \\data D (n : Nat) (Box n) | D (zero) _ => d");
    DataDefinition d = (DataDefinition) result.getDefinition("D");
    assertEquals(
        Pi(d.getConstructor("d").getDataTypeParameters(), DataCall(d, Zero(), Reference(d.getConstructor("d").getDataTypeParameters()))),
        d.getConstructor("d").getType(new LevelSubstitution())
    );
  }
}
