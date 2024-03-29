package org.arend.term.expr.visitor;

import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.sort.Sort;
import org.arend.core.subst.Levels;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterConfigImpl;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteCompareVisitor;
import org.arend.term.group.AccessModifier;
import org.arend.term.group.ChildGroup;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.term.prettyprint.ToAbstractVisitor;
import org.arend.typechecking.TypeCheckingTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.term.concrete.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class PrettyPrintingTest extends TypeCheckingTestCase {

  public static PrettyPrinterConfig EMPTY = new PrettyPrinterConfig() {
    @Override
    public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
      return EnumSet.of(PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE);
    }

    @Override
    public @Nullable NormalizationMode getNormalizationMode() {
      return null;
    }
  };

  @Test
  public void prettyPrintingLam() {
    // \x. x x
    SingleDependentLink x = singleParam("x", Pi(Nat(), Nat()));
    Expression expr = Lam(x, Apps(Ref(x), Ref(x)));
    expr.prettyPrint(new StringBuilder(), PrettyPrinterConfig.DEFAULT);
  }

  @Test
  public void prettyPrintingLam2() {
    // \x. x (\y. y x) (\z w. x w z)
    SingleDependentLink x = singleParam("x", Pi(Nat(), Pi(Nat(), Nat())));
    SingleDependentLink y = singleParam("y", Pi(Nat(), Nat()));
    SingleDependentLink zw = singleParam(true, vars("z", "w"), Nat());
    Expression expr = Lam(x, Apps(Ref(x), Lam(y, Apps(Ref(y), Ref(x))), Lam(zw, Apps(Ref(x), Ref(zw.getNext()), Ref(zw)))));
    expr.prettyPrint(new StringBuilder(), PrettyPrinterConfig.DEFAULT);
  }

  @Test
  public void prettyPrintingU() {
    // (X : Type0) -> X -> X
    SingleDependentLink X = singleParam("x", Universe(0));
    Expression expr = Pi(X, Pi(singleParam(null, Ref(X)), Ref(X)));
    expr.prettyPrint(new StringBuilder(), PrettyPrinterConfig.DEFAULT);
  }

  @Test
  public void prettyPrintingPi() {
    // (t : Nat -> Nat -> Nat) (x y : Nat) (z w : Nat -> Nat) -> ((s : Nat) -> t (z s) (w x)) -> Nat
    SingleDependentLink t = singleParam("t", Pi(Nat(), Pi(Nat(), Nat())));
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    SingleDependentLink zw = singleParam(true, vars("z", "w"), Pi(singleParam(null, Nat()), Nat()));
    SingleDependentLink s = singleParam("s", Nat());
    Expression expr = Pi(t, Pi(xy, Pi(zw, Pi(singleParam(null, Pi(s, Apps(Ref(t), Apps(Ref(zw), Ref(s)), Apps(Ref(zw.getNext()), Ref(xy))))), Nat()))));
    expr.prettyPrint(new StringBuilder(), PrettyPrinterConfig.DEFAULT);
  }

  @Test
  public void prettyPrintingFunDef() {
    // f (X : Type0) (x : X) : X => x;
    List<Concrete.Parameter> arguments = new ArrayList<>(2);
    LocalReferable X = ref("X");
    LocalReferable x = ref("X");
    arguments.add(cTele(cvars(X), cUniverseStd(0)));
    arguments.add(cTele(cvars(x), cVar(X)));
    ConcreteLocatedReferable reference = new ConcreteLocatedReferable(null, AccessModifier.PUBLIC, "f", Precedence.DEFAULT, null, Precedence.DEFAULT, MODULE_REF, GlobalReferable.Kind.FUNCTION);
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(FunctionKind.FUNC, reference, arguments, cVar(X), null, body(cVar(x)));
    reference.setDefinition(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), 0), null);
  }

  @Test
  public void prettyPrintingLet() {
    // \let x {A : Type0} (y : A) : A => y \in x Zero()
    SingleDependentLink A = singleParam("A", Universe(0));
    SingleDependentLink y = singleParam("y", Ref(A));
    LetClause clause = let("x", Lam(A, Lam(y, Ref(y))));
    LetExpression expr = let(lets(clause), Apps(Ref(clause), Zero()));
    expr.prettyPrint(new StringBuilder(), PrettyPrinterConfig.DEFAULT);
  }

  @Test
  public void prettyPrintingPatternDataDef() {
    Concrete.Definition def = (Concrete.Definition) ((ConcreteLocatedReferable) parseDef("\\data LE Nat Nat \\with | zero, m => LE-zero | suc n, suc m => LE-suc (LE n m)").getReferable()).getDefinition();
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), Concrete.Expression.PREC), null);
  }

  @Test
  public void prettyPrintingDataWithConditions() {
    Concrete.Definition def = (Concrete.Definition) ((ConcreteLocatedReferable) parseDef("\\data Z | neg Nat | pos Nat { zero => neg zero }").getReferable()).getDefinition();
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), Concrete.Expression.PREC), null);
  }

  private void testDefinition(String s) {
    ConcreteLocatedReferable def = (ConcreteLocatedReferable) resolveNamesDef(s);
    StringBuilder sb = new StringBuilder();
    PrettyPrintVisitor visitor = new PrettyPrintVisitor(sb, 0);
    ((Concrete.Definition) def.getDefinition()).accept(visitor, null);
    String s2 = sb.toString();
    ConcreteLocatedReferable def2 = (ConcreteLocatedReferable) resolveNamesDef(s2);
    assertTrue(ConcreteCompareVisitor.compare(def.getDefinition(), def2.getDefinition()));
  }

  @Test
  public void prettyPrintData1() {
    testDefinition(
      """
        \\data S1\s
        | base
        | loop Nat \\with {
          | left => base
          | right => base
        }
        """);
  }

  @Test
  public void prettyPrintClass1() {
    testDefinition(
      """
        \\class C0 {
          | f0 : \\Pi {X Y : \\Type0} -> X -> Y -> \\Type0
          | f1 : \\Pi {X Y : \\Type0} (x : X) (y : Y) -> x = y
        }
      """);
  }

  @Test
  public void prettyPrintData2() {
    testDefinition(
      """
        \\data D2 {A : \\Type0} (y : Nat) (x : Nat) \\elim x
            | suc x' => c0 (y = x')
            | suc x' => c1 (p : D2 y x')
        """);
  }

  @Test
  public void prettyPrintPiField() {
    testDefinition("\\func f {A : \\Type} (P : A -> \\Type): \\Pi (u : A) ((P u).1) -> A => {?}");
  }

  @Test
  public void prettyPrintEmptyTuple() {
    testDefinition("\\func f => ()");
  }

  @Test
  public void prettyPrintSigma() {
    testDefinition("\\func f => \\Sigma (x : Nat)");
  }

  @Test
  public void prettyPrintEmptySigma() {
    testDefinition("\\func f => \\Sigma");
  }

  @Test
  public void fieldCallTest() {
    ClassDefinition def = (ClassDefinition) typeCheckDef("\\record R (f : Nat)");
    Expression expr = FieldCallExpression.make(def.getPersonalFields().get(0), new ReferenceExpression(new TypedBinding("r", new ClassCallExpression(def, Levels.EMPTY))));
    assertEquals("r.f", expr.toString());
  }

  @Test
  public void boundRenamedTest() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func f (x y : Nat) => x");
    SingleDependentLink lamParam = singleParam("f", Nat());
    Expression expr = new LamExpression(Sort.SET0, lamParam, FunCallExpression.make(def, Levels.EMPTY, Arrays.asList(new ReferenceExpression(lamParam), new ReferenceExpression(lamParam))));
    assertEquals("\\lam (f1 : Nat) => f f1 f1", expr.toString());
  }

  @Test
  public void twoDefsTest() {
    typeCheckModule(
      "\\func foo (x : Nat) => x\n" +
      "\\module M \\where { \\func foo => 1 }");
    Expression expr = FunCallExpression.make((FunctionDefinition) getDefinition("foo"), Levels.EMPTY, Collections.singletonList(FunCallExpression.make((FunctionDefinition) getDefinition("M.foo"), Levels.EMPTY, Collections.emptyList())));
    assertEquals(MODULE_PATH + ".foo M.foo", expr.toString());
  }

  @Test
  public void twoDefsTest2() {
    typeCheckModule(
      "\\module M \\where { \\func foo (x : Nat) => x }\n" +
      "\\func foo => 1");
    Expression expr = FunCallExpression.make((FunctionDefinition) getDefinition("M.foo"), Levels.EMPTY, Collections.singletonList(FunCallExpression.make((FunctionDefinition) getDefinition("foo"), Levels.EMPTY, Collections.emptyList())));
    assertEquals("M.foo " + MODULE_PATH + ".foo", expr.toString());
  }

  @Test
  public void twoDefsTest3() {
    typeCheckModule(
      "\\module M \\where { \\func foo (x : Nat) => x }\n" +
      "\\module N \\where { \\func foo => 1 }");
    Expression expr = FunCallExpression.make((FunctionDefinition) getDefinition("M.foo"), Levels.EMPTY, Collections.singletonList(FunCallExpression.make((FunctionDefinition) getDefinition("N.foo"), Levels.EMPTY, Collections.emptyList())));
    assertEquals("M.foo N.foo", expr.toString());
  }

  @Test
  public void twoDefsBoundNotRenamedTest() {
    typeCheckModule(
      "\\module M \\where { \\func foo (x y : Nat) => x }\n" +
      "\\module N \\where { \\func foo => 1 }");
    SingleDependentLink lamParam = singleParam("foo", Nat());
    Expression expr = new LamExpression(Sort.SET0, lamParam, FunCallExpression.make((FunctionDefinition) getDefinition("M.foo"), Levels.EMPTY, Arrays.asList(new ReferenceExpression(lamParam), FunCallExpression.make((FunctionDefinition) getDefinition("N.foo"), Levels.EMPTY, Collections.emptyList()))));
    assertEquals("\\lam (foo : Nat) => M.foo foo N.foo", expr.toString());
  }

  private String printTestExpr() {
    return ToAbstractVisitor.convert((Expression) ((FunctionDefinition) getDefinition("test")).getBody(), new PrettyPrinterConfig() {
      @Override
      public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
        return EnumSet.noneOf(PrettyPrinterFlag.class);
      }
    }).toString();
  }

  @Test
  public void prefixConstructorRight() {
    typeCheckModule(
      "\\data List (A : \\Type) | nil | cons A (List A)\n" +
      "\\func test => cons 0 (cons 1 (cons 2 nil))");
    assertEquals("cons 0 (cons 1 (cons 2 nil))", printTestExpr());
  }

  @Test
  public void prefixConstructorLeft() {
    typeCheckModule(
      "\\data List (A : \\Type) | nil | cons (List A) A\n" +
      "\\func test => cons (cons (cons nil 2) 1) 0");
    assertEquals("cons (cons (cons nil 2) 1) 0", printTestExpr());
  }

  @Test
  public void infixConstructorRight() {
    typeCheckModule(
      "\\data List (A : \\Type) | nil | \\infixr 5 :: A (List A)\n" +
      "\\func test => 0 :: 1 :: 2 :: nil");
    assertEquals("0 :: 1 :: 2 :: nil", printTestExpr());
  }

  @Test
  public void infixConstructorLeft() {
    typeCheckModule(
      "\\data List (A : \\Type) | nil | \\infixr 5 :: (List A) A\n" +
      "\\func test => ((nil :: 2) :: 1) :: 0");
    assertEquals("((nil :: 2) :: 1) :: 0", printTestExpr());
  }

  @Test
  public void infixConstructorLeftAssoc() {
    typeCheckModule(
      "\\data List (A : \\Type) | nil | \\infixl 5 :: (List A) A\n" +
      "\\func test => ((nil :: 2) :: 1) :: 0");
    assertEquals("nil :: 2 :: 1 :: 0", printTestExpr());
  }

  @Test
  public void letTest() {
    String expr = "\n  \\let x => 0\n  \\in x";
    typeCheckModule("\\func test =>" + expr);
    assertEquals(expr, printTestExpr());
  }

  @Test
  public void letTest2() {
    String expr = "\n  \\let (x, y) => (0, 1)\n  \\in (x, y)";
    typeCheckModule("\\func test =>" + expr);
    assertEquals(expr, printTestExpr());
  }

  private void testRevealing(String module, Function<ChildGroup, Expression> moduleSelector, String expected, Function<Expression, Expression> selector) {
    ChildGroup result = typeCheckModule(module);
    Expression baseExpr = moduleSelector.apply(result);
    Expression incremented = selector.apply(baseExpr);
    PrettyPrinterConfig config = new PrettyPrinterConfigImpl(EMPTY) {
      @Override
      public int getVerboseLevel(@NotNull CoreExpression expression) {
        return expression == incremented ? 1 : 0;
      }
    };
    assertEquals(expected, ToAbstractVisitor.convert(baseExpr, config).toString());
  }

  @Test
  public void revealing1() {
    testRevealing("\\func f : idp = {1 = 1} idp => idp",
            (group) -> ((FunctionDefinition) getDefinition(group, "f")).getResultType(),
            "idp {Nat} = idp",
            (result) -> result.cast(FunCallExpression.class).getDefCallArguments().get(1));
  }

  @Test
  public void revealing2() {
    testRevealing("\\func f : idp = {1 = 1} idp => idp",
            (group) -> ((FunctionDefinition) getDefinition(group, "f")).getResultType(),
            "idp = {1 = 1} idp",
            (result) -> result);
  }

  @Test
  public void revealing3() {
    testRevealing(
      "\\class A { | f {n : Nat} : n = 1 }\n" +
      "\\func e (q : A) (p : q.f = idp) : q.f = idp => p",
            (group) -> ((FunctionDefinition) getDefinition(group, "e")).getResultType(),
            "q.f {1} = idp",
            (result) -> result.cast(FunCallExpression.class).getDefCallArguments().get(1).cast(AppExpression.class).getFunction());
  }

  private void testLamPatterns(String body) {
    Concrete.FunctionDefinition def = (Concrete.FunctionDefinition) getDefinition(resolveNamesDef("\\func foo => " + body));
    assertEquals(body, Objects.requireNonNull(def.getBody().getTerm()).toString());
  }

  @Test
  public void lamPatternsTest1() {
    testLamPatterns("\\lam n (path f) m => f");
  }

  @Test
  public void lamPatternsTest2() {
    testLamPatterns("\\lam (path f) m (path g) => f");
  }

  @Test
  public void lamPatternsTest3() {
    testLamPatterns("\\lam (path f) (path g) => f");
  }

  @Test
  public void lamPatternsTest4() {
    testLamPatterns("\\lam n m => n");
  }
}
