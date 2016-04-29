package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Pi;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.assertEquals;

public class Pi {
  @Test
  public void arrowUniverse() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(3, 7)));
    context.add(new TypedBinding("B", Universe(4, 6)));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "A -> B", null);
    assertEquals(Universe(4, 6), result.type);
  }

  @Test
  public void piImplicit() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(4, 6)));
    context.add(new TypedBinding("B", Pi(Reference(context.get(0)), Universe(2, 8))));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\Pi {x : A} -> B x", null);
    assertEquals(Universe(4, 8), result.type);
  }

  @Test
  public void piTest() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Pi (x y : Nat) {z : \\4-Type3} -> \\Pi (w : \\5-Type2) -> Nat", null);
    assertEquals(Universe(4, 0), result.type);
  }

  @Test
  public void piProp() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(3, 7)));
    context.add(new TypedBinding("B", Pi(Reference(context.get(0)), Universe(4, -1))));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\Pi (x : A) -> B x", null);
    assertEquals(Universe(0, -1), result.type);
  }

  @Test
  public void piPropDom() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("A", Universe(4, -1)));
    context.add(new TypedBinding("B", Universe(2, 6)));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "A -> B", null);
    assertEquals(Universe(2, 6), result.type);
  }
}
