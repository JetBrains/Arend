package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static org.junit.Assert.assertEquals;

public class ElimTest {
  @Test
  public void elim2() {
    typeCheckClass(
        "\\static \\data D Nat (x y : Nat) | con1 Nat | con2 (Nat -> Nat) (a b c : Nat)\n" +
        "\\static \\function P (a1 b1 c1 : Nat) (d1 : D a1 b1 c1) (a2 b2 c2 : Nat) (d2 : D a2 b2 c2) : \\Type0 <= \\elim d1\n" +
        "  | con2 _ _ _ _ => Nat -> Nat\n" +
        "  | con1 _ => Nat\n" +
        "\\static \\function test (q w : Nat) (e : D w 0 q) (r : D q w 1) : P w 0 q e q w 1 r <= \\elim e, r\n" +
        " | con2 x y z t, con1 s => x\n" +
        " | con1 _, con1 s => s\n" +
        " | con1 s, con2 x y z t => x q\n" +
        " | con2 _ y z t, con2 x y z t => x");
  }

  @Test
  public void elim3() {
    typeCheckClass(
        "\\static \\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
        "\\static \\function test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat <= \\elim e, r\n" +
        "  | con2 _ {y} {z} {t}, con1 s => q t\n" +
        "  | con1 {z} _, con1 s => z\n" +
        "  | con1 s, con2 y => y s\n" +
        "  | con2 _ {a} {b}, con2 y => y (q b)");
  }

  @Test
  public void elim4() {
    typeCheckClass(
        "\\static \\function test (x : Nat) : Nat <= \\elim x | zero => 0 | _ => 1\n" +
        "\\static \\function test2 (x : Nat) : 1 = 1 => path (\\lam _ => test x)", 1);
  }

  @Test
  public void elim5() {
    typeCheckClass(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc n) => d1\n" +
        "\\static \\function test (x : D 0) : Nat <= \\elim x | d0 => 0");
  }

  @Test
  public void elimUnknownIndex1() {
    typeCheckClass(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimTest() {
    typeCheckClass(
        "\\static \\function test (x : Nat) : Nat => \\case x | zero a => 0 | sucs n => 1", 2);
  }

  @Test
  public void elimUnknownIndex2() {
    typeCheckClass(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | d0 => 0 | _ => 1", 1);
  }

  @Test
  public void elimUnknownIndex3() {
    typeCheckClass(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | _ => 0", 0);
  }

  @Test
  public void elimUnknownIndex4() {
    typeCheckClass(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex5() {
    typeCheckClass(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1 | d2 => 2", 3);
  }

  @Test
  public void elimUnknownIndex6() {
    typeCheckClass(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1 | _ => 2", 2);
  }

  @Test
  public void elimUnknownIndex7() {
    typeCheckClass(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | _ => 0", 0);
  }

  @Test
  public void elimTooManyArgs() {
    typeCheckClass("\\static \\data A | a Nat Nat \\static \\function test (a : A) : Nat <= \\elim a | a _ _ _ =>0", 1);
  }

  @Test
  public void elim6() {
    typeCheckClass(
        "\\static \\data D | d Nat Nat\n" +
        "\\static \\function test (x : D) : Nat <= \\elim x | d zero zero => 0 | d (suc _) _ => 1 | d _ (suc _) => 2");
  }

  @Test
  public void elim7() {
    typeCheckClass(
        "\\static \\data D | d Nat Nat\n" +
        "\\static \\function test (x : D) : Nat <= \\elim x | d zero zero => 0 | d (suc (suc _)) zero => 0", 1);
  }

  @Test
  public void elim8() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data D | d Nat Nat\n" +
        "\\static \\function test (x : D) : Nat <= \\elim x | d zero zero => 0 | d _ _ => 1");
    FunctionDefinition test = (FunctionDefinition) member.namespace.getDefinition("test");
    Constructor d = (Constructor) member.namespace.getDefinition("d");
    Binding binding = new TypedBinding("y", Nat());
    Expression call1 = Apps(ConCall(d), Zero(), Reference(binding));
    Expression call2 = Apps(ConCall(d), Suc(Zero()), Reference(binding));
    assertEquals(Apps(FunCall(test), call1), Apps(FunCall(test), call1).normalize(NormalizeVisitor.Mode.NF));
    assertEquals(Suc(Zero()), Apps(FunCall(test), call2).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void elim9() {
    typeCheckClass(
        "\\static \\data D Nat | D (suc n) => d1 | D _ => d | D zero => d0\n" +
        "\\static \\function test (n : Nat) (a : D (suc n)) : Nat <= \\elim a | d => 0", 1);
  }

  @Test
  public void elim10() {
    typeCheckClass("\\static \\data Bool | true | false\n" +
                   "\\static \\function tp : \\Pi (x : Bool) -> \\Type0 => \\lam x => \\case x\n" +
                   "| true => Bool\n" +
                   "| false => Nat\n" +
                   "\\static \\function f (x : Bool) : tp x <= \\elim x\n" +
                   "| true => true\n" +
                   "| false => zero\n");
  }

  @Test
  public void elimEmptyBranch() {
    typeCheckClass(
        "\\static \\data D Nat | D (suc n) => dsuc\n" +
        "\\static \\function test (n : Nat) (d : D n) : Nat <= \\elim n, d | zero, _! | suc n, dsuc => 0");
  }

  @Test
  public void elimEmptyBranchError() {
    typeCheckClass(
        "\\static \\data D Nat | D (suc n) => dsuc\n" +
        "\\static \\function test (n : Nat) (d : D n) : Nat <= \\elim n, d | suc n, _! | zero, _! => 0", 1);
  }

  @Test
  public void elimUnderLetError() {
    typeCheckClass("\\static \\function test (n : Nat) : Nat <= \\let x => 0 \\in \\elim n | _! => 0", 1);
  }

  @Test
  public void elimOutOfDefinitionError() {
    typeCheckClass("\\static \\function test (n : Nat) : Nat <= \\let x : Nat <= \\elim n | _ => 0 \\in 1", 1);
  }

  @Test
  public void elimLetError() {
    typeCheckClass("\\static \\function test => \\let x => 0 \\in \\let y : Nat <= \\elim x | _ => 0 \\in 1", 1);
  }

  @Test
  public void testSide() {
    typeCheckClass("\\static \\function test (n : Nat) <= suc (\\elim n | suc n => n | zero => 0)", 1);
  }

  @Test
  public void testPrepreludeSuc() {
    typeCheckClass(
        "\\static \\function test' => ::Preprelude.suc\n"
    );
  }

  @Test
  public void testNoPatterns() {
    typeCheckClass("\\static \\function test (n : Nat) : 0 = 1 <= \\elim n", 1);
  }

  @Test
  public void testAuto() {
    typeCheckClass(
        "\\static \\data Empty\n" +
        "\\static \\function test (n : Nat) (e : Empty) : Empty <= \\elim n, e");
  }

  @Test
  public void testAuto1() {
    typeCheckClass(
        "\\static \\data Geq Nat Nat | Geq _ zero => Geq-zero | Geq (suc n) (suc m) => Geq-suc (Geq n m)\n" +
        "\\static \\function test (n m : Nat) (p : Geq n m) : Nat <= \\elim n, m, p\n" +
        "  | _!, zero, Geq-zero => 0\n" +
        "  | suc n, suc m, Geq-suc p => 1");
  }

  @Test
  public void testAutoNonData() {
    typeCheckClass(
        "\\static \\data D Nat | D zero => dcons\n" +
        "\\static \\data E (n : Nat) (Nat -> Nat) (D n) | econs\n" +
        "\\static \\function test (n : Nat) (d : D n) (e : E n (\\lam x => x) d) : Nat <= \\elim n, d, e\n" +
        "  | zero, dcons, econs => 1");
  }

  @Test
  public void testElimNeedNormalize() {
    typeCheckClass(
      "\\static \\data D Nat | D (suc n) => c\n" +
      "\\static \\function f => D (suc zero)\n" +
      "\\static \\function test (x : f) : Nat <= \\elim x\n" +
          " | c => 0"
    );
  }

  @Test
  public void elimFail() {
      typeCheckClass("\\static \\function\n" +
                     "test (x y : Nat) : y = 0 <= \\elim x, y\n" +
                     "| _, zero => path (\\lam _ => zero)" +
                     "| zero, suc y' => test x y'" +
                     "| suc x', suc y' => test x y'" +

                     "\\static \\function" +
                     "zero-is-one : 1 = 0 => test 0 1", 2);
  }

  @Test
  public void testSmthing() {
    typeCheckClass(
        "\\static \\data Geq (x y : Nat)\n" +
        "  | Geq m zero => EqBase \n" +
        "  | Geq (suc n) (suc m) => EqSuc (p : Geq n m)\n" +
        "\n" +
        "\\static \\function f (x y : Nat) (p : Geq x y) : Nat <=\n" +
        "  \\case x, y, p\n" +
        "    | m, zero, EqBase <= zero \n" +
        "    | zero, suc _, _!\n" +
        "    | suc _, suc _, EqSuc q <= suc zero", 3);
  }

  @Test
  public void testElimOrderError() {
    typeCheckClass("\\static \\data \\infix 4\n" +
                   "(=<) (n m : Nat)\n" +
                   "  | (=<) zero m => le_z\n" +
                   "  | (=<) (suc n) (suc m) => le_ss (n =< m)\n" +

                   "\\static \\function\n" +
                   "leq-trans {n m k : Nat} (nm : n =< m) (mk : m =< k) : n =< k <= \\elim n, nm, m\n" +
                   "  | zero, le_z, _ => {?}\n" +
                   "  | suc n', le_ss nm', suc m' => {?}", 1);
  }

  @Test
  public void arrowTest() {
    typeCheckDef(
        "\\function (+) (x y : Nat) : Nat => \\elim x" +
        "  | zero => y\n" +
        "  | suc x => suc (x + y)", 1);
  }

  @Test
  public void testAnyNoElimError() {
    typeCheckClass(
        "\\static \\data D Nat | D zero => d0\n" +
            "\\function test (x : Nat) (d : D x) : Nat <= \\elim d\n" +
            " | _! => 0", 1
    );
  }

  @Test
  public void testElimTranslationSubst() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef(
      "\\function test (n m : Nat) : Nat <= \\elim m\n" +
      " | zero => n\n" +
      " | _ => n\n"
    );
    assertEquals(def.getElimTree(), top(def.getParameters(), branch(def.getParameters().getNext(), tail(), clause(Preprelude.ZERO, EmptyDependentLink.getInstance(), Reference(def.getParameters())), clause(Reference(def.getParameters())))));
  }

  @Test
  public void testElimOnIntervalError() {
    typeCheckDef(
        "\\function test (i : I) : Nat <= \\elim i\n" +
        " | left => 0\n" +
        " | right => 1\n"  +
        " | _ => 0\n"
    , 2);
  }

  @Test
  public void emptyAfterAFew() {
    typeCheckClass(
        "\\static \\data D Nat | D zero => d\n" +
        "\\static \\function test (x : Nat) (y : \\Pi(z : Nat) -> x = z) (a : D (suc x)) : Nat <= \\elim x\n");
  }

  @Test
  public void testElimEmpty1() {
    typeCheckClass(
        "\\static \\data D Nat | D zero => d1 | D (suc zero) => d2 \n" +
        "\\static \\data E (n : Nat) | e (D n)\n" +
        "\\static \\function test (n : Nat) (e : E n) : Nat <= \\elim n, e\n" +
            " | zero, _ => 0\n" +
            " | suc zero, _ => 1\n" +
            " | suc (suc _), e (_!)"
    );
  }

  @Test
  public void testMultiArg() {
    typeCheckClass(
      "\\static \\data D (A B : \\Type0) | c A B\n" +
      "\\static \\function test (f : Nat -> Nat) (d : D Nat (Nat -> Nat)) : Nat <= \\elim d\n" +
          " | c x y => f x"
    );
  }

  @Test
  public void testEmptyLet() {
    typeCheckClass(
        "\\static \\data D\n" +
        "\\static \\function test (d : D) : 0 = 1 <= \\let x (d : D) : 0 = 1 <= \\elim d \\in x d"
    );
  }
}
