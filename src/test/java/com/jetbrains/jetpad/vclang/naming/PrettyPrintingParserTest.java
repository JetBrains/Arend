package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedSingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.CaseExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.frontend.reference.ParsedLocalReferable;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.term.Precedence;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertTrue;

public class PrettyPrintingParserTest extends TypeCheckingTestCase {
  private void testExpr(Concrete.Expression expected, Expression expr, EnumSet<ToAbstractVisitor.Flag> flags) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    ToAbstractVisitor.convert(expr, flags).accept(new PrettyPrintVisitor(builder, 0), Precedence.DEFAULT);
    Concrete.Expression result = resolveNamesExpr(builder.toString());
    assertTrue(compareAbstract(expected, result));
  }

  private void testExpr(Concrete.Expression expected, Expression expr) throws UnsupportedEncodingException {
    testExpr(expected, expr, EnumSet.of(ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM, ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS));
  }

  private void testDef(Concrete.FunctionDefinition expected, Concrete.FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    def.accept(new PrettyPrintVisitor(builder, 0), null);

    Concrete.FunctionDefinition result = (Concrete.FunctionDefinition) resolveNamesDef(builder.toString()).getDefinition();
    List<Concrete.TypeParameter> expectedArguments = new ArrayList<>();
    for (Concrete.Parameter argument : expected.getParameters()) {
      expectedArguments.add((Concrete.TypeParameter) argument);
    }
    List<Concrete.TypeParameter> actualArguments = new ArrayList<>();
    for (Concrete.Parameter argument : result.getParameters()) {
      actualArguments.add((Concrete.TypeParameter) argument);
    }
    Concrete.Expression expectedType = cPi(expectedArguments, expected.getResultType());
    Concrete.Expression actualType = cPi(actualArguments, result.getResultType());
    assertTrue(compareAbstract(expectedType, actualType));
    assertTrue(result.getBody() instanceof Concrete.TermFunctionBody);
    assertTrue(compareAbstract(
      cLam(new ArrayList<>(expected.getParameters()), ((Concrete.TermFunctionBody) expected.getBody()).getTerm()),
      cLam(new ArrayList<>(result.getParameters()), ((Concrete.TermFunctionBody) result.getBody()).getTerm())));
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    ParsedLocalReferable cx = ref("x");
    ParsedLocalReferable cy = ref("y");
    Concrete.Expression expected = cApps(cLam(cargs(cTele(cvars(cx, cy), cPi(cUniverseInf(1), cUniverseInf(1)))), cApps(cVar(cx), cApps(cVar(cx), cVar(cy)))), cLam(cargs(cTele(cvars(cx, cy), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx)), cApps(cLam(cargs(cTele(cvars(cx), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx)), cLam(cargs(cTele(cvars(cx), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx))));
    SingleDependentLink x = singleParam("x", Pi(Universe(1), Universe(1)));
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Pi(Universe(1), Universe(1)));
    Expression expr = Apps(Lam(xy, Apps(Ref(xy), Apps(Ref(xy), Ref(xy.getNext())))), Lam(xy, Ref(xy)), Apps(Lam(x, Ref(x)), Lam(x, Ref(x))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y z : \Type1 -> \Type1 -> \Type1) -> \Type1 -> \Type1 -> (x y -> y x) -> z x y
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable z = ref("z");
    Concrete.Expression expected = cPi(ctypeArgs(cTele(cvars(x, y, z), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cUniverseInf(1))))), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cPi(cPi(cApps(cVar(x), cVar(y)), cApps(cVar(y), cVar(x))), cApps(cVar(z), cVar(x), cVar(y))))));
    SingleDependentLink xyz = singleParams(true, vars("x", "y", "z"), Pi(Universe(1), Pi(Universe(1), Universe(1))));
    Expression expr = Pi(xyz, Pi(Universe(1), Pi(Universe(1), Pi(Pi(Apps(Ref(xyz), Ref(xyz.getNext())), Apps(Ref(xyz.getNext()), Ref(xyz))), Apps(Ref(xyz.getNext().getNext()), Ref(xyz), Ref(xyz.getNext()))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (w : \Type1 -> \Type1 -> \Type1 -> \Type1 -> \Type1) (x : \Type1) {y z : \Type1} -> \Type1 -> (t z' : \Type1) {x' : \Type1 -> \Type1} -> w x' y z' t
    ParsedLocalReferable cx = ref("x");
    ParsedLocalReferable cy = ref("y");
    ParsedLocalReferable cz = ref("z");
    ParsedLocalReferable ct = ref("t");
    ParsedLocalReferable cx_ = ref("x'");
    ParsedLocalReferable cz_ = ref("z'");
    ParsedLocalReferable cw = ref("w");
    Concrete.Expression expected = cPi(cw, cPi(cUniverseInf(1), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cUniverseInf(1))))), cPi(cx, cUniverseInf(1), cPi(ctypeArgs(cTele(false, cvars(cy, cz), cUniverseInf(1))), cPi(cUniverseInf(1), cPi(ctypeArgs(cTele(cvars(ct, cz_), cUniverseInf(1))), cPi(false, cx_, cPi(cUniverseInf(1), cUniverseInf(1)), cApps(cVar(cw), cVar(cx_), cVar(cy), cVar(cz_), cVar(ct))))))));
    SingleDependentLink w = singleParam("w", Pi(Universe(1), Pi(Universe(1), Pi(Universe(1), Pi(Universe(1), Universe(1))))));
    SingleDependentLink x = singleParam("x", Universe(1));
    SingleDependentLink yz = singleParam(false, vars("y", "z"), Universe(1));
    SingleDependentLink tz_ = singleParam(true, vars("t", "z'"), Universe(1));
    SingleDependentLink x_ = singleParam(false, vars("x'"), Pi(singleParam(null, Universe(1)), Universe(1)));
    Expression expr = Pi(w, Pi(x, Pi(yz, Pi(Universe(1), Pi(tz_, Pi(x_, Apps(Ref(w), Ref(x_), Ref(yz), Ref(tz_.getNext()), Ref(tz_))))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
    // f {x : \Type1} (A : \Type1 -> \Type0) : A x -> (\Type1 -> \Type1) -> \Type1 -> \Type1 => \t y z. y z;
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable A = ref("A");
    ParsedLocalReferable t = ref("t");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable z = ref("z");
    ConcreteGlobalReferable reference = new ConcreteGlobalReferable(null, "f", Precedence.DEFAULT);
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(reference, cargs(cTele(false, cvars(x), cUniverseStd(1)), cTele(cvars(A), cPi(cUniverseStd(1), cUniverseStd(0)))), cPi(cApps(cVar(A), cVar(x)), cPi(cPi(cUniverseStd(1), cUniverseStd(1)), cPi(cUniverseStd(1), cUniverseStd(1)))), body(cLam(cargs(cName(t), cName(y), cName(z)), cApps(cVar(y), cVar(z)))));
    reference.setDefinition(def);
    testDef(def, def);
  }

  @Test
  public void prettyPrintPiLam() throws UnsupportedEncodingException {
    // A : \Type
    // a : A
    // D : (A -> A) -> A -> A
    // \Pi (x : \Pi (y : A) -> A) -> D x (\lam y => a)
    SingleDependentLink A = singleParam("A", Universe(0));
    SingleDependentLink D = singleParam("D", Pi(Pi(Ref(A), Ref(A)), Pi(Ref(A), Ref(A))));
    SingleDependentLink x = singleParam("x", Pi(singleParam("y", Ref(A)), Ref(A)));
    SingleDependentLink a = singleParam("a", Ref(A));
    Expression actual = Pi(A, Pi(a, Pi(D, Pi(x, Apps(Ref(D), Ref(x), Lam(singleParam("y", Ref(A)), Ref(a)))))));

    ParsedLocalReferable cx = ref("x");
    ParsedLocalReferable cy = ref("y");
    ParsedLocalReferable ca = ref("a");
    ParsedLocalReferable cA = ref("A");
    ParsedLocalReferable cD = ref("D");
    Concrete.Expression expected = cPi(cA, cUniverseInf(0), cPi(ca, cVar(cA), cPi(cD, cPi(cPi(cVar(cA), cVar(cA)), cPi(cVar(cA), cVar(cA))), cPi(cx, cPi(cy, cVar(cA), cVar(cA)), cApps(cVar(cD), cVar(cx), cLam(cName(ref("y")), cVar(ca)))))));
    testExpr(expected, actual, EnumSet.of(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS));
  }

  @Test
  public void prettyPrintCase() throws UnsupportedEncodingException {
    TypedSingleDependentLink x = singleParam("x", Nat());
    HashMap<Constructor, ElimTree> myMap = new HashMap<>();
    myMap.put(Prelude.ZERO, new LeafElimTree(EmptyDependentLink.getInstance(), Zero()));
    TypedSingleDependentLink y = singleParam("y", Nat());
    myMap.put(Prelude.SUC, new LeafElimTree(y, Ref(y)));
    ElimTree elimTree = new BranchElimTree(EmptyDependentLink.getInstance(), myMap);
    CaseExpression cExpr = new CaseExpression(x, Nat(), elimTree, Collections.singletonList(Ref(x)));

    ParsedLocalReferable cx = ref("x");
    ParsedLocalReferable cy = ref("y");
    List<Concrete.FunctionClause> cfc = new ArrayList<>();
    cfc.add(cClause(Collections.singletonList(cConPattern(true, Prelude.SUC.getReferable(), Collections.singletonList(cNamePattern(true, cy)))), cVar(cy)));
    cfc.add(cClause(Collections.singletonList(cConPattern(true, Prelude.ZERO.getReferable(), Collections.emptyList())), cZero()));
    Concrete.CaseExpression ccExpr = cCase(Collections.singletonList(cVar(cx)), cfc);

    testExpr(ccExpr, cExpr);
  }
}
