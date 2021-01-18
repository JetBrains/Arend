package org.arend.term;

import org.arend.Matchers;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.SmallIntegerExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.subst.ExprSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.arend.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class FinTest extends TypeCheckingTestCase {
  @Test
  public void emptySet() {
    typeCheckDef("\\func xy (_ : Fin 0) : Empty | ()" +
      "\\where \\data Empty");
  }

  @Test
  public void zeroAsBoundary() {
    typeCheckDef("\\func meow : Fin 0 => 0", 1);
  }

  @Test
  public void zeroDontAllowOne() {
    typeCheckDef("\\func nyan : Fin 0 => 1", 1);
  }

  @Test
  public void oneDontAllowOne() {
    typeCheckDef("\\func sekai : Fin 1 => 1", 1);
  }

  @Test
  public void literalFin() {
    typeCheckDef("\\func ren : Fin 1 => 0");
    typeCheckDef("\\func xyr : Fin 2 => 1");
    typeCheckDef("\\func xyren : Fin 101 => 100");
  }

  @Test
  public void getTypeModCoerce() {
    typeCheckDef("\\func kiva (a : Nat) : Fin 8 => Nat.mod a 0");
  }

  @Test
  public void getTypeModFailing() {
    typeCheckDef("\\func kiva (a : Nat) : Fin 0 => Nat.mod a 0", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void getTypeModFailing2() {
    typeCheckDef("\\func kiva (a : Nat) : Fin 8 => Nat.mod a 9", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void getTypeMod() {
    typeCheckDef("\\func kiva : Fin 8 => Nat.mod 10 8");
    typeCheckDef("\\func oyama (a : Nat) : Fin (suc a) => Nat.mod 114514 (suc a)");
    typeCheckDef("\\func kiwa : Nat.mod 10 8 = {Fin 8} 2 => idp {Fin 8} {2}");
  }

  @Test
  public void getTypeDivMod() {
    typeCheckDef("\\func emmmer (a : Nat) : \\Sigma Nat (Fin (suc a)) => Nat.divMod 10 (suc a)");
  }

  @Test(timeout = 5000)
  public void fromNatCoercion() {
    typeCheckDef("\\func darkflames : Fin 1919811 => Fin.fromNat 1919810");
  }

  @Test
  public void unfiniteFin() {
    typeCheckDef("\\func dalao : 0 = {Nat} (\\let | x : Fin 1 => 0 \\in x) => idp {Nat}");
    typeCheckDef("\\func julao : 0 = {Fin 1} (\\let | x : Fin 1 => 0 \\in x) => idp {Fin 1} {0}");
  }

  @Test
  public void weakenFin() {
    typeCheckDef("\\func julao : Fin 2 => 0");
    typeCheckDef("\\func juruo : Fin 514 => 114");
  }

  @Test
  public void matchZero() {
    typeCheckDef("\\func wsl (a : Nat) (_ : Fin 0) : Nat");
  }

  @Test
  public void matchOne() {
    typeCheckDef("\\func wsl (a : Nat) (_ : Fin 1) : Nat" +
      "  | x, zero => x");
  }

  @Test
  public void matchStuck() {
    typeCheckDef("\\func wsl (a : Nat) (_ : Fin a) : Nat" +
      "  | x, zero => x", 1);
  }

  @Test
  public void matchTwo() {
    typeCheckModule(
      "\\func sdl (_ : Fin 2) : Nat\n" +
      "  | zero => 123\n" +
      "  | suc n => 666\n" +
      "\\func test1 : sdl 0 = 123 => idp\n" +
      "\\func test2 : sdl 1 = 666 => idp");
  }

  @Test
  public void matchThree() {
    typeCheckModule(
      "\\func test (x : Fin 3) : Nat\n" +
      "  | 0 => 7\n" +
      "  | 1 => 13\n" +
      "  | 2 => 25\n" +
      "\\func test1 : test 0 = 7 => idp\n" +
      "\\func test2 : test 1 = 13 => idp\n" +
      "\\func test3 : test 2 = 25 => idp");
  }

  @Test
  public void matchTwoError() {
    typeCheckDef(
      "\\func test (x : Fin 2) : Nat\n" +
      "  | 0 => 0\n" +
      "  | 1 => 1\n" +
      "  | 2 => 2", 1);
  }

  @Test
  public void modType() {
    assertEquals(Fin(7), typeCheckExpr("Nat.mod 17 7", null).type);
    assertEquals(divModType(Fin(7)), typeCheckExpr("Nat.divMod 17 7", null).type);
  }

  @Test
  public void modType2() {
    List<Binding> context = Collections.singletonList(new TypedBinding("n", Nat()));
    assertEquals(Fin(13), typeCheckExpr(context, "Nat.mod n 13", null).type);
    assertEquals(divModType(Fin(13)), typeCheckExpr(context, "Nat.divMod n 13", null).type);
  }

  @Test
  public void modType3() {
    List<Binding> context = Collections.singletonList(new TypedBinding("n", Nat()));
    assertEquals(Nat(), typeCheckExpr(context, "Nat.mod n 0", null).type);
    assertEquals(Prelude.DIV_MOD_TYPE, typeCheckExpr(context, "Nat.divMod n 0", null).type);
  }

  @Test
  public void modType4() {
    TypedBinding binding = new TypedBinding("n", Nat());
    Type type = Fin(Suc(new ReferenceExpression(binding)));
    assertEquals(type, typeCheckExpr(Collections.singletonList(binding), "Nat.mod n (suc n)", null).type);
    assertEquals(divModType(type), typeCheckExpr(Collections.singletonList(binding), "Nat.divMod n (suc n)", null).type);
  }

  @Test
  public void finSubtype() {
    typeCheckDef("\\func test (n : Nat) (x : Fin (n Nat.+ 2)) : Fin (n Nat.+ 5) => x");
  }

  @Test
  public void finSubtype2() {
    typeCheckDef("\\func test (n : Nat) (x : Fin 1) : Fin (n Nat.+ 1) => x");
  }

  @Test
  public void finSubtypeError() {
    typeCheckDef("\\func test (n : Nat) (x : Fin (n Nat.+ 3)) : Fin (n Nat.+ 2) => x", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void finSubtypeError2() {
    typeCheckDef("\\func test (n m : Nat) (x : Fin (n Nat.+ 2)) : Fin (m Nat.+ 3) => x", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void zeroTest() {
    typeCheckModule(
      "\\func test1 (n : Nat) : Fin (suc n) => zero\n" +
      "\\func test2 : Fin 1 => zero");
    assertEquals(new SmallIntegerExpression(0), ((FunctionDefinition) getDefinition("test1")).getBody());
    assertEquals(new SmallIntegerExpression(0), ((FunctionDefinition) getDefinition("test2")).getBody());
  }

  @Test
  public void sucTest() {
    typeCheckModule(
      "\\func test1 (n : Nat) (x : Fin n) : Fin (suc n) => suc x\n" +
      "\\func test2 (n : Nat) (x : Fin n) => suc x");
    FunctionDefinition fun1 = (FunctionDefinition) getDefinition("test1");
    FunctionDefinition fun2 = (FunctionDefinition) getDefinition("test2");
    assertEquals(fun1.getResultType(), fun2.getResultType().subst(fun2.getParameters(), new ReferenceExpression(fun1.getParameters())));
    assertEquals(fun1.getBody(), ((Expression) Objects.requireNonNull(fun2.getBody())).subst(new ExprSubstitution().add(fun2.getParameters(), Arrays.asList(new ReferenceExpression(fun1.getParameters()), new ReferenceExpression(fun1.getParameters().getNext())))));
  }

  @Test
  public void sucTest2() {
    typeCheckModule(
      "\\func test1 (x : Fin 2) : Fin 3 => suc x\n" +
      "\\func test2 (x : Fin 2) => suc x");
    FunctionDefinition fun1 = (FunctionDefinition) getDefinition("test1");
    FunctionDefinition fun2 = (FunctionDefinition) getDefinition("test2");
    assertEquals(fun1.getResultType(), fun2.getResultType());
    assertEquals(fun1.getBody(), ((Expression) Objects.requireNonNull(fun2.getBody())).subst(fun2.getParameters(), new ReferenceExpression(fun1.getParameters())));
  }

  @Test
  public void zeroImplicitTest() {
    typeCheckModule(
      "\\func test1 (n : Nat) : Fin (suc n) => zero\n" +
      "\\func test2 (n : Nat) => zero {n}");
    FunctionDefinition fun1 = (FunctionDefinition) getDefinition("test1");
    FunctionDefinition fun2 = (FunctionDefinition) getDefinition("test2");
    assertEquals(fun1.getResultType(), fun2.getResultType().subst(fun2.getParameters(), new ReferenceExpression(fun1.getParameters())));
    assertEquals(fun1.getBody(), ((Expression) Objects.requireNonNull(fun2.getBody())).subst(fun2.getParameters(), new ReferenceExpression(fun1.getParameters())));
  }

  @Test
  public void sucImplicitTest() {
    typeCheckModule(
      "\\func test1 (n : Nat) (x : Fin n) : Fin (suc n) => suc x\n" +
      "\\func test2 (n : Nat) (x : Fin n) => suc {n} x");
    FunctionDefinition fun1 = (FunctionDefinition) getDefinition("test1");
    FunctionDefinition fun2 = (FunctionDefinition) getDefinition("test2");
    assertEquals(fun1.getResultType(), fun2.getResultType().subst(fun2.getParameters(), new ReferenceExpression(fun1.getParameters())));
    assertEquals(fun1.getBody(), ((Expression) Objects.requireNonNull(fun2.getBody())).subst(new ExprSubstitution().add(fun2.getParameters(), Arrays.asList(new ReferenceExpression(fun1.getParameters()), new ReferenceExpression(fun1.getParameters().getNext())))));
  }

  @Test
  public void sucImplicitTest2() {
    typeCheckModule(
      "\\func f1 (n : Nat) (x : Fin n) : Fin (suc n) => suc x\n" +
      "\\func f2 (n : Nat) => suc {n}\n" +
      "\\func test (n : Nat) : f1 n = {Fin n -> Fin (suc n)} f2 n => idp");
  }

  @Test
  public void listTest() {
    typeCheckModule(
      "\\data List (A : \\Type) | nil | \\infixr 5 :: A (List A)\n" +
      "\\func length {A : \\Type} (list : List A) : Nat \\elim list\n" +
      "  | nil => 0\n" +
      "  | :: a list => suc (length list)\n" +
      "\\func \\infix 7 !! {A : \\Set} (v : List A) (index : Fin (length v)) : A \\elim v, index\n" +
      "  | :: a v, suc index => v !! index\n" +
      "  | :: a v, 0 => a\n" +
      "\\data Term (A : \\Set) (context : List A) (termSort : A) : \\Set | term (index : Fin (length context)) (termSort = context !! index) | foo\n" +
      "\\func enlarge-substitution {A : \\Set} (list : List A) (index : Fin (length list)) : Term A list (list !! index) \\elim list, index\n" +
      "  | :: a list, 0 => term 0 idp\n" +
      "  | _, _ => foo");
  }
}
