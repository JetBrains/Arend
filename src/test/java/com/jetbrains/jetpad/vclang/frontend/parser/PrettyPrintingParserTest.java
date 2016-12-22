package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ToAbstractVisitor;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class PrettyPrintingParserTest extends ParserTestCase {
  private void testExpr(Abstract.Expression expected, Expression expr) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    List<String> context = new ArrayList<>();
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory(), context);
    visitor.setFlags(EnumSet.of(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS, ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM));
    expr.accept(visitor, null).accept(new PrettyPrintVisitor(builder, 0), Abstract.Expression.PREC);
    Concrete.Expression result = parseExpr(builder.toString());
    assertEquals(expected, result);
  }

  private void testDef(Concrete.FunctionDefinition expected, Concrete.FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    def.accept(new PrettyPrintVisitor(builder, 0), null);

    Concrete.FunctionDefinition result = (Concrete.FunctionDefinition) parseDef(builder.toString());
    assertEquals(expected.getArguments().size(), result.getArguments().size());
    for (int i = 0; i < expected.getArguments().size(); ++i) {
      assertTrue(compareAbstract(((Concrete.TypeArgument) expected.getArguments().get(i)).getType(), ((Concrete.TypeArgument) result.getArguments().get(i)).getType()));
    }
    assertTrue(compareAbstract(expected.getResultType(), result.getResultType()));
    assertNotNull(result.getTerm());
    assertEquals(expected.getTerm(), result.getTerm());
    assertEquals(expected.getArrow(), result.getArrow());
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    Concrete.Expression expected = cApps(cLam(cargs(cTele(cvars("x", "y"), cPi(cUniverse(1), cUniverse(1)))), cApps(cVar("x"), cApps(cVar("x"), cVar("y")))), cLam(cargs(cTele(cvars("x", "y"), cPi(cUniverse(1), cUniverse(1)))), cVar("x")), cApps(cLam(cargs(cTele(cvars("x"), cPi(cUniverse(1), cUniverse(1)))), cVar("x")), cLam(cargs(cTele(cvars("x"), cPi(cUniverse(1), cUniverse(1)))), cVar("x"))));
    DependentLink x = param("x", Pi(Universe(1), Universe(1)));
    DependentLink xy = param(true, vars("x", "y"), Pi(Universe(1), Universe(1)));
    Expression expr = Apps(Lam(xy, Apps(Reference(xy), Apps(Reference(xy), Reference(xy.getNext())))), Lam(xy, Reference(xy)), Apps(Lam(x, Reference(x)), Lam(x, Reference(x))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y z : \Type1 -> \Type1 -> \Type1) -> \Type1 -> \Type1 -> (x y -> y x) -> z x y
    Concrete.Expression expected = cPi(ctypeArgs(cTele(cvars("x", "y", "z"), cPi(cUniverse(1), cPi(cUniverse(1), cUniverse(1))))), cPi(cUniverse(1), cPi(cUniverse(1), cPi(cPi(cApps(cVar("x"), cVar("y")), cApps(cVar("y"), cVar("x"))), cApps(cVar("z"), cVar("x"), cVar("y"))))));
    DependentLink xyz = param(true, vars("x", "y", "z"), Pi(Universe(1), Pi(Universe(1), Universe(1))));
    Expression expr = Pi(xyz, Pi(Universe(1), Pi(Universe(1), Pi(Pi(Apps(Reference(xyz), Reference(xyz.getNext())), Apps(Reference(xyz.getNext()), Reference(xyz))), Apps(Reference(xyz.getNext().getNext()), Reference(xyz), Reference(xyz.getNext()))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (w : \Type1 -> \Type1 -> \Type1 -> \Type1 -> \Type1) (x : \Type1) {y z : \Type1} -> \Type1 -> (t z' : \Type1) {x' : \Type1 -> \Type1} -> w x' y z' t
    Concrete.Expression expected = cPi("w", cPi(cUniverse(1), cPi(cUniverse(1), cPi(cUniverse(1), cPi(cUniverse(1), cUniverse(1))))), cPi("x", cUniverse(1), cPi(ctypeArgs(cTele(false, cvars("y", "z"), cUniverse(1))), cPi(cUniverse(1), cPi(ctypeArgs(cTele(cvars("t", "z'"), cUniverse(1))), cPi(false, "x'", cPi(cUniverse(1), cUniverse(1)), cApps(cVar("w"), cVar("x'"), cVar("y"), cVar("z'"), cVar("t"))))))));
    DependentLink w = param("w", Pi(Universe(1), Pi(Universe(1), Pi(Universe(1), Pi(Universe(1), Universe(1))))));
    DependentLink x = param("x", Universe(1));
    DependentLink yz = param(false, vars("y", "z"), Universe(1));
    DependentLink tz_ = param(true, vars("t", "z'"), Universe(1));
    DependentLink x_ = param(false, "x'", Pi(param(Universe(1)), Universe(1)));
    Expression expr = Pi(w, Pi(x, Pi(yz, Pi(Universe(1), Pi(tz_, Pi(x_, Apps(Reference(w), Reference(x_), Reference(yz), Reference(tz_.getNext()), Reference(tz_))))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
    // f {x : \Type1} (A : \Type1 -> \Type0) : A x -> (\Type1 -> \Type1) -> \Type1 -> \Type1 => \t y z. y z;
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(POSITION, "f", Abstract.Precedence.DEFAULT, cargs(cTele(false, cvars("x"), cUniverse(1)), cTele(cvars("A"), cPi(cUniverse(1), cUniverse(0)))), cPi(cApps(cVar("A"), cVar("x")), cPi(cPi(cUniverse(1), cUniverse(1)), cPi(cUniverse(1), cUniverse(1)))), Abstract.Definition.Arrow.RIGHT, cLam(cargs(cName("t"), cName("y"), cName("z")), cApps(cVar("y"), cVar("z"))), Collections.<Concrete.Statement>emptyList());
    testDef(def, def);
  }
}
