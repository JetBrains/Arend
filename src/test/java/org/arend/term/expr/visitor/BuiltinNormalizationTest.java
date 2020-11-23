package org.arend.term.expr.visitor;

import org.arend.core.context.binding.TypedBinding;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.arend.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class BuiltinNormalizationTest extends TypeCheckingTestCase {
  private static Expression funCall(FunctionDefinition definition, Expression arg1, Expression arg2) {
    List<Expression> args = new ArrayList<>(2);
    args.add(arg1);
    args.add(arg2);
    return FunCallExpression.make(definition, Sort.STD, args);
  }

  private static IntegerExpression val(int x) {
    return new SmallIntegerExpression(x);
  }

  private static Expression plus(Expression arg1, Expression arg2) {
    return funCall(Prelude.PLUS, arg1, arg2);
  }

  private static Expression minus(Expression arg1, Expression arg2) {
    return funCall(Prelude.MINUS, arg1, arg2);
  }

  @Test
  public void testPlusConst() {
    // 5 + 3 = 8
    assertEquals(val(8), plus(val(5), val(3)).normalize(NormalizationMode.WHNF));
    // 5 + 0 = 5
    assertEquals(val(5), plus(val(5), val(0)).normalize(NormalizationMode.WHNF));
  }

  @Test
  public void testVarPlusConst() {
    ReferenceExpression x = new ReferenceExpression(new TypedBinding("x", Nat()));
    // x + 3 = suc (x + 2)
    assertEquals(Suc(plus(x, val(2))), plus(x, val(3)).normalize(NormalizationMode.WHNF));
    // x + 3 = suc (suc (suc x))
    assertEquals(Suc(Suc(Suc(x))), plus(x, val(3)).normalize(NormalizationMode.NF));
  }

  @Test
  public void testVarPlusVar() {
    ReferenceExpression x = new ReferenceExpression(new TypedBinding("x", Nat()));
    ReferenceExpression y = new ReferenceExpression(new TypedBinding("y", Nat()));
    // x + suc (suc (suc y)) = suc (x + suc (suc y))
    assertEquals(Suc(plus(x, Suc(Suc(y)))), plus(x, Suc(Suc(Suc(y)))).normalize(NormalizationMode.WHNF));
    // x + suc (suc (suc y)) = suc (suc (suc (x + y)))
    assertEquals(Suc(Suc(Suc(plus(x, y)))), plus(x, Suc(Suc(Suc(y)))).normalize(NormalizationMode.NF));
  }

  @Test
  public void testMinusConst() {
    // 0 - 0 = 0
    assertEquals(Pos(val(0)), minus(Zero(), Zero()).normalize(NormalizationMode.WHNF));
    // 0 - 3 = -3
    assertEquals(Neg(val(3)), minus(Zero(), val(3)).normalize(NormalizationMode.WHNF));
    // 5 - 3 = +2
    assertEquals(Pos(val(2)), minus(val(5), val(3)).normalize(NormalizationMode.WHNF));
    // 3 - 5 = -2
    assertEquals(Neg(val(2)), minus(val(3), val(5)).normalize(NormalizationMode.WHNF));
  }

  @Test
  public void testConstMinusVar() {
    ReferenceExpression x = new ReferenceExpression(new TypedBinding("x", Nat()));
    // 5 - (x + 3) = 2 - x
    assertEquals(minus(val(2), x), minus(val(5), plus(x, val(3))).normalize(NormalizationMode.WHNF));
    // 3 - (x + 3) = 0 - x
    assertEquals(minus(Zero(), x), minus(val(3), plus(x, val(3))).normalize(NormalizationMode.WHNF));
    // 3 - (x + 5) = -(x + 2)
    assertEquals(Neg(plus(x, val(2))), minus(val(3), plus(x, val(5))).normalize(NormalizationMode.WHNF));
    // 3 - (x + 5) = -(suc (suc x))
    assertEquals(Neg(Suc(Suc(x))), minus(val(3), plus(x, val(5))).normalize(NormalizationMode.NF));
  }

  @Test
  public void testSucMinusConst() {
    ReferenceExpression x = new ReferenceExpression(new TypedBinding("x", Nat()));
    // (x + 3) - 0 = +(suc (x + 2))
    assertEquals(Pos(Suc(plus(x, val(2)))), minus(plus(x, val(3)), Zero()).normalize(NormalizationMode.WHNF));
    // (x + 3) - 0 = +(suc (suc (suc x)))
    assertEquals(Pos(Suc(Suc(Suc(x)))), minus(plus(x, val(3)), Zero()).normalize(NormalizationMode.NF));
    // (x + 5) - 3 = +(suc (x + 1))
    assertEquals(Pos(Suc(plus(x, val(1)))), minus(plus(x, val(5)), val(3)).normalize(NormalizationMode.WHNF));
    // (x + 5) - 3 = +(suc (suc x))
    assertEquals(Pos(Suc(Suc(x))), minus(plus(x, val(5)), val(3)).normalize(NormalizationMode.NF));
    // (x + 3) - 5 = x - 2
    assertEquals(minus(x, val(2)), minus(plus(x, val(3)), val(5)).normalize(NormalizationMode.WHNF));
    // (x + 3) - 3 = x - 0
    assertEquals(minus(x, Zero()), minus(plus(x, val(3)), val(3)).normalize(NormalizationMode.WHNF));
    // (x + 4) - 3 = +(suc x)
    assertEquals(Pos(Suc(x)), minus(plus(x, val(4)), val(3)).normalize(NormalizationMode.WHNF));
  }

  @Test
  public void testSucMinusSuc() {
    ReferenceExpression x = new ReferenceExpression(new TypedBinding("x", Nat()));
    ReferenceExpression y = new ReferenceExpression(new TypedBinding("y", Nat()));
    // (x + 5) - (y + 3) = suc (x + 1) - y
    assertEquals(minus(Suc(plus(x, val(1))), y), minus(plus(x, val(5)), plus(y, val(3))).normalize(NormalizationMode.WHNF));
    // (x + 5) - (y + 3) = suc (suc x) - y
    assertEquals(minus(Suc(Suc(x)), y), minus(plus(x, val(5)), plus(y, val(3))).normalize(NormalizationMode.NF));
    // (x + 3) - (y + 5) = x - (y + 2)
    assertEquals(minus(x, plus(y, val(2))), minus(plus(x, val(3)), plus(y, val(5))).normalize(NormalizationMode.WHNF));
    // (x + 3) - (y + 5) = x - suc (suc y)
    assertEquals(minus(x, Suc(Suc(y))), minus(plus(x, val(3)), plus(y, val(5))).normalize(NormalizationMode.NF));
    // (x + 3) - (y + 3) = x - y
    assertEquals(minus(x, y), minus(plus(x, val(3)), plus(y, val(3))).normalize(NormalizationMode.WHNF));
  }

  @Test
  public void testVarMinusConst() {
    ReferenceExpression x = new ReferenceExpression(new TypedBinding("x", Nat()));
    ReferenceExpression y = new ReferenceExpression(new TypedBinding("y", Nat()));
    // x - 0 = x - 0
    assertEquals(minus(x, Zero()), minus(x, Zero()).normalize(NormalizationMode.WHNF));
    // x - (y + 3) = x - (y + 3)
    Expression expr = minus(x, plus(y, val(3)));
    assertEquals(expr, expr.normalize(NormalizationMode.WHNF));
  }

  @Test
  public void testDivMod() {
    ReferenceExpression x = new ReferenceExpression(new TypedBinding("x", Nat()));
    // x / 0 = x
    assertEquals(x, funCall(Prelude.DIV, x, Zero()).normalize(NormalizationMode.WHNF));
    // x % 0 = x
    assertEquals(x, funCall(Prelude.MOD, x, Zero()).normalize(NormalizationMode.WHNF));
    // x / 1 = x
    assertEquals(x, funCall(Prelude.DIV, x, Suc(Zero())).normalize(NormalizationMode.WHNF));
    // x % 1 = 0
    assertEquals(Zero(), funCall(Prelude.MOD, x, Suc(Zero())).normalize(NormalizationMode.WHNF));
    // divMod 100000 378 = (264,208)
    assertEquals(new TupleExpression(Arrays.asList(val(264), val(208)), finDivModType(val(378))), funCall(Prelude.DIV_MOD, val(100000), val(378)).normalize(NormalizationMode.WHNF));
    // div 1000000000000 56789 = 17609044
    assertEquals(new BigIntegerExpression(new BigInteger("17609044")), funCall(Prelude.DIV, new BigIntegerExpression(new BigInteger("1000000000000")), val(56789)).normalize(NormalizationMode.WHNF));
    // mod 1000000000000 98765 = 29340
    assertEquals(new BigIntegerExpression(new BigInteger("29340")), funCall(Prelude.MOD, new BigIntegerExpression(new BigInteger("1000000000000")), val(98765)).normalize(NormalizationMode.WHNF));
  }
}
