package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
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

  @Test
  public void dataType() {
    // \data D {A B : Type0} (I : A -> B -> Type0) (a : A) (b : B) | con1 (x : A) (I x b) | con2 {y : B} (I a y)
    List<TypeArgument> parameters = new ArrayList<>(4);
    parameters.add(Tele(false, vars("A", "B"), Universe(0)));
    parameters.add(Tele(vars("I"), Pi(Index(1), Pi(Index(0), Universe(0)))));
    parameters.add(Tele(vars("a"), Index(2)));
    parameters.add(Tele(vars("b"), Index(2)));

    List<Constructor> constructors = new ArrayList<>(2);
    DataDefinition def = new DataDefinition("D", null, parameters, constructors);

    List<TypeArgument> arguments1 = new ArrayList<>(2);
    arguments1.add(Tele(vars("x"), Index(4)));
    arguments1.add(TypeArg(Apps(Index(3), Index(0), Index(1))));
    constructors.add(new Constructor("con1", null, arguments1, def));

    List<TypeArgument> arguments2 = new ArrayList<>(2);
    arguments2.add(Tele(false, vars("y"), Index(3)));
    arguments2.add(TypeArg(Apps(Index(3), Index(2), Index(0))));
    constructors.add(new Constructor("con2", null, arguments2, def));

    List<TypeCheckingError> errors = new ArrayList<>();
    def = def.checkTypes(Prelude.DEFINITIONS, new ArrayList<Binding>(), errors);
    assertEquals(0, errors.size());
    assertNotNull(def);
    assertEquals(Pi(parameters, Universe()), def.getType());
    assertEquals(2, def.getConstructors().size());
    assertEquals(Pi(arguments1, Apps(App(App(DefCall(def), Index(6), false), Index(5), false), Index(4), Index(3), Index(2))), def.getConstructor(0).getType());
    assertEquals(Pi(arguments2, Apps(App(App(DefCall(def), Index(6), false), Index(5), false), Index(4), Index(3), Index(2))), def.getConstructor(1).getType());
  }
}
