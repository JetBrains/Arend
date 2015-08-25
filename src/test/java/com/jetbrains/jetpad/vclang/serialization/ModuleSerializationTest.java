package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class ModuleSerializationTest {
  ModuleLoader dummyModuleLoader;
  @Before
  public void initialize() {
    dummyModuleLoader = new ModuleLoader();
    dummyModuleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
  }

  @Test
  public void serializeExprTest() throws IOException {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = new ClassDefinition("test", moduleLoader.rootModule());
    Expression term = Lam(lamArgs(Tele(false, vars("x", "y"), Nat()), Tele(vars("z"), Pi(Nat(), Nat()))), Pi(args(Tele(vars("A"), Universe()), TypeArg(false, Index(0))), Index(1)));
    FunctionDefinition functionDefinition = new FunctionDefinition(new Utils.Name("f"), def, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, term);
    def.addField(functionDefinition, null);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition("test", moduleLoader.rootModule());
    moduleLoader.rootModule().addField(newDef, null);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef);
    assertEquals(0, errors);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) def.getStaticField("f")).getTerm(), ((FunctionDefinition) newDef.getStaticField("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void serializeElimTest() throws IOException {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = new ClassDefinition("test", moduleLoader.rootModule());
    List<Clause> clauses1 = new ArrayList<>(2);
    ElimExpression term1 = Elim(Index(0), clauses1);
    List<Clause> clauses2 = new ArrayList<>(2);
    ElimExpression term2 = Elim(Index(1), clauses2);
    clauses1.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Zero(), term1));
    clauses1.add(new Clause(match(Prelude.SUC, match("x")), Abstract.Definition.Arrow.LEFT, term2, term1));
    clauses2.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Index(0), term2));
    clauses2.add(new Clause(match(Prelude.SUC, match("x")), Abstract.Definition.Arrow.LEFT, Suc(Index(0)), term2));
    FunctionDefinition functionDefinition = new FunctionDefinition(new Utils.Name("f"), def, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, term1);
    def.addField(functionDefinition, null);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition("test", moduleLoader.rootModule());
    moduleLoader.rootModule().addField(newDef, null);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef);
    assertEquals(0, errors);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) def.getStaticField("f")).getTerm(), ((FunctionDefinition) newDef.getStaticField("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void noAbstractTestError() throws IOException {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = new ClassDefinition("test", moduleLoader.rootModule());
    ClassDefinition aClass = new ClassDefinition("A", def);
    def.addField(aClass, null);
    FunctionDefinition functionDefinition = new FunctionDefinition(new Utils.Name("f"), aClass, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), null, null);
    aClass.addField(functionDefinition, null);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition("test", moduleLoader.rootModule());
    moduleLoader.rootModule().addField(newDef, null);
    ClassDefinition bClass = new ClassDefinition("A", newDef);
    newDef.addField(bClass, null);
    bClass.addField(new FunctionDefinition(new Utils.Name("g"), aClass, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), null, null), null);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef);
    assertEquals(0, errors);
    assertEquals(1, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void serializeDataTest() throws IOException {
    ClassDefinition def = new ClassDefinition("test", dummyModuleLoader.rootModule());
    DataDefinition dataDefinition = new DataDefinition(new Utils.Name("D"), def, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(Tele(vars("A"), Universe(0))));
    dataDefinition.addConstructor(new Constructor(0, new Utils.Name("con1"), dataDefinition, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(TypeArg(Index(0)))));
    dataDefinition.addConstructor(new Constructor(1, new Utils.Name("con2"), dataDefinition, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0), args(TypeArg(Nat()), TypeArg(Index(1)))));
    def.addField(dataDefinition, null);
    def.addField(dataDefinition.getConstructors().get(0), null);
    def.addField(dataDefinition.getConstructors().get(1), null);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(dummyModuleLoader);
    ClassDefinition newDef = new ClassDefinition("test", dummyModuleLoader.rootModule());
    dummyModuleLoader.rootModule().addField(newDef, null);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef);
    assertEquals(0, errors);
    assertEquals(def.getFields().size(), newDef.getFields().size());
    assertEquals(def.getStaticFields().size(), newDef.getStaticFields().size());
    assertEquals(CompareVisitor.CMP.EQUALS, compare(dataDefinition.getType(), newDef.getStaticField("D").getType(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, dummyModuleLoader.getErrors().size());
    assertEquals(0, dummyModuleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void serializeFunctionTest() throws IOException {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = new ClassDefinition("test", moduleLoader.rootModule());
    FunctionDefinition funcDef = new FunctionDefinition(new Utils.Name("f"), def, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, null);
    FunctionDefinition innerFunc = new FunctionDefinition(new Utils.Name("g"), funcDef, Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, Zero());
    funcDef.addField(innerFunc, moduleLoader.getErrors());
    funcDef.setTerm(DefCall(innerFunc));
    def.addField(funcDef, null);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    ClassDefinition newDef = new ClassDefinition("test", moduleLoader.rootModule());
    moduleLoader.rootModule().addField(newDef, null);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef);
    assertEquals(0, errors);
    assertEquals(1, newDef.getFields().size());
    assertEquals(1, (newDef.getStaticField("f")).getFields().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void serializeNestedTest() throws IOException {
    ClassDefinition def = (ClassDefinition) parseDef(dummyModuleLoader, "\\class A { \\class B { \\class C { } } }").getParent();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def, dataStream);

    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition newDef = (ClassDefinition) parseDef(moduleLoader, "\\class B {}").getParent();
    moduleLoader.rootModule().addField(newDef, moduleLoader.getErrors());
    ModuleDeserialization moduleDeserialization = new ModuleDeserialization(moduleLoader);
    int errors = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), newDef);
    assertEquals(0, errors);
  }
}