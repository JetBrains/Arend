package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Test;

import java.util.List;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ValidateTypeTest {

  private ValidateTypeVisitor.ErrorReporter fail(Expression expr) {
    ValidateTypeVisitor.ErrorReporter res = expr.checkType();
    assertTrue(res.errors() > 0);
    return res;
  }

  private ValidateTypeVisitor.ErrorReporter ok(Expression expr) {
    ValidateTypeVisitor.ErrorReporter res = expr.checkType();
    assertTrue(res.errors() == 0);
    return res;
  }

  private ValidateTypeVisitor.ErrorReporter ok(ElimTreeNode elimTree) {
    return ok(elimTree, null);
  }

  private ValidateTypeVisitor.ErrorReporter ok(ElimTreeNode elimTree, Expression type) {
    ValidateTypeVisitor visitor = new ValidateTypeVisitor();
    elimTree.accept(visitor, type);
    assertEquals(0, visitor.myErrorReporter.errors());
    return visitor.myErrorReporter;
  }

  @Test
  public void testId() {
    FunctionDefinition definition = (FunctionDefinition) typeCheckDef("\\function id {A : \\Set0} (a : A) => (\\lam (x : A) => x) a");
    Expression expr = ((LeafElimTreeNode) definition.getElimTree()).getExpression();
    ok(expr);
  }

  @Test(expected = AssertionError.class)
  public void testAppNotPi() {
    Expression expr = Apps(Nat(), Nat());
    fail(expr);
  }

  @Test
  public void testProjNotSigma() {
    Expression expr = Proj(Nat(), 0);
    fail(expr);
  }

  @Test
  public void testSigmaWrongType() {
    DependentLink param = param("x", Nat());
    param.setNext(param("y", Universe()));
    Expression expr = Proj(Tuple(Sigma(param), Zero(), Zero()), 0);
    fail(expr);
  }

  @Test
  public void testProjTriple() {
    DependentLink link = params(param("x", Nat()), param("y", Nat()), param("z", Nat()));
    Expression expr = Proj(Tuple(Sigma(link), Zero(), Zero(), Zero()), 2);
    ok(expr);
  }

  @Test(expected = IllegalStateException.class)
  public void testProjTooLargeIndex() {
    DependentLink link = params(param("x", Nat()), param("y", Nat()), param("z", Nat()));
    Expression expr = Proj(Tuple(Sigma(link), Zero(), Zero(), Zero()), 3);
    fail(expr);
  }

  @Test
  public void testSigma() {
    FunctionDefinition f = (FunctionDefinition) typeCheckDef(
            "\\function f (n : Nat) : \\Sigma (m : Nat) (suc n = suc m) => (n, path (\\lam (i : I) => suc n))");
    ok(f.getElimTree(), f.getResultType());
  }

  @Test
  public void testSqueeze1() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function\n" +
            "squeeze1 (i j : I) : I\n" +
            "    <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n");
    ok(fun.getElimTree(), fun.getResultType());
  }

  @Test
  public void testAt() {
    Expression at = typeCheckExpr("(@)", null).expression;
    PiExpression atType = (PiExpression) at.getType();
//    System.err.println(atType);
    DependentLink first = atType.getParameters();
//    System.err.println("first = " + first);
    DependentLink second = first.getNext();
//    System.err.println(second);
    Binding a2 = ((ReferenceExpression) ((AppExpression) second.getType()).getFunction()).getBinding();
//    System.err.println("a2 = " + a2);
//    System.err.println(a2 == first);
    assertEquals(a2, first);
  }

  @Test
  public void testFunEqToHom() {
    FunctionDefinition funEqToHom = (FunctionDefinition) typeCheckDef("\\function\n" +
            "funEqToHom {A : \\Type0} (B : A -> \\Type0) {f g : \\Pi (x : A) -> B x} (p : f = g) (x : A) : f x = g x => \n" +
            "    path (\\lam i => (p @ i) x)\n");
    AppExpression app = (AppExpression) ((LeafElimTreeNode)funEqToHom.getElimTree()).getExpression();
    List<? extends Expression> args = app.getArguments();
    Expression argType = args.get(0).getType();
    System.err.println(argType);
  }

  @Test
  public void testFunEqToHomNonDep() {
    FunctionDefinition funEqToHom = (FunctionDefinition) typeCheckDef("\\function\n" +
            "funEqToHom' {A B : \\Type0} {f g : A -> B} (p : f = g) (x : A) : f x = g x =>\n" +
            "    path (\\lam i => (p @ i) x)\n");
    AppExpression app = (AppExpression) ((LeafElimTreeNode)funEqToHom.getElimTree()).getExpression();
    List<? extends Expression> args = app.getArguments();
    Expression argType = args.get(0).getType();
    System.err.println(argType);
  }
  @Test
  public void testMoreArgsThanParamsOk() {
//    CheckTypeVisitor.Result e = typeCheckExpr("(\\lam (x: Nat) => suc) zero zero", null);
    CheckTypeVisitor.Result e = typeCheckExpr("\\lam (p : suc = suc) => (p @ left) zero", null);
    ok(e.expression);
    System.err.println(e.expression.getType());
  }

  @Test
  public void testLet() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function\n" +
            "f : Nat => \\let | n : Nat => 2 \\in n");
    ok(fun.getElimTree(), Nat());
  }

  @Test
  public void testCase() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function\n" +
            "isZero (n : Nat) : Nat => \\case n | zero => 1 | suc _ => 0");
    ok(fun.getElimTree(), Nat());
  }

  @Test
  public void testProj() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef(
            "\\function f : Nat => (\\lam (p : \\Sigma Nat Nat) => p.2) (1, 2)"
    );
    ok(fun.getElimTree(), Nat());
  }

  @Test
  public void testIIsContr() {
    NamespaceMember member = typeCheckClass(
            "\\static \\function squeeze1 (i j : I) : I\n" +
            "    <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
            "\\static \\function squeeze (i j : I)\n" +
            "    <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
            "\\static \\function isContr (A : \\Type0) => \\Sigma (a : A) (\\Pi (a' : A) -> a = a')\n" +
            "\\static \\function I-isContr : isContr I => (left, \\lam i => path (\\lam j => squeeze i j))\n"
            );
    FunctionDefinition isContr = (FunctionDefinition) member.namespace.getMember("isContr").definition;
    FunctionDefinition iIsContr = (FunctionDefinition) member.namespace.getMember("I-isContr").definition;
    Expression type = Apps(FunCall(isContr), DataCall(Prelude.INTERVAL));
    ok(iIsContr.getElimTree(), type);
  }
}
