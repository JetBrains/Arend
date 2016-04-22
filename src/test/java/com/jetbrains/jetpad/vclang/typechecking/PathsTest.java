package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import java.util.EnumSet;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.*;
import static org.junit.Assert.assertEquals;

public class PathsTest {
  @Test
  public void idpTest() {
    typeCheckDef("\\function idp {A : \\Type0} (a : A) : a = a => path (\\lam _ => a)");
  }

  @Test
  public void idpUntyped() {
    CheckTypeVisitor.Result idp = typeCheckExpr("\\lam {A : \\Type0} (a : A) => path (\\lam _ => a)", null);
    DependentLink A = param(false, "A", Universe(0));
    A.setNext(param("a", Reference(A)));
    DependentLink C = param((String) null, DataCall(Preprelude.INTERVAL));
    Expression pathCall = ConCall(Prelude.PATH_CON)
            .addArgument(ZeroLvl(), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Inf(), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Lam(C, Reference(A)), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(A.getNext()), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(A.getNext()), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Lam(C, Reference(A.getNext())), AppExpression.DEFAULT);
    assertEquals(Lam(A, pathCall).normalize(NormalizeVisitor.Mode.NF), idp.expression);
    assertEquals(Pi(A, Apps(FunCall(Prelude.PATH_INFIX).addArgument(ZeroLvl(), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(Inf(), EnumSet.noneOf(AppExpression.Flag.class)), Reference(A), Reference(A.getNext()), Reference(A.getNext()))).normalize(NormalizeVisitor.Mode.NF), idp.type.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void squeezeTest() {
    typeCheckClass(
        "\\static \\function squeeze1 (i j : I) <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
        "\\static \\function squeeze (i j : I) <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j"
    );
  }

  @Test
  public void concatTest() {
    typeCheckClass(
        "\\static \\function transport {A : \\Type0} (B : A -> \\Type0) {a a' : A} (p : a = a') (b : B a) <= coe (\\lam i => B (p @ i)) b right\n" +
        "\\static \\function concat {A : I -> \\Type0} {a : A left} {a' a'' : A right} (p : Path A a a') (q : a' = a'') <= transport (Path A a) q p\n" +
        "\\static \\function (*>) {A : \\Type0} {a a' a'' : A} (p : a = a') (q : a' = a'') <= concat p q");
  }

  @Test
  public void inv0Test() {
    typeCheckClass(
        "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function transport {A : \\Type0} (B : A -> \\Type0) {a a' : A} (p : a = a') (b : B a) <= coe (\\lam i => B (p @ i)) b right\n" +
        "\\static \\function inv {A : \\Type0} {a a' : A} (p : a = a') <= transport (\\lam a'' => a'' = a) p idp\n" +
        "\\static \\function squeeze1 (i j : I) : I <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
        "\\static \\function squeeze (i j : I) <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
        "\\static \\function psqueeze {A : \\Type0} {a a' : A} (p : a = a') (i : I) : a = p @ i => path (\\lam j => p @ squeeze i j)\n" +
        "\\static \\function Jl {A : \\Type0} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type0) (b : B a idp) {a' : A} (p : a = a') : B a' p\n" +
        "  <= coe (\\lam i => B (p @ i) (psqueeze p i)) b right\n" +
        "\\static \\function inv-inv {A : \\Type0} {a a' : A} (p : a = a') : inv (inv p) = p <= Jl (\\lam _ p => inv (inv p) = p) idp p\n" +
        "\\static \\function path-sym {A : \\Type0} (a a' : A) : (a = a') = (a' = a) => path (iso inv inv inv-inv inv-inv)");
  }

  @Test
  public void invTest() {
    typeCheckClass(
        "\\static \\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function transport {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} (B : A -> \\Type (lp, lh)) {a a' : A} (p : a = a') (b : B a) <= coe (\\lam i => B (p @ i)) b right\n" +
        "\\static \\function inv {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a a' : A} (p : a = a') <= transport (\\lam a'' => a'' = a) p idp\n" +
        "\\static \\function squeeze1 (i j : I) : I <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
        "\\static \\function squeeze (i j : I) <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
        "\\static \\function psqueeze {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a a' : A} (p : a = a') (i : I) : a = p @ i => path (\\lam j => p @ squeeze i j)\n" +
        "\\static \\function Jl {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type (lp, lh)) (b : B a idp) {a' : A} (p : a = a') : B a' p\n" +
        "  <= coe (\\lam i => B (p @ i) (psqueeze p i)) b right\n" +
        "\\static \\function inv-inv {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a a' : A} (p : a = a') : inv (inv p) = p <= Jl (\\lam _ p => inv (inv p) = p) idp p\n" +
        "\\static \\function path-sym {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} (a a' : A) : (a = a') = (a' = a) => path (iso inv inv inv-inv inv-inv)");
  }
}
