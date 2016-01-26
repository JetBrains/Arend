package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class PrettyPrintingParserTest {
  private void testExpr(Abstract.Expression expected, Expression expr) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    List<String> context = new ArrayList<>();
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory());
    visitor.setFlags(EnumSet.of(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS, ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM));
    expr.accept(visitor, null).accept(new PrettyPrintVisitor(builder, context, 0), Abstract.Expression.PREC);
    Concrete.Expression result = parseExpr(builder.toString());
    assertTrue(compare(expected, result));
  }

  private void testDef(Concrete.FunctionDefinition expected, Concrete.FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    def.accept(new PrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);

    Concrete.FunctionDefinition result = (Concrete.FunctionDefinition) parseDef(builder.toString());
    assertEquals(expected.getArguments().size(), result.getArguments().size());
    for (int i = 0; i < expected.getArguments().size(); ++i) {
      assertTrue(compare(((Concrete.TypeArgument) expected.getArguments().get(i)).getType(), ((Concrete.TypeArgument) result.getArguments().get(i)).getType()));
    }
    assertTrue(compare(expected.getResultType(), result.getResultType()));
    assertNotNull(result.getTerm());
    // TODO: fix
    //assertTrue(compare(expected.getElimTree(), result.getElimTree()));
    assertEquals(expected.getArrow(), result.getArrow());
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    Concrete.Expression expected = cApps(cLam(cargs(cTele(cvars("x", "y"), cNat())), cApps(cVar("x"), cApps(cVar("x"), cVar("y")))), cLam(cargs(cTele(cvars("x"), cNat()), cTele(cvars("y"), cNat())), cVar("x")), cApps(cLam("x", cVar("x")), cLam("x", cVar("x"))));
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    Expression expr = Apps(Lam(params(x, y), Apps(Reference(x), Apps(Reference(x), Reference(y)))), Lam(params(x, y), Reference(x)), Apps(Lam(x, Reference(x)), Lam(x, Reference(x))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y : Nat) -> Nat -> Nat -> (x y -> y x) -> Nat x y
    Concrete.Expression expected = cPi(ctypeArgs(cTele(cvars("x", "y"), cNat())), cPi(cNat(), cPi(cNat(), cPi(cPi(cApps(cVar("x"), cVar("y")), cApps(cVar("y"), cVar("x"))), cApps(cNat(), cVar("x"), cVar("y"))))));
    DependentLink x = param("x", Nat());
    DependentLink y = param("y", Nat());
    Expression expr = Pi(params(x, y), Pi(param(Nat()), Pi(param(Nat()), Pi(param(Pi(param(Apps(Reference(x), Reference(y))), Apps(Reference(y), Reference(x)))), Apps(Nat(), Reference(x), Reference(y))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (x : Nat) {y z : Nat} -> Nat -> (t z' : Nat) {x' : Nat -> Nat} -> Nat x' y z' t
    Concrete.Expression expected = cPi("x", cNat(), cPi(ctypeArgs(cTele(false, cvars("y", "z"), cNat())), cPi(cNat(), cPi(ctypeArgs(cTele(cvars("t", "z'"), cNat())), cPi(false, "x'", cPi(cNat(), cNat()), cApps(cNat(), cVar("x'"), cVar("y"), cVar("z'"), cVar("t")))))));
    DependentLink x = param("x", Nat());
    DependentLink y = param(false, "y", Nat());
    DependentLink z = param(false, "y", Nat());
    DependentLink z_ = param("z'", Nat());
    DependentLink t = param("t", Nat());
    DependentLink x_ = param(false, "t", Pi(param(Nat()), Nat()));
    Expression expr = Pi(params(x, y, z), Pi(param(Nat()), Pi(params(t, z_), Pi(x_, Apps(Nat(), Reference(x_), Reference(y), Reference(z_), Reference(t))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
    // f {x : Nat} (A : Nat -> \Type0) : A x -> (Nat -> Nat) -> Nat -> Nat => \t y z. y z;
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(POSITION, "f", Abstract.Definition.DEFAULT_PRECEDENCE, cargs(cTele(false, cvars("x"), cNat()), cTele(cvars("A"), cPi(cNat(), cUniverse(0)))), cPi(cApps(cVar("A"), cVar("x")), cPi(cPi(cNat(), cNat()), cPi(cNat(), cNat()))), Abstract.Definition.Arrow.RIGHT, cLam(cargs(cName("t"), cName("y"), cName("z")), cApps(cVar("y"), cVar("z"))), false, null, new ArrayList<Concrete.Statement>());
    testDef(def, def);
  }
}
