package org.arend.typechecking;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.BigIntegerExpression;
import org.arend.core.expr.SmallIntegerExpression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import java.math.BigInteger;

import static org.arend.core.expr.ExpressionFactory.Neg;
import static org.arend.core.expr.ExpressionFactory.Pos;
import static org.junit.Assert.assertEquals;

public class EvaluationTest extends TypeCheckingTestCase {
  @Test
  public void evalPlus() {
    typeCheckModule(
      "\\open Nat\n" +
      "\\func f1 => 20 + 36\n" +
      "\\func f2 => 0 + 23\n" +
      "\\func f3 => 11 + 0");
    assertEquals(new SmallIntegerExpression(56), ((LeafElimTree) ((FunctionDefinition) getDefinition("f1")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(new SmallIntegerExpression(23), ((LeafElimTree) ((FunctionDefinition) getDefinition("f2")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(new SmallIntegerExpression(11), ((LeafElimTree) ((FunctionDefinition) getDefinition("f3")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
  }

  @Test
  public void evalMul() {
    typeCheckModule(
      "\\open Nat\n" +
      "\\func f1 => 20 * 36\n" +
      "\\func f2 => 0 * 23\n" +
      "\\func f3 => 11 * 0\n" +
      "\\func f4 => 1 * 23\n" +
      "\\func f5 => 11 * 1\n" +
      "\\func f6 => 100000 * 100000");
    assertEquals(new SmallIntegerExpression(720), ((LeafElimTree) ((FunctionDefinition) getDefinition("f1")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(new SmallIntegerExpression(0), ((LeafElimTree) ((FunctionDefinition) getDefinition("f2")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(new SmallIntegerExpression(0), ((LeafElimTree) ((FunctionDefinition) getDefinition("f3")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(new SmallIntegerExpression(23), ((LeafElimTree) ((FunctionDefinition) getDefinition("f4")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(new SmallIntegerExpression(11), ((LeafElimTree) ((FunctionDefinition) getDefinition("f5")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(new BigIntegerExpression(BigInteger.valueOf(100000).multiply(BigInteger.valueOf(100000))), ((LeafElimTree) ((FunctionDefinition) getDefinition("f6")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
  }

  @Test
  public void evalMinus() {
    typeCheckModule(
      "\\open Nat\n" +
      "\\func f1 => 36 - 20\n" +
      "\\func f2 => 20 - 36\n" +
      "\\func f3 => 11 - 0\n" +
      "\\func f4 => 0 - 23\n" +
      "\\func f5 => 11 - 1\n" +
      "\\func f6 => 1 - 23\n" +
      "\\func f7 => 100000 - 100000");
    assertEquals(Pos(new SmallIntegerExpression(16)), ((LeafElimTree) ((FunctionDefinition) getDefinition("f1")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(Neg(new SmallIntegerExpression(16)), ((LeafElimTree) ((FunctionDefinition) getDefinition("f2")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(Pos(new SmallIntegerExpression(11)), ((LeafElimTree) ((FunctionDefinition) getDefinition("f3")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(Neg(new SmallIntegerExpression(23)), ((LeafElimTree) ((FunctionDefinition) getDefinition("f4")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(Pos(new SmallIntegerExpression(10)), ((LeafElimTree) ((FunctionDefinition) getDefinition("f5")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(Neg(new SmallIntegerExpression(22)), ((LeafElimTree) ((FunctionDefinition) getDefinition("f6")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(Pos(new SmallIntegerExpression(0)),  ((LeafElimTree) ((FunctionDefinition) getDefinition("f7")).getBody()).getExpression().normalize(NormalizeVisitor.Mode.WHNF));
  }
}
