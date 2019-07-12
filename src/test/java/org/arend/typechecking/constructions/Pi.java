package org.arend.typechecking.constructions;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ExpressionFactory.Pi;
import static org.arend.ExpressionFactory.*;
import static org.arend.ExpressionFactory.Universe;
import static org.junit.Assert.assertEquals;

public class Pi extends TypeCheckingTestCase {
  @Test
  public void arrowUniverse() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(3, 7)));
    context.add(new TypedBinding("B", Universe(4, 6)));
    TypecheckingResult result = typeCheckExpr(context, "A -> B", null);
    assertEquals(Universe(4, 6), result.type);
  }

  @Test
  public void piImplicit() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(4, 6)));
    context.add(new TypedBinding("B", Pi(Ref(context.get(0)), Universe(2, 8))));
    TypecheckingResult result = typeCheckExpr(context, "\\Pi {x : A} -> B x", null);
    assertEquals(Universe(4, 8), result.type);
  }

  @Test
  public void piTest() {
    TypecheckingResult result = typeCheckExpr("\\Pi (x y : Nat) {z : \\4-Type3} -> \\Pi (w : \\5-Type2) -> Nat", null);
    assertEquals(Universe(4, 0), result.type);
  }

  @Test
  public void piProp() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(3, 7)));
    context.add(new TypedBinding("B", Pi(Ref(context.get(0)), Universe(4, -1))));
    TypecheckingResult result = typeCheckExpr(context, "\\Pi (x : A) -> B x", null);
    assertEquals(Universe(0, -1), result.type);
  }

  @Test
  public void piPropDom() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(4, -1)));
    context.add(new TypedBinding("B", Universe(2, 6)));
    TypecheckingResult result = typeCheckExpr(context, "A -> B", null);
    assertEquals(Universe(2, 6), result.type);
  }
}
