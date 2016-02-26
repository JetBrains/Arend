package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.SigmaExpression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.constructions.Sigma;
import org.junit.Test;

import java.util.Arrays;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ValidateTypeTest {

  private ValidateTypeVisitor.FailedResult fail(Expression expr) {
    ValidateTypeVisitor.Result res = expr.checkType();
    assertTrue(res instanceof ValidateTypeVisitor.FailedResult);
    return (ValidateTypeVisitor.FailedResult) res;
  }

  private ValidateTypeVisitor.OKResult ok(Expression expr) {
    ValidateTypeVisitor.Result res = expr.checkType();
    assertTrue(res instanceof ValidateTypeVisitor.OKResult);
    return (ValidateTypeVisitor.OKResult) res;
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
    ValidateTypeVisitor.FailedResult res = fail(expr);
    assertTrue(res.getReasons().contains(ValidateTypeVisitor.FailedResult.Reason.PiExpected));
  }

  @Test
  public void testProjNotSigma() {
    Expression expr = Proj(Nat(), 0);
    ValidateTypeVisitor.FailedResult res = fail(expr);
    assertTrue(res.getReasons().contains(ValidateTypeVisitor.FailedResult.Reason.ProjNotTuple));
  }

  @Test
  public void testSigmaWrongType() {
    DependentLink param = param("x", Nat());
    param.setNext(param("y", Universe()));
    Expression expr = Proj(Tuple(Sigma(param), Zero(), Zero()), 0);
    ValidateTypeVisitor.FailedResult res = fail(expr);
    assertTrue(res.getReasons().contains(ValidateTypeVisitor.FailedResult.Reason.TypeMismatch));
  }
}
