package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Test;

import java.util.EnumSet;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static org.junit.Assert.assertEquals;

public class GetTypeTest {
  @Test
  public void constructorTest() {
    NamespaceMember member = typeCheckClass("\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => cons 0 nil");
    assertEquals(Apps(member.namespace.getDefinition("List").getDefCall(), Nat()), member.namespace.getDefinition("test").getType());
    assertEquals(Apps(member.namespace.getDefinition("List").getDefCall(), Nat()), ((LeafElimTreeNode) ((FunctionDefinition) member.namespace.getDefinition("test")).getElimTree()).getExpression().getType());
  }

  @Test
  public void nilConstructorTest() {
    NamespaceMember member = typeCheckClass("\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => (List Nat).nil");
    assertEquals(Apps(member.namespace.getDefinition("List").getDefCall(), Nat()), member.namespace.getDefinition("test").getType());
    assertEquals(Apps(member.namespace.getDefinition("List").getDefCall(), Nat()), ((LeafElimTreeNode) ((FunctionDefinition) member.namespace.getDefinition("test")).getElimTree()).getExpression().getType());
  }

  @Test
  public void classExtTest() {
    NamespaceMember member = typeCheckClass("\\static \\class Test { \\abstract A : \\Type0 \\abstract a : A } \\static \\function test => Test { A => Nat }");
    assertEquals(Universe(1), member.namespace.getDefinition("Test").getType());
    assertEquals(Universe(0), member.namespace.getDefinition("test").getType());
    assertEquals(Universe(TypeUniverse.SetOfLevel(0)), ((LeafElimTreeNode) ((FunctionDefinition) member.namespace.getDefinition("test")).getElimTree()).getExpression().getType());
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
    NamespaceMember member = typeCheckClass("\\static \\class C { \\abstract x : Nat \\function f (p : 0 = x) => p } \\static \\function test (p : Nat -> C) => (p 0).f");
    DependentLink p = param("p", Pi(Nat(), member.namespace.getDefinition("C").getDefCall()));
    Expression type = FunCall(Prelude.PATH_INFIX)
      .addArgument(ZeroLvl(), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Fin(Suc(Zero())), EnumSet.noneOf(AppExpression.Flag.class))
      .addArgument(Nat(), EnumSet.noneOf(AppExpression.Flag.class))
      .addArgument(Zero(), AppExpression.DEFAULT)
      .addArgument(Apps(member.namespace.getMember("C").namespace.getDefinition("x").getDefCall(), Apps(Reference(p), Zero())), AppExpression.DEFAULT);
    assertEquals(Pi(p, Pi(type, type)).normalize(NormalizeVisitor.Mode.NF), member.namespace.getDefinition("test").getType().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void tupleTest() {
    Definition def = typeCheckDef("\\function test : \\Sigma (x y : Nat) (x = y) => (0, 0, path (\\lam _ => 0))");
    DependentLink xy = param(true, vars("x", "y"), Nat());
    assertEquals(Sigma(params(xy, param(Apps(FunCall(Prelude.PATH_INFIX).addArgument(ZeroLvl(), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(Fin(Suc(Zero())), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Nat(), EnumSet.noneOf(AppExpression.Flag.class)), Reference(xy), Reference(xy.getNext()))))), ((LeafElimTreeNode)((FunctionDefinition) def).getElimTree()).getExpression().getType());
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
    NamespaceMember member = typeCheckClass(
        "\\static \\data C (n : Nat) | C (zero) => c1 | C (suc n) => c2 Nat");
    DataDefinition data = (DataDefinition) member.namespace.getMember("C").definition;
    assertEquals(Apps(data.getDefCall(), Zero()), data.getConstructor("c1").getType());
    DependentLink params = data.getConstructor("c2").getDataTypeParameters();
    assertEquals(
        Pi(params, Pi(param(Nat()), Apps(data.getDefCall(), Suc(Reference(params))))),
        data.getConstructor("c2").getType()
    );
  }

  @Test
  public void patternConstructor2() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data Vec \\Type0 Nat | Vec A zero => Nil | Vec A (suc n) => Cons A (Vec A n)" +
        "\\static \\data D (n : Nat) (Vec Nat n) | D zero _ => dzero | D (suc n) _ => done");
    DataDefinition vec = (DataDefinition) member.namespace.getMember("Vec").definition;
    DataDefinition d = (DataDefinition) member.namespace.getMember("D").definition;
    assertEquals(
        Pi(d.getConstructor("dzero").getDataTypeParameters(), Apps(DataCall(d), Zero(), Reference(d.getConstructor("dzero").getDataTypeParameters()))),
        d.getConstructor("dzero").getType()
    );
    DependentLink doneParams = d.getConstructor("done").getDataTypeParameters();
    assertEquals(
        Pi(d.getConstructor("done").getDataTypeParameters(), Apps(DataCall(d), Suc(Reference(doneParams)), Reference(doneParams.getNext()))),
        d.getConstructor("done").getType()
    );
    DependentLink consParams = vec.getConstructor("Cons").getDataTypeParameters();
    assertEquals(
        Pi(consParams, Pi(Reference(consParams), Pi(Apps(DataCall(vec), Reference(consParams), Reference(consParams.getNext())), Apps(DataCall(vec), Reference(consParams), Suc(Reference(consParams.getNext())))))),
        vec.getConstructor("Cons").getType()
    );
  }

  @Test
  public void patternConstructor3() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data D | d \\Type0\n" +
        "\\static \\data C D | C (d A) => c A");
    DataDefinition d = (DataDefinition) member.namespace.getMember("D").definition;
    DataDefinition c = (DataDefinition) member.namespace.getMember("C").definition;
    DependentLink A = c.getConstructor("c").getDataTypeParameters();
    assertEquals(
        Pi(c.getConstructor("c").getDataTypeParameters(), Pi(Reference(A), Apps(DataCall(c), Apps(ConCall(d.getConstructor("d")), Reference(A))))),
        c.getConstructor("c").getType()
    );
  }

  @Test
  public void patternConstructorDep() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data Box (n : Nat) | box\n" +
        "\\static \\data D (n : Nat) (Box n) | D (zero) _ => d");
    DataDefinition d = (DataDefinition) member.namespace.getMember("D").definition;
    assertEquals(
        Pi(d.getConstructor("d").getDataTypeParameters(), Apps(DataCall(d), Zero(), Reference(d.getConstructor("d").getDataTypeParameters()))),
        d.getConstructor("d").getType()
    );
  }
}
