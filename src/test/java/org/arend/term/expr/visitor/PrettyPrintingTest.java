package org.arend.term.expr.visitor;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.LetExpression;
import org.arend.core.expr.let.LetClause;
import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.term.FunctionKind;
import org.arend.term.Precedence;
import org.arend.term.concrete.Concrete;
import org.arend.term.expr.ConcreteCompareVisitor;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.term.prettyprint.PrettyPrinterConfig;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.term.concrete.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PrettyPrintingTest extends TypeCheckingTestCase {
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
    List<Concrete.TelescopeParameter> arguments = new ArrayList<>(2);
    LocalReferable X = ref("X");
    LocalReferable x = ref("X");
    arguments.add(cTele(cvars(X), cUniverseStd(0)));
    arguments.add(cTele(cvars(x), cVar(X)));
    ConcreteLocatedReferable reference = new ConcreteLocatedReferable(null, "f", Precedence.DEFAULT, MODULE_PATH, GlobalReferable.Kind.TYPECHECKABLE);
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
    ConcreteLocatedReferable def = resolveNamesDef(s);
    StringBuilder sb = new StringBuilder();
    PrettyPrintVisitor visitor = new PrettyPrintVisitor(sb, 0);
    ((Concrete.Definition) def.getDefinition()).accept(visitor, null);
    String s2 = sb.toString();
    ConcreteLocatedReferable def2 = resolveNamesDef(s2);
    assertTrue(ConcreteCompareVisitor.compare(def.getDefinition(), def2.getDefinition()));
  }

  @Test
  public void prettyPrintData1() {
    testDefinition(
      "\\data S1 \n" +
      "| base\n" +
      "| loop Nat \\with {\n" +
      "  | left => base\n" +
      "  | right => base\n" +
      "}");
  }

  @Test
  public void prettyPrintClass1() {
    testDefinition(
      "\\class C0 {\n" +
      "  | f0 : \\Pi {X Y : \\Type0} -> X -> Y -> \\Type0\n" +
      "  | f1 : \\Pi {X Y : \\Type0} (x : X) (y : Y) -> x = y\n" +
      "}");
  }

  @Test
  public void prettyPrintData2() {
    testDefinition(
      "\\data D2 {A : \\Type0} (y : Nat) (x : Nat) \\elim x\n" +
      "    | suc x' => c0 (y = x')\n" +
      "    | suc x' => c1 (p : D2 y x')");
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
}
