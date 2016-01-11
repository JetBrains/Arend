package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NormalizationTest {
  Namespace testNS;
  // \function (+) (x y : Nat) : Nat <= elim x | zero => y | suc x' => suc (x' + y)
  private final FunctionDefinition plus;
  // \function (*) (x y : Nat) : Nat <= elim x | zero => zero | suc x' => y + x' * y
  private final FunctionDefinition mul;
  // \function fac (x : Nat) : Nat <= elim x | zero => suc zero | suc x' => suc x' * fac x'
  private final FunctionDefinition fac;
  // \function nelim (z : Nat) (s : Nat -> Nat -> Nat) (x : Nat) : Nat <= elim x | zero => z | suc x' => s x' (nelim z s x')
  private final FunctionDefinition nelim;

  private DataDefinition bdList;
  private Constructor bdNil;
  private Constructor bdCons;
  private Constructor bdSnoc;


  public NormalizationTest() {
    testNS = new Namespace("test");
    BranchElimTreeNode plusElimTree = branch(1);
    plus = new FunctionDefinition(testNS, new Name("+", Abstract.Definition.Fixity.INFIX), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 6), args(Tele(vars("x", "y"), Nat())), Nat(), plusElimTree);
    testNS.addDefinition(plus);
    plusElimTree.addClause(Prelude.ZERO, leaf(Index(0)));
    plusElimTree.addClause(Prelude.SUC, leaf(Suc(BinOp(Index(0), plus, Index(1)))));

    BranchElimTreeNode mulElimTree = branch(1);
    mul = new FunctionDefinition(testNS, new Name("*", Abstract.Definition.Fixity.INFIX), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 7), args(Tele(vars("x", "y"), Nat())), Nat(), mulElimTree);
    testNS.addDefinition(mul);
    mulElimTree.addClause(Prelude.ZERO, leaf(Zero()));
    mulElimTree.addClause(Prelude.SUC, leaf(BinOp(Index(0), plus, BinOp(Index(1), mul, Index(0)))));

    BranchElimTreeNode facElimTree = branch(0);
    fac = new FunctionDefinition(testNS, new Name("fac"), Abstract.Definition.DEFAULT_PRECEDENCE, args(Tele(vars("x"), Nat())), Nat(), facElimTree);
    testNS.addDefinition(fac);
    facElimTree.addClause(Prelude.ZERO, leaf(Suc(Zero())));
    facElimTree.addClause(Prelude.SUC, leaf(BinOp(Suc(Index(0)), mul, Apps(FunCall(fac), Index(0)))));

    BranchElimTreeNode nelimElimTree = branch(0);
    nelim = new FunctionDefinition(testNS, new Name("nelim"), Abstract.Definition.DEFAULT_PRECEDENCE, args(Tele(vars("z"), Nat()), Tele(vars("s"), Pi(Nat(), Pi(Nat(), Nat()))), Tele(vars("x"), Nat())), Nat(), nelimElimTree);
    testNS.addDefinition(nelim);
    nelimElimTree.addClause(Prelude.ZERO, leaf(Index(1)));
    nelimElimTree.addClause(Prelude.SUC, leaf(Apps(Index(1), Index(0), Apps(FunCall(nelim), Index(2), Index(1), Index(0)))));
  }

  private void initializeBDList() {
    ClassDefinition classDefinition = typeCheckClass(
        "\\static \\data BD-list (A : \\Type0) | nil | cons A (BD-list A) | snoc (BD-list A) A" +
            "\\with | snoc (cons x xs) x => cons x (snoc xs x) | snoc nil x => cons x nil\n"
    );
    bdList = (DataDefinition) classDefinition.getResolvedName().toNamespace().getDefinition("BD-list");
    bdNil = bdList.getConstructor("nil");
    bdCons = bdList.getConstructor("cons");
    bdSnoc = bdList.getConstructor("snoc");
  }

  @Test
  public void normalizeLamId() {
    // normalize( (\x.x) (suc zero) ) = suc zero
    Expression expr = Apps(Lam("x", Nat(), Index(0)), Suc(Zero()));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeLamK() {
    // normalize( (\x y. x) (suc zero) ) = \z. suc zero
    Expression expr = Apps(Lam("x", Nat(), Lam("y", Nat(), Index(1))), Suc(Zero()));
    assertEquals(Lam("z", Nat(), Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeLamKstar() {
    // normalize( (\x y. y) (suc zero) ) = \z. z
    Expression expr = Apps(Lam("x", Nat(), Lam("y", Nat(), Index(0))), Suc(Zero()));
    assertEquals(Lam("z", Nat(), Index(0)), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeLamKOpen() {
    // normalize( (\x y. x) (suc (var(0))) ) = \z. suc (var(0))
    Expression expr = Apps(Lam("x", Nat(), Lam("y", Nat(), Index(1))), Suc(Index(0)));
    assertEquals(Lam("z", Nat(), Suc(Index(1))), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeNelimZero() {
    // normalize( N-elim (suc zero) suc 0 ) = suc zero
    Expression expr = Apps(FunCall(nelim), Suc(Zero()), Suc(), Zero());
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeNelimOne() {
    // normalize( N-elim (suc zero) (\x y. (var(0)) y) (suc zero) ) = var(0) (suc zero)
    Expression expr = Apps(FunCall(nelim), Suc(Zero()), Lam("x", Nat(), Lam("y", Nat(), Apps(Index(2), Index(0)))), Suc(Zero()));
    assertEquals(Apps(Index(0), Suc(Zero())), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeNelimArg() {
    // normalize( N-elim (suc zero) (var(0)) ((\x. x) zero) ) = suc zero
    Expression arg = Apps(Lam("x", Nat(), Index(0)), Zero());
    Expression expr = Apps(FunCall(nelim), Suc(Zero()), Index(0), arg);
    Expression result = expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>());
    assertEquals(Suc(Zero()), result);
  }

  @Test
  public void normalizePlus0a3() {
    // normalize (plus 0 3) = 3
    Expression expr = BinOp(Zero(), plus, Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizePlus3a0() {
    // normalize (plus 3 0) = 3
    Expression expr = BinOp(Suc(Suc(Suc(Zero()))), plus, Zero());
    assertEquals(Suc(Suc(Suc(Zero()))), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizePlus3a3() {
    // normalize (plus 3 3) = 6
    Expression expr = BinOp(Suc(Suc(Suc(Zero()))), plus, Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeMul3a0() {
    // normalize (mul 3 0) = 0
    Expression expr = BinOp(Suc(Suc(Suc(Zero()))), mul, Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeMul0a3() {
    // normalize (mul 0 3) = 0
    Expression expr = BinOp(Zero(), mul, Suc(Suc(Suc(Zero()))));
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeMul3a3() {
    // normalize (mul 3 3) = 9
    Expression expr = BinOp(Suc(Suc(Suc(Zero()))), mul, Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero()))))))))), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeFac3() {
    // normalize (fac 3) = 6
    Expression expr = Apps(FunCall(fac), Suc(Suc(Suc(Zero()))));
    assertEquals(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  private static Expression typecheckExpression(Abstract.Expression expr) {
    return typecheckExpression(expr, new ArrayList<Binding>());
  }

  private static Expression typecheckExpression(Abstract.Expression expr, List<Binding> ctx) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(ctx, errorReporter).build(), null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertTrue(result.equations.isEmpty());
    return result.expression;
  }

  @Test
  public void normalizeLet1() {
    // normalize (\let | x => zero \in \let | y = suc \in y x) = 1
    Expression expr = typecheckExpression(cLet(clets(clet("x", cZero())), cLet(clets(clet("y", cSuc())), cApps(cVar("y"), cVar("x")))));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeLet2() {
    // normalize (\let | x => suc \in \let | y = zero \in x y) = 1
    Expression expr = typecheckExpression(cLet(clets(clet("x", cSuc())), cLet(clets(clet("y", cZero())), cApps(cVar("x"), cVar("y")))));
    assertEquals(Suc(Zero()), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeLetNo() {
    // normalize (\let | x (y z : N) => zero \in x zero) = \lam (z : N) => zero
    Expression expr = typecheckExpression(cLet(clets(clet("x", cargs(cTele(cvars("y", "z"), cNat())), cZero())), cApps(cVar("x"), cZero())));
    assertEquals(Lam("x", Nat(), Zero()), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeLetElimStuck() {
    // normalize (\let | x (y : N) : N <= \elim y | zero => zero | suc _ => zero \in x <1>) = the same
    Concrete.Expression elimTree = cElim(Collections.<Concrete.Expression>singletonList(cVar("y")), cClause(cPatterns(cConPattern(Prelude.ZERO.getName())), Abstract.Definition.Arrow.RIGHT, cZero()), cClause(cPatterns(cConPattern(Prelude.SUC.getName(), cPatternArg(cNamePattern(null), true, false))), Abstract.Definition.Arrow.RIGHT, cZero()));
    Expression expr = typecheckExpression(cLet(clets(clet("x", cargs(cTele(cvars("y"), cNat())), cNat(), Abstract.Definition.Arrow.LEFT, elimTree)),
        cApps(cVar("x"), cVar("n"))), new ArrayList<Binding>(Collections.singleton(new TypedBinding("n", Nat()))));
    assertEquals(expr, expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeLetElimNoStuck() {
    // normalize (\let | x (y : N) : \Type2 <= \elim y | \Type0 => \Type1 | succ _ => \Type1 \in x zero) = \Type0
    Concrete.Expression elimTree = cElim(Collections.<Concrete.Expression>singletonList(cVar("y")),
        cClause(cPatterns(cConPattern(Prelude.ZERO.getName())), Abstract.Definition.Arrow.RIGHT, cUniverse(0)),
        cClause(cPatterns(cConPattern(Prelude.SUC.getName(), cPatternArg(cNamePattern(null), true, false))), Abstract.Definition.Arrow.RIGHT, cUniverse(1))
    );
    Expression expr = typecheckExpression(cLet(clets(clet("x", cargs(cTele(cvars("y"), cNat())), cUniverse(2), Abstract.Definition.Arrow.LEFT, elimTree)), cApps(cVar("x"), cZero())));
    assertEquals(Universe(0), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void normalizeElimAnyConstructor() {
    ClassDefinition def = typeCheckClass(
        "\\static \\data D | d Nat\n" +
        "\\static \\function test (x : D) : Nat <= \\elim x | _! => 0");
    FunctionDefinition test = (FunctionDefinition) def.getParentNamespace().getChild(def.getName()).getMember("test").definition;
    assertEquals(Apps(FunCall(test), Index(0)).normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()), Apps(FunCall(test), Index(0)));
  }

  @Test
  public void letNormalizationContext() {
    List<Binding> ctx = new ArrayList<>();
    Let(lets(let("x", typeArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, Zero())), Index(0)).normalize(NormalizeVisitor.Mode.NF, ctx);
    assertTrue(ctx.isEmpty());
  }

  @Test
  public void testConditionNormalization() {
    typeCheckClass(
        "\\static \\data Z | pos Nat | neg Nat \\with | pos zero => neg 0\n" +
            "\\static \\function only-one-zero : pos 0 = neg 0 => path (\\lam _ => pos 0)"
    );
  }

  @Test
  public void testConCallNormFull() {
    initializeBDList();
    Expression expr1 = Apps(ConCall(bdSnoc, Nat()), ConCall(bdNil, Nat()), Zero());
    assertEquals(Apps(ConCall(bdCons, Nat()), Zero(), ConCall(bdNil, Nat())), expr1.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void testConCallPartial() {
    initializeBDList();
    Expression expr1 = Apps(ConCall(bdSnoc, Nat()), ConCall(bdNil, Nat()));
    assertEquals(Lam(teleArgs(Tele(vars("y"), Nat())), Apps(ConCall(bdCons, Nat()), Index(0), ConCall(bdNil, Nat()))), expr1.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void testFuncNorm() {
    Expression expr1 = Apps(FunCall(Prelude.PATH_INFIX), Nat(), Zero());
    assertEquals(
        Lam(teleArgs(Tele(vars("a'"), Nat())), Apps(FunCall(Prelude.PATH_INFIX), Nat(), Zero(), Index(0))).normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()),
        expr1.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>())
    );
  }

  @Test
  public void testIsoleft() {
    Expression expr = Apps(FunCall(Prelude.ISO), Index(0), Index(1), Index(2), Index(3), Index(4), Index(5), ConCall(Prelude.LEFT));
    assertEquals(Index(0), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void testIsoRight() {
    Expression expr = Apps(FunCall(Prelude.ISO), Index(0), Index(1), Index(2), Index(3), Index(4), Index(5), ConCall(Prelude.RIGHT));
    assertEquals(Index(1), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void testCoeIso() {
    Expression expr = Apps(FunCall(Prelude.COERCE), Lam("k", DataCall(Prelude.INTERVAL), Apps(FunCall(Prelude.ISO), Index(1), Index(2), Index(3), Index(4), Index(5), Index(6), Index(0))), Index(6), ConCall(Prelude.RIGHT));
    assertEquals(Apps(Index(2), Index(6)), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void testCoeIsoFreeVar() {
    Expression expr = Apps(FunCall(Prelude.COERCE), Lam("k", DataCall(Prelude.INTERVAL), Apps(FunCall(Prelude.ISO), Apps(DataCall(Prelude.PATH), Lam("i", DataCall(Prelude.INTERVAL), DataCall(Prelude.INTERVAL)), Index(0), Index(0)), Index(2), Index(3), Index(4), Index(5), Index(6), Index(0))), Index(6), ConCall(Prelude.RIGHT));
    assertEquals(expr, expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }

  @Test
  public void testAppProj() {
    Expression expr = Apps(Proj(Tuple(Sigma(typeArgs(TypeArg(Pi(Nat(), Nat())))), Lam("x", Nat(), Index(0))), 0), Zero());
    assertEquals(Zero(), expr.normalize(NormalizeVisitor.Mode.NF, new ArrayList<Binding>()));
  }
}
