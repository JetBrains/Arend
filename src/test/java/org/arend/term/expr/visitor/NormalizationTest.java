package org.arend.term.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.naming.reference.LocalReferable;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.term.concrete.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class NormalizationTest extends TypeCheckingTestCase {
  private FunctionDefinition plus;
  private FunctionDefinition mul;
  private FunctionDefinition fac;
  private FunctionDefinition nelim;

  @Before
  public void initialize() {
    typeCheckModule(
      "\\func \\infixl 6 + (x y : Nat) : Nat \\elim x\n" +
      "  | zero => y\n" +
      "  | suc x => suc (x + y)\n" +
      "\\func \\infixl 7 * (x y : Nat) : Nat \\elim x\n" +
      "  | zero => zero\n" +
      "  | suc x => y + x * y\n" +
      "\\func fac (x : Nat) : Nat\n" +
      "  | zero => 1\n" +
      "  | suc x => suc x * fac x\n" +
      "\\func nelim (z : Nat) (s : Nat -> Nat -> Nat) (x : Nat) : Nat \\elim x\n" +
      "  | zero => z\n" +
      "  | suc x => s x (nelim z s x)");
    plus = (FunctionDefinition) getDefinition("+");
    mul = (FunctionDefinition) getDefinition("*");
    fac = (FunctionDefinition) getDefinition("fac");
    nelim = (FunctionDefinition) getDefinition("nelim");
  }

  @Test
  public void normalizeLamId() {
    // normalize( (\x.x) (suc zero) ) = suc zero
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = Apps(Lam(x, Ref(x)), Suc(Zero()));
    assertEquals(Suc(Zero()), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeLamK() {
    // normalize( (\x y. x) (suc zero) ) = \z. suc zero
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    Expression expr = Apps(Lam(x, Lam(y, Ref(x))), Suc(Zero()));
    assertEquals(Lam(z, Suc(Zero())), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeLamKstar() {
    // normalize( (\x y. y) (suc zero) ) = \z. z
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    Expression expr = Apps(Lam(x, Lam(y, Ref(y))), Suc(Zero()));
    assertEquals(Lam(z, Ref(z)), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeLamKOpen() {
    // normalize( (\x y. x) (suc (var(0))) ) = \z. suc (var(0))
    DependentLink var0 = param("var0", Universe(0));
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    SingleDependentLink z = singleParam("z", Nat());
    Expression expr = Apps(Lam(xy, Ref(xy)), Suc(Ref(var0)));
    assertEquals(Lam(z, Suc(Ref(var0))), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeNelimZero() {
    // normalize( N-elim (suc zero) (\x. suc x) 0 ) = suc zero
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = FunCall(nelim, LevelPair.SET0, Suc(Zero()), Lam(x, Suc(Ref(x))), Zero());
    assertEquals(Suc(Zero()), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeNelimOne() {
    // normalize( N-elim (suc zero) (\x y. (var(0)) y) (suc zero) ) = var(0) (suc zero)
    DependentLink var0 = param("var0", Pi(Nat(), Nat()));
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Nat());
    Expression expr = FunCall(nelim, LevelPair.SET0, Suc(Zero()), Lam(x, Lam(y, Apps(Ref(var0), Ref(y)))), Suc(Zero()));
    assertEquals(Apps(Ref(var0), Suc(Zero())), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeNelimArg() {
    // normalize( N-elim (suc zero) (var(0)) ((\x. x) zero) ) = suc zero
    DependentLink var0 = param("var0", Universe(0));
    SingleDependentLink x = singleParam("x", Nat());
    Expression arg = Apps(Lam(x, Ref(x)), Zero());
    Expression expr = FunCall(nelim, LevelPair.SET0, Suc(Zero()), Ref(var0), arg);
    Expression result = expr.normalize(NormalizationMode.NF);
    assertEquals(Suc(Zero()), result);
  }

  @Test
  public void normalizePlus0a3() {
    // normalize (plus 0 3) = 3
    Expression expr = FunCall(plus, LevelPair.SET0, Zero(), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizePlus3a0() {
    // normalize (plus 3 0) = 3
    Expression expr = FunCall(plus, LevelPair.SET0, Suc(Suc(Suc(Zero()))), Zero());
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizePlus3a3() {
    // normalize (plus 3 3) = 6
    Expression expr = FunCall(plus, LevelPair.SET0, Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeMul3a0() {
    // normalize (mul 3 0) = 0
    Expression expr = FunCall(mul, LevelPair.SET0, Suc(Suc(Suc(Zero()))), Zero());
    assertEquals(Zero(), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeMul0a3() {
    // normalize (mul 0 3) = 0
    Expression expr = FunCall(mul, LevelPair.SET0, Zero(), Suc(Suc(Suc(Zero()))));
    assertEquals(Zero(), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeMul3a3() {
    // normalize (mul 3 3) = 9
    Expression expr = FunCall(mul, LevelPair.SET0, Suc(Suc(Suc(Zero()))), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero()))))))))), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeFac3() {
    // normalize (fac 3) = 6
    Expression expr = FunCall(fac, LevelPair.SET0, Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeLet1() {
    // normalize (\let | x => zero \in \let | y = suc \in y x) = 1
    LocalReferable x = ref("x");
    LocalReferable y = ref("y");
    Concrete.LetClause xClause = clet(x, cZero());
    Concrete.LetClause yClause = clet(y, cSuc());
    TypecheckingResult result = typeCheckExpr(cLet(clets(xClause), cLet(clets(yClause), cApps(cVar(y), cVar(x)))), null);
    assertEquals(Suc(Zero()), result.expression.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeLet2() {
    // normalize (\let | x => suc \in \let | y = zero \in x y) = 1
    LocalReferable x = ref("x");
    LocalReferable y = ref("y");
    Concrete.LetClause xClause = clet(x, cSuc());
    Concrete.LetClause yClause = clet(y, cZero());
    TypecheckingResult result = typeCheckExpr(cLet(clets(xClause), cLet(clets(yClause), cApps(cVar(x), cVar(y)))), null);
    assertEquals(Suc(Zero()), result.expression.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeLetNo() {
    // normalize (\let | x (y z : N) => zero \in x zero) = \lam (z : N) => zero
    TypecheckingResult result = typeCheckExpr("\\let x (y z : Nat) => 0 \\in x 0", null);
    SingleDependentLink x = singleParam("x", Nat());
    assertEquals(Lam(x, Zero()), result.expression.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeCaseStuck() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("n", Nat()));
    TypecheckingResult result = typeCheckExpr(context, "\\case n \\with { zero => zero | suc _ => zero }", Nat());
    assertEquals(result.expression, result.expression.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeLetElimNoStuck() {
    // normalize (\let | x (y : N) : \oo-Type2 => \Type0 \in x zero) = \Type0
    LocalReferable y = ref("y");
    LocalReferable x = ref("x");
    Concrete.LetClause xClause = clet(x, cargs(cTele(cvars(y), cNat())), cUniverseInf(2), cUniverseStd(0));
    TypecheckingResult result = typeCheckExpr(cLet(clets(xClause), cApps(cVar(x), cZero())), null);
    assertEquals(Universe(new Level(0), new Level(LevelVariable.HVAR)), result.expression.normalize(NormalizationMode.NF));
  }

  @Test
  public void normalizeElimAnyConstructor() {
    DependentLink var0 = param("var0", Universe(0));
    typeCheckModule(
        "\\data D | d Nat\n" +
        "\\func test (x : D) : Nat | _ => 0");
    FunctionDefinition test = (FunctionDefinition) getDefinition("test");
    assertEquals(Zero(), FunCall(test, LevelPair.SET0, Ref(var0)).normalize(NormalizationMode.NF));
  }

  @Test
  public void letNormalizationContext() {
    LetClause let = let("x", Zero());
    let(lets(let), Ref(let)).normalize(NormalizationMode.NF);
  }

  @Test
  public void testConditionNormalization() {
    typeCheckModule(
        "\\data Z | neg Nat | pos Nat { zero => neg 0 }\n" +
        "\\func only-one-zero : pos 0 = neg 0 => idp"
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
    Expression linvType = FunCall(Prelude.PATH_INFIX, LevelPair.SET0,
        Ref(A),
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, LevelPair.SET0,
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, LevelPair.SET0,
        Ref(A), Ref(B),
        Ref(f), Ref(g),
        Ref(linv), Ref(rinv),
        Left());
    assertEquals(Ref(A), iso_expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void testIsoRight() {
    DependentLink A = param("A", Universe(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)));
    DependentLink B = param("B", Universe(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)));
    DependentLink f = param("f", Pi(Ref(A), Ref(B)));
    DependentLink g = param("g", Pi(Ref(B), Ref(A)));
    SingleDependentLink a = singleParam("a", Ref(A));
    SingleDependentLink b = singleParam("b", Ref(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, LevelPair.STD,
        Ref(A),
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, LevelPair.STD,
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    Expression iso_expr = FunCall(Prelude.ISO, LevelPair.STD,
        Ref(A), Ref(B),
        Ref(f), Ref(g),
        Ref(linv), Ref(rinv),
        Right());
    assertEquals(Ref(B), iso_expr.normalize(NormalizationMode.NF));
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
    Expression linvType = FunCall(Prelude.PATH_INFIX, LevelPair.SET0,
        Ref(A),
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, LevelPair.SET0,
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    DependentLink aleft = param("aleft", Ref(A));
    Expression iso_expr = FunCall(Prelude.ISO, LevelPair.SET0,
        Ref(A), Ref(B),
        Ref(f), Ref(g),
        Ref(linv), Ref(rinv),
        Ref(k));
    Expression expr = FunCall(Prelude.COERCE, LevelPair.SET0,
        Lam(k, iso_expr),
        Ref(aleft),
        Right());
    assertEquals(Apps(Ref(f), Ref(aleft)), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void testCoeIsoFreeVar() {
    SingleDependentLink k = singleParam("k", Interval());
    SingleDependentLink i = singleParam("i", Interval());
    LevelPair levels = new LevelPair(new Level(0), Level.INFINITY);
    DataCallExpression A = DataCall(Prelude.PATH, levels, Lam(i, Interval()), Ref(k), Ref(k));
    DependentLink B = param("B", Universe(Sort.SET0));
    DependentLink f = param("f", Pi(A, Ref(B)));
    DependentLink g = param("g", Pi(Ref(B), A));
    SingleDependentLink a = singleParam("a", A);
    SingleDependentLink b = singleParam("b", Ref(B));
    Expression linvType = FunCall(Prelude.PATH_INFIX, levels,
        A,
        Apps(Ref(g), Apps(Ref(f), Ref(a))),
        Ref(a));
    DependentLink linv = param("linv", Pi(a, linvType));
    Expression rinvType = FunCall(Prelude.PATH_INFIX, levels,
        Ref(B),
        Apps(Ref(f), Apps(Ref(g), Ref(b))),
        Ref(b));
    DependentLink rinv = param("rinv", Pi(b, rinvType));
    DependentLink aleft = paramExpr("aleft", A.subst(k, Right()));
    Expression expr = FunCall(Prelude.COERCE, levels,
        Lam(k, FunCall(Prelude.ISO, levels,
            DataCall(Prelude.PATH, levels,
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
    assertEquals(expr, expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void testAppProj() {
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = Apps(ProjExpression.make(Tuple(new SigmaExpression(Sort.SET0, param("_", Pi(Nat(), Nat()))), Lam(x, Ref(x))), 0), Zero());
    assertEquals(Zero(), expr.normalize(NormalizationMode.NF));
  }

  @Test
  public void testConCallEta() {
    typeCheckModule(
        "\\func \\infixl 1 $ {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
        "\\data Fin Nat \\with\n" +
        "  | suc n => fzero\n" +
        "  | suc n => fsuc (Fin n)\n" +
        "\\func f (n : Nat) (x : Fin n) => fsuc $ x");
    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    Expression term = ((Expression) Objects.requireNonNull(f.getBody())).normalize(NormalizationMode.NF);
    ConCallExpression conCall = term.cast(ConCallExpression.class);
    assertEquals(getDefinition("fsuc"), conCall.getDefinition());
    assertEquals(1, conCall.getDefCallArguments().size());
    assertEquals(f.getParameters().getNext(), conCall.getDefCallArguments().get(0).cast(ReferenceExpression.class).getBinding());
  }
}
