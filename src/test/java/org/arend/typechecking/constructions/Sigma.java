package org.arend.typechecking.constructions;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ExpressionFactory.Pi;
import static org.arend.ExpressionFactory.*;
import static org.arend.ExpressionFactory.Universe;
import static org.junit.Assert.assertEquals;

public class Sigma extends TypeCheckingTestCase {
  @Test
  public void sigmaUniverse() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(3, 7)));
    context.add(new TypedBinding("B", Universe(4, 6)));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\Sigma A B", null);
    assertEquals(Universe(4, 7), result.type);
  }

  @Test
  public void sigmaArgName() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(4, 6)));
    context.add(new TypedBinding("B", Pi(Ref(context.get(0)), Universe(2, 8))));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\Sigma (x : A) (B x)", null);
    assertEquals(Universe(4, 8), result.type);
  }

  @Test
  public void sigmaTest() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Sigma (x y : Nat) (\\Sigma (z : \\4-Type3) (w : \\5-Type2))", null);
    assertEquals(Universe(4, 6), result.type);
  }

  @Test
  public void sigmaProp() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(3, 7)));
    context.add(new TypedBinding("B", Pi(Ref(context.get(0)), Universe(5, -1))));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\Sigma (x : A) (B x)", null);
    assertEquals(Universe(3, 7), result.type);
  }

  @Test
  public void sigmaPropDom() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(4, -1)));
    context.add(new TypedBinding("B", Universe(2, 6)));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\Sigma A B", null);
    assertEquals(Universe(2, 6), result.type);
  }
}
