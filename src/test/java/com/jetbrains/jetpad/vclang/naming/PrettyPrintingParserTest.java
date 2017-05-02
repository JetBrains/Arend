package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.singleParams;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class PrettyPrintingParserTest extends NameResolverTest {
  private void testExpr(Abstract.Expression expected, Expression expr, EnumSet<ToAbstractVisitor.Flag> flags) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    List<String> context = new ArrayList<>();
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory(), context);
    if (flags != null) {
      visitor.setFlags(flags);
    }
    expr.accept(visitor, null).accept(new PrettyPrintVisitor(builder, 0), Abstract.Expression.PREC);
    Concrete.Expression result = resolveNamesExpr(builder.toString());
    assertEquals(expected, result);
  }

  private void testExpr(Abstract.Expression expected, Expression expr) throws UnsupportedEncodingException {
    testExpr(expected, expr, EnumSet.of(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS, ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM));
  }

  private void testDef(Concrete.FunctionDefinition expected, Concrete.FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    def.accept(new PrettyPrintVisitor(builder, 0), null);

    Concrete.FunctionDefinition result = (Concrete.FunctionDefinition) resolveNamesDef(builder.toString());
    List<Concrete.TypeArgument> expectedArguments = new ArrayList<>();
    for (Concrete.Argument argument : expected.getArguments()) {
      expectedArguments.add((Concrete.TypeArgument) argument);
    }
    List<Concrete.TypeArgument> actualArguments = new ArrayList<>();
    for (Concrete.Argument argument : result.getArguments()) {
      actualArguments.add((Concrete.TypeArgument) argument);
    }
    Concrete.Expression expectedType = cPi(expectedArguments, expected.getResultType());
    Concrete.Expression actualType = cPi(actualArguments, result.getResultType());
    assertTrue(compareAbstract(expectedType, actualType));
    assertNotNull(result.getTerm());
    assertEquals(cLam(new ArrayList<>(expected.getArguments()), expected.getTerm()), cLam(new ArrayList<>(result.getArguments()), result.getTerm()));
    assertEquals(expected.getArrow(), result.getArrow());
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    Concrete.ReferableSourceNode cx = ref("x");
    Concrete.ReferableSourceNode cy = ref("y");
    Concrete.Expression expected = cApps(cLam(cargs(cTele(cvars(cx, cy), cPi(cUniverseInf(1), cUniverseInf(1)))), cApps(cVar(cx), cApps(cVar(cx), cVar(cy)))), cLam(cargs(cTele(cvars(cx, cy), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx)), cApps(cLam(cargs(cTele(cvars(cx), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx)), cLam(cargs(cTele(cvars(cx), cPi(cUniverseInf(1), cUniverseInf(1)))), cVar(cx))));
    SingleDependentLink x = singleParam("x", Pi(Universe(1), Universe(1)));
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Pi(Universe(1), Universe(1)));
    Expression expr = Apps(Lam(xy, Apps(Ref(xy), Apps(Ref(xy), Ref(xy.getNext())))), Lam(xy, Ref(xy)), Apps(Lam(x, Ref(x)), Lam(x, Ref(x))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y z : \Type1 -> \Type1 -> \Type1) -> \Type1 -> \Type1 -> (x y -> y x) -> z x y
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode z = ref("z");
    Concrete.Expression expected = cPi(ctypeArgs(cTele(cvars(x, y, z), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cUniverseInf(1))))), cPi(cUniverseInf(1), cPi(cUniverseInf(1), cPi(cPi(cApps(cVar(x), cVar(y)), cApps(cVar(y), cVar(x))), cApps(cVar(z), cVar(x), cVar(y))))));
    SingleDependentLink xyz = singleParams(true, vars("x", "y", "z"), Pi(Universe(1), Pi(Universe(1), Universe(1))));
    Expression expr = Pi(xyz, Pi(Universe(1), Pi(Universe(1), Pi(Pi(Apps(Ref(xyz), Ref(xyz.getNext())), Apps(Ref(xyz.getNext()), Ref(xyz))), Apps(Ref(xyz.getNext().getNext()), Ref(xyz), Ref(xyz.getNext()))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (w : \Type1 -> \Type1 -> \Type1 -> \Type1 -> \Type1) (x : \Type1) {y z : \Type1} -> \Type1 -> (t z' : \Type1) {x' : \Type1 -> \Type1} -> w x' y z' t
    Concrete.ReferableSourceNode cx = ref("x");
    Concrete.ReferableSourceNode cy = ref("y");
    Concrete.ReferableSourceNode cz = ref("z");
    Concrete.ReferableSourceNode ct = ref("t");
    Concrete.ReferableSourceNode cx_ = ref("x'");
    Concrete.ReferableSourceNode cz_ = ref("z'");
    Concrete.ReferableSourceNode cw = ref("w");
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
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode A = ref("A");
    Concrete.ReferableSourceNode t = ref("t");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode z = ref("z");
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(POSITION, "f", Abstract.Precedence.DEFAULT, cargs(cTele(false, cvars(x), cUniverseStd(1)), cTele(cvars(A), cPi(cUniverseStd(1), cUniverseStd(0)))), cPi(cApps(cVar(A), cVar(x)), cPi(cPi(cUniverseStd(1), cUniverseStd(1)), cPi(cUniverseStd(1), cUniverseStd(1)))), Abstract.Definition.Arrow.RIGHT, cLam(cargs(cName(t), cName(y), cName(z)), cApps(cVar(y), cVar(z))), Collections.emptyList());
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

    Concrete.ReferableSourceNode cx = ref("x");
    Concrete.ReferableSourceNode cy = ref("y");
    Concrete.ReferableSourceNode ca = ref("a");
    Concrete.ReferableSourceNode cA = ref("A");
    Concrete.ReferableSourceNode cD = ref("D");
    Concrete.Expression expected = cPi(cA, cUniverseInf(0), cPi(ca, cVar(cA), cPi(cD, cPi(cPi(cVar(cA), cVar(cA)), cPi(cVar(cA), cVar(cA))), cPi(cx, cPi(cy, cVar(cA), cVar(cA)), cApps(cVar(cD), cVar(cx), cLam(cy, cVar(ca)))))));
    testExpr(expected, actual, null);
  }
}
