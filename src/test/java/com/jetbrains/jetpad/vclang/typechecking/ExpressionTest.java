package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseExpr;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ExpressionTest {
  @Test
  public void typeCheckingLam() {
    // \x. x : Nat -> Nat
    ModuleLoader moduleLoader = new ModuleLoader();
    parseExpr(moduleLoader, "\\lam x => x").accept(new CheckTypeVisitor(null, null, new ArrayList<Binding>(), moduleLoader, CheckTypeVisitor.Side.RHS), Pi(Nat(), Nat()));
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typeCheckingLamError() {
    // \x. x : Nat -> Nat -> Nat
    ModuleLoader moduleLoader = new ModuleLoader();
    parseExpr(moduleLoader, "\\lam x => x").accept(new CheckTypeVisitor(null, null, new ArrayList<Binding>(), moduleLoader, CheckTypeVisitor.Side.RHS), Pi(Nat(), Pi(Nat(), Nat())));
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typeCheckingId() {
    // \X x. x : (X : Type0) -> X -> X
    Expression type = Pi("X", Universe(0), Pi(Index(0), Index(0)));
    ModuleLoader moduleLoader = new ModuleLoader();
    parseExpr(moduleLoader, "\\lam X x => x").accept(new CheckTypeVisitor(null, null, new ArrayList<Binding>(), moduleLoader, CheckTypeVisitor.Side.RHS), type);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typeCheckingIdError() {
    // \X x. X : (X : Type0) -> X -> X
    Expression type = Pi("X", Universe(0), Pi(Index(0), Index(0)));
    ModuleLoader moduleLoader = new ModuleLoader();
    assertEquals(null, parseExpr(moduleLoader, "\\lam X x => X").accept(new CheckTypeVisitor(null, null, new ArrayList<Binding>(), moduleLoader, CheckTypeVisitor.Side.RHS), type));
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
    assertTrue(moduleLoader.getTypeCheckingErrors().get(0) instanceof TypeMismatchError);
  }

  @Test
  public void typeCheckingApp() {
    // \x y. y (y x) : Nat -> (Nat -> Nat) -> Nat
    Expression type = Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat()));
    ModuleLoader moduleLoader = new ModuleLoader();
    parseExpr(moduleLoader, "\\lam x y => y (y x)").accept(new CheckTypeVisitor(null, null, new ArrayList<Binding>(), moduleLoader, CheckTypeVisitor.Side.RHS), type);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typeCheckingAppIndex() {
    // \x y. y (y x) : N -> (N -> N) -> N
    Expression expr = Lam("x", Lam("y", Apps(Index(0), Apps(Index(0), Index(1)))));
    ModuleLoader moduleLoader = new ModuleLoader();
    expr.checkType(new ArrayList<Binding>(), Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat())), moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typeCheckingAppPiIndex() {
    // \f g. g zero (f zero) : (f : (x : N) -> N x) -> ((x : N) -> N x -> N (f x)) -> N (f zero)
    Expression expr = Lam("f", Lam("g", Apps(Index(0), Zero(), Apps(Index(1), Zero()))));
    Expression type = Pi("f", Pi("x", Nat(), Apps(Nat(), Index(0))), Pi(Pi("x", Nat(), Pi(Apps(Nat(), Index(0)), Apps(Nat(), Apps(Index(1), Index(0))))), Apps(Nat(), Apps(Index(0), Zero()))));
    ModuleLoader moduleLoader = new ModuleLoader();
    expr.checkType(new ArrayList<Binding>(), type, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typeCheckingAppLamPiIndex() {
    // \f h. h (\k -> k (suc zero)) : (f : (g : N -> N) -> N (g zero)) -> ((z : (N -> N) -> N) -> N (f (\x. z (\_. x)))) -> N (f (\x. x))
    Expression expr = Lam("f", Lam("h", Apps(Index(0), Lam("k", Apps(Index(0), Apps(Suc(), Zero()))))));
    Expression type = Pi("f", Pi("g", Pi(Nat(), Nat()), Apps(Nat(), Apps(Index(0), Zero()))), Pi(Pi("z", Pi(Pi(Nat(), Nat()), Nat()), Apps(Nat(), Apps(Index(1), Lam("x", Apps(Index(1), Lam("_", Index(1))))))), Apps(Nat(), Apps(Index(0), Lam("x", Index(0))))));
    ModuleLoader moduleLoader = new ModuleLoader();
    expr.checkType(new ArrayList<Binding>(), type, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typeCheckingInferPiIndex() {
    // (X : Type1) -> X -> X : Type2
    Expression expr = Pi("X", Universe(1), Pi(Index(0), Index(0)));
    ModuleLoader moduleLoader = new ModuleLoader();
    assertEquals(Universe(2), expr.checkType(new ArrayList<Binding>(), null, moduleLoader).type);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typeCheckingUniverse() {
    // (f : Type1 -> Type1) -> f Type1
    Expression expr = Pi("f", Pi(Universe(1), Universe(1)), Apps(Index(0), Universe(1)));
    ModuleLoader moduleLoader = new ModuleLoader();
    assertEquals(null, expr.checkType(new ArrayList<Binding>(), null, moduleLoader));
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
    assertTrue(moduleLoader.getTypeCheckingErrors().get(0) instanceof TypeMismatchError);
  }

  @Test
  public void typeCheckingTwoErrors() {
    // f : Nat -> Nat -> Nat |- f S (f 0 S) : Nat
    Expression expr = Apps(Index(0), Suc(), Apps(Index(0), Zero(), Suc()));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(Nat(), Pi(Nat(), Nat()))));

    ModuleLoader moduleLoader = new ModuleLoader();
    assertNull(expr.checkType(defs, null, moduleLoader));
    assertEquals(2, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typedLambda() {
    // \x:Nat. x : Nat -> Nat
    Expression expr = Lam(lamArgs(Tele(true, vars("x"), Nat())), Index(0));
    ModuleLoader moduleLoader = new ModuleLoader();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), null, moduleLoader);
    assertEquals(Pi(Nat(), Nat()), result.type);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void tooManyLambdasError() {
    // \x y. x : Nat -> Nat
    Expression expr = Lam(lamArgs(Name("x"), Name("y")), Index(1));
    ModuleLoader moduleLoader = new ModuleLoader();
    assertNull(expr.checkType(new ArrayList<Binding>(), Pi(Nat(), Nat()), moduleLoader));
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void typedLambdaExpectedType() {
    // \(X : Type1) x. x : (X : Type0) (X) -> X
    Expression expr = Lam(lamArgs(Tele(vars("X"), Universe(1)), Name("x")), Index(0));
    ModuleLoader moduleLoader = new ModuleLoader();
    assertEquals(expr, expr.checkType(new ArrayList<Binding>(), Pi(args(Tele(vars("X"), Universe(0)), TypeArg(Index(0))), Index(1)), moduleLoader).expression);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void lambdaExpectedError() {
    // \x. x : (Nat -> Nat) -> Nat
    Expression expr = Lam("x", Index(0));
    ModuleLoader moduleLoader = new ModuleLoader();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), Pi(Pi(Nat(), Nat()), Nat()), moduleLoader);
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(null, result);
    assertTrue(moduleLoader.getTypeCheckingErrors().get(0) instanceof TypeMismatchError);
  }

  @Test
  public void lambdaOmegaError() {
    // \x. x x : (Nat -> Nat) -> Nat
    Expression expr = Lam("x", Apps(Index(0), Index(0)));
    ModuleLoader moduleLoader = new ModuleLoader();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), Pi(Pi(Nat(), Nat()), Nat()), moduleLoader);
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(null, result);
    assertTrue(moduleLoader.getTypeCheckingErrors().get(0) instanceof TypeMismatchError);
  }

  @Test
  public void lambdaExpectedError2() {
    // \x. x 0 : (Nat -> Nat) -> Nat -> Nat
    Expression expr = Lam("x", Apps(Index(0), Zero()));
    ModuleLoader moduleLoader = new ModuleLoader();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat())), moduleLoader);
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(null, result);
    assertTrue(moduleLoader.getTypeCheckingErrors().get(0) instanceof TypeMismatchError);
  }

  @Test
  public void letDependentType() {
    // \lam (F : \Pi N -> \Type0) (f : \Pi (x : N) -> F x) => \\let | x => 0 \\in f x");
    Expression expr = Lam(lamArgs(Tele(vars("F"), Pi(Nat(), Universe(0))), Tele(vars("f"), Pi(args(Tele(vars("x"), Nat())), Apps(Index(1), Index(0))))),
            Let(lets(let("x", Zero())), Apps(Index(1), Index(0))));
    ModuleLoader moduleLoader = new ModuleLoader();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), null, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
  }

  @Test
  public void letTypeHasBoundVarError() {
    // \lam (F : \Pi {A : \Type0}  (a : A) -> \Type1) (f : \Pi {A : \Type0} (x : A) -> F x) =>
    //   \let | x (y : Nat) : Nat <= \elim y | zero => zero
    //                                       | suc x' => suc x' \in f x)
    List<Clause> clauses = new ArrayList<>();
    ElimExpression elim = Elim(Index(0), clauses, null);
    clauses.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Zero(), elim));
    clauses.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Suc(), Index(0)), elim));
    Expression expr = Lam(lamArgs(
                    Tele(vars("F"), Pi(args(Tele(false, vars("A"), Universe(0)), Tele(vars("a"), Index(0))), Universe(1))),
                    Tele(vars("f"), Pi(args(Tele(false, vars("A"), Universe(0)), Tele(vars("x"), Index(0))), Apps(Index(2), Index(0))))),
            Let(lets(let("x", lamArgs(Tele(vars("y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, elim)), Apps(Index(1), Index(0))));
    ModuleLoader moduleLoader = new ModuleLoader();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), null, moduleLoader);
    assertEquals(1, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void letArrowType() {
    // \let | x (y : Nat) => Zero \in x : Nat -> Nat
    Expression expr = Let(lets(let("x", lamArgs(Tele(vars("y"), Nat())), Zero())), Index(0));
    ModuleLoader moduleLoader = new ModuleLoader();
    assertEquals(Pi(Nat(), Nat()), expr.checkType(new ArrayList<Binding>(), Pi(Nat(), Nat()), moduleLoader).type);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
  }

  @Test
  public void caseTranslation() {
    ModuleLoader moduleLoader = new ModuleLoader();
    FunctionDefinition def = (FunctionDefinition) parseDef(moduleLoader, "\\function test : Nat => \\case 1 | zero => 0 | suc y => y");
    FunctionDefinition def2 = (FunctionDefinition) parseDef(moduleLoader, "\\function test => \\let | caseF (caseA : Nat) : Nat <= \\elim caseA | zero => 0 | suc y => y \\in caseF 1");
    assertEquals(def.getTerm(), def2.getTerm());
  }

  @Test
  public void caseNoExpectedError() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDef(moduleLoader, "\\function test => \\case 1 | zero => 0 | suc y => y", 1);
  }
}
