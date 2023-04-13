package org.arend.typechecking.definition;

import org.arend.Matchers;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.NotPiType;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class DefinableMetaTest extends TypeCheckingTestCase {
  @Test
  public void noArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red \\where \\meta red => 114514");
    assertEquals("114514", ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void uniArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red 114 \\where \\meta red x => x Nat.+ 514");
    assertEquals(String.valueOf(114 + 514), ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void tooManyArgSubst() {
    typeCheckDef("\\func thaut => warm 114 514 \\where \\meta warm x => x", 1);
  }

  @Test
  public void tooLittleArgSubst() {
    typeCheckDef("\\func thaut => warm \\where \\meta warm x => x", 1);
  }

  @Test
  public void appBody() {
    var def = (FunctionDefinition) typeCheckDef("\\func alendia => matchy suc zero \\where \\meta matchy f x => f x");
    assertEquals("1", ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void invokeTwice() {
    var def = (FunctionDefinition) typeCheckDef("\\func him => self 65 Nat.+ self 65 \\where \\meta self x => x");
    assertEquals(String.valueOf(65 + 65), ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void invokeManyTimes() {
    var def = (FunctionDefinition) typeCheckDef("\\func him => self 65 Nat.+ self 65 Nat.+ self 65 Nat.+ self 65 \\where \\meta self x => x");
    assertEquals(String.valueOf(65 + 65 + 65 + 65), ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void hierarchy() {
    var def = (FunctionDefinition) typeCheckDef("""
      \\func zhang => ice \\where {
        \\meta ice => alendia.tesla
        \\func alendia => 1 \\where \\meta tesla => 1
      }
      """);
    assertEquals(String.valueOf(1), ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void biArgSubst() {
    var def = (FunctionDefinition) typeCheckDef("\\func redy => red 114 514 \\where \\meta red x y => x Nat.+ y");
    assertEquals(String.valueOf(114 + 514), ((Expression) Objects.requireNonNull(def.getBody())).normalize(NormalizationMode.WHNF).toString());
  }

  @Test
  public void parametersTest() {
    typeCheckDef("\\meta test (x : Nat Nat) => x", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotPiType.class));
  }

  @Test
  public void implicitParameter() {
    resolveNamesDef("\\meta f {x} => x", 1);
  }

  @Test
  public void implicitArgument() {
    typeCheckModule(
      "\\meta f x => x\n" +
      "\\func test => f {3} 2", 1);
  }

  @Test
  public void parametersCallTest() {
    typeCheckModule(
      "\\meta f (x : Nat) => x\n" +
      "\\func test (y : Int) => f y", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void parametersCallTest2() {
    typeCheckModule(
      "\\meta f x (y : Nat) z (w : Int) => (x y, z w)\n" +
      "\\func test : f (\\lam x => x) 3 (\\lam x => x) -3 = (3,-3) => idp");
  }

  @Test
  public void parametersCallTest3() {
    typeCheckModule(
      "\\meta f {x : Nat} (p : x = 3) => x\n" +
      "\\func test : f idp = 3 => idp");
  }

  @Test
  public void levelsTest() {
    typeCheckModule(
      "\\meta f \\plevels p1 >= p2, x => x\n" +
      "\\func test => f \\levels 3 2 100");
  }

  @Test
  public void levelsTest2() {
    typeCheckModule(
      "\\meta f \\plevels p, (A : \\Type p) => A\n" +
      "\\func test => f \\levels 3 _ \\Type2");
  }

  @Test
  public void levelsTest3() {
    typeCheckModule(
      "\\meta f \\plevels p => \\Type p\n" +
      "\\func test : f \\levels 3 _ = \\Type3 => idp");
  }

  @Test
  public void levelsTest4() {
    typeCheckModule(
      "\\meta f (A : \\Type) => A\n" +
      "\\func test => f \\levels 3 _ \\Type2");
  }

  @Test
  public void levelsTest5() {
    typeCheckModule(
      "\\meta f => \\Type\n" +
      "\\func test : f \\levels 3 _ = \\Type3 => idp");
  }

  @Test
  public void levelsError() {
    typeCheckModule(
      "\\meta f \\plevels p1 <= p2, x => x\n" +
      "\\func test => f \\levels (3,2) _ 100", 1);
  }

  @Test
  public void levelsError2() {
    typeCheckModule(
      "\\meta f \\plevels p, (A : \\Type p) => A\n" +
      "\\func test => f \\levels 3 _ \\Type3", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void levelsError3() {
    typeCheckModule(
      "\\meta f (A : \\Type) => A\n" +
      "\\func test => f \\levels 3 _ \\Type3", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }
}
