package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ValidateTypeTest {

  private ValidateTypeVisitor.ErrorReporter fail(Expression expr) {
    ValidateTypeVisitor.ErrorReporter res = expr.checkType();
    assertTrue(res.errors() > 0);
    return res;
  }

  private ValidateTypeVisitor.ErrorReporter ok(Expression expr) {
    ValidateTypeVisitor.ErrorReporter res = expr.checkType();
    assertTrue(res.errors() == 0);
    return res;
  }

  private ValidateTypeVisitor.ErrorReporter ok(ElimTreeNode elimTree) {
    return ok(elimTree, null);
  }

  private ValidateTypeVisitor.ErrorReporter ok(ElimTreeNode elimTree, Expression type) {
    ValidateTypeVisitor visitor = new ValidateTypeVisitor();
    elimTree.accept(visitor, type);
    assertEquals(0, visitor.myErrorReporter.errors());
    return visitor.myErrorReporter;
  }

  private void checkFunction(String function, String classCode) {
    NamespaceMember member = typeCheckClass(classCode);
    FunctionDefinition fun = (FunctionDefinition) member.namespace.getMember(function).definition;
    ok(fun.getElimTree(), fun.getResultType());
  }

  @Test
  public void testId() {
    FunctionDefinition definition = (FunctionDefinition) typeCheckDef("\\function id {A : \\Set0} (a : A) => (\\lam (x : A) => x) a");
    Expression expr = ((LeafElimTreeNode) definition.getElimTree()).getExpression();
    ok(expr);
  }

  @Test(expected = AssertionError.class)
  public void testAppNotPi() {
    Expression expr = Apps(Nat(), Nat());
    fail(expr);
  }

  @Test
  public void testProjNotSigma() {
    Expression expr = Proj(Nat(), 0);
    fail(expr);
  }

  @Test
  public void testSigmaWrongType() {
    DependentLink param = param("x", Nat());
    param.setNext(param("y", Universe()));
    Expression expr = Proj(Tuple(Sigma(param), Zero(), Zero()), 0);
    fail(expr);
  }

  @Test
  public void testProjTriple() {
    DependentLink link = params(param("x", Nat()), param("y", Nat()), param("z", Nat()));
    Expression expr = Proj(Tuple(Sigma(link), Zero(), Zero(), Zero()), 2);
    ok(expr);
  }

  @Test(expected = IllegalStateException.class)
  public void testProjTooLargeIndex() {
    DependentLink link = params(param("x", Nat()), param("y", Nat()), param("z", Nat()));
    Expression expr = Proj(Tuple(Sigma(link), Zero(), Zero(), Zero()), 3);
    fail(expr);
  }

  @Test
  public void testSigma() {
    FunctionDefinition f = (FunctionDefinition) typeCheckDef(
            "\\function f (n : Nat) : \\Sigma (m : Nat) (suc n = suc m) => (n, path (\\lam (i : I) => suc n))");
    ok(f.getElimTree(), f.getResultType());
  }

  @Test
  public void testSqueeze1() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function\n" +
            "squeeze1 (i j : I) : I\n" +
            "    <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n");
    ok(fun.getElimTree(), fun.getResultType());
  }

  @Test
  public void testAt() {
    Expression at = typeCheckExpr("(@)", null).expression;
    PiExpression atType = (PiExpression) at.getType();
//    System.err.println(atType);
    DependentLink first = atType.getParameters();
//    System.err.println("first = " + first);
    DependentLink second = first.getNext();
//    System.err.println(second);
    Binding a2 = ((ReferenceExpression) ((AppExpression) second.getType()).getFunction()).getBinding();
//    System.err.println("a2 = " + a2);
//    System.err.println(a2 == first);
    assertEquals(a2, first);
  }

  @Test
  public void testFunEqToHom() {
    FunctionDefinition funEqToHom = (FunctionDefinition) typeCheckDef("\\function\n" +
            "funEqToHom {A : \\Type0} (B : A -> \\Type0) {f g : \\Pi (x : A) -> B x} (p : f = g) (x : A) : f x = g x => \n" +
            "    path (\\lam i => (p @ i) x)\n");
    AppExpression app = (AppExpression) ((LeafElimTreeNode)funEqToHom.getElimTree()).getExpression();
    List<? extends Expression> args = app.getArguments();
    Expression argType = args.get(0).getType();
    System.err.println(argType);
  }

  @Test
  public void testFunEqToHomNonDep() {
    FunctionDefinition funEqToHom = (FunctionDefinition) typeCheckDef("\\function\n" +
            "funEqToHom' {A B : \\Type0} {f g : A -> B} (p : f = g) (x : A) : f x = g x =>\n" +
            "    path (\\lam i => (p @ i) x)\n");
    AppExpression app = (AppExpression) ((LeafElimTreeNode)funEqToHom.getElimTree()).getExpression();
    List<? extends Expression> args = app.getArguments();
    Expression argType = args.get(0).getType();
    System.err.println(argType);
  }
  @Test
  public void testMoreArgsThanParamsOk() {
//    CheckTypeVisitor.Result e = typeCheckExpr("(\\lam (x: Nat) => suc) zero zero", null);
    CheckTypeVisitor.Result e = typeCheckExpr("\\lam (p : suc = suc) => (p @ left) zero", null);
    ok(e.expression);
    System.err.println(e.expression.getType());
  }

  @Test
  public void testLet() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function\n" +
            "f : Nat => \\let | n : Nat => 2 \\in n");
    ok(fun.getElimTree(), Nat());
  }

  @Test
  public void testCase() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function\n" +
            "isZero (n : Nat) : Nat => \\case n | zero => 1 | suc _ => 0");
    ok(fun.getElimTree(), Nat());
  }

  @Test
  public void testProj() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef(
            "\\function f : Nat => (\\lam (p : \\Sigma Nat Nat) => p.2) (1, 2)"
    );
    ok(fun.getElimTree(), Nat());
  }

  @Test
  public void testIIsContr() {
    checkFunction("I-isContr", "" +
            "\\static \\function squeeze1 (i j : I) : I\n" +
            "    <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
            "\\static \\function squeeze (i j : I)\n" +
            "    <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
            "\\static \\function isContr (A : \\Type0) => \\Sigma (a : A) (\\Pi (a' : A) -> a = a')\n" +
            "\\static \\function I-isContr : isContr I => (left, \\lam i => path (\\lam j => squeeze i j))\n"
            );
  }

  @Test
  public void testOfHlevel1IsProp() {
    checkFunction("ofHlevel1-isProp", "" +
            "\\static \\function isContr (A : \\Type0) => \\Sigma (a : A) (\\Pi (a' : A) -> a = a')\n" +
            "\\static \\function isProp (A : \\Type0) => \\Pi (a a' : A) -> a = a'\n" +
            "\\static \\function ofHlevel (n : Nat) (A : \\Type0) : \\Type0 <= \\elim n\n" +
            "    | zero => isContr A\n" +
            "    | suc n => \\Pi (a a' : A) -> ofHlevel n (a = a')\n" +
            "\\static \\function ofHlevel1-isProp (A : \\Type0) (f : ofHlevel (suc zero) A) : isProp A => \\lam a a' => (f a a').1\n"
    );
  }

  @Test
  public void testIsContrIsProp() {
    checkFunction("isContr-isProp", "" +
            "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
            "\\static \\function transport {A : \\Type0} (B : A -> \\Type0) {a a' : A} (p : a = a') (b : B a)\n" +
            "    <= coe (\\lam i => B (p @ i)) b right\n" +
            "\\static \\function inv {A : \\Type0} {a a' : A} (p : a = a')\n" +
            "    <= transport (\\lam a'' => a'' = a) p idp\n" +
            "\\static \\function concat {A : I -> \\Type0} {a : A left} {a' a'' : A right} (p : Path A a a') (q : a' = a'')\n" +
            "    <= transport (Path A a) q p\n" +
            "\\static \\function \\infixr 9\n" +
            "(*>) {A : \\Type0} {a a' a'' : A} (p : a = a') (q : a' = a'')\n" +
            "    <= concat p q\n" +
            "\\static \\function isContr (A : \\Type0) => \\Sigma (a : A) (\\Pi (a' : A) -> a = a')\n" +
            "\\static \\function isProp (A : \\Type0) => \\Pi (aa aa' : A) -> aa = aa'\n" +
            "\\static \\function isContr-isProp (A : \\Type0) (c : isContr A) : isProp A => \\lam aaa aaa' => inv (c.2 aaa) *> c.2 aaa'\n");
  }

  @Test
  public void testIsPropIsSet() {
    checkFunction("isProp-isSet", "" +
            "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
            "\\static \\function squeeze1 (i j : I) : I\n" +
            "    <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
            "\\static \\function squeeze (i j : I)\n" +
            "    <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
            "\\static \\function psqueeze {A : \\Type0} {a a' : A} (p : a = a') (i : I) : a = p @ i\n" +
            "    => path (\\lam j => p @ squeeze i j)\n" +
            "\\static \\function Jl {A : \\Type0} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type0) (b : B a idp) {a' : A} (p : a = a') : B a' p\n" +
            "    <= coe (\\lam i => B (p @ i) (psqueeze p i)) b right\n" +
            "\\static \\function transport {A : \\Type0} (B : A -> \\Type0) {a a' : A} (p : a = a') (b : B a)\n" +
            "    <= coe (\\lam i => B (p @ i)) b right\n" +
            "\\static \\function inv {A : \\Type0} {a a' : A} (p : a = a')\n" +
            "    <= transport (\\lam a'' => a'' = a) p idp\n" +
            "\\static \\function concat {A : I -> \\Type0} {a : A left} {a' a'' : A right} (p : Path A a a') (q : a' = a'')\n" +
            "    <= transport (Path A a) q p\n" +
            "\\static \\function \\infixr 9\n" +
            "(*>) {A : \\Type0} {a a' a'' : A} (p : a = a') (q : a' = a'')\n" +
            "    <= concat p q\n" +
            "\\static \\function inv-concat {A : \\Type0} {a a' : A} (p : a = a') : inv p *> p = idp\n" +
            "    <= Jl (\\lam _ q => inv q *> q = idp) idp p\n" +
            "\\static \\function isContr (A : \\Type0) => \\Sigma (a : A) (\\Pi (a' : A) -> a = a')\n" +
            "\\static \\function isProp (A : \\Type0) => \\Pi (a a' : A) -> a = a'\n" +
            "\\static \\function isSet (A : \\Type0) => \\Pi (a a' : A) -> isProp (a = a')\n" +
            "\\static \\function ofHlevel (n : Nat) (A : \\Type0) : \\Type0 <= \\elim n\n" +
            "    | zero => isContr A\n" +
            "    | suc n => \\Pi (a a' : A) -> ofHlevel n (a = a')\n" +
            "\\static \\function isProp-ofHlevel1 (A : \\Type0) (f : isProp A) : ofHlevel (suc zero) A => \\lam a a' => (inv (f a a) *> f a a', Jl (\\lam x q => inv (f a a) *> f a x = q) (inv-concat (f a a)))\n" +
            "\\static \\function isContr-isProp (A : \\Type0) (c : isContr A) : isProp A => \\lam a a' => inv (c.2 a) *> c.2 a'\n" +
            "\\static \\function isProp-isSet (A : \\Type0) (p : isProp A) : isSet A => \\lam a a' => isContr-isProp (a = a') (isProp-ofHlevel1 A p a a')\n"
    );
  }

  @Test
  public void testQinvToEquiv() {
    checkFunction("qinv-to-equiv", "" +
            "\\static \\function id {X : \\Type0} (x : X) => x\n" +
            "\\static \\function \\infixr 1\n" +
            "($) {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
            "\\static \\function \\infixr 8\n" +
            "o {X Y Z : \\Type0} (g : Y -> Z) (f : X -> Y) (x : X) => g $ f x\n" +
            "\\static \\function \\infix 2\n" +
            "(~) {A B : \\Type0} (f : A -> B) (g : A -> B) <= \\Pi (x : A) -> f x = g x\n" +
            "\\static \\function linv {A B : \\Type0} (f : A -> B) <= \\Sigma (g : B -> A) (g `o` f ~ id)\n" +
            "\\static \\function rinv {A B : \\Type0} (f : A -> B) <= \\Sigma (g : B -> A) (f `o` g ~ id)\n" +
            "\\static \\function isequiv {A B : \\Type0} (f : A -> B) <= \\Sigma (linv f) (rinv f)\n" +
            "\\static \\function qinv {A B : \\Type0} (f : A -> B) <= \\Sigma (g : B -> A) (g `o` f ~ id) (f `o` g ~ id)\n" +
            "\\static \\function qinv-to-equiv {A B : \\Type0} (f : A -> B) (x : qinv f) : isequiv f => \n" +
            "  ((x.1, x.2), (x.1, x.3))\n");
  }

  @Test
  public void testEquivSymm() {
    checkFunction("equiv-symm", "" +
            "\\static \\function pmap {A B : \\Type0} (f : A -> B) {a a' : A} (p : a = a')\n" +
            "    => path (\\lam i => f (p @ i))\n" +
            "\\static \\function transport {A : \\Type0} (B : A -> \\Type0) {a a' : A} (p : a = a') (b : B a)\n" +
            "    <= coe (\\lam i => B (p @ i)) b right\n" +
            "\\static \\function concat {A : I -> \\Type0} {a : A left} {a' a'' : A right} (p : Path A a a') (q : a' = a'')\n" +
            "    <= transport (Path A a) q p\n" +
            "\\static \\function \\infixr 9\n" +
            "(*>) {A : \\Type0} {a a' a'' : A} (p : a = a') (q : a' = a'')\n" +
            "    <= concat p q\n" +
            "\\static \\function id {X : \\Type0} (x : X) => x\n" +
            "\\static \\function \\infixr 1\n" +
            "($) {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
            "\\static \\function \\infixr 8\n" +
            "o {X Y Z : \\Type0} (g : Y -> Z) (f : X -> Y) (x : X) => g $ f x\n" +
            "\\static \\function \\infix 2\n" +
            "(~) {A B : \\Type0} (f : A -> B) (g : A -> B) <= \\Pi (x : A) -> f x = g x\n" +
            "\\static \\function linv {A B : \\Type0} (f : A -> B) <= \\Sigma (g : B -> A) (g `o` f ~ id)\n" +
            "\\static \\function rinv {A B : \\Type0} (f : A -> B) <= \\Sigma (g : B -> A) (f `o` g ~ id)\n" +
            "\\static \\function isequiv {A B : \\Type0} (f : A -> B) <= \\Sigma (linv f) (rinv f)\n" +
            "\\static \\function \\infix 2\n" +
            "(=~=) (A : \\Type0) (B : \\Type0) <= \\Sigma (f : A -> B) (isequiv f)\n" +
            "\\static \\function qinv {A B : \\Type0} (f : A -> B) <= \\Sigma (g : B -> A) (g `o` f ~ id) (f `o` g ~ id)\n" +
            "\\static \\function qinv-to-equiv {A B : \\Type0} (f : A -> B) (x : qinv f) : isequiv f => \n" +
            "  ((x.1, x.2), (x.1, x.3))\n" +
            "\\static \\function equiv-to-qinv {A B : \\Type0} (f : A -> B) (x : isequiv f) : qinv f => \n" +
            "  (x.1.1 `o` f `o` x.2.1, \n" +
            "   \\lam x' => pmap x.1.1 (x.2.2 (f x')) *> x.1.2 x',\n" +
            "   \\lam x' => pmap f (x.1.2 (x.2.1 x')) *> x.2.2 x'\n" +
            "  )\n" +
            "\\static \\function equiv-symm {A B : \\Type0} (e : A =~= B) : B =~= A =>\n" +
            "  \\let | qe => equiv-to-qinv e.1 e.2 \n" +
            "  \\in (qe.1, qinv-to-equiv qe.1 (e.1, qe.3, qe.2))\n"
    );
  }

  @Test
  public void testAxiomKImplIsSet() {
    checkFunction("axK-impl-isSet", "" +
            "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
            "\\static \\function squeeze1 (i j : I) : I\n" +
            "    <= coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
            "\\static \\function squeeze (i j : I)\n" +
            "    <= coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
            "\\static \\function psqueeze {A : \\Type0} {a a' : A} (p : a = a') (i : I) : a = p @ i\n" +
            "    => path (\\lam j => p @ squeeze i j)\n" +
            "\\static \\function Jl {A : \\Type0} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type0) (b : B a idp) {a' : A} (p : a = a') : B a' p\n" +
            "    <= coe (\\lam i => B (p @ i) (psqueeze p i)) b right\n" +
            "\\static \\function isProp (A : \\Type0) => \\Pi (a a' : A) -> a = a'\n" +
            "\\static \\function isSet (A : \\Type0) => \\Pi (a a' : A) -> isProp (a = a')\n" +
            "\\static \\function axiomK (A : \\Type0) => \\Pi (x : A) (p : x = x) -> (idp = p)\n" +
            "\\static \\function axK-impl-isSet (A : \\Type0) (axK : axiomK A) : (isSet A) => \n" +
            "    \\lam x y p => (Jl (\\lam z (p' : x = z) => \\Pi (q : x = z) ->  p' = q) (axK x) p)      \n"
    );
  }

  @Test
  public void testIdpOver() {
    checkFunction("idpOver", "" +
            "\\function\n" +
            "idpOver (A : I -> \\Type0) (a : A left) : Path A a (coe A a right) => path (coe A a)\n"
    );
  }
}
