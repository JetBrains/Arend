package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class NormalizationTest extends TypeCheckingTestCase {
  // \function (+) (x y : Nat) : Nat => elim x | zero => y | suc x' => suc (x' + y)
  private final FunctionDefinition plus;
  // \function (*) (x y : Nat) : Nat => elim x | zero => zero | suc x' => y + x' * y
  private final FunctionDefinition mul;
  // \function fac (x : Nat) : Nat => elim x | zero => suc zero | suc x' => suc x' * fac x'
  private final FunctionDefinition fac;
  // \function nelim (z : Nat) (s : Nat -> Nat -> Nat) (x : Nat) : Nat => elim x | zero => z | suc x' => s x' (nelim z s x')
  private final FunctionDefinition nelim;

  public NormalizationTest() throws IOException {
    DependentLink xPlus = param("x", Nat());
    DependentLink yPlus = param("y", Nat());
    plus = new FunctionDefinition(null);
    plus.setParameters(params(xPlus, yPlus));
    plus.setResultType(Nat());
    plus.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    Map<Constructor, ElimTree> plusChildren = new HashMap<>();
    plusChildren.put(Prelude.ZERO, new LeafElimTree(yPlus, Ref(yPlus)));
    plusChildren.put(Prelude.SUC, new LeafElimTree(xPlus, Suc(FunCall(plus, Sort.SET0, Ref(xPlus), Ref(yPlus)))));
    plus.setBody(new BranchElimTree(EmptyDependentLink.getInstance(), plusChildren));
    plus.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink xMul = param("x", Nat());
    DependentLink yMul = param("y", Nat());
    mul = new FunctionDefinition(null);
    mul.setParameters(params(xMul, yMul));
    mul.setResultType(Nat());
    mul.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    Map<Constructor, ElimTree> mulChildren = new HashMap<>();
    mulChildren.put(Prelude.ZERO, new LeafElimTree(yMul, Zero()));
    mulChildren.put(Prelude.SUC, new LeafElimTree(xMul, FunCall(plus, Sort.SET0, Ref(yMul), FunCall(mul, Sort.SET0, Ref(xMul), Ref(yMul)))));
    mul.setBody(new BranchElimTree(EmptyDependentLink.getInstance(), mulChildren));
    mul.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink xFac = param("x", Nat());
    fac = new FunctionDefinition(null);
    fac.setParameters(xFac);
    fac.setResultType(Nat());
    fac.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    Map<Constructor, ElimTree> facChildren = new HashMap<>();
    facChildren.put(Prelude.ZERO, new LeafElimTree(EmptyDependentLink.getInstance(), Suc(Zero())));
    facChildren.put(Prelude.SUC, new LeafElimTree(xFac, FunCall(mul, Sort.SET0, Suc(Ref(xFac)), FunCall(fac, Sort.SET0, Ref(xFac)))));
    fac.setBody(new BranchElimTree(EmptyDependentLink.getInstance(), facChildren));
    fac.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    DependentLink zNElim = param("z", Nat());
    DependentLink sNElim = param("s", Pi(Nat(), Pi(Nat(), Nat())));
    DependentLink xNElim = param("x", Nat());
    nelim = new FunctionDefinition(null);
    nelim.setParameters(params(zNElim, sNElim, xNElim));
    nelim.setResultType(Nat());
    nelim.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
    DependentLink nelimParams = zNElim.subst(new ExprSubstitution(), LevelSubstitution.EMPTY, 2);
    Map<Constructor, ElimTree> nelimChildren = new HashMap<>();
    nelimChildren.put(Prelude.ZERO, new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nelimParams)));
    nelimChildren.put(Prelude.SUC, new LeafElimTree(xNElim, Apps(Ref(nelimParams.getNext()), Ref(xNElim), FunCall(nelim, Sort.SET0, Ref(nelimParams), Ref(nelimParams.getNext()), Ref(xNElim)))));
    nelim.setBody(new BranchElimTree(nelimParams, nelimChildren));
    nelim.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
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
    Concrete.LetClause x = clet("x", cZero());
    Concrete.LetClause y = clet("y", cSuc());
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(x), cLet(clets(y), cApps(cVar(y), cVar(x)))), null);
    assertEquals(Suc(Zero()), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLet2() {
    // normalize (\let | x => suc \in \let | y = zero \in x y) = 1
    Concrete.LetClause x = clet("x", cSuc());
    Concrete.LetClause y = clet("y", cZero());
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(x), cLet(clets(y), cApps(cVar(x), cVar(y)))), null);
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
  public void normalizeCaseStuck() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("n", Nat()));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\case n { zero => zero | suc _ => zero }", Nat());
    assertEquals(result.expression, result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeLetElimNoStuck() {
    // normalize (\let | x (y : N) : \oo-Type2 => \Type0 \in x zero) = \Type0
    Concrete.LocalVariable y = ref("y");
    Concrete.LetClause x = clet("x", cargs(cTele(cvars(y), cNat())), cUniverseInf(2), cUniverseStd(0));
    CheckTypeVisitor.Result result = typeCheckExpr(cLet(clets(x), cApps(cVar(x), cZero())), null);
    assertEquals(Universe(new Level(0), new Level(LevelVariable.HVAR)), result.expression.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void normalizeElimAnyConstructor() {
    DependentLink var0 = param("var0", Universe(0));
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\data D | d Nat\n" +
        "\\function test (x : D) : Nat => \\elim x | _ => 0");
    FunctionDefinition test = (FunctionDefinition) result.getDefinition("test");
    assertEquals(Zero(), FunCall(test, Sort.SET0, Ref(var0)).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void letNormalizationContext() {
    LetClause let = let("x", Zero());
    new LetExpression(lets(let), Ref(let)).normalize(NormalizeVisitor.Mode.NF);
  }

  @Test
  public void testConditionNormalization() {
    typeCheckClass(
        "\\data Z | neg Nat | pos Nat { zero => neg 0 }\n" +
        "\\function only-one-zero : pos 0 = neg 0 => path (\\lam _ => pos 0)"
    );
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
    Expression linvType = FunCall(Prelude.PATH_INFIX, Sort.STD,
        Ref(A),
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, Sort.STD,
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, Sort.STD,
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
    Expression expr = Apps(new ProjExpression(Tuple(new SigmaExpression(Sort.SET0, param("_", Pi(Nat(), Nat()))), Lam(x, Ref(x))), 0), Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void testConCallEta() {
    TypeCheckClassResult result = typeCheckClass(
        "\\function ($) {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
        "\\data Fin Nat \\with\n" +
        "  | suc n => fzero\n" +
        "  | suc n => fsuc (Fin n)\n" +
        "\\function f (n : Nat) (x : Fin n) => fsuc $ x");
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    Expression term = ((LeafElimTree) f.getBody()).getExpression().normalize(NormalizeVisitor.Mode.NF);
    ConCallExpression conCall = term.cast(ConCallExpression.class);
    assertEquals(result.getDefinition("fsuc"), conCall.getDefinition());
    assertEquals(1, conCall.getDefCallArguments().size());
    assertEquals(f.getParameters().getNext(), conCall.getDefCallArguments().get(0).cast(ReferenceExpression.class).getBinding());
  }
}
