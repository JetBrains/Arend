package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefinitionTest {
  @Test
  public void function() {
    // f : N => 0;
    FunctionDefinition def = new FunctionDefinition("f", new ArrayList<TelescopeArgument>(), Nat(), Definition.Arrow.RIGHT, Zero());
    List<TypeCheckingError> errors = new ArrayList<>();
    def = def.checkTypes(Prelude.DEFINITIONS, new ArrayList<Binding>(), errors);
    assertNotNull(def);
    assertEquals(0, errors.size());
  }

  @Test
  public void functionUntyped() {
    // f => 0;
    FunctionDefinition def = new FunctionDefinition("f", new ArrayList<TelescopeArgument>(), null, Definition.Arrow.RIGHT, Zero());
    List<TypeCheckingError> errors = new ArrayList<>();
    def = def.checkTypes(Prelude.DEFINITIONS, new ArrayList<Binding>(), errors);
    assertEquals(0, errors.size());
    assertNotNull(def);
    assertEquals(Nat(), def.getType());
  }

  @Test
  public void functionWithArgs() {
    // f (x : N) (y : N -> N) => y;
    List<TelescopeArgument> arguments = new ArrayList<>();
    arguments.add(Tele(vars("x"), Nat()));
    arguments.add(Tele(vars("y"), Pi(Nat(), Nat())));
    FunctionDefinition def = new FunctionDefinition("f", arguments, null, Definition.Arrow.RIGHT, Index(0));
    List<TypeCheckingError> errors = new ArrayList<>();
    def = def.checkTypes(Prelude.DEFINITIONS, new ArrayList<Binding>(), errors);
    assertEquals(0, errors.size());
    assertNotNull(def);
    assertEquals(Pi(Nat(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat()))), def.getType());
  }
}
