package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.LetClause;
import com.jetbrains.jetpad.vclang.core.expr.LetExpression;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertNotNull;

public class PrettyPrintingTest extends TypeCheckingTestCase {
  @Test
  public void prettyPrintingLam() {
    // \x. x x
    SingleDependentLink x = singleParam("x", Pi(Nat(), Nat()));
    Expression expr = Lam(x, Apps(Ref(x), Ref(x)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingLam2() {
    // \x. x (\y. y x) (\z w. x w z)
    SingleDependentLink x = singleParam("x", Pi(Nat(), Pi(Nat(), Nat())));
    SingleDependentLink y = singleParam("y", Pi(Nat(), Nat()));
    SingleDependentLink zw = singleParam(true, vars("z", "w"), Nat());
    Expression expr = Lam(x, Apps(Ref(x), Lam(y, Apps(Ref(y), Ref(x))), Lam(zw, Apps(Ref(x), Ref(zw.getNext()), Ref(zw)))));
    expr.prettyPrint(new StringBuilder(), new ArrayList<>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingU() {
    // (X : Type0) -> X -> X
    SingleDependentLink X = singleParam("x", Universe(0));
    Expression expr = Pi(X, Pi(singleParam(null, Ref(X)), Ref(X)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingPi() {
    // (t : Nat -> Nat -> Nat) (x y : Nat) (z w : Nat -> Nat) -> ((s : Nat) -> t (z s) (w x)) -> Nat
    SingleDependentLink t = singleParam("t", Pi(Nat(), Pi(Nat(), Nat())));
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    SingleDependentLink zw = singleParam(true, vars("z", "w"), Pi(singleParam(null, Nat()), Nat()));
    SingleDependentLink s = singleParam("s", Nat());
    Expression expr = Pi(t, Pi(xy, Pi(zw, Pi(singleParam(null, Pi(s, Apps(Ref(t), Apps(Ref(zw), Ref(s)), Apps(Ref(zw.getNext()), Ref(xy))))), Nat()))));
    expr.prettyPrint(new StringBuilder(), new ArrayList<>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingFunDef() {
    // f (X : Type0) (x : X) : X => x;
    List<Concrete.Parameter> arguments = new ArrayList<>(2);
    Concrete.LocalVariable X = ref("X");
    Concrete.LocalVariable x = ref("X");
    arguments.add(cTele(cvars(X), cUniverseStd(0)));
    arguments.add(cTele(cvars(x), cVar(X)));
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(ConcreteExpressionFactory.POSITION, "f", Abstract.Precedence.DEFAULT, arguments, cVar(X), body(cVar(x)), Collections.emptyList());
    def.accept(new PrettyPrintVisitor(new StringBuilder(), 0), null);
  }

  @Test
  public void prettyPrintingLet() {
    // \let x {A : Type0} (y : A) : A => y \in x Zero()
    SingleDependentLink A = singleParam("A", Universe(0));
    SingleDependentLink y = singleParam("y", Ref(A));
    LetClause clause = let("x", Lam(A, Lam(y, Ref(y))));
    LetExpression expr = new LetExpression(lets(clause), Apps(Ref(clause), Zero()));
    expr.prettyPrint(new StringBuilder(), new ArrayList<>(), Abstract.Expression.PREC, 0);
  }

  @Test
  public void prettyPrintingPatternDataDef() {
    Concrete.Definition def = parseDef("\\data LE Nat Nat \\with | zero, m => LE-zero | suc n, suc m => LE-suc (LE n m)");
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), Abstract.Expression.PREC), null);
  }

  @Test
  public void prettyPrintingDataWithConditions() {
    Concrete.Definition def = parseDef("\\data Z | neg Nat | pos Nat { zero => neg zero }");
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), Abstract.Expression.PREC), null);
  }
}
