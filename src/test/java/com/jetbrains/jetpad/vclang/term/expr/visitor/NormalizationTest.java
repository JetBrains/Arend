package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NormalizationTest {
  // \function (+) (x y : Nat) : Nat <= elim x | zero => y | suc x' => suc (x' + y)
  private final FunctionDefinition plus;
  // \function (*) (x y : Nat) : Nat <= elim x | zero => zero | suc x' => y + x' * y
  private final FunctionDefinition mul;
  // \function fac (x : Nat) : Nat <= elim x | zero => suc zero | suc x' => suc x' * fac x'
  private final FunctionDefinition fac;
  // \function nelim (z : Nat) (s : Nat -> Nat -> Nat) (x : Nat) : Nat <= elim x | zero => z | suc x' => s x' (nelim z s x')
  private final FunctionDefinition nelim;

  public NormalizationTest() {
    List<Clause> plusClauses = new ArrayList<>(2);
    ElimExpression plusTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), plusClauses, null);
    plus = new FunctionDefinition("+", null, new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 6), Abstract.Definition.Fixity.INFIX, lamArgs(Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, plusTerm);
    plusClauses.add(new Clause(Prelude.ZERO, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Index(0), plusTerm));
    plusClauses.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Suc(BinOp(Index(0), plus, Index(1))), plusTerm));

    List<Clause> mulClauses = new ArrayList<>(2);
    ElimExpression mulTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), mulClauses, null);
    mul = new FunctionDefinition("*", null, new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 7), Abstract.Definition.Fixity.INFIX, lamArgs(Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, mulTerm);
    mulClauses.add(new Clause(Prelude.ZERO, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Zero(), mulTerm));
    mulClauses.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, BinOp(Index(0), plus, BinOp(Index(1), mul, Index(0))), mulTerm));

    List<Clause> facClauses = new ArrayList<>(2);
    ElimExpression facTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), facClauses, null);
    fac = new FunctionDefinition("fac", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, lamArgs(Tele(vars("x"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, facTerm);
    facClauses.add(new Clause(Prelude.ZERO, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Suc(Zero()), facTerm));
    facClauses.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, BinOp(Suc(Index(0)), mul, Apps(DefCall(fac), Index(0))), facTerm));

    List<Clause> nelimClauses = new ArrayList<>(2);
    ElimExpression nelimTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), nelimClauses, null);
    nelim = new FunctionDefinition("nelim", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, lamArgs(Tele(vars("z"), Nat()), Tele(vars("s"), Pi(Nat(), Pi(Nat(), Nat()))), Tele(vars("x"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, nelimTerm);
    nelimClauses.add(new Clause(Prelude.ZERO, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Index(1), nelimTerm));
    nelimClauses.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Index(1), Index(0), Apps(DefCall(nelim), Index(2), Index(1), Index(0))), nelimTerm));
  }

  @Test
  public void normalizeLamId() {
    // normalize( (\x.x) (suc zero) ) = suc zero
    Expression expr = Apps(Lam("x", Index(0)), Suc(Zero()));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamK() {
    // normalize( (\x y. x) (suc zero) ) = \z. suc zero
    Expression expr = Apps(Lam("x", Lam("y", Index(1))), Suc(Zero()));
    assertEquals(Lam("z", Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamKstar() {
    // normalize( (\x y. y) (suc zero) ) = \z. z
    Expression expr = Apps(Lam("x", Lam("y", Index(0))), Suc(Zero()));
    assertEquals(Lam("z", Index(0)), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamKOpen() {
    // normalize( (\x y. x) (suc (var(0))) ) = \z. suc (var(0))
    Expression expr = Apps(Lam("x", Lam("y", Index(1))), Suc(Index(0)));
    assertEquals(Lam("z", Suc(Index(1))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimZero() {
    // normalize( N-elim (suc zero) suc 0 ) = suc zero
    Expression expr = Apps(DefCall(nelim), Suc(Zero()), Suc(), Zero());
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimOne() {
    // normalize( N-elim (suc zero) (\x y. (var(0)) y) (suc zero) ) = var(0) (suc zero)
    Expression expr = Apps(DefCall(nelim), Suc(Zero()), Lam("x", Lam("y", Apps(Index(2), Index(0)))), Suc(Zero()));
    assertEquals(Apps(Index(0), Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimArg() {
    // normalize( N-elim (suc zero) (var(0)) ((\x. x) zero) ) = suc zero
    Expression arg = Apps(Lam("x", Index(0)), Zero());
    Expression expr = Apps(DefCall(nelim), Suc(Zero()), Index(0), arg);
    Expression result = expr.normalize(NormalizeVisitor.Mode.NF);
    assertEquals(Suc(Zero()), result);
  }

  @Test
  public void normalizePlus0a3() {
    // normalize (plus 0 3) = 3
    Expression expr = BinOp(Zero(), plus, Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizePlus3a0() {
    // normalize (plus 3 0) = 3
    Expression expr = BinOp(Suc(Suc(Suc(Zero()))), plus, Zero());
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizePlus3a3() {
    // normalize (plus 3 3) = 6
    Expression expr = BinOp(Suc(Suc(Suc(Zero()))), plus, Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul3a0() {
    // normalize (mul 3 0) = 0
    Expression expr = BinOp(Suc(Suc(Suc(Zero()))), mul, Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul0a3() {
    // normalize (mul 0 3) = 0
    Expression expr = BinOp(Zero(), mul, Suc(Suc(Suc(Zero()))));
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul3a3() {
    // normalize (mul 3 3) = 9
    Expression expr = BinOp(Suc(Suc(Suc(Zero()))), mul, Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero()))))))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeFac3() {
    // normalize (fac 3) = 6
    Expression expr = Apps(DefCall(fac), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  private static Expression typecheckExpression(Expression expr) {
    return typecheckExpression(expr, new ArrayList<Binding>());
  }

  private static Expression typecheckExpression(Expression expr, List<Binding> ctx) {
    ModuleLoader moduleLoader = new ModuleLoader();
    CheckTypeVisitor.Result result = expr.checkType(ctx, null, moduleLoader);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertTrue(result.equations.isEmpty());
    return result.expression;
  }

  @Test
  public void normalizeLet1() {
    // normalize (\let | x => zero \in \let | y = S \in y x) = 1
    Expression expr = typecheckExpression(Let(lets(let("x", Zero())), Let(lets(let("y", Suc())), Apps(Index(0), Index(1)))));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLet2() {
    // normalize (\let | x => zero \in \let | y = S \in y x) = 1
    Expression expr = typecheckExpression(Let(lets(let("x", Suc())), Let(lets(let("y", Zero())), Apps(Index(1), Index(0)))));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetNo() {
    // normalize (\let | x (y z : N) => zero \in x zero) = \lam (z : N) => zero
    Expression expr = typecheckExpression(Let(lets(let("x", lamArgs(Tele(vars("y", "z"), Nat())), Zero())), Apps(Index(0), Zero())));
    assertEquals(Lam("x", Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetElimStuck() {
    // normalize (\let | x (y : N) : N <= \elim y | zero => zero | succ _ => zero \in x <1>) = the same
    List<Clause> clauses = new ArrayList<>();
    ElimExpression elim = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), clauses, null);
    clauses.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Zero(), elim));
    clauses.add(new Clause(Prelude.SUC, nameArgs(Name("_")), Abstract.Definition.Arrow.RIGHT, Zero(), elim));
    Expression expr = typecheckExpression(Let(lets(let("x", lamArgs(Tele(vars("y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, elim)),
            Apps(Index(0), Index(1))), new ArrayList<Binding>(Collections.singleton(new TypedBinding("n", Nat()))));
    assertEquals(expr, expr.normalize(NormalizeVisitor.Mode.NF));
  }


  @Test
  public void normalizeLetElimNoStuck() {
    // normalize (\let | x (y : N) : \Type2 <= \elim y | \Type0 => \Type1 | succ _ => \Type1 \in x zero) = \Type0
    List<Clause> clauses = new ArrayList<>();
    ElimExpression elim = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), clauses, null);
    clauses.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Universe(0), elim));
    clauses.add(new Clause(Prelude.SUC, nameArgs(Name("_")), Abstract.Definition.Arrow.RIGHT, Universe(1), elim));
    Expression expr = typecheckExpression(Let(lets(let("x", lamArgs(Tele(vars("y"), Nat())), Universe(2), Abstract.Definition.Arrow.LEFT, elim)), Apps(Index(0), Zero())));
    assertEquals(Universe(0), expr.normalize(NormalizeVisitor.Mode.NF));
  }
}
