package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class DefinitionTest {
  @Test
  public void function() {
    // f : N => 0;
    ModuleLoader moduleLoader = new ModuleLoader();
    FunctionDefinition def = new FunctionDefinition("f", new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new ArrayList<Argument>(), Nat(), Definition.Arrow.RIGHT, Zero());
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(moduleLoader, (ClassDefinition) def.getParent(), def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(moduleLoader, (ClassDefinition) def.getParent(), def.getTerm(), typedDef, localContext, null);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertFalse(typedDef.hasErrors());
  }

  @Test
  public void functionUntyped() {
    // f => 0;
    ModuleLoader moduleLoader = new ModuleLoader();
    FunctionDefinition def = new FunctionDefinition("f", new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new ArrayList<Argument>(), null, Definition.Arrow.RIGHT, Zero());
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(moduleLoader, (ClassDefinition) def.getParent(), def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(moduleLoader, (ClassDefinition) def.getParent(), def.getTerm(), typedDef, localContext, null);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertFalse(def.hasErrors());
    assertEquals(Nat(), typedDef.getType());
  }

  @Test
  public void functionWithArgs() {
    // f (x : N) (y : N -> N) => y;
    List<Argument> arguments = new ArrayList<>();
    arguments.add(Tele(vars("x"), Nat()));
    arguments.add(Tele(vars("y"), Pi(Nat(), Nat())));

    ModuleLoader moduleLoader = new ModuleLoader();
    FunctionDefinition def = new FunctionDefinition("f", new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, arguments, null, Definition.Arrow.RIGHT, Index(0));
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(moduleLoader, (ClassDefinition) def.getParent(), def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(moduleLoader, (ClassDefinition) def.getParent(), def.getTerm(), typedDef, localContext, null);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(Nat(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat()))), typedDef.getType());
  }

  @Test
  public void dataType() {
    // \data D {A B : \Type0} (I : A -> B -> Type0) (a : A) (b : B) | con1 (x : A) (I x b) | con2 {y : B} (I a y)
    List<TypeArgument> parameters = new ArrayList<>(4);
    parameters.add(Tele(false, vars("A", "B"), Universe(0)));
    parameters.add(Tele(vars("I"), Pi(Index(1), Pi(Index(0), Universe(0)))));
    parameters.add(Tele(vars("a"), Index(2)));
    parameters.add(Tele(vars("b"), Index(2)));

    ModuleLoader moduleLoader = new ModuleLoader();
    List<Constructor> constructors = new ArrayList<>(2);
    DataDefinition def = new DataDefinition("D", new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, parameters, constructors);

    List<TypeArgument> arguments1 = new ArrayList<>(2);
    arguments1.add(Tele(vars("x"), Index(4)));
    arguments1.add(TypeArg(Apps(Index(3), Index(0), Index(1))));
    constructors.add(new Constructor(0, "con1", def, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, arguments1));

    List<TypeArgument> arguments2 = new ArrayList<>(2);
    arguments2.add(Tele(false, vars("y"), Index(3)));
    arguments2.add(TypeArg(Apps(Index(3), Index(2), Index(0))));
    constructors.add(new Constructor(1, "con2", def, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, arguments2));

    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(moduleLoader, (ClassDefinition) def.getParent(), def, localContext);
    assertNotNull(typedDef);
    for (int i = 0; i < constructors.size(); i++) {
      TypeChecking.typeCheckConstructor(moduleLoader, typedDef, constructors.get(i), localContext, i);
    }
    TypeChecking.typeCheckDataEnd(moduleLoader, (ClassDefinition) def.getParent(), def, typedDef, localContext);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(parameters, Universe(0)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());
    assertEquals(Pi(arguments1, Apps(Apps(Apps(DefCall(typedDef), Index(6), false, false), Index(5), false, false), Index(4), Index(3), Index(2))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(Apps(Apps(DefCall(typedDef), Index(6), false, false), Index(5), false, false), Index(4), Index(3), Index(2))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void dataType2() {
    // \data D (A : \7-Type2) = con1 (X : \1-Type5) X | con2 (Y : \2-Type3) A Y
    List<TypeArgument> parameters = new ArrayList<>(1);
    parameters.add(Tele(vars("A"), Universe(2, 7)));
    ModuleLoader moduleLoader = new ModuleLoader();
    List<Constructor> constructors = new ArrayList<>(2);
    DataDefinition def = new DataDefinition("D", new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, parameters, constructors);

    List<TypeArgument> arguments1 = new ArrayList<>(2);
    arguments1.add(Tele(vars("X"), Universe(5, 1)));
    arguments1.add(TypeArg(Index(0)));
    constructors.add(new Constructor(0, "con1", def, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, arguments1));

    List<TypeArgument> arguments2 = new ArrayList<>(3);
    arguments2.add(Tele(vars("Y"), Universe(3, 2)));
    arguments2.add(TypeArg(Index(1)));
    arguments2.add(TypeArg(Index(1)));
    constructors.add(new Constructor(1, "con2", def, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, arguments2));

    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(moduleLoader, (ClassDefinition) def.getParent(), def, localContext);
    assertNotNull(typedDef);
    for (int i = 0; i < constructors.size(); i++) {
      TypeChecking.typeCheckConstructor(moduleLoader, typedDef, constructors.get(i), localContext, i);
    }
    TypeChecking.typeCheckDataEnd(moduleLoader, (ClassDefinition) def.getParent(), def, typedDef, localContext);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(parameters, Universe(6, 7)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());
    assertEquals(Pi(arguments1, Apps(DefCall(typedDef), Index(2))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(DefCall(typedDef), Index(3))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void constructor() {
    // \data D (A : \Type0) = con (B : \Type1) A B |- con Nat zero zero : D Nat
    ModuleLoader moduleLoader = new ModuleLoader();
    List<Constructor> constructors = new ArrayList<>(1);
    DataDefinition def = new DataDefinition("D", new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("A"), Universe(0))), constructors);
    Constructor con = new Constructor(0, "con", def, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))));
    constructors.add(con);

    Expression expr = Apps(DefCall(con), Nat(), Zero(), Zero());
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), null, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertNotNull(result);
    assertEquals(Apps(DefCall(def), Nat()), result.type);
  }

  @Test
  public void constructorInfer() {
    // \data D (A : \Type0) = con (B : \Type1) A B, f : D (Nat -> Nat) -> Nat |- f (con Nat (\lam x => x) zero) : Nat
    List<Constructor> constructors = new ArrayList<>(1);
    ModuleLoader moduleLoader = new ModuleLoader();
    DataDefinition def = new DataDefinition("D", new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("A"), Universe(0))), constructors);
    Constructor con = new Constructor(0, "con", def, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))));
    constructors.add(con);

    Expression expr = Apps(Index(0), Apps(DefCall(con), Nat(), Lam("x", Index(0)), Zero()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Apps(DefCall(def), Pi(Nat(), Nat())), Nat())));

    CheckTypeVisitor.OKResult result = expr.checkType(localContext, null, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertNotNull(result);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void constructorConst() {
    // \data D (A : \Type0) = con A, f : (Nat -> D Nat) -> Nat -> Nat |- f con : Nat -> Nat
    List<Constructor> constructors = new ArrayList<>(1);
    ModuleLoader moduleLoader = new ModuleLoader();
    DataDefinition def = new DataDefinition("D", new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(Tele(vars("A"), Universe(0))), constructors);
    Constructor con = new Constructor(0, "con", def, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, null, args(TypeArg(Index(0))));
    constructors.add(con);

    Expression expr = Apps(Index(0), DefCall(con));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Pi(Nat(), Apps(DefCall(def), Nat())), Pi(Nat(), Nat()))));

    CheckTypeVisitor.OKResult result = expr.checkType(localContext, null, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertNotNull(result);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }
}
