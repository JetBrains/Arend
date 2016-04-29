package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LetClause;
import com.jetbrains.jetpad.vclang.term.expr.LetExpression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertNotNull;

public class PrettyPrintingTest {
  @Test
  public void prettyPrintingLam() {
    // \x. x x
    DependentLink x = param("x", Nat());
    Expression expr = Lam(x, Apps(Reference(x), Reference(x)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingLam2() {
    // \x. x (\y. y x) (\z w. x w z)
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Nat());
    DependentLink w = param("w", Nat());
    Expression expr = Lam(x, Apps(Reference(x), Lam(y, Apps(Reference(y), Reference(x))), Lam(params(z, w), Apps(Reference(x), Reference(w), Reference(z)))));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingU() {
    // (X : Type0) -> X -> X
    DependentLink X = param("x", Universe(0));
    Expression expr = Pi(X, Pi(param(Reference(X)), Reference(X)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingPi() {
    // (x y : N) (z w : N -> N) -> ((s : N) -> N (z s) (w x)) -> N
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Pi(param(Nat()), Nat()));
    DependentLink w = param("w", Pi(param(Nat()), Nat()));
    DependentLink s = param("s", Nat());
    Expression expr = Pi(params(x, y, z, w), Pi(param(Pi(s, Apps(Nat(), Apps(Reference(z), Reference(s)), Apps(Reference(w), Reference(x))))), Nat()));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingFunDef() {
    // f (X : Type0) (x : X) : X => x;
    List<Concrete.Argument> arguments = new ArrayList<>(2);
    arguments.add(cTele(cvars("X"), cUniverse(0)));
    arguments.add(cTele(cvars("x"), cVar("X")));
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(ConcreteExpressionFactory.POSITION, "f", Abstract.Binding.DEFAULT_PRECEDENCE, arguments, cVar("X"), Abstract.Definition.Arrow.RIGHT, cLam("X", cLam("x", cVar("x"))), false, null, Collections.<Concrete.Statement>emptyList());
    def.accept(new PrettyPrintVisitor(new StringBuilder(), new ArrayList<String>(), 0), null);
  }

  @Test
  public void prettyPrintingLet() {
    // \let x {A : Type0} (y ; A) : A => y \in x Zero()
    DependentLink A = param("A", Universe(0));
    DependentLink y = param("y", Reference(A));
    LetClause clause = let("x", params(A, y), Reference(A));
    LetExpression expr = Let(lets(clause), Apps(Reference(clause), Zero()));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingLetEmpty() {
    // \let x {A : Type0} (y ; A) : A <= \elim y
    DependentLink A = param("A", Universe(0));
    DependentLink y = param("y", Reference(A));
    LetClause clause = let("x", params(A, y), Reference(A), EmptyElimTreeNode.getInstance());
    LetExpression expr = Let(lets(clause), Apps(Reference(clause), Zero()));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingPatternDataDef() {
    Concrete.Definition def = parseDef("\\data LE (n m : Nat) | LE (zero) m => LE-zero | LE (suc n) (suc m) => LE-suc (LE n m)");
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC), null);
  }

  @Test
  public void prettyPrintingDataWithConditions() {
    Concrete.Definition def = parseDef("\\data Z | neg Nat | pos Nat \\with | pos zero => neg zero");
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC), null);
  }
}
