package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
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

}
