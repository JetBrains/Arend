package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LetClause;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.*;

public class ComparisonTest {

  @Test
  public void lambdas() {
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    Expression expr1 = Lam(params(param("x", Nat()), y), Reference(x));
    Expression expr2 = Lam(x, Lam(y, Reference(x)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdas2() {
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink xy = param(true, vars("x", "y"), Nat());
    Expression expr1 = Lam(xy, Reference(xy));
    Expression expr2 = Lam(x, Lam(y, Reference(x)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdas3() {
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Nat());
    DependentLink xy = param(true, vars("x", "y"), Nat());
    Expression expr1 = Lam(params(xy, z), Reference(xy.getNext()));
    Expression expr2 = Lam(x, Lam(params(y, z), Reference(y)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdasNotEqual() {
    DependentLink x = param("x", Nat());
    DependentLink xy = param(true, vars("x", "y"), Nat());
    Expression expr1 = Lam(xy, Reference(xy.getNext()));
    Expression expr2 = Lam(x, Reference(x));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void lambdasImplicit() {
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink yImpl = param(false, "y", Nat());
    DependentLink xImpl = param(false, "x", Nat());
    Expression expr1 = Lam(params(x, yImpl), Reference(x));
    Expression expr2 = Lam(params(xImpl, y), Reference(xImpl));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi() {
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink xy = param(true, vars("x", "y"), Nat());
    DependentLink _Impl = param(false, (String) null, Nat());
    Binding A = new TypedBinding("A", Pi(Nat(), Pi(Nat(), Universe(0))));
    Expression expr1 = Pi(params(xy, _Impl), Apps(Reference(A), Reference(xy), Reference(xy.getNext())));
    Expression expr2 = Pi(params(x, y), Pi(_Impl, Apps(Reference(A), Reference(x), Reference(y))));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi2() {
    DependentLink xy = param(true, vars("x", "y"), Nat());
    DependentLink zw = param(true, vars("z", "w"), Nat());
    DependentLink xyz = param(true, vars("x", "y", "z"), Nat());
    DependentLink ts = param(true, vars("t", "s"), Nat());
    DependentLink wts = param(true, vars("w", "t", "s"), Nat());
    Expression expr1 = Pi(params(xy, zw, ts), Nat());
    Expression expr2 = Pi(params(xyz, wts), Nat());
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi3() {
    DependentLink xy = param(true, vars("x", "y"), Nat());
    DependentLink zw = param(true, vars("z", "w"), Nat());
    DependentLink ts = param(true, vars("t", "s"), Nat());
    Expression expr1 = Pi(params(xy, zw, ts), Nat());
    Expression expr2 = Pi(param(Nat()), Pi(param(Nat()), Pi(param(Nat()), Pi(param(Nat()), Pi(param(Nat()), Pi(param(Nat()), Nat()))))));
    assertEquals(expr1, expr2);
  }

  @Test
  public void piNotEquals() {
    DependentLink xy = param(true, vars("x", "y"), Nat());
    DependentLink xyz = param(true, vars("x", "y", "z"), Nat());
    DependentLink zw = param(true, vars("z", "w"), Nat());
    DependentLink w = param("w", Pi(param(Nat()), Nat()));
    Expression expr1 = Pi(params(xy, zw), Nat());
    Expression expr2 = Pi(params(xyz, w), Nat());
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
    DependentLink X0 = param("X", Universe(0));
    DependentLink X1 = param("X", Universe(1));
    Expression expr1 = Pi(X0, Pi(param(Reference(X0)), Reference(X0)));
    Expression expr2 = Pi(X1, Pi(param(Reference(X1)), Reference(X1)));
    assertFalse(compare(expr1, expr2, Equations.CMP.LE));
  }

  @Test
  public void letsNotEqual() {
    DependentLink y = param("y", Nat());
    LetClause let1 = let("x", y, Nat(), Reference(y));
    Expression expr1 = Let(lets(let1), Apps(Reference(let1), Zero()));
    DependentLink y_ = param("y", Universe(0));
    LetClause let2 = let("x", y_, Universe(0), Reference(y_));
    Expression expr2 = Let(lets(let2), Apps(Reference(let2), Nat()));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void letsTelesEqual() {
    DependentLink A = param(Universe(0));
    DependentLink yz = param(true, vars("y", "z"), Reference(A));
    DependentLink y = param("y", Reference(A));
    DependentLink z = param("z", Reference(A));
    LetClause let1 = let("x", yz, Universe(0), Reference(A));
    Expression expr1 = Let(lets(let1), Apps(Reference(let1), Zero()));
    LetClause let2 = let("x", params(y, z), Universe(0), Reference(A));
    Expression expr2 = Let(lets(let2), Apps(Reference(let2), Zero()));
    assertEquals(expr1, expr2);
    assertEquals(expr2, expr1);
  }

  @Test
  public void letsTelesNotEqual() {
    DependentLink A = param(Universe(0));
    DependentLink yz = param(true, vars("y", "z"), Reference(A));
    DependentLink y = param("y", Reference(A));
    DependentLink z = param("z", Reference(y));
    LetClause let1 = let("x", yz, Universe(0), Reference(A));
    Expression expr1 = Let(lets(let1), Apps(Reference(let1), Zero()));
    LetClause let2 = let("x", params(y, z), Universe(0), Reference(A));
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
    Expression type = Pi(param(Nat()), Apps(Prelude.PATH.getDefCall(), ZeroLvl(), Fin(Suc(Zero())), Lam(param("i", Preprelude.INTERVAL.getDefCall()), Nat()), Zero(), Zero()));
    CheckTypeVisitor.Result result1 = typeCheckExpr("\\lam a x => path (\\lam i => a x @ i)", Pi(param(type), type));
    assertNotNull(result1);
    CheckTypeVisitor.Result result2 = typeCheckExpr("\\lam a => a", Pi(param(type), type));
    assertNotNull(result2);
    assertEquals(result2.expression, result1.expression);
  }

  @Test
  public void etaPath() {
    DependentLink x = param("x", Nat());
    Expression type = Apps(Prelude.PATH.getDefCall(), ZeroLvl(), Fin(Suc(Zero())), Lam(param(Preprelude.INTERVAL.getDefCall()), Pi(param(Nat()), Nat())), Lam(x, Reference(x)), Lam(x, Reference(x)));
    CheckTypeVisitor.Result result1 = typeCheckExpr("\\lam a => path (\\lam i x => (a @ i) x)", Pi(param(type), type));
    assertNotNull(result1);
    CheckTypeVisitor.Result result2 = typeCheckExpr("\\lam a => a", Pi(param(type), type));
    assertNotNull(result2);
    assertEquals(result2.expression, result1.expression);
  }
}
