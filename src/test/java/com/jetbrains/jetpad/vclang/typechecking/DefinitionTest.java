package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefinitionTest {
  @Test
  public void function() {
    // f : N => 0;
    FunctionDefinition def = new FunctionDefinition("f", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new ArrayList<TelescopeArgument>(), Nat(), Definition.Arrow.RIGHT, Zero());
    List<VcError> errors = new ArrayList<>();
    def = new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors).visitFunction(def, new ArrayList<Binding>());
    assertNotNull(def);
    assertEquals(0, errors.size());
  }

  @Test
  public void functionUntyped() {
    // f => 0;
    FunctionDefinition def = new FunctionDefinition("f", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new ArrayList<TelescopeArgument>(), null, Definition.Arrow.RIGHT, Zero());
    List<VcError> errors = new ArrayList<>();
    def = new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors).visitFunction(def, new ArrayList<Binding>());
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
    FunctionDefinition def = new FunctionDefinition("f", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, arguments, null, Definition.Arrow.RIGHT, Index(0));
    List<VcError> errors = new ArrayList<>();
    def = new DefinitionCheckTypeVisitor(null, Prelude.getDefinitions(), errors).visitFunction(def, new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(def);
    assertEquals(Pi(Nat(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat()))), def.getType());
  }

  @Test
  public void dataType() {
    // \data D {A B : \Type0} (I : A -> B -> Type0) (a : A) (b : B) | con1 (x : A) (I x b) | con2 {y : B} (I a y)
    List<TypeArgument> parameters = new ArrayList<>(4);
    parameters.add(Tele(false, vars("A", "B"), Universe(0)));
    parameters.add(Tele(vars("I"), Pi(Index(1), Pi(Index(0), Universe(0)))));
    parameters.add(Tele(vars("a"), Index(2)));
    parameters.add(Tele(vars("b"), Index(2)));

    List<Constructor> constructors = new ArrayList<>(2);
    DataDefinition def = new DataDefinition("D", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, parameters, constructors);

    List<TypeArgument> arguments1 = new ArrayList<>(2);
    arguments1.add(Tele(vars("x"), Index(4)));
    arguments1.add(TypeArg(Apps(Index(3), Index(0), Index(1))));
    constructors.add(new Constructor(0, "con1", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, arguments1, def));

    List<TypeArgument> arguments2 = new ArrayList<>(2);
    arguments2.add(Tele(false, vars("y"), Index(3)));
    arguments2.add(TypeArg(Apps(Index(3), Index(2), Index(0))));
    constructors.add(new Constructor(1, "con2", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, arguments2, def));

    Map<String, Definition> definitions = Prelude.getDefinitions();
    List<VcError> errors = new ArrayList<>();
    def = new DefinitionCheckTypeVisitor(null, definitions, errors).visitData(def, new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(def);
    assertEquals(Pi(parameters, Universe(0)), def.getType());
    assertEquals(2, def.getConstructors().size());
    assertEquals(Pi(arguments1, Apps(Apps(Apps(DefCall(def), Index(6), false, false), Index(5), false, false), Index(4), Index(3), Index(2))), def.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(Apps(Apps(DefCall(def), Index(6), false, false), Index(5), false, false), Index(4), Index(3), Index(2))), def.getConstructors().get(1).getType());
  }

  @Test
  public void dataType2() {
    // \data D (A : \7-Type2) = con1 (X : \1-Type5) X | con2 (Y : \2-Type3) A Y
    List<TypeArgument> parameters = new ArrayList<>(1);
    parameters.add(Tele(vars("A"), Universe(2, 7)));
    List<Constructor> constructors = new ArrayList<>(2);
    DataDefinition def = new DataDefinition("D", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, parameters, constructors);

    List<TypeArgument> arguments1 = new ArrayList<>(2);
    arguments1.add(Tele(vars("X"), Universe(5, 1)));
    arguments1.add(TypeArg(Index(0)));
    constructors.add(new Constructor(0, "con1", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, arguments1, def));

    List<TypeArgument> arguments2 = new ArrayList<>(3);
    arguments2.add(Tele(vars("Y"), Universe(3, 2)));
    arguments2.add(TypeArg(Index(1)));
    arguments2.add(TypeArg(Index(1)));
    constructors.add(new Constructor(1, "con2", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, arguments2, def));

    Map<String, Definition> definitions = Prelude.getDefinitions();
    List<VcError> errors = new ArrayList<>();
    def = new DefinitionCheckTypeVisitor(null, definitions, errors).visitData(def, new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(def);
    assertEquals(Pi(parameters, Universe(6, 7)), def.getType());
    assertEquals(2, def.getConstructors().size());
    assertEquals(Pi(arguments1, Apps(DefCall(def), Index(2))), def.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(DefCall(def), Index(3))), def.getConstructors().get(1).getType());
  }

  @Test
  public void constructor() {
    // \data D (A : \Type0) = con (B : \Type1) A B |- con Nat zero zero : D Nat
    List<Constructor> constructors = new ArrayList<>(1);
    DataDefinition def = new DataDefinition("D", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("A"), Universe(0))), constructors);
    Constructor con = new Constructor(0, "con", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))), def);
    constructors.add(con);

    Map<String, Definition> definitions = Prelude.getDefinitions();
    definitions.put("D", def);
    definitions.put("con", con);

    Expression expr = Apps(DefCall(con), Nat(), Zero(), Zero());
    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(definitions, new ArrayList<Binding>(), null, errors);
    assertEquals(0, errors.size());
    assertNotNull(result);
    assertEquals(Apps(DefCall(def), Nat()), result.type);
  }

  @Test
  public void constructorInfer() {
    // \data D (A : \Type0) = con (B : \Type1) A B, f : D (Nat -> Nat) -> Nat |- f (con Nat (\lam x => x) zero) : Nat
    List<Constructor> constructors = new ArrayList<>(1);
    DataDefinition def = new DataDefinition("D", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("A"), Universe(0))), constructors);
    Constructor con = new Constructor(0, "con", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))), def);
    constructors.add(con);

    Expression expr = Apps(Index(0), Apps(DefCall(con), Nat(), Lam("x", Index(0)), Zero()));
    List<VcError> errors = new ArrayList<>();
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Apps(DefCall(def), Pi(Nat(), Nat())), Nat())));

    Map<String, Definition> definitions = Prelude.getDefinitions();
    definitions.put("D", def);
    definitions.put("con", con);

    CheckTypeVisitor.OKResult result = expr.checkType(definitions, localContext, null, errors);
    assertEquals(0, errors.size());
    assertNotNull(result);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void constructorConst() {
    // \data D (A : \Type0) = con A, f : (Nat -> D Nat) -> Nat -> Nat |- f con : Nat -> Nat
    List<Constructor> constructors = new ArrayList<>(1);
    DataDefinition def = new DataDefinition("D", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("A"), Universe(0))), constructors);
    Constructor con = new Constructor(0, "con", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(TypeArg(Index(0))), def);
    constructors.add(con);

    Expression expr = Apps(Index(0), DefCall(con));
    List<VcError> errors = new ArrayList<>();
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Pi(Nat(), Apps(DefCall(def), Nat())), Pi(Nat(), Nat()))));

    Map<String, Definition> definitions = Prelude.getDefinitions();
    definitions.put("D", def);
    definitions.put("con", con);

    CheckTypeVisitor.OKResult result = expr.checkType(definitions, localContext, null, errors);
    assertEquals(0, errors.size());
    assertNotNull(result);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }
}
