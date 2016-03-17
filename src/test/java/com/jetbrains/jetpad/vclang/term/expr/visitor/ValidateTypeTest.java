package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Test;

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
    ValidateTypeVisitor visitor = new ValidateTypeVisitor();
    elimTree.accept(visitor, null);
    assertEquals(0, visitor.myErrorReporter.errors());
    return visitor.myErrorReporter;
  }

  @Test
  public void testId() {
    FunctionDefinition definition = (FunctionDefinition) typeCheckDef("\\function id {A : \\Set0} (a : A) => (\\lam (x : A) => x) a");
    Expression expr = ((LeafElimTreeNode) definition.getElimTree()).getExpression();
    ok(expr);
  }

  @Test
  public void testAppNotPi() {
    Expression expr = Apps(Nat(), Nat());
    fail(expr);
  }

  @Test(expected = NullPointerException.class)
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
    ok(f.getElimTree());
  }

  @Test
  public void testSqueeze1() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function\n" +
            "squeeze1 (i j : I) : I\n" +
            "    <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n");
    ok(fun.getElimTree());
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

}
