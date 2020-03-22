package org.arend.naming;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.ToAbstractVisitor;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.sort.Sort;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.reference.Precedence;
import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.LocalReferable;
import org.arend.prelude.Prelude;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.PrettyPrintVisitor;
import org.arend.typechecking.TypeCheckingTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.arend.term.concrete.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrettyPrintingParserTest extends TypeCheckingTestCase {
  private void testExpr(String expected, Expression expr, EnumSet<PrettyPrinterFlag> flags) {
    StringBuilder builder = new StringBuilder();
    ToAbstractVisitor.convert(expr, new PrettyPrinterConfig() {
      @NotNull
      @Override
      public EnumSet<PrettyPrinterFlag> getExpressionFlags() {
        return flags;
      }

      @Override
      public NormalizationMode getNormalizationMode() {
        return null;
      }
    }).accept(new PrettyPrintVisitor(new AssertedTextBuilder(builder), 0), new Precedence(Concrete.Expression.PREC));
    assertEquals(expected, builder.toString());
  }

  private void testExpr(String expected, Expression expr) {
    testExpr(expected, expr, EnumSet.of(PrettyPrinterFlag.SHOW_TYPES_IN_LAM, PrettyPrinterFlag.SHOW_IMPLICIT_ARGS));
  }

  private void testExpr(Concrete.Expression expected, Expression expr, EnumSet<PrettyPrinterFlag> flags) {
    StringBuilder builder = new StringBuilder();
    ToAbstractVisitor.convert(expr, new PrettyPrinterConfig() {
      @NotNull
      @Override
      public EnumSet<PrettyPrinterFlag> getExpressionFlags() {
        return flags;
      }

      @Override
      public NormalizationMode getNormalizationMode() {
        return null;
      }
    }).accept(new PrettyPrintVisitor(new AssertedTextBuilder(builder), 0), new Precedence(Concrete.Expression.PREC));
    Concrete.Expression result = resolveNamesExpr(builder.toString());
    assertTrue(compareAbstract(expected, result));
  }

  private void testExpr(Concrete.Expression expected, Expression expr) {
    testExpr(expected, expr, EnumSet.of(PrettyPrinterFlag.SHOW_TYPES_IN_LAM, PrettyPrinterFlag.SHOW_IMPLICIT_ARGS));
  }

  private void testDef(Concrete.FunctionDefinition expected, Concrete.FunctionDefinition def) {
    StringBuilder builder = new StringBuilder();
    def.accept(new PrettyPrintVisitor(builder, 0), null);

    Concrete.FunctionDefinition result = (Concrete.FunctionDefinition) resolveNamesDef(builder.toString()).getDefinition();
    List<Concrete.TypeParameter> expectedParams = new ArrayList<>();
    for (Concrete.Parameter parameter : expected.getParameters()) {
      expectedParams.add((Concrete.TypeParameter) parameter);
    }
    List<Concrete.TypeParameter> resultParams = new ArrayList<>();
    for (Concrete.Parameter parameter : result.getParameters()) {
      resultParams.add((Concrete.TypeParameter) parameter);
    }
    Concrete.Expression expectedType = cPi(expectedParams, expected.getResultType());
    Concrete.Expression actualType = cPi(resultParams, result.getResultType());
    assertTrue(compareAbstract(expectedType, actualType));
    assertTrue(result.getBody() instanceof Concrete.TermFunctionBody);
    assertTrue(compareAbstract(
      cLam(new ArrayList<>(expected.getParameters()), ((Concrete.TermFunctionBody) expected.getBody()).getTerm()),
      cLam(new ArrayList<>(result.getParameters()), ((Concrete.TermFunctionBody) result.getBody()).getTerm())));
  }

  @Test
  public void prettyPrintingParserLamApp() {
    // \a. a (\x y. x (x y)) (\x y. x) (a (\x. x) (\x. x))
    LocalReferable cx = ref("x");
    LocalReferable cy = ref("y");
    LocalReferable ca = ref("a");
    Concrete.Expression expected = cLam(cargs(cTele(cvars(ca), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cUniverseInf(1))))), cApps(cVar(ca), cLam(cargs(cTele(cvars(cx, cy), cPi(cUniverseInf(1), cUniverseInf(1)))), cApps(cVar(cx), cApps(cVar(cx), cVar(cy)))), cLam(cargs(cTele(cvars(cx, cy), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx)), cApps(cVar(ca), cLam(cargs(cTele(cvars(cx), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx)), cLam(cargs(cTele(cvars(cx), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx)))));
    SingleDependentLink x = singleParam("x", Pi(Universe(1), Universe(1)));
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Pi(Universe(1), Universe(1)));
    SingleDependentLink a = singleParam("a", Pi(Universe(1), Pi(Universe(1), Universe(1))));
    Expression expr = Lam(a, Apps(Ref(a), Lam(xy, Apps(Ref(xy), Apps(Ref(xy), Ref(xy.getNext())))), Lam(xy, Ref(xy)), Apps(Ref(a), Lam(x, Ref(x)), Lam(x, Ref(x)))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() {
    // (x y z : \Type1 -> \Type1 -> \Type1) -> \Type1 -> \Type1 -> (x y -> y x) -> z x y
    LocalReferable x = ref("x");
    LocalReferable y = ref("y");
    LocalReferable z = ref("z");
    Concrete.Expression expected = cPi(ctypeArgs(cTele(cvars(x, y, z), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cUniverseInf(1))))), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cPi(cPi(cApps(cVar(x), cVar(y)), cApps(cVar(y), cVar(x))), cApps(cVar(z), cVar(x), cVar(y))))));
    SingleDependentLink xyz = singleParams(true, vars("x", "y", "z"), Pi(Universe(1), Pi(Universe(1), Universe(1))));
    Expression expr = Pi(xyz, Pi(Universe(1), Pi(Universe(1), Pi(Pi(Apps(Ref(xyz), Ref(xyz.getNext())), Apps(Ref(xyz.getNext()), Ref(xyz))), Apps(Ref(xyz.getNext().getNext()), Ref(xyz), Ref(xyz.getNext()))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() {
    // (w : \Type1 -> \Type1 -> \Type1 -> \Type1 -> \Type1) (x : \Type1) {y z : \Type1} -> \Type1 -> (t z' : \Type1) {x' : \Type1 -> \Type1} -> w x' y z' t
    LocalReferable cy = ref("y");
    LocalReferable cz = ref("z");
    LocalReferable ct = ref("t");
    LocalReferable cx_ = ref("x'");
    LocalReferable cz_ = ref("z'");
    LocalReferable cw = ref("w");
    Concrete.Expression expected = cPi(cw, cPi(cUniverseInf(1), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cUniverseInf(1))))), cPi(cUniverseInf(1), cPi(ctypeArgs(cTele(false, cvars(cy, cz), cUniverseInf(1))), cPi(cUniverseInf(1), cPi(ctypeArgs(cTele(cvars(ct, cz_), cUniverseInf(1))), cPi(false, cx_, cPi(cUniverseInf(1), cUniverseInf(1)), cApps(cVar(cw), cVar(cx_), cVar(cy), cVar(cz_), cVar(ct))))))));
    SingleDependentLink w = singleParam("w", Pi(Universe(1), Pi(Universe(1), Pi(Universe(1), Pi(Universe(1), Universe(1))))));
    SingleDependentLink x = singleParam("x", Universe(1));
    SingleDependentLink yz = singleParam(false, vars("y", "z"), Universe(1));
    SingleDependentLink tz_ = singleParam(true, vars("t", "z'"), Universe(1));
    SingleDependentLink x_ = singleParam(false, vars("x'"), Pi(singleParam(null, Universe(1)), Universe(1)));
    Expression expr = Pi(w, Pi(x, Pi(yz, Pi(Universe(1), Pi(tz_, Pi(x_, Apps(Ref(w), Ref(x_), Ref(yz), Ref(tz_.getNext()), Ref(tz_))))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() {
    // f {x : \Type1} (A : \Type1 -> \Type0) : A x -> (\Type1 -> \Type1) -> \Type1 -> \Type1 => \t y z. y z;
    LocalReferable x = ref("x");
    LocalReferable A = ref("A");
    LocalReferable t = ref("t");
    LocalReferable y = ref("y");
    LocalReferable z = ref("z");
    ConcreteLocatedReferable reference = new ConcreteLocatedReferable(null, "f", Precedence.DEFAULT, MODULE_PATH, GlobalReferable.Kind.TYPECHECKABLE);
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(FunctionKind.FUNC, reference, cargs(cTele(false, cvars(x), cUniverseStd(1)), cTele(cvars(A), cPi(cUniverseStd(1), cUniverseStd(0)))), cPi(cApps(cVar(A), cVar(x)), cPi(cPi(cUniverseStd(1), cUniverseStd(1)), cPi(cUniverseStd(1), cUniverseStd(1)))), null, body(cLam(cargs(cName(t), cName(y), cName(z)), cApps(cVar(y), cVar(z)))));
    reference.setDefinition(def);
    testDef(def, def);
  }

  @Test
  public void prettyPrintPiLam() {
    // A : \Type
    // a : A
    // D : (A -> A) -> A -> A
    // \Pi (x : \Pi (y : A) -> A) -> D x (\lam y => a)
    SingleDependentLink A = singleParam("A", Universe(0));
    SingleDependentLink D = singleParam("D", Pi(Pi(Ref(A), Ref(A)), Pi(Ref(A), Ref(A))));
    SingleDependentLink x = singleParam("x", Pi(singleParam("y", Ref(A)), Ref(A)));
    SingleDependentLink a = singleParam("a", Ref(A));
    Expression actual = Pi(A, Pi(a, Pi(D, Pi(x, Apps(Ref(D), Ref(x), Lam(singleParam("y", Ref(A)), Ref(a)))))));

    LocalReferable cx = ref("x");
    LocalReferable ca = ref("a");
    LocalReferable cA = ref("A");
    LocalReferable cD = ref("D");
    Concrete.Expression expected = cPi(cA, cUniverseInf(0), cPi(ca, cVar(cA), cPi(cD, cPi(cPi(cVar(cA), cVar(cA)), cPi(cVar(cA), cVar(cA))), cPi(cx, cPi(cVar(cA), cVar(cA)), cApps(cVar(cD), cVar(cx), cLam(cName(null), cVar(ca)))))));
    testExpr(expected, actual, EnumSet.of(PrettyPrinterFlag.SHOW_IMPLICIT_ARGS));
  }

  @Test
  public void prettyPrintCase() {
    TypedSingleDependentLink x = singleParam("x", Nat());
    TypedSingleDependentLink y = singleParam("y", Nat());
    List<ElimClause<Pattern>> clauses = new ArrayList<>();
    clauses.add(new ElimClause<>(Collections.singletonList(ConstructorPattern.make(Prelude.ZERO, Collections.emptyList())), Zero()));
    clauses.add(new ElimClause<>(Collections.singletonList(ConstructorPattern.make(Prelude.SUC, Collections.singletonList(new BindingPattern(y)))), Ref(y)));
    Expression cExpr = new LamExpression(Sort.SET0, x, new CaseExpression(false, x, Nat(), null, new ElimBody(clauses, null), Collections.singletonList(Ref(x))));

    LocalReferable cx = ref("x");
    LocalReferable cy = ref("y");
    List<Concrete.FunctionClause> cfc = new ArrayList<>();
    cfc.add(cClause(Collections.singletonList(cConPattern(true, Prelude.ZERO.getReferable(), Collections.emptyList())), cZero()));
    cfc.add(cClause(Collections.singletonList(cConPattern(true, Prelude.SUC.getReferable(), Collections.singletonList(cNamePattern(true, cy)))), cVar(cy)));
    Concrete.Expression ccExpr = cLam(cargs(cTele(cvars(cx), cNat())), cCase(false, Collections.singletonList(cCaseArg(cVar(cx), null, null)), null, null, cfc));

    testExpr(ccExpr, cExpr);
  }

  @Test
  public void assocTest() {
    typeCheckModule(
      "\\func \\infix 6 + (x y : Nat) => x\n" +
      "\\func \\infixl 6 * (x y : Nat) => x\n" +
      "\\func \\infixr 6 & (x y : Nat) => x\n" +
      "\\func f1 => (0 + 1) + 2\n" +
      "\\func f2 => 0 + (1 + 2)\n" +
      "\\func g1 => (0 * 1) * 2\n" +
      "\\func g2 => 0 * (1 * 2)\n" +
      "\\func h1 => (0 & 1) & 2\n" +
      "\\func h2 => 0 & (1 & 2)");
    testExpr("(0 + 1) + 2", (Expression) ((FunctionDefinition) getDefinition("f1")).getBody());
    testExpr("0 + (1 + 2)", (Expression) ((FunctionDefinition) getDefinition("f2")).getBody());
    testExpr("0 * 1 * 2", (Expression) ((FunctionDefinition) getDefinition("g1")).getBody());
    testExpr("0 * (1 * 2)", (Expression) ((FunctionDefinition) getDefinition("g2")).getBody());
    testExpr("(0 & 1) & 2", (Expression) ((FunctionDefinition) getDefinition("h1")).getBody());
    testExpr("0 & 1 & 2", (Expression) ((FunctionDefinition) getDefinition("h2")).getBody());
  }

  @Test
  public void parenthesisTest() {
    typeCheckModule(
      "\\func \\infix 6 + (x y : Nat) => x\n" +
      "\\func \\infixl 7 * (x y : Nat) => x\n" +
      "\\func \\infixr 6 & (x y : Nat) => x\n" +
      "\\func f (x y z : Nat) => ((x + suc y) * (suc y & (z & x))) * (suc (z + y))");
    testExpr("(x + suc y) * (suc y & z & x) * suc (z + y)", (Expression) ((FunctionDefinition) getDefinition("f")).getBody());
  }

  @Test
  public void highOrderTest() {
    typeCheckModule(
            "\\func R {A : \\Type} (a a' : A) => a = a'\n" +
                    "\\func F {A : \\Type} (T : A -> A -> \\Type) => \\Pi (a : A) -> T a a\n" +
                    "\\func fooxy => F {Nat} R");
    testExpr("F R", ((Expression)((FunctionDefinition) getDefinition("fooxy")).getBody()), EnumSet.noneOf(PrettyPrinterFlag.class));
  }
}
