package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.LetClause;
import com.jetbrains.jetpad.vclang.core.expr.LetExpression;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.frontend.reference.ParsedLocalReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertNotNull;

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
    List<Concrete.Parameter> arguments = new ArrayList<>(2);
    ParsedLocalReferable X = ref("X");
    ParsedLocalReferable x = ref("X");
    arguments.add(cTele(cvars(X), cUniverseStd(0)));
    arguments.add(cTele(cvars(x), cVar(X)));
    ConcreteGlobalReferable reference = new ConcreteGlobalReferable(null, "f", Precedence.DEFAULT);
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(reference, arguments, cVar(X), body(cVar(x)));
    reference.setDefinition(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), 0), null);
  }

  @Test
  public void prettyPrintingLet() {
    // \let x {A : Type0} (y : A) : A => y \in x Zero()
    SingleDependentLink A = singleParam("A", Universe(0));
    SingleDependentLink y = singleParam("y", Ref(A));
    LetClause clause = let("x", Lam(A, Lam(y, Ref(y))));
    LetExpression expr = new LetExpression(lets(clause), Apps(Ref(clause), Zero()));
    expr.prettyPrint(new StringBuilder(), PrettyPrinterConfig.DEFAULT);
  }

  @Test
  public void prettyPrintingPatternDataDef() {
    Concrete.Definition def = (Concrete.Definition) ((ConcreteGlobalReferable) parseDef("\\data LE Nat Nat \\with | zero, m => LE-zero | suc n, suc m => LE-suc (LE n m)").getReferable()).getDefinition();
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), Concrete.Expression.PREC), null);
  }

  @Test
  public void prettyPrintingDataWithConditions() {
    Concrete.Definition def = (Concrete.Definition) ((ConcreteGlobalReferable) parseDef("\\data Z | neg Nat | pos Nat { zero => neg zero }").getReferable()).getDefinition();
    assertNotNull(def);
    def.accept(new PrettyPrintVisitor(new StringBuilder(), Concrete.Expression.PREC), null);
  }

  private void testDefinition(String s) {
    ConcreteGlobalReferable def = resolveNamesDef(s);
    StringBuilder sb = new StringBuilder();
    PrettyPrintVisitor visitor = new PrettyPrintVisitor(sb, 0);
    ((Concrete.Definition) def.getDefinition()).accept(visitor, null);
    String s2 = sb.toString();
    ConcreteGlobalReferable def2 = resolveNamesDef(s2);
    //TODO: Current implementation only ensures that output of PrettyPrinter parses back but does not ensure that the parse result has not changed along the way
    //TODO: Implement some comparator for definitions to fix this
  }

  @Test
  public void prettyPrintData1() {
    String s1 =
        "\\data S1 \n" +
        "| base\n" +
        "| loop Nat \\with {\n" +
        "  | left => base\n" +
        "  | right => base\n" +
        "}";
    testDefinition(s1);
  }

  @Test
  public void prettyPrintClass1(){
    String s1 = "\\class C0 {\n" +
                "  | f0 : \\Pi {X Y : \\Type0} -> X -> Y -> \\Type0\n" +
                "  | f1 : \\Pi {X Y : \\Type0} (x : X) (y : Y) -> TrP (x = y)\n" +
                "}";
    testDefinition(s1);
  }

  @Test
  public void prettyPrintData2(){
    String s1 = "\\data D2 {A : \\Type0} (y : Nat) (x : Nat) \\elim x\n" +
                "    | suc x' => c0 (y = x')\n" +
                "    | suc x' => c1 (p : D2 y x')";
    testDefinition(s1);
  }

  @Test
  public void prettyPrintPiField(){
    String s1 = "\\func f {A : \\Type} (P : A -> \\Type): \\Pi (u : A) ((P u).1) -> A => {?}";
    testDefinition(s1);
  }
}
