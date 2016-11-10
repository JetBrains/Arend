package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class PathsTest extends TypeCheckingTestCase {
  @Test
  public void idpTest() {
    typeCheckDef("\\function idp {A : \\Type0} (a : A) : a = a => path (\\lam _ => a)");
  }

  @Test
  public void idpUntyped() {
    CheckTypeVisitor.Result idp = typeCheckExpr("\\lam {A : \\Type0} (a : A) => path (\\lam _ => a)", null);
    DependentLink A = param(false, "A", Universe(0));
    A.setNext(param("a", Reference(A)));
    DependentLink C = param((String) null, Interval());
    List<Expression> pathArgs = new ArrayList<>();
    pathArgs.add(Lam(C, Reference(A)));
    pathArgs.add(Reference(A.getNext()));
    pathArgs.add(Reference(A.getNext()));
    Expression pathCall = ConCall(Prelude.PATH_CON, new Level(0), Level.INFINITY, pathArgs, Lam(C, Reference(A.getNext())));
    assertEquals(Lam(A, pathCall).normalize(NormalizeVisitor.Mode.NF), idp.getExpression());
    assertEquals(Pi(A, FunCall(Prelude.PATH_INFIX, new Level(0), Level.INFINITY, Reference(A), Reference(A.getNext()), Reference(A.getNext()))).normalize(NormalizeVisitor.Mode.NF), idp.getType().normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void squeezeTest() {
    typeCheckClass(
        "\\function squeeze1 (i j : I) <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
        "\\function squeeze (i j : I) <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j"
    );
  }

  @Test
  public void concatTest() {
    typeCheckClass(
        "\\function transport {A : \\Type} (B : A -> \\Type) {a a' : A} (p : a = a') (b : B a) <= coe (\\lam i => B (p @ i)) b right\n" +
        "\\function concat {A : I -> \\Type} {a : A left} {a' a'' : A right} (p : Path A a a') (q : a' = a'') <= transport (Path A a) q p\n" +
        "\\function (*>) {A : \\Type} {a a' a'' : A} (p : a = a') (q : a' = a'') <= concat p q");
  }

  @Test
  public void inv0Test() {
    typeCheckClass(
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function transport {A : \\Type} (B : A -> \\Type) {a a' : A} (p : a = a') (b : B a) <= coe (\\lam i => B (p @ i)) b right\n" +
        "\\function inv {A : \\Type} {a a' : A} (p : a = a') <= transport (\\lam a'' => a'' = a) p idp\n" +
        "\\function squeeze1 (i j : I) : I <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
        "\\function squeeze (i j : I) <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
        "\\function psqueeze {A : \\Type} {a a' : A} (p : a = a') (i : I) : a = p @ i => path (\\lam j => p @ squeeze i j)\n" +
        "\\function Jl {A : \\Type} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type) (b : B a idp) {a' : A} (p : a = a') : B a' p\n" +
        "  <= coe (\\lam i => B (p @ i) (psqueeze p i)) b right\n" +
        "\\function inv-inv {A : \\Type} {a a' : A} (p : a = a') : inv (inv p) = p <= Jl (\\lam _ p => inv (inv p) = p) idp p\n" +
        "\\function path-sym {A : \\Type} (a a' : A) : (a = a') = (a' = a) => path (iso inv inv inv-inv inv-inv)");
  }

  @Test
  public void invTest() {
    typeCheckClass(
        "\\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\function transport {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} (B : A -> \\Type (lp, lh)) {a a' : A} (p : a = a') (b : B a) <= coe (\\lam i => B (p @ i)) b right\n" +
        "\\function inv {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a a' : A} (p : a = a') <= transport (\\lam a'' => a'' = a) p idp\n" +
        "\\function squeeze1 (i j : I) : I <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
        "\\function squeeze (i j : I) <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
        "\\function psqueeze {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a a' : A} (p : a = a') (i : I) : a = p @ i => path (\\lam j => p @ squeeze i j)\n" +
        "\\function Jl {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type (lp, lh)) (b : B a idp) {a' : A} (p : a = a') : B a' p\n" +
        "  <= coe (\\lam i => B (p @ i) (psqueeze p i)) b right\n" +
        "\\function inv-inv {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a a' : A} (p : a = a') : inv (inv p) = p <= Jl (\\lam _ p => inv (inv p) = p) idp p\n" +
        "\\function path-sym {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} (a a' : A) : (a = a') = (a' = a) => path (iso inv inv inv-inv inv-inv)");
  }
}
