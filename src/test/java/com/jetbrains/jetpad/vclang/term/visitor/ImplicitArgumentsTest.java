package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;
import static org.junit.Assert.assertEquals;

public class ImplicitArgumentsTest {
  @Test
  public void inferId() {
    // f : {A : Type0} -> A -> A |- f 0 : N
    Expression expr = Apps(Index(0), Zero());
    List<Definition> defs = new ArrayList<>();
    defs.add(new FunctionDefinition("f", new Signature(Pi(false, "A", Universe(0), Pi(Index(0), Index(0)))), Var("f")));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(App(Index(0), Nat(), false), Zero()), result.expression);
    assertEquals(Nat(), result.type);
  }
}
