package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NormalizationTest extends TypeCheckingTestCase {
  // \function (+) (x y : Nat) : Nat <= elim x | zero => y | suc x' => suc (x' + y)
  private final FunctionDefinition plus;
  // \function (*) (x y : Nat) : Nat <= elim x | zero => zero | suc x' => y + x' * y
  private final FunctionDefinition mul;
  // \function fac (x : Nat) : Nat <= elim x | zero => suc zero | suc x' => suc x' * fac x'
  private final FunctionDefinition fac;
  // \function nelim (z : Nat) (s : Nat -> Nat -> Nat) (x : Nat) : Nat <= elim x | zero => z | suc x' => s x' (nelim z s x')
  private final FunctionDefinition nelim;

  private DataDefinition bdList;
  private Constructor bdNil;
  private Constructor bdCons;
  private Constructor bdSnoc;

  public NormalizationTest() throws IOException {
    DependentLink xPlus = param("x", Nat());
    DependentLink yPlus = param("y", Nat());
    plus = new FunctionDefinition(null);
    plus.setParameters(params(xPlus, yPlus));
    plus.setResultType(Nat());
    plus.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink xPlusMinusOne = param("x'", Nat());
    ElimTreeNode plusElimTree = top(xPlus, branch(xPlus, tail(yPlus),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Ref(yPlus)),
        clause(Prelude.SUC, xPlusMinusOne, Suc(FunCall(plus, Sort.SET0, Ref(xPlusMinusOne), Ref(yPlus))))));
    plus.setElimTree(plusElimTree);
    plus.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink xMul = param("x", Nat());
    DependentLink yMul = param("y", Nat());
    mul = new FunctionDefinition(null);
    mul.setParameters(params(xMul, yMul));
    mul.setResultType(Nat());
    mul.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    DependentLink xMulMinusOne = param("x'", Nat());
    ElimTreeNode mulElimTree = top(xMul, branch(xMul, tail(yMul),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Zero()),
        clause(Prelude.SUC, xMulMinusOne, FunCall(plus, Sort.SET0, Ref(yMul), FunCall(mul, Sort.SET0, Ref(xMulMinusOne), Ref(yMul))))
    ));
    mul.setElimTree(mulElimTree);
    mul.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink xFac = param("x", Nat());
    fac = new FunctionDefinition(null);
    fac.setParameters(xFac);
    fac.setResultType(Nat());
    fac.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    DependentLink xFacMinusOne = param("x'", Nat());
    ElimTreeNode facElimTree = top(xFac, branch(xFac, tail(),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Suc(Zero())),
        clause(Prelude.SUC, xFacMinusOne, FunCall(mul, Sort.SET0, Suc(Ref(xFacMinusOne)), FunCall(fac, Sort.SET0, Ref(xFacMinusOne))))
    ));
    fac.setElimTree(facElimTree);
    fac.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink zNElim = param("z", Nat());
    DependentLink sNElim = param("s", Pi(Nat(), Pi(Nat(), Nat())));
    DependentLink xNElim = param("x", Nat());
    nelim = new FunctionDefinition(null);
    nelim.setParameters(params(zNElim, sNElim, xNElim));
    nelim.setResultType(Nat());
    nelim.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    DependentLink xNElimMinusOne = param("x'", Nat());
    ElimTreeNode nelimElimTree = top(zNElim, branch(xNElim, tail(),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Ref(zNElim)),
        clause(Prelude.SUC, xNElimMinusOne, Apps(Ref(sNElim), Ref(xNElimMinusOne), FunCall(nelim, Sort.SET0, Ref(zNElim), Ref(sNElim), Ref(xNElimMinusOne))))
    ));
    nelim.setElimTree(nelimElimTree);
    nelim.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
  }

  private void initializeBDList() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\data BD-list (A : \\Set0) | nil | cons A (BD-list A) | snoc (BD-list A) A\n" +
        "  \\with | snoc (cons x xs) x => cons x (snoc xs x) | snoc nil x => cons x nil\n"
    );
    bdList = (DataDefinition) result.getDefinition("BD-list");
    bdNil = bdList.getConstructor("nil");
    bdCons = bdList.getConstructor("cons");
    bdSnoc = bdList.getConstructor("snoc");
  }

  @Test
  public void normalizeLamId() {
    // normalize( (\x.x) (suc zero) ) = suc zero
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = Apps(Lam(x, Ref(x)), Suc(Zero()));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamK() {
    // normalize( (\x y. x) (suc zero) ) = \z. suc zero
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    Expression expr = Apps(Lam(x, Lam(y, Ref(x))), Suc(Zero()));
    assertEquals(Lam(z, Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamKstar() {
    // normalize( (\x y. y) (suc zero) ) = \z. z
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    Expression expr = Apps(Lam(x, Lam(y, Ref(y))), Suc(Zero()));
    assertEquals(Lam(z, Ref(z)), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamKOpen() {
    // normalize( (\x y. x) (suc (var(0))) ) = \z. suc (var(0))
    DependentLink var0 = param("var0", Universe(0));
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    SingleDependentLink z = singleParam("z", Nat());
    Expression expr = Apps(Lam(xy, Ref(xy)), Suc(Ref(var0)));
    assertEquals(Lam(z, Suc(Ref(var0))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimZero() {
    // normalize( N-elim (suc zero) (\x. suc x) 0 ) = suc zero
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = FunCall(nelim, Sort.SET0, Suc(Zero()), Lam(x, Suc(Ref(x))), Zero());
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimOne() {
    // normalize( N-elim (suc zero) (\x y. (var(0)) y) (suc zero) ) = var(0) (suc zero)
    DependentLink var0 = param("var0", Pi(Nat(), Nat()));
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    Expression expr = FunCall(nelim, Sort.SET0, Suc(Zero()), Lam(x, Lam(y, Apps(Ref(var0), Ref(y)))), Suc(Zero()));
    assertEquals(Apps(Ref(var0), Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimArg() {
    // normalize( N-elim (suc zero) (var(0)) ((\x. x) zero) ) = suc zero
    DependentLink var0 = param("var0", Universe(0));
    SingleDependentLink x = singleParam("x", Nat());
    Expression arg = Apps(Lam(x, Ref(x)), Zero());
    Expression expr = FunCall(nelim, Sort.SET0, Suc(Zero()), Ref(var0), arg);
    Expression result = expr.normalize(NormalizeVisitor.Mode.NF);
    assertEquals(Suc(Zero()), result);
  }

  @Test
  public void normalizePlus0a3() {
    // normalize (plus 0 3) = 3
    Expression expr = FunCall(plus, Sort.SET0, Zero(), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizePlus3a0() {
    // normalize (plus 3 0) = 3
    Expression expr = FunCall(plus, Sort.SET0, Suc(Suc(Suc(Zero()))), Zero());
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizePlus3a3() {
    // normalize (plus 3 3) = 6
    Expression expr = FunCall(plus, Sort.SET0, Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul3a0() {
    // normalize (mul 3 0) = 0
    Expression expr = FunCall(mul, Sort.SET0, Suc(Suc(Suc(Zero()))), Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul0a3() {
    // normalize (mul 0 3) = 0
    Expression expr = FunCall(mul, Sort.SET0, Zero(), Suc(Suc(Suc(Zero()))));
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul3a3() {
    // normalize (mul 3 3) = 9
    Expression expr = FunCall(mul, Sort.SET0, Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero()))))))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeFac3() {
    // normalize (fac 3) = 6
    Expression expr = FunCall(fac, Sort.SET0, Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLet1() {
    // normalize (\let | x => zero \in \let | y = suc \in y x) = 1
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cZero())), cLet(clets(clet("y", cSuc())), cApps(cVar("y"), cVar("x")))), null);
    assertEquals(Suc(Zero()), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLet2() {
    // normalize (\let | x => suc \in \let | y = zero \in x y) = 1
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cSuc())), cLet(clets(clet("y", cZero())), cApps(cVar("x"), cVar("y")))), null);
    assertEquals(Suc(Zero()), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetNo() {
    // normalize (\let | x (y z : N) => zero \in x zero) = \lam (z : N) => zero
    CheckTypeVisitor.Result result = typeCheckExpr("\\let x (y z : Nat) => 0 \\in x 0", null);
    SingleDependentLink x = singleParam("x", Nat());
    assertEquals(Lam(x, Zero()), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetElimStuck() {
    // normalize (\let | x (y : N) : N <= \elim y | zero => zero | suc _ => zero \in x <1>) = the same
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("n", Nat()));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\let x (y : Nat) : Nat <= \\elim y | zero => zero | suc _ => zero \\in x n", null);
    assertEquals(result.expression, result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetElimNoStuck() {
    // normalize (\let | x (y : N) : \oo-Type2 <= \elim y | zero => \Type0 | suc _ => \Type1 \in x zero) = \Type0
    Concrete.Expression elimTree = cElim(Collections.singletonList(cVar("y")),
        cClause(cPatterns(cConPattern(Prelude.ZERO.getName())), Abstract.Definition.Arrow.RIGHT, cUniverseStd(0)),
        cClause(cPatterns(cConPattern(Prelude.SUC.getName(), cPatternArg(cNamePattern(null), true, false))), Abstract.Definition.Arrow.RIGHT, cUniverseStd(1))
    );
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cargs(cTele(cvars("y"), cNat())), cUniverseInf(2), Abstract.Definition.Arrow.LEFT, elimTree)), cApps(cVar("x"), cZero())), null);
    assertEquals(Universe(new Level(0), new Level(LevelVariable.HVAR)), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeElimAnyConstructor() {
    DependentLink var0 = param("var0", Universe(0));
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\data D | d Nat\n" +
        "\\function test (x : D) : Nat <= \\elim x | _! => 0");
    FunctionDefinition test = (FunctionDefinition) result.getDefinition("test");
    assertEquals(FunCall(test, Sort.SET0, Ref(var0)), FunCall(test, Sort.SET0, Ref(var0)).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void letNormalizationContext() {
    LetClause let = let("x", Collections.emptyList(), Nat(), top(EmptyDependentLink.getInstance(), new LeafElimTreeNode(Abstract.Definition.Arrow.RIGHT, Zero())));
    new LetExpression(lets(let), Ref(let)).normalize(NormalizeVisitor.Mode.NF);
  }

  @Test
  public void testConditionNormalization() {
    typeCheckClass(
        "\\data Z | pos Nat | neg Nat \\with | pos zero => neg 0\n" +
        "\\function only-one-zero : pos 0 = neg 0 => path (\\lam _ => pos 0)"
    );
  }

  @Test
  public void testConCallNormFull() {
    initializeBDList();
    Expression expr1 = ConCall(bdSnoc, Sort.SET0, Collections.singletonList(Nat()), ConCall(bdNil, Sort.SET0, Collections.singletonList(Nat())), Zero());
    assertEquals(ConCall(bdCons, Sort.SET0, Collections.singletonList(Nat()), Zero(), ConCall(bdNil, Sort.SET0, Collections.singletonList(Nat()))), expr1.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testIsoLeft() {
    DependentLink A = param("A", Universe(Sort.SET0));
    DependentLink B = param("B", Universe(Sort.SET0));
    DependentLink f = param("f", Pi(Ref(A), Ref(B)));
    DependentLink g = param("g", Pi(Ref(B), Ref(A)));
    SingleDependentLink a = singleParam("a", Ref(A));
    SingleDependentLink b = singleParam("b", Ref(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, Sort.SET0,
        Ref(A),
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, Sort.SET0,
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, Sort.SET0,
        Ref(A), Ref(B),
        Ref(f), Ref(g),
        Ref(linv), Ref(rinv),
        Left());
    assertEquals(Ref(A), iso_expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testIsoRight() {
    DependentLink A = param("A", Universe(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)));
    DependentLink B = param("B", Universe(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)));
    DependentLink f = param("f", Pi(Ref(A), Ref(B)));
    DependentLink g = param("g", Pi(Ref(B), Ref(A)));
    SingleDependentLink a = singleParam("a", Ref(A));
    SingleDependentLink b = singleParam("b", Ref(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR),
        Ref(A),
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR),
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR),
        Ref(A), Ref(B),
        Ref(f), Ref(g),
        Ref(linv), Ref(rinv),
        Right());
    assertEquals(Ref(B), iso_expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testCoeIso() {
    DependentLink A = param("A", Universe(Sort.SET0));
    DependentLink B = param("B", Universe(Sort.SET0));
    DependentLink f = param("f", Pi(Ref(A), Ref(B)));
    DependentLink g = param("g", Pi(Ref(B), Ref(A)));
    SingleDependentLink a = singleParam("a", Ref(A));
    SingleDependentLink b = singleParam("b", Ref(B));
    SingleDependentLink k = singleParam("k", Interval());
    Expression linvType = FunCall(Prelude.PATH_INFIX, Sort.SET0,
        Ref(A),
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, Sort.SET0,
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    DependentLink aleft = param("aleft", Ref(A));
    Expression iso_expr = FunCall(Prelude.ISO, Sort.SET0,
        Ref(A), Ref(B),
        Ref(f), Ref(g),
        Ref(linv), Ref(rinv),
        Ref(k));
    Expression expr = FunCall(Prelude.COERCE, Sort.SET0,
        Lam(k, iso_expr),
        Ref(aleft),
        Right());
    assertEquals(Apps(Ref(f), Ref(aleft)), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testCoeIsoFreeVar() {
    SingleDependentLink k = singleParam("k", Interval());
    SingleDependentLink i = singleParam("i", Interval());
    DataCallExpression A = DataCall(Prelude.PATH, Sort.SET0, Lam(i, Interval()), Ref(k), Ref(k));
    DependentLink B = param("B", Universe(Sort.SET0));
    DependentLink f = param("f", Pi(A, Ref(B)));
    DependentLink g = param("g", Pi(Ref(B), A));
    SingleDependentLink a = singleParam("a", A);
    SingleDependentLink b = singleParam("b", Ref(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, Sort.SET0,
        A,
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, Sort.SET0,
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    DependentLink aleft = paramExpr("aleft", A.subst(k, Right()));
    Expression expr = FunCall(Prelude.COERCE, Sort.SET0,
        Lam(k, FunCall(Prelude.ISO, Sort.SET0,
            DataCall(Prelude.PATH, Sort.SET0,
                Lam(i, Interval()),
                Ref(k),
                Ref(k)),
            Ref(B),
            Ref(f),
            Ref(g),
            Ref(linv),
            Ref(rinv),
            Ref(k))),
        Ref(aleft),
        Right());
    assertEquals(expr, expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testAppProj() {
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = Apps(new ProjExpression(Tuple(new SigmaExpression(Sort.SET0, param(null, Pi(Nat(), Nat()))), Lam(x, Ref(x))), 0), Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testConCallEta() {
    TypeCheckClassResult result = typeCheckClass(
        "\\function ($) {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
        "\\data Fin (n : Nat)\n" +
        "  | Fin (suc n) => fzero\n" +
        "  | Fin (suc n) => fsuc (Fin n)\n" +
        "\\function f (n : Nat) (x : Fin n) => fsuc $ x");
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    Expression term = ((LeafElimTreeNode) f.getElimTree()).getExpression().normalize(NormalizeVisitor.Mode.NF);
    assertNotNull(term.toConCall());
    assertEquals(result.getDefinition("fsuc"), term.toConCall().getDefinition());
    assertEquals(1, term.toConCall().getDefCallArguments().size());
    assertNotNull(term.toConCall().getDefCallArguments().get(0).toReference());
    assertEquals(f.getParameters().getNext(), term.toConCall().getDefCallArguments().get(0).toReference().getBinding());
  }
}
