package org.arend.typechecking.patternmatching;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.Expression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class ElimTest extends TypeCheckingTestCase {
  @Test
  public void elim2() {
    typeCheckModule(
        "\\data D Nat (x y : Nat) | con1 Nat | con2 (Nat -> Nat) (a b c : Nat)\n" +
        "\\func P (a1 b1 c1 : Nat) (d1 : D a1 b1 c1) (a2 b2 c2 : Nat) (d2 : D a2 b2 c2) : \\oo-Type0 \\elim d1\n" +
        "  | con2 _ _ _ _ => Nat -> Nat\n" +
        "  | con1 _ => Nat\n" +
        "\\func test (q w : Nat) (e : D w 0 q) (r : D q w 1) : P w 0 q e q w 1 r \\elim e, r\n" +
        "  | con2 x y z t, con1 s => x\n" +
        "  | con1 _, con1 s => s\n" +
        "  | con1 s, con2 x y z t => x q\n" +
        "  | con2 _ y z t, con2 x y' z' t' => x");
  }

  @Test
  public void elim3() {
    typeCheckModule(
        "\\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
        "\\func test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat \\elim e, r\n" +
        "  | con2 _ {y} {z} {t}, con1 s => q t\n" +
        "  | con1 {z} _, con1 s => z\n" +
        "  | con1 s, con2 y => y s\n" +
        "  | con2 _ {a} {b}, con2 y => y (q b)");
  }

  @Test
  public void elim3_() {
    typeCheckModule(
      "\\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
      "\\func test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat \\elim e, r\n" +
      "  | con2 _ {y} {z} {t}, con1 s => q t\n" +
      "  | con1 {z} _, con1 s => z\n" +
      "  | con1 s, con2 y => y s\n" +
      "  | con2 _ {a} {zero} {c}, con2 y => y (q a)\n" +
      "  | con2 _ {a} {suc b} {c}, con2 y => y (q b)");
  }

  @Test
  public void elim4() {
    typeCheckModule(
        "\\func test (x : Nat) : Nat | zero => 0 | _ => 1\n" +
        "\\func test2 (x : Nat) : 1 = 1 => path (\\lam _ => test x)", 1);
  }

  @Test
  public void elim5() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d0 | suc n => d1\n" +
        "\\func test (x : D 0) : Nat | d0 => 0");
  }

  @Test
  public void elimUnknownIndex1() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d0 | suc _ => d1\n" +
        "\\func test (x : Nat) (y : D x) : Nat \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex2() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d0 | suc _ => d1\n" +
        "\\func test (x : Nat) (y : D x) : Nat \\elim y | d0 => 0 | _ => 1", 1);
  }

  @Test
  public void elimUnknownIndex3() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d0 | suc _ => d1\n" +
        "\\func test (x : Nat) (y : D x) : Nat \\elim y | _ => 0", 0);
  }

  @Test
  public void elimUnknownIndex4() {
    typeCheckModule(
        "\\data E | A | B | C\n" +
        "\\data D E \\with | A => d0 | B => d1 | _ => d2\n" +
        "\\func test (x : E) (y : D x) : Nat \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex5() {
    typeCheckModule(
        "\\data E | A | B | C\n" +
        "\\data D E \\with | A => d0 | B => d1 | _ => d2\n" +
        "\\func test (x : E) (y : D x) : Nat \\elim y | d0 => 0 | d1 => 1 | d2 => 2", 2);
  }

  @Test
  public void elimUnknownIndex6() {
    typeCheckModule(
        "\\data E | A | B | C\n" +
        "\\data D E \\with | A => d0 | B => d1 | _ => d2\n" +
        "\\func test (x : E) (y : D x) : Nat \\elim y | d0 => 0 | d1 => 1 | _ => 2", 2);
  }

  @Test
  public void elimUnknownIndex7() {
    typeCheckModule(
        "\\data E | A | B | C\n" +
        "\\data D E \\with | A => d0 | B => d1 | _ => d2\n" +
        "\\func test (x : E) (y : D x) : Nat \\elim y | _ => 0");
  }

  @Test
  public void elimTooManyArgs() {
    typeCheckModule("\\data A | a Nat Nat \\func test (a : A) : Nat | a _ _ _ => 0", 1);
  }

  @Test
  public void elim6() {
    typeCheckModule(
        "\\data D | d Nat Nat\n" +
        "\\func test (x : D) : Nat | d zero zero => 0 | d (suc _) _ => 1 | d _ (suc _) => 2");
  }

  @Test
  public void elim7() {
    typeCheckModule(
        "\\data D | d Nat Nat\n" +
        "\\func test (x : D) : Nat | d zero zero => 0 | d (suc (suc _)) zero => 0", 1);
  }

  @Test
  public void elim8() {
    typeCheckModule(
        "\\data D | d Nat Nat\n" +
        "\\func test (x : D) : Nat | d zero zero => 0 | d _ _ => 1");
    FunctionDefinition test = (FunctionDefinition) getDefinition("test");
    Constructor d = (Constructor) getDefinition("d");
    Binding binding = new TypedBinding("y", Nat());
    Expression call1 = ConCall(d, Sort.SET0, Collections.emptyList(), Zero(), Ref(binding));
    Expression call2 = ConCall(d, Sort.SET0, Collections.emptyList(), Suc(Zero()), Ref(binding));
    assertEquals(FunCall(test, Sort.SET0, call1), FunCall(test, Sort.SET0, call1).normalize(NormalizeVisitor.Mode.NF));
    assertEquals(Suc(Zero()), FunCall(test, Sort.SET0, call2).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void elim9() {
    typeCheckModule(
        "\\data D Nat \\with | suc n => d1 | _ => d | zero => d0\n" +
        "\\func test (n : Nat) (a : D (suc n)) : Nat \\elim a | d => 0", 1);
  }

  @Test
  public void elim10() {
    typeCheckModule("\\data Bool | true | false\n" +
                   "\\func tp : \\Pi (x : Bool) -> \\oo-Type0 => \\lam x => \\case x \\with {\n" +
                   "  | true => Bool\n" +
                   "  | false => Nat\n" +
                   "}\n" +
                   "\\func f (x : Bool) : tp x\n" +
                   "  | true => true\n" +
                   "  | false => zero\n");
  }

  @Test
  public void elimEmptyBranch() {
    typeCheckModule(
        "\\data D Nat \\with | suc n => dsuc\n" +
        "\\func test (n : Nat) (d : D n) : Nat | zero, () | suc n, dsuc => 0");
  }

  @Test
  public void elimEmptyBranchError() {
    typeCheckModule(
        "\\data D Nat \\with | suc n => dsuc\n" +
        "\\func test (n : Nat) (d : D n) : Nat | suc n, () | zero, ()", 1);
  }

  @Test
  public void testNoPatterns() {
    typeCheckModule("\\func test (n : Nat) : 0 = 1 \\elim n", 1);
  }

  @Test
  public void testAbsurdPattern() {
    typeCheckModule("\\func test (n : Nat) : 0 = 1 \\elim n | ()", 1);
  }

  @Test
  public void testAuto() {
    typeCheckModule(
        "\\data Empty\n" +
        "\\func test (n : Nat) (e : Empty) : Empty \\elim n, e");
  }

  @Test
  public void testAuto1() {
    typeCheckModule(
        "\\data Geq Nat Nat \\with | _, zero => Geq-zero | suc n, suc m => Geq-suc (Geq n m)\n" +
        "\\func test (n m : Nat) (p : Geq n m) : Nat\n" +
        "  | _, zero, Geq-zero => 0\n" +
        "  | suc n, suc m, Geq-suc p => 1");
  }

  @Test
  public void testAutoNonData() {
    typeCheckModule(
        "\\data D Nat \\with | zero => dcons\n" +
        "\\data E (n : Nat) (Nat -> Nat) (D n) | econs\n" +
        "\\func test (n : Nat) (d : D n) (e : E n (\\lam x => x) d) : Nat\n" +
        "  | zero, dcons, econs => 1");
  }

  @Test
  public void testElimNeedNormalize() {
    typeCheckModule(
      "\\data D Nat \\with | suc n => c\n" +
      "\\func f => D (suc zero)\n" +
      "\\func test (x : f) : Nat\n" +
      "  | c => 0"
    );
  }

  @Test
  public void elimFail() {
      typeCheckModule("\\func test (x y : Nat) : y = 0\n" +
                     "  | _, zero => path (\\lam _ => zero)\n" +
                     "  | zero, suc y' => test zero y'\n" +
                     "  | suc x', suc y' => test (suc x') y'\n" +
                     "\n" +
                     "\\func zero-is-one : 1 = 0 => test 0 1", 2);
  }

  @Test
  public void testSmthing() {
    typeCheckModule(
        "\\data Geq Nat Nat \\with\n" +
        "  | m, zero => EqBase \n" +
        "  | suc n, suc m => EqSuc (p : Geq n m)\n" +
        "\n" +
        "\\func f (x y : Nat) (p : Geq x y) : Nat =>\n" +
        "  \\case x, y, p \\with {\n" +
        "    | m, zero, EqBase => zero \n" +
        "    | zero, suc _, ()\n" +
        "    | suc _, suc _, EqSuc q => suc zero\n" +
        "  }", 3);
  }

  @Test
  public void testElimOrderError() {
    typeCheckModule("\\data \\infix 4\n" +
                   "=< Nat Nat \\with\n" +
                   "  | zero, m => le_z\n" +
                   "  | suc n, suc m => le_ss (n =< m)\n" +
                   "\n" +
                   "\\func leq-trans {n m k : Nat} (nm : n =< m) (mk : m =< k) : n =< k \\elim n, nm, m\n" +
                   "  | zero, le_z, _ => {?}\n" +
                   "  | suc n', le_ss nm', suc m' => {?}", 1);
  }

  @Test
  public void testEmptyNoElimError() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d0\n" +
        "\\func test (x : Nat) (d : D x) : Nat \\elim d\n" +
        "  | () => 0", 1);
  }

  @Test
  public void testElimTranslationSubst() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef(
      "\\func test (n m : Nat) : Nat \\elim m\n" +
        " | _ => n"
    );
    assertEquals(new LeafElimTree(def.getParameters(), Ref(def.getParameters())), def.getBody());
  }

  @Test
  public void testElimTranslationSubst2() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef(
      "\\func test (n m : Nat) : Nat \\elim m\n" +
      " | zero => n\n" +
      " | _ => n"
    );
    DependentLink nParam = DependentLink.Helper.take(def.getParameters(), 1);
    Map<Constructor, ElimTree> children = new HashMap<>();
    children.put(Prelude.ZERO, new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nParam)));
    children.put(Prelude.SUC, new LeafElimTree(param("m", Nat()), Ref(nParam)));
    assertEquals(new BranchElimTree(nParam, children), def.getBody());
  }

  @Test
  public void testElimTranslationSubst3() {
    typeCheckModule(
      "\\data D | A | B | C\n" +
      "\\func f (n m : D) : D \\elim m\n" +
      " | A => n\n" +
      " | _ => n"
    );
    FunctionDefinition def = (FunctionDefinition) getDefinition("f");
    DataDefinition dataDef = (DataDefinition) getDefinition("D");
    DependentLink nParam = DependentLink.Helper.take(def.getParameters(), 1);
    Map<Constructor, ElimTree> children = new HashMap<>();
    children.put(dataDef.getConstructor("A"), new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nParam)));
    children.put(dataDef.getConstructor("B"), new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nParam)));
    children.put(dataDef.getConstructor("C"), new LeafElimTree(EmptyDependentLink.getInstance(), Ref(nParam)));
    assertEquals(new BranchElimTree(nParam, children), def.getBody());
  }

  @Test
  public void emptyAfterAFewError() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d\n" +
        "\\func test (x : Nat) (y : \\Pi(z : Nat) -> x = z) (a : D (suc x)) : Nat \\elim x\n", 1);
  }

  @Test
  public void emptyAfterAFew() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d\n" +
        "\\func test (x : Nat) (y : \\Pi(z : Nat) -> x = z) (a : D (suc x)) : Nat \\elim a\n");
  }

  @Test
  public void testElimEmpty1() {
    typeCheckModule(
        "\\data D Nat \\with | zero => d1 | suc zero => d2 \n" +
        "\\data E (n : Nat) | e (D n)\n" +
        "\\func test (n : Nat) (e : E n) : Nat\n" +
        "  | zero, _ => 0\n" +
        "  | suc zero, _ => 1\n" +
        "  | suc (suc _), e ()"
    );
  }

  @Test
  public void testMultiArg() {
    typeCheckModule(
      "\\data D (A B : \\Type0) | c A B\n" +
      "\\func test (f : Nat -> Nat) (d : D Nat (Nat -> Nat)) : Nat \\elim d\n" +
      "  | c x y => f x"
    );
  }

  @Test
  public void testEmptyCase() {
    typeCheckModule(
        "\\data D\n" +
        "\\func test (d : D) : 0 = 1 => \\case d \\with { () }"
    );
  }

  @Test
  public void threeVars() {
    typeCheckModule(
      "\\func f (x y z : Nat) : Nat\n" +
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
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func if (b : Bool) : \\Set | true => Nat | false => Nat -> Nat\n" +
      "\\func test (b : Bool) (x : if b) : Nat | true, zero => 0 | true, suc n => n | false, _ => 0"
    );
  }

  @Test
  public void numberElim() {
    typeCheckModule("\\func f (n : Nat) : Nat | 2 => 0 | 0 => 1 | 1 => 2 | suc (suc (suc n)) => n");
  }

  @Test
  public void numberElim2() {
    typeCheckModule("\\func f (n : Nat) : Nat | 0 => 1 | 1 => 2 | suc (suc (suc n)) => n", 1);
  }

  @Test
  public void threePatterns() {
    typeCheckModule(
      "\\func f (n m k : Nat) : Nat\n" +
      "  | zero, _, zero => 1\n" +
      "  | _, zero, suc _ => 2\n" +
      "  | _, _, _ => 0\n" +
      "\\func g (n : Nat) : f 0 n 0 = 1 => path (\\lam _ => 1)", 1);
  }

  @Test
  public void threePatterns2() {
    typeCheckModule(
      "\\func f (n m k : Nat) : Nat\n" +
      "  | zero, zero, _ => 1\n" +
      "  | _, zero, zero => 2\n" +
      "  | _, _, _ => 0\n" +
      "\\func g (n : Nat) : f 0 0 n = 1 => path (\\lam _ => 1)", 1);
  }

  @Test
  public void threePatternsError() {
    typeCheckModule(
      "\\func f (n m k : Nat) : Nat\n" +
      "  | _, zero, zero => 1\n" +
      "  | zero, zero, _ => 2\n" +
      "  | _, _, _ => 0\n" +
      "\\func g (n : Nat) : f 0 0 n = 2 => path (\\lam _ => 2)", 1);
  }

  @Test
  public void elimExpression() {
    parseModule(
      "\\func + (a b : Nat) => a\n" +
      "\\func f (a b : Nat) : Nat \\elim (a + b)\n" +
      "  | zero => zero\n" +
      "  | suc n' => zero", -1);
  }

  @Test
  public void testAbsurd() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func not (b : Bool) : Bool | true => false | false => true\n" +
      "\\data T (b : Bool) \\with | true => tt\n" +
      "\\func f (b : Bool) (p : T (not b)) : Nat\n" +
      "  | false, tt => 0");
  }
}
