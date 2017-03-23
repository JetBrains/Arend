package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.LetClause;
import com.jetbrains.jetpad.vclang.core.expr.PiExpression;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.jetbrains.jetpad.vclang.core.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ComparisonTest extends TypeCheckingTestCase {
  @Test
  public void lambdas() {
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    Expression expr1 = Lam(xy, Reference(xy));
    Expression expr2 = Lam(x, Lam(y, Reference(x)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdas2() {
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink yz = singleParam(true, vars("y", "z"), Nat());
    SingleDependentLink xyz = singleParam(true, vars("x", "y", "z"), Nat());
    Expression expr1 = Lam(xyz, Reference(xyz.getNext()));
    Expression expr2 = Lam(x, Lam(yz, Reference(yz)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdasNotEqual() {
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    Expression expr1 = Lam(xy, Reference(xy.getNext()));
    Expression expr2 = Lam(x, Reference(x));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void lambdasImplicit() {
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    SingleDependentLink yImpl = singleParam(false, vars("y"), Nat());
    SingleDependentLink xImpl = singleParam(false, vars("x"), Nat());
    Expression expr1 = Lam(x, Lam(yImpl, Reference(x)));
    Expression expr2 = Lam(xImpl, Lam(y, Reference(xImpl)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi() {
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    SingleDependentLink _Impl = singleParam(false, Collections.singletonList(null), Nat());
    Binding A = new TypedBinding("A", Pi(Nat(), Pi(Nat(), Universe(0))));
    Expression expr1 = Pi(xy, Pi(_Impl, Apps(Reference(A), Reference(xy), Reference(xy.getNext()))));
    Expression expr2 = Pi(x, Pi(y, Pi(_Impl, Apps(Reference(A), Reference(x), Reference(y)))));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi2() {
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    SingleDependentLink zw = singleParam(true, vars("z", "w"), Nat());
    SingleDependentLink xyz = singleParam(true, vars("x", "y", "z"), Nat());
    SingleDependentLink ts = singleParam(true, vars("t", "s"), Nat());
    SingleDependentLink wts = singleParam(true, vars("w", "t", "s"), Nat());
    Expression expr1 = Pi(xy, Pi(zw, Pi(ts, Nat())));
    Expression expr2 = Pi(xyz, Pi(wts, Nat()));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi3() {
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    SingleDependentLink zw = singleParam(true, vars("z", "w"), Nat());
    SingleDependentLink ts = singleParam(true, vars("t", "s"), Nat());
    Expression expr1 = Pi(xy, Pi(zw, Pi(ts, Nat())));
    Expression expr2 = Pi(singleParam(null, Nat()), Pi(singleParam(null, Nat()), Pi(singleParam(null, Nat()), Pi(singleParam(null, Nat()), Pi(singleParam(null, Nat()), Pi(singleParam(null, Nat()), Nat()))))));
    assertEquals(expr1, expr2);
  }

  @Test
  public void piNotEquals() {
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    SingleDependentLink xyz = singleParam(true, vars("x", "y", "z"), Nat());
    SingleDependentLink zw = singleParam(true, vars("z", "w"), Nat());
    SingleDependentLink w = singleParam("w", Pi(singleParam(null, Nat()), Nat()));
    Expression expr1 = Pi(xy, Pi(zw, Nat()));
    Expression expr2 = Pi(xyz, Pi(w, Nat()));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void compareLeq() {
    // Expression expr1 = Pi("X", UniverseOld(1), Pi(Index(0), Index(0)));
    // Expression expr2 = Pi("X", UniverseOld(0), Pi(Index(0), Index(0)));
    Expression expr1 = Universe(0);
    Expression expr2 = Universe(1);
    assertTrue(compare(expr1, expr2, Equations.CMP.LE));
  }

  @Test
  public void compareNotLeq() {
    SingleDependentLink X0 = singleParam("X", Universe(0));
    SingleDependentLink X1 = singleParam("X", Universe(1));
    Expression expr1 = Pi(X0, Pi(singleParam(null, Reference(X0)), Reference(X0)));
    Expression expr2 = Pi(X1, Pi(singleParam(null, Reference(X1)), Reference(X1)));
    assertFalse(compare(expr1, expr2, Equations.CMP.LE));
  }

  @Test
  public void letsNotEqual() {
    SingleDependentLink y = singleParam("y", Nat());
    LetClause let1 = let("x", y, Nat(), Reference(y));
    Expression expr1 = Let(lets(let1), Apps(Reference(let1), Zero()));
    SingleDependentLink y_ = singleParam("y", Universe(0));
    LetClause let2 = let("x", y_, Universe(0), Reference(y_));
    Expression expr2 = Let(lets(let2), Apps(Reference(let2), Nat()));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void letsTelesEqual() {
    DependentLink A = param(Universe(0));
    SingleDependentLink yz = singleParam(true, vars("y", "z"), Reference(A));
    SingleDependentLink y = singleParam("y", Reference(A));
    SingleDependentLink z = singleParam("z", Reference(A));
    LetClause let1 = let("x", yz, Universe(0), Reference(A));
    Expression expr1 = Let(lets(let1), Apps(Reference(let1), Zero()));
    LetClause let2 = let("x", Arrays.asList(y, z), Universe(0), Reference(A));
    Expression expr2 = Let(lets(let2), Apps(Reference(let2), Zero()));
    assertEquals(expr1, expr2);
    assertEquals(expr2, expr1);
  }

  @Test
  public void letsTelesNotEqual() {
    DependentLink A = param(Universe(0));
    SingleDependentLink yz = singleParam(true, vars("y", "z"), Reference(A));
    SingleDependentLink y = singleParam("y", Reference(A));
    SingleDependentLink z = singleParam("z", Reference(y));
    LetClause let1 = let("x", yz, Universe(0), Reference(A));
    Expression expr1 = Let(lets(let1), Apps(Reference(let1), Zero()));
    LetClause let2 = let("x", Arrays.asList(y, z), Universe(0), Reference(A));
    Expression expr2 = Let(lets(let2), Apps(Reference(let2), Zero()));
    assertNotEquals(expr1, expr2);
    assertNotEquals(expr2, expr1);
  }

  @Test
  public void letsNotEquiv() {
    LetClause let1 = let("x", EmptyDependentLink.getInstance(), Universe(1), Universe(0));
    Expression expr1 = Let(lets(let1), Reference(let1));
    LetClause let2 = let("x", EmptyDependentLink.getInstance(), Universe(2), Universe(1));
    Expression expr2 = Let(lets(let2), Reference(let2));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void letsLess() {
    Expression expr1 = Let(lets(let("x", Nat())), Universe(0));
    Expression expr2 = Let(lets(let("x", Nat())), Universe(1));
    assertTrue(compare(expr1, expr2, Equations.CMP.LE));
  }

  @Test
  public void letsNested() {
    Definition def1 = typeCheckDef("\\function test => \\let x => 0 \\in \\let y => 1 \\in zero");
    Definition def2 = typeCheckDef("\\function test => \\let | x => 0 | y => 1 \\in zero");
    assertEquals(((FunctionDefinition) def1).getElimTree(), ((FunctionDefinition) def2).getElimTree());
  }

  @Test
  public void etaLam() {
    PiExpression type = Pi(singleParam(null, Nat()), DataCall(Prelude.PATH, new Level(0), new Level(1),
            Lam(singleParam("i", Interval()), Nat()), Zero(), Zero()));
    CheckTypeVisitor.Result result1 = typeCheckExpr("\\lam a x => path (\\lam i => a x @ i)", Pi(singleParam(null, type), type));
    CheckTypeVisitor.Result result2 = typeCheckExpr("\\lam a => a", Pi(singleParam(null, type), type));
    assertEquals(result2.expression, result1.expression);
  }

  @Test
  public void etaLamBody() {
    PiExpression type = Pi(singleParam(null, Nat()), DataCall(Prelude.PATH, new Level(0), new Level(1),
      Lam(singleParam("i", Interval()), Nat()), Zero(), Zero()));
    CheckTypeVisitor.Result result1 = typeCheckExpr("\\lam a x => path (\\lam i => a x @ i)", Pi(singleParam(null, type), type));
    CheckTypeVisitor.Result result2 = typeCheckExpr("\\lam a => \\lam x => a x", Pi(singleParam(null, type), type));
    assertEquals(result2.expression, result1.expression);
  }

  @Test
  public void etaPath() {
    SingleDependentLink x = singleParam("x", Nat());
    DataCallExpression type = DataCall(Prelude.PATH, new Level(0), new Level(1),
            Lam(singleParam("i", Interval()), Pi(singleParam(null, Nat()), Nat())), Lam(x, Reference(x)), Lam(x, Reference(x)));
    CheckTypeVisitor.Result result1 = typeCheckExpr("\\lam a => path (\\lam i x => (a @ i) x)", Pi(singleParam(null, type), type));
    CheckTypeVisitor.Result result2 = typeCheckExpr("\\lam a => a", Pi(singleParam(null, type), type));
    assertEquals(result2.expression, result1.expression);
  }
}
