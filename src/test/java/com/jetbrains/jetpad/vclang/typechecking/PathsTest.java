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
            .addArgument(Level(ZeroLvl(), Inf()), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Lam(C, Reference(A)), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(A.getNext()), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Reference(A.getNext()), EnumSet.noneOf(AppExpression.Flag.class))
            .addArgument(Lam(C, Reference(A.getNext())), AppExpression.DEFAULT);
    assertEquals(Lam(A, pathCall).normalize(NormalizeVisitor.Mode.NF), idp.expression);
    assertEquals(Pi(A, Apps(FunCall(Prelude.PATH_INFIX).addArgument(Level(ZeroLvl(), Inf()), EnumSet.noneOf(AppExpression.Flag.class)), Reference(A), Reference(A.getNext()), Reference(A.getNext()))).normalize(NormalizeVisitor.Mode.NF), idp.type.normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void squeezeTest() {
    typeCheckClass(
        "\\static \\function squeeze1 (i j : I) <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
        "\\static \\function squeeze (i j : I) <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j"
    );
  }

  @Test
  public void pathEtaLeftTest() {
    typeCheckDef("\\function test (p : 0 = 0) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTest() {
    typeCheckDef("\\function test (p : 0 = 0) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void pathEtaLeftTestLevel() {
    typeCheckDef("\\function test (p : Nat = Nat) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTestLevel() {
    typeCheckDef("\\function test (p : Nat = Nat) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }
}

