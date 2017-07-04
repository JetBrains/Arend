package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class ElimTest extends TypeCheckingTestCase {
  @Test
  public void elim2() {
    typeCheckClass(
        "\\data D Nat (x y : Nat) | con1 Nat | con2 (Nat -> Nat) (a b c : Nat)\n" +
        "\\function P (a1 b1 c1 : Nat) (d1 : D a1 b1 c1) (a2 b2 c2 : Nat) (d2 : D a2 b2 c2) : \\oo-Type0 => \\elim d1\n" +
        "  | con2 _ _ _ _ => Nat -> Nat\n" +
        "  | con1 _ => Nat\n" +
        "\\function test (q w : Nat) (e : D w 0 q) (r : D q w 1) : P w 0 q e q w 1 r => \\elim e, r\n" +
        "  | con2 x y z t, con1 s => x\n" +
        "  | con1 _, con1 s => s\n" +
        "  | con1 s, con2 x y z t => x q\n" +
        "  | con2 _ y z t, con2 x y' z' t' => x");
  }

  @Test
  public void elim3() {
    typeCheckClass(
        "\\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
        "\\function test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat => \\elim e, r\n" +
        "  | con2 _ {y} {z} {t}, con1 s => q t\n" +
        "  | con1 {z} _, con1 s => z\n" +
        "  | con1 s, con2 y => y s\n" +
        "  | con2 _ {a} {b}, con2 y => y (q b)");
  }

  @Test
  public void elim3_() {
    typeCheckClass(
      "\\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
      "\\function test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat => \\elim e, r\n" +
      "  | con2 _ {y} {z} {t}, con1 s => q t\n" +
      "  | con1 {z} _, con1 s => z\n" +
      "  | con1 s, con2 y => y s\n" +
      "  | con2 _ {a} {zero} {c}, con2 y => y (q a)\n" +
      "  | con2 _ {a} {suc b} {c}, con2 y => y (q b)");
  }

  @Test
  public void elim4() {
    typeCheckClass(
        "\\function test (x : Nat) : Nat => \\elim x | zero => 0 | _ => 1\n" +
        "\\function test2 (x : Nat) : 1 = 1 => path (\\lam _ => test x)", 1);
  }

  @Test
  public void elim5() {
    typeCheckClass(
        "\\data D Nat \\with | zero => d0 | suc n => d1\n" +
        "\\function test (x : D 0) : Nat => \\elim x | d0 => 0");
  }

  @Test
  public void elimUnknownIndex1() {
    typeCheckClass(
        "\\data D Nat \\with | zero => d0 | suc _ => d1\n" +
        "\\function test (x : Nat) (y : D x) : Nat => \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex2() {
    typeCheckClass(
        "\\data D Nat \\with | zero => d0 | suc _ => d1\n" +
        "\\function test (x : Nat) (y : D x) : Nat => \\elim y | d0 => 0 | _ => 1", 1);
  }

  @Test
  public void elimUnknownIndex3() {
    typeCheckClass(
        "\\data D Nat \\with | zero => d0 | suc _ => d1\n" +
        "\\function test (x : Nat) (y : D x) : Nat => \\elim y | _ => 0", 0);
  }

  @Test
  public void elimUnknownIndex4() {
    typeCheckClass(
        "\\data E | A | B | C\n" +
        "\\data D E \\with | A => d0 | B => d1 | _ => d2\n" +
        "\\function test (x : E) (y : D x) : Nat => \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex5() {
    typeCheckClass(
        "\\data E | A | B | C\n" +
        "\\data D E \\with | A => d0 | B => d1 | _ => d2\n" +
        "\\function test (x : E) (y : D x) : Nat => \\elim y | d0 => 0 | d1 => 1 | d2 => 2", 2);
  }

  @Test
  public void elimUnknownIndex6() {
    typeCheckClass(
        "\\data E | A | B | C\n" +
        "\\data D E \\with | A => d0 | B => d1 | _ => d2\n" +
        "\\function test (x : E) (y : D x) : Nat => \\elim y | d0 => 0 | d1 => 1 | _ => 2", 2);
  }

  @Test
  public void elimUnknownIndex7() {
    typeCheckClass(
        "\\data E | A | B | C\n" +
        "\\data D E \\with | A => d0 | B => d1 | _ => d2\n" +
        "\\function test (x : E) (y : D x) : Nat => \\elim y | _ => 0");
  }

  @Test
  public void elimTooManyArgs() {
    typeCheckClass("\\data A | a Nat Nat \\function test (a : A) : Nat => \\elim a | a _ _ _ => 0", 1);
  }

  @Test
  public void elim6() {
    typeCheckClass(
        "\\data D | d Nat Nat\n" +
        "\\function test (x : D) : Nat => \\elim x | d zero zero => 0 | d (suc _) _ => 1 | d _ (suc _) => 2");
  }

  @Test
  public void elim7() {
    typeCheckClass(
        "\\data D | d Nat Nat\n" +
        "\\function test (x : D) : Nat => \\elim x | d zero zero => 0 | d (suc (suc _)) zero => 0", 1);
  }

  @Test
  public void elim8() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\data D | d Nat Nat\n" +
        "\\function test (x : D) : Nat => \\elim x | d zero zero => 0 | d _ _ => 1");
    FunctionDefinition test = (FunctionDefinition) result.getDefinition("test");
    Constructor d = (Constructor) result.getDefinition("d");
    Binding binding = new TypedBinding("y", Nat());
    Expression call1 = ConCall(d, Sort.SET0, Collections.emptyList(), Zero(), Ref(binding));
    Expression call2 = ConCall(d, Sort.SET0, Collections.emptyList(), Suc(Zero()), Ref(binding));
    assertEquals(FunCall(test, Sort.SET0, call1), FunCall(test, Sort.SET0, call1).normalize(NormalizeVisitor.Mode.NF));
    assertEquals(Suc(Zero()), FunCall(test, Sort.SET0, call2).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void elim9() {
    typeCheckClass(
        "\\data D Nat \\with | suc n => d1 | _ => d | zero => d0\n" +
        "\\function test (n : Nat) (a : D (suc n)) : Nat => \\elim a | d => 0", 1);
  }

  @Test
  public void elim10() {
    typeCheckClass("\\data Bool | true | false\n" +
                   "\\function tp : \\Pi (x : Bool) -> \\oo-Type0 => \\lam x => \\case x\n" +
                   "| true => Bool\n" +
                   "| false => Nat\n" +
                   "\\function f (x : Bool) : tp x => \\elim x\n" +
                   "| true => true\n" +
                   "| false => zero\n");
  }

  @Test
  public void elimEmptyBranch() {
    typeCheckClass(
        "\\data D Nat \\with | suc n => dsuc\n" +
        "\\function test (n : Nat) (d : D n) : Nat => \\elim n, d | zero, () | suc n, dsuc => 0");
  }

  @Test
  public void elimEmptyBranchError() {
    typeCheckClass(
        "\\data D Nat \\with | suc n => dsuc\n" +
        "\\function test (n : Nat) (d : D n) : Nat => \\elim n, d | suc n, () | zero, ()", 1);
  }

  @Test
  public void testNoPatterns() {
    typeCheckClass("\\function test (n : Nat) : 0 = 1 => \\elim n", 1);
  }

  @Test
  public void testAuto() {
    typeCheckClass(
        "\\data Empty\n" +
        "\\function test (n : Nat) (e : Empty) : Empty => \\elim n, e");
  }

  @Test
  public void testAuto1() {
    typeCheckClass(
        "\\data Geq Nat Nat \\with | _, zero => Geq-zero | suc n, suc m => Geq-suc (Geq n m)\n" +
        "\\function test (n m : Nat) (p : Geq n m) : Nat => \\elim n, m, p\n" +
        "  | _, zero, Geq-zero => 0\n" +
        "  | suc n, suc m, Geq-suc p => 1");
  }

  @Test
  public void testAutoNonData() {
    typeCheckClass(
        "\\data D Nat \\with | zero => dcons\n" +
        "\\data E (n : Nat) (Nat -> Nat) (D n) | econs\n" +
        "\\function test (n : Nat) (d : D n) (e : E n (\\lam x => x) d) : Nat => \\elim n, d, e\n" +
        "  | zero, dcons, econs => 1");
  }

  @Test
  public void testElimNeedNormalize() {
    typeCheckClass(
      "\\data D Nat \\with | suc n => c\n" +
      "\\function f => D (suc zero)\n" +
      "\\function test (x : f) : Nat => \\elim x\n" +
      "  | c => 0"
    );
  }

  @Test
  public void elimFail() {
      typeCheckClass("\\function\n" +
                     "test (x y : Nat) : y = 0 => \\elim x, y\n" +
                     "  | _, zero => path (\\lam _ => zero)\n" +
                     "  | zero, suc y' => test zero y'\n" +
                     "  | suc x', suc y' => test (suc x') y'\n" +
                     "\n" +
                     "\\function\n" +
                     "zero-is-one : 1 = 0 => test 0 1", 3);
  }

  @Test
  public void testSmthing() {
    typeCheckClass(
        "\\data Geq Nat Nat \\with\n" +
        "  | m, zero => EqBase \n" +
        "  | suc n, suc m => EqSuc (p : Geq n m)\n" +
        "\n" +
        "\\function f (x y : Nat) (p : Geq x y) : Nat =>\n" +
        "  \\case x, y, p\n" +
        "    | m, zero, EqBase => zero \n" +
        "    | zero, suc _, ()\n" +
        "    | suc _, suc _, EqSuc q => suc zero", 3);
  }

  @Test
  public void testElimOrderError() {
    typeCheckClass("\\data \\infix 4\n" +
                   "(=<) Nat Nat \\with\n" +
                   "  | zero, m => le_z\n" +
                   "  | suc n, suc m => le_ss (n =< m)\n" +
                   "\n" +
                   "\\function\n" +
                   "leq-trans {n m k : Nat} (nm : n =< m) (mk : m =< k) : n =< k => \\elim n, nm, m\n" +
                   "  | zero, le_z, _ => {?}\n" +
                   "  | suc n', le_ss nm', suc m' => {?}", 1);
  }

  @Test
  public void testEmptyNoElimError() {
    typeCheckClass(
        "\\data D Nat \\with | zero => d0\n" +
        "\\function test (x : Nat) (d : D x) : Nat => \\elim d\n" +
        "  | () => 0", 1);
  }

  @Test
  public void testElimTranslationSubst() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef(
      "\\function test (n m : Nat) : Nat => \\elim m\n" +
        " | _ => n"
    );
    assertEquals(new LeafElimTree(def.getParameters(), Ref(def.getParameters())), def.getElimTree());
  }

  @Test
  public void testElimTranslationSubst2() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef(
      "\\function test (n m : Nat) : Nat => \\elim m\n" +
      " | zero => n\n" +
      " | _ => n"
    );
    DependentLink nParam = def.getParameters().subst(new ExprSubstitution(), LevelSubstitution.EMPTY, 1);
    Map<BranchElimTree.Pattern, ElimTree> children = new HashMap<>();
    children.put(Prelude.ZERO, new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nParam)));
    children.put(Prelude.SUC, new LeafElimTree(param("m", Nat()), Ref(nParam)));
    assertEquals(new BranchElimTree(nParam, children), def.getElimTree());
  }

  @Test
  public void testElimTranslationSubst3() {
    TypeCheckClassResult result = typeCheckClass(
      "\\data D | A | B | C\n" +
      "\\function f (n m : D) : D => \\elim m\n" +
      " | A => n\n" +
      " | _ => n"
    );
    FunctionDefinition def = (FunctionDefinition) result.getDefinition("f");
    DataDefinition dataDef = (DataDefinition) result.getDefinition("D");
    DependentLink nParam = def.getParameters().subst(new ExprSubstitution(), LevelSubstitution.EMPTY, 1);
    Map<BranchElimTree.Pattern, ElimTree> children = new HashMap<>();
    children.put(dataDef.getConstructor("A"), new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nParam)));
    children.put(dataDef.getConstructor("B"), new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nParam)));
    children.put(dataDef.getConstructor("C"), new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nParam)));
    assertEquals(new BranchElimTree(nParam, children), def.getElimTree());
  }

  @Test
  public void testElimOnIntervalError() {
    typeCheckDef(
        "\\function test (i : I) : Nat => \\elim i\n" +
        "  | left => 0\n" +
        "  | right => 1\n"  +
        "  | _ => 0\n"
    , 1);
  }

  @Test
  public void emptyAfterAFewError() {
    typeCheckClass(
        "\\data D Nat \\with | zero => d\n" +
        "\\function test (x : Nat) (y : \\Pi(z : Nat) -> x = z) (a : D (suc x)) : Nat => \\elim x\n", 1);
  }

  @Test
  public void emptyAfterAFew() {
    typeCheckClass(
        "\\data D Nat \\with | zero => d\n" +
        "\\function test (x : Nat) (y : \\Pi(z : Nat) -> x = z) (a : D (suc x)) : Nat => \\elim a\n");
  }

  @Test
  public void testElimEmpty1() {
    typeCheckClass(
        "\\data D Nat \\with | zero => d1 | suc zero => d2 \n" +
        "\\data E (n : Nat) | e (D n)\n" +
        "\\function test (n : Nat) (e : E n) : Nat => \\elim n, e\n" +
        "  | zero, _ => 0\n" +
        "  | suc zero, _ => 1\n" +
        "  | suc (suc _), e ()"
    );
  }

  @Test
  public void testMultiArg() {
    typeCheckClass(
      "\\data D (A B : \\Type0) | c A B\n" +
      "\\function test (f : Nat -> Nat) (d : D Nat (Nat -> Nat)) : Nat => \\elim d\n" +
      "  | c x y => f x"
    );
  }

  @Test
  public void testEmptyCase() {
    typeCheckClass(
        "\\data D\n" +
        "\\function test (d : D) : 0 = 1 => \\case d | ()"
    );
  }

  @Test
  public void threeVars() {
    typeCheckClass(
      "\\function f (x y z : Nat) : Nat => \\elim x, y, z\n" +
      "  | zero, zero, zero => zero\n" +
      "  | zero, zero, suc k => k\n" +
      "  | zero, suc m, zero => m\n" +
      "  | zero, suc m, suc k => k\n" +
      "  | suc n, zero, zero => n\n" +
      "  | suc n, zero, suc k => k\n" +
      "  | suc n, suc m, zero => m\n" +
      "  | suc n, suc m, suc k => n");
  }

  @Test
  public void dependentElim() {
    typeCheckClass(
      "\\data Bool | true | false\n" +
      "\\function if (b : Bool) : \\Set => \\elim b | true => Nat | false => Nat -> Nat\n" +
      "\\function test (b : Bool) (x : if b) : Nat => \\elim b, x | true, zero => 0 | true, suc n => n | false, _ => 0"
    );
  }

  @Test
  public void numberElim() {
    typeCheckClass("\\function f (n : Nat) : Nat => \\elim n | 2 => 0 | 0 => 1 | 1 => 2 | suc (suc (suc n)) => n");
  }

  @Test
  public void numberElim2() {
    typeCheckClass("\\function f (n : Nat) : Nat => \\elim n | 0 => 1 | 1 => 2 | suc (suc (suc n)) => n", 1);
  }
}
