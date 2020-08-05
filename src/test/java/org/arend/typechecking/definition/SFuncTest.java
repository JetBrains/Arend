package org.arend.typechecking.definition;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ExpressionFactory;
import org.arend.core.sort.Sort;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.util.SingletonList;
import org.junit.Test;

import java.util.Collections;

import static org.arend.Matchers.typeMismatchError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class SFuncTest extends TypeCheckingTestCase {
  @Test
  public void normTest() {
    typeCheckModule("\\sfunc pred (n : Nat) : Nat | 0 => 0 | suc n => n");
    Expression expr = getDefinition("pred").getDefCall(Sort.STD, new SingletonList<>(ExpressionFactory.Zero()));
    assertSame(expr, expr.normalize(NormalizationMode.WHNF));
  }

  @Test
  public void evalTest() {
    typeCheckModule(
      "\\sfunc pred (n : Nat) : Nat | 0 => 0 | suc n => n\n" +
      "\\func test => \\eval pred 2");
    Expression expr = getDefinition("test").getDefCall(Sort.STD, Collections.emptyList());
    assertEquals(ExpressionFactory.Suc(ExpressionFactory.Zero()), expr.normalize(NormalizationMode.WHNF));
  }

  @Test
  public void evalNotSFunc() {
    typeCheckModule(
      "\\func pred (n : Nat) : Nat | 0 => 0 | suc n => n\n" +
      "\\func test => \\eval pred 2", 1);
  }

  @Test
  public void pevalTest() {
    typeCheckModule(
      "\\sfunc pred (n : Nat) : Nat | 0 => 0 | suc n => n\n" +
      "\\func test : pred 3 = 2 => \\peval pred 3");
  }

  @Test
  public void pevalNotSFunc() {
    typeCheckModule(
      "\\func pred (n : Nat) : Nat | 0 => 0 | suc n => n\n" +
      "\\func test => \\peval pred 1", 1);
  }

  @Test
  public void squashedByUseTest() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\sfunc f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) : A \\elim d | con a => a");
  }

  @Test
  public void squashedByUseError() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) : A \\elim d | con a => a", 1);
  }

  @Test
  public void squashedByUseWithLevelTest() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\sfunc f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) : \\level A p \\elim d | con a => a");
  }

  @Test
  public void squashedByUseWithLevelError() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) : \\level A p \\elim d | con a => a", 1);
  }

  @Test
  public void squashedByTruncationTest() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Prop | con A\n" +
      "\\sfunc f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A) : \\level A p \\elim d | con a => a");
  }

  @Test
  public void squashedByTruncationError() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Prop | con A\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A) : \\level A p \\elim d | con a => a", 1);
  }

  @Test
  public void squashedWithFuncLevelTest() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Prop | con A\n" +
      "\\func id {A : \\Type} (p : \\Pi (x y : A) -> x = y) => A\n" +
      " \\where \\use \\level levelProp {A : \\Type} (p : \\Pi (x y : A) -> x = y) (x y : id p) : x = y => p x y\n" +
      "\\sfunc f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A) : id p \\elim d | con a => a");
  }

  @Test
  public void squashedWithFuncLevelError() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Prop | con A\n" +
      "\\func id {A : \\Type} (p : \\Pi (x y : A) -> x = y) => A\n" +
      " \\where \\use \\level levelProp {A : \\Type} (p : \\Pi (x y : A) -> x = y) (x y : id p) : x = y => p x y\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A) : id p \\elim d | con a => a", 1);
  }

  @Test
  public void pevalPropTest() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\sfunc f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) : \\level A p \\elim d | con a => a\n" +
      "\\func test {A : \\Type} (p : \\Pi (x y : A) -> x = y) (a1 a2 : A) : (\\peval f p (con a1)) = (\\peval f p (con a2)) => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void pevalCmpTest() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\sfunc f (b : Bool) : Nat | true => 0 | false => 0\n" +
      "\\func test : (\\peval f true) = (\\peval f true) => idp");
  }

  @Test
  public void pevalCmpError() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\sfunc f (b : Bool) : Nat | true => 0 | false => 0\n" +
      "\\func test : (\\peval f true) = (\\peval f false) => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void caseNormTest() {
    typeCheckModule("\\func test : Nat => \\scase 0 \\with { | 0 => 0 | suc n => n }");
    Expression expr = (Expression) ((FunctionDefinition) getDefinition("test")).getBody();
    assertEquals(expr, expr.normalize(NormalizationMode.WHNF));
  }

  @Test
  public void evalCaseTest() {
    typeCheckModule("\\func test : Nat => \\eval \\scase 2 \\with { | 0 => 0 | suc n => n }");
    Expression expr = (Expression) ((FunctionDefinition) getDefinition("test")).getBody();
    assertEquals(ExpressionFactory.Suc(ExpressionFactory.Zero()), expr.normalize(NormalizationMode.WHNF));
  }

  @Test
  public void evalCaseNotSFunc() {
    typeCheckModule("\\func test => \\eval \\case 2 \\return Nat \\with { | 0 => 0 | suc n => n }", 1);
  }

  @Test
  public void pevalCaseTest() {
    typeCheckModule("\\func test : (\\scase 3 \\return Nat \\with { | 0 => 0 | suc n => n }) = 2 => \\peval \\scase 3 \\return Nat \\with { | 0 => 0 | suc n => n }");
  }

  @Test
  public void pevalCaseNotSFunc() {
    typeCheckModule("\\func test => \\peval \\case 1 \\return Nat \\with { | 0 => 0 | suc n => n }", 1);
  }

  @Test
  public void squashedByUseCaseTest() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) => \\scase d \\return A \\with { | con a => a }");
  }

  @Test
  public void squashedByUseCaseError() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) => \\case d \\return A \\with { | con a => a }", 1);
  }

  @Test
  public void squashedByUseCaseWithLevelTest() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) => \\scase d \\return \\level A p \\with { | con a => a }");
  }

  @Test
  public void squashedByUseCaseWithLevelError() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) => \\case d \\return \\level A p \\with { | con a => a }", 1);
  }

  @Test
  public void squashedByTruncationCaseTest() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Prop | con A\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A) => \\scase d \\return \\level A p \\with { | con a => a }");
  }

  @Test
  public void squashedByTruncationCaseError() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Prop | con A\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A) => \\case d \\return \\level A p \\with { | con a => a }", 1);
  }

  @Test
  public void squashedWithFuncLevelCaseTest() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Prop | con A\n" +
      "\\func id {A : \\Type} (p : \\Pi (x y : A) -> x = y) => A\n" +
      " \\where \\use \\level levelProp {A : \\Type} (p : \\Pi (x y : A) -> x = y) (x y : id p) : x = y => p x y\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A) => \\scase d \\return id p \\with { | con a => a }");
  }

  @Test
  public void squashedWithFuncLevelCaseError() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Prop | con A\n" +
      "\\func id {A : \\Type} (p : \\Pi (x y : A) -> x = y) => A\n" +
      " \\where \\use \\level levelProp {A : \\Type} (p : \\Pi (x y : A) -> x = y) (x y : id p) : x = y => p x y\n" +
      "\\func f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A) => \\case d \\return id p \\with { | con a => a }", 1);
  }

  @Test
  public void pevalPropCaseTest() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\func test {A : \\Type} (p : \\Pi (x y : A) -> x = y) (a1 a2 : A) : (\\peval \\scase con a1 \\return \\level A p \\with { | con a => a }) = (\\peval \\scase con a2 \\return \\level A p \\with { | con a => a }) => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void pevalCmpCaseTest() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func f (b : Bool) : Nat | true => 0 | false => 0\n" +
      "\\func test : (\\peval \\scase true \\return Nat \\with { | true => 0 | false => 0 }) = (\\peval \\scase true \\return Nat \\with { | true => 0 | false => 0 }) => idp");
  }

  @Test
  public void pevalCmpCaseError() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func test : (\\peval \\scase true \\return Nat \\with { | true => 0 | false => 0 }) = (\\peval \\scase false \\return Nat \\with { | true => 0 | false => 0 }) => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }
}
