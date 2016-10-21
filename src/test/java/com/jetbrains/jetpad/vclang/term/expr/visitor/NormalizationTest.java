package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LetClause;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
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
    plus = new FunctionDefinition(null, EmptyNamespace.INSTANCE, params(xPlus, yPlus), Nat(), null);

    DependentLink xPlusMinusOne = param("x'", Nat());
    ElimTreeNode plusElimTree = top(xPlus, branch(xPlus, tail(yPlus),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Reference(yPlus)),
        clause(Prelude.SUC, xPlusMinusOne, Suc(FunCall(plus, new LevelSubstitution(), Reference(xPlusMinusOne), Reference(yPlus))))));
    plus.setElimTree(plusElimTree);

    DependentLink xMul = param("x", Nat());
    DependentLink yMul = param("y", Nat());
    mul = new FunctionDefinition(null, EmptyNamespace.INSTANCE, params(xMul, yMul), Nat(), null);
    DependentLink xMulMinusOne = param("x'", Nat());
    ElimTreeNode mulElimTree = top(xMul, branch(xMul, tail(yMul),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Zero()),
        clause(Prelude.SUC, xMulMinusOne, FunCall(plus, new LevelSubstitution(), Reference(yMul), FunCall(mul, new LevelSubstitution(), Reference(xMulMinusOne), Reference(yMul))))
    ));
    mul.setElimTree(mulElimTree);

    DependentLink xFac = param("x", Nat());
    fac = new FunctionDefinition(null, EmptyNamespace.INSTANCE, xFac, Nat(), null);
    DependentLink xFacMinusOne = param("x'", Nat());
    ElimTreeNode facElimTree = top(xFac, branch(xFac, tail(),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Suc(Zero())),
        clause(Prelude.SUC, xFacMinusOne, FunCall(mul, new LevelSubstitution(), Suc(Reference(xFacMinusOne)), FunCall(fac, new LevelSubstitution(), Reference(xFacMinusOne))))
    ));
    fac.setElimTree(facElimTree);

    DependentLink zNElim = param("z", Nat());
    DependentLink sNElim = param("s", Pi(param(Nat()), Pi(param(Nat()), Nat())));
    DependentLink xNElim = param("x", Nat());
    nelim = new FunctionDefinition(null, EmptyNamespace.INSTANCE, params(zNElim, sNElim, xNElim), Nat(), null);
    DependentLink xNElimMinusOne = param("x'", Nat());
    ElimTreeNode nelimElimTree = top(zNElim, branch(xNElim, tail(),
        clause(Prelude.ZERO, EmptyDependentLink.getInstance(), Reference(zNElim)),
        clause(Prelude.SUC, xNElimMinusOne, Apps(Reference(sNElim), Reference(xNElimMinusOne), FunCall(nelim, new LevelSubstitution(), Reference(zNElim), Reference(sNElim), Reference(xNElimMinusOne))))
    ));
    nelim.setElimTree(nelimElimTree);
  }

  private void initializeBDList() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\static \\data BD-list (A : \\Type0) | nil | cons A (BD-list A) | snoc (BD-list A) A\n" +
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
    DependentLink x = param("x", Nat());
    Expression expr = Apps(Lam(x, Reference(x)), Suc(Zero()));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamK() {
    // normalize( (\x y. x) (suc zero) ) = \z. suc zero
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Nat());
    Expression expr = Apps(Lam(x, Lam(y, Reference(x))), Suc(Zero()));
    assertEquals(Lam(z, Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamKstar() {
    // normalize( (\x y. y) (suc zero) ) = \z. z
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Nat());
    Expression expr = Apps(Lam(x, Lam(y, Reference(y))), Suc(Zero()));
    assertEquals(Lam(z, Reference(z)), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLamKOpen() {
    // normalize( (\x y. x) (suc (var(0))) ) = \z. suc (var(0))
    DependentLink var0 = param("var0", Universe(0));
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    DependentLink z = param("z", Nat());
    Expression expr = Apps(Lam(params(x, y), Reference(x)), Suc(Reference(var0)));
    assertEquals(Lam(z, Suc(Reference(var0))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimZero() {
    // normalize( N-elim (suc zero) (\x. suc x) 0 ) = suc zero
    DependentLink x = param("x", Nat());
    Expression expr = FunCall(nelim, new LevelSubstitution(), Suc(Zero()), Lam(x, Suc(Reference(x))), Zero());
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimOne() {
    // normalize( N-elim (suc zero) (\x y. (var(0)) y) (suc zero) ) = var(0) (suc zero)
    DependentLink var0 = param("var0", Pi(Nat(), Nat()));
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    Expression expr = FunCall(nelim, new LevelSubstitution(), Suc(Zero()), Lam(x, Lam(y, Apps(Reference(var0), Reference(y)))), Suc(Zero()));
    assertEquals(Apps(Reference(var0), Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeNelimArg() {
    // normalize( N-elim (suc zero) (var(0)) ((\x. x) zero) ) = suc zero
    DependentLink var0 = param("var0", Universe(0));
    DependentLink x = param("x", Nat());
    Expression arg = Apps(Lam(x, Reference(x)), Zero());
    Expression expr = FunCall(nelim, new LevelSubstitution(), Suc(Zero()), Reference(var0), arg);
    Expression result = expr.normalize(NormalizeVisitor.Mode.NF);
    assertEquals(Suc(Zero()), result);
  }

  @Test
  public void normalizePlus0a3() {
    // normalize (plus 0 3) = 3
    Expression expr = FunCall(plus, new LevelSubstitution(), Zero(), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizePlus3a0() {
    // normalize (plus 3 0) = 3
    Expression expr = FunCall(plus, new LevelSubstitution(), Suc(Suc(Suc(Zero()))), Zero());
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizePlus3a3() {
    // normalize (plus 3 3) = 6
    Expression expr = FunCall(plus, new LevelSubstitution(), Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul3a0() {
    // normalize (mul 3 0) = 0
    Expression expr = FunCall(mul, new LevelSubstitution(), Suc(Suc(Suc(Zero()))), Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul0a3() {
    // normalize (mul 0 3) = 0
    Expression expr = FunCall(mul, new LevelSubstitution(), Zero(), Suc(Suc(Suc(Zero()))));
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeMul3a3() {
    // normalize (mul 3 3) = 9
    Expression expr = FunCall(mul, new LevelSubstitution(), Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero()))))))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeFac3() {
    // normalize (fac 3) = 6
    Expression expr = FunCall(fac, new LevelSubstitution(), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLet1() {
    // normalize (\let | x => zero \in \let | y = suc \in y x) = 1
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cZero())), cLet(clets(clet("y", cSuc())), cApps(cVar("y"), cVar("x")))), null);
    assertEquals(Suc(Zero()), result.getExpression().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLet2() {
    // normalize (\let | x => suc \in \let | y = zero \in x y) = 1
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cSuc())), cLet(clets(clet("y", cZero())), cApps(cVar("x"), cVar("y")))), null);
    assertEquals(Suc(Zero()), result.getExpression().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetNo() {
    // normalize (\let | x (y z : N) => zero \in x zero) = \lam (z : N) => zero
    CheckTypeVisitor.Result result = typeCheckExpr("\\let x (y z : Nat) => 0 \\in x 0", null);
    DependentLink x = param("x", Nat());
    assertEquals(Lam(x, Zero()), result.getExpression().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetElimStuck() {
    // normalize (\let | x (y : N) : N <= \elim y | zero => zero | suc _ => zero \in x <1>) = the same
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("n", Nat()));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\let x (y : Nat) : Nat <= \\elim y | zero => zero | suc _ => zero \\in x n", null);
    assertEquals(result.getExpression(), result.getExpression().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetElimNoStuck() {
    // normalize (\let | x (y : N) : \Type2 <= \elim y | \Type0 => \Type1 | succ _ => \Type1 \in x zero) = \Type0
    Concrete.Expression elimTree = cElim(Collections.<Concrete.Expression>singletonList(cVar("y")),
        cClause(cPatterns(cConPattern(Prelude.ZERO.getName())), Abstract.Definition.Arrow.RIGHT, cUniverse(0)),
        cClause(cPatterns(cConPattern(Prelude.SUC.getName(), cPatternArg(cNamePattern(null), true, false))), Abstract.Definition.Arrow.RIGHT, cUniverse(1))
    );
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(clet("x", cargs(cTele(cvars("y"), cNat())), cUniverse(2), Abstract.Definition.Arrow.LEFT, elimTree)), cApps(cVar("x"), cZero())), null);
    assertEquals(Universe(0), result.getExpression().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeElimAnyConstructor() {
    DependentLink var0 = param("var0", Universe(0));
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\static \\data D | d Nat\n" +
        "\\static \\function test (x : D) : Nat <= \\elim x | _! => 0");
    FunctionDefinition test = (FunctionDefinition) result.getDefinition("test");
    assertEquals(FunCall(test, new LevelSubstitution(), Reference(var0)), FunCall(test, new LevelSubstitution(), Reference(var0)).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void letNormalizationContext() {
    LetClause let = let("x", EmptyDependentLink.getInstance(), Nat(), top(EmptyDependentLink.getInstance(), leaf(Abstract.Definition.Arrow.RIGHT, Zero())));
    Let(lets(let), Reference(let)).normalize(NormalizeVisitor.Mode.NF);
  }

  @Test
  public void testConditionNormalization() {
    typeCheckClass(
        "\\static \\data Z | pos Nat | neg Nat \\with | pos zero => neg 0\n" +
        "\\static \\function only-one-zero : pos 0 = neg 0 => path (\\lam _ => pos 0)"
    );
  }

  @Test
  public void testConCallNormFull() {
    initializeBDList();
    Expression expr1 = ConCall(bdSnoc, new LevelSubstitution(), Collections.<Expression>singletonList(Nat()), ConCall(bdNil, new LevelSubstitution(), Collections.<Expression>singletonList(Nat())), Zero());
    assertEquals(ConCall(bdCons, new LevelSubstitution(), Collections.<Expression>singletonList(Nat()), Zero(), ConCall(bdNil, new LevelSubstitution(), Collections.<Expression>singletonList(Nat()))), expr1.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testIsoleft() {
    DependentLink A = param("A", Universe(new Level(0), new Level(0)));
    DependentLink B = param("B", Universe(new Level(0), new Level(0)));
    DependentLink f = param("f", Pi(param(Reference(A)), Reference(B)));
    DependentLink g = param("g", Pi(param(Reference(B)), Reference(A)));
    DependentLink a = param("a", Reference(A));
    DependentLink b = param("b", Reference(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, new LevelSubstitution(),
        Reference(A),
        Apps(Reference(g), Apps(Reference(f), Reference(a))),
        Reference(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new LevelSubstitution(),
        Reference(B),
        Apps(Reference(f), Apps(Reference(g), Reference(b))),
        Reference(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, new LevelSubstitution(),
        Reference(A), Reference(B),
        Reference(f), Reference(g),
        Reference(linv), Reference(rinv),
        Left());
    assertEquals(Reference(A), iso_expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testIsoRight() {
    Binding lp = Prelude.PATH.getPolyParamByType(Prelude.LVL);
    Binding lh = Prelude.PATH.getPolyParamByType(Prelude.CNAT);
    DependentLink A = param("A", Universe(new Level(lp), new Level(lh)));
    DependentLink B = param("B", Universe(new Level(lp), new Level(lh)));
    DependentLink f = param("f", Pi(param(Reference(A)), Reference(B)));
    DependentLink g = param("g", Pi(param(Reference(B)), Reference(A)));
    DependentLink a = param("a", Reference(A));
    DependentLink b = param("b", Reference(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, new LevelSubstitution(),
        Reference(A),
        Apps(Reference(g), Apps(Reference(f), Reference(a))),
        Reference(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new LevelSubstitution(),
        Reference(B),
        Apps(Reference(f), Apps(Reference(g), Reference(b))),
        Reference(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, new LevelSubstitution(),
        Reference(A), Reference(B),
        Reference(f), Reference(g),
        Reference(linv), Reference(rinv),
        Right());
    assertEquals(Reference(B), iso_expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testCoeIso() {
    DependentLink A = param("A", Universe(new Level(0), new Level(0)));
    DependentLink B = param("B", Universe(new Level(0), new Level(0)));
    DependentLink f = param("f", Pi(param(Reference(A)), Reference(B)));
    DependentLink g = param("g", Pi(param(Reference(B)), Reference(A)));
    DependentLink a = param("a", Reference(A));
    DependentLink b = param("b", Reference(B));
    DependentLink k = param("k", Interval());
    Expression linvType = FunCall(Prelude.PATH_INFIX, new LevelSubstitution(),
        Reference(A),
        Apps(Reference(g), Apps(Reference(f), Reference(a))),
        Reference(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new LevelSubstitution(),
        Reference(B),
        Apps(Reference(f), Apps(Reference(g), Reference(b))),
        Reference(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    DependentLink aleft = param("aleft", Reference(A));
    Expression iso_expr = FunCall(Prelude.ISO, new LevelSubstitution(),
        Reference(A), Reference(B),
        Reference(f), Reference(g),
        Reference(linv), Reference(rinv),
        Reference(k));
    Expression expr = FunCall(Prelude.COERCE, new LevelSubstitution(),
        Lam(k, iso_expr),
        Reference(aleft),
        Right());
    assertEquals(Apps(Reference(f), Reference(aleft)), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testCoeIsoFreeVar() {
    DependentLink k = param("k", Interval());
    DependentLink i = param("i", Interval());
    Expression A = DataCall(Prelude.PATH, new LevelSubstitution(), Lam(i, Interval()), Reference(k), Reference(k));
    DependentLink B = param("B", Universe(new Level(0), new Level(0)));
    DependentLink f = param("f", Pi(param(A), Reference(B)));
    DependentLink g = param("g", Pi(param(Reference(B)), A));
    DependentLink a = param("a", A);
    DependentLink b = param("b", Reference(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, new LevelSubstitution(),
        A,
        Apps(Reference(g), Apps(Reference(f), Reference(a))),
        Reference(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, new LevelSubstitution(),
        Reference(B),
        Apps(Reference(f), Apps(Reference(g), Reference(b))),
        Reference(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    DependentLink aleft = param("aleft", A.subst(k, Right()));
    Expression expr = FunCall(Prelude.COERCE, new LevelSubstitution(),
        Lam(k, FunCall(Prelude.ISO, new LevelSubstitution(),
            DataCall(Prelude.PATH, new LevelSubstitution(),
                Lam(i, Interval()),
                Reference(k),
                Reference(k)),
            Reference(B),
            Reference(f),
            Reference(g),
            Reference(linv),
            Reference(rinv),
            Reference(k))),
        Reference(aleft),
        Right());
    assertEquals(expr, expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testAppProj() {
    DependentLink x = param("x", Nat());
    Expression expr = Apps(Proj(Tuple(Sigma(param(Pi(param(Nat()), Nat()))), Lam(x, Reference(x))), 0), Zero());
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
    assertEquals(f.getParameters().getNext().getNext(), term.toConCall().getDefCallArguments().get(0).toReference().getBinding());
  }
}
