package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.NameModuleID;
import com.jetbrains.jetpad.vclang.module.ReportingModuleLoader;
import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static org.junit.Assert.*;

public class ModuleSerializationTest {
  ListErrorReporter errorReporter;
  ReportingModuleLoader moduleLoader;

  @Before
  public void initialize() {
    Root.initialize();
    errorReporter = new ListErrorReporter();
    moduleLoader = new ReportingModuleLoader(errorReporter, false);
  }

  @Test
  public void serializeExprTest() throws IOException {
    NamespaceMember member = typeCheckClass("\\static \\function f => \\lam {x y : Nat} (z : Nat -> Nat) => \\Pi (A : \\Type0) {x : A} -> A");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    NameModuleID moduleID = (NameModuleID) member.definition.getResolvedName().getModuleID();
    ModuleSerialization.writeStream(moduleID, dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleLoader.Result result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(((FunctionDefinition) member.namespace.getDefinition("f")).getElimTree(), ((FunctionDefinition) result.namespaceMember.namespace.getDefinition("f")).getElimTree());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeElimTest() throws IOException {
    NamespaceMember member = typeCheckClass("\\static \\function\n" +
        " f (x y : Nat) : Nat <= \\elim x, y\n" +
        "  | _, zero => 0\n" +
        "  | x, suc zero => x\n" +
        "  | _, suc (suc x) => suc x");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    NameModuleID moduleID = (NameModuleID) member.definition.getResolvedName().getModuleID();
    ModuleSerialization.writeStream(moduleID, dataStream);

    Root.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    ModuleLoader.Result result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    FunctionDefinition oldFunc = ((FunctionDefinition) member.namespace.getDefinition("f"));
    FunctionDefinition newFunc = ((FunctionDefinition) result.namespaceMember.namespace.getDefinition("f"));
    Map<Binding, Binding>  argsBinding = new HashMap<>();
    argsBinding.put(oldFunc.getParameters(), newFunc.getParameters());
    argsBinding.put(oldFunc.getParameters().getNext(), newFunc.getParameters().getNext());
    assertTrue(CompareVisitor.compare(argsBinding, DummyEquations.getInstance(), Equations.CMP.EQ, oldFunc.getElimTree(), newFunc.getElimTree()));
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeDataTest() throws IOException {
    NamespaceMember member = typeCheckClass("\\static \\data D (A : \\Type0) | con1 A | con2 Nat A");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    NameModuleID moduleID = (NameModuleID) member.definition.getResolvedName().getModuleID();
    ModuleSerialization.writeStream(moduleID, dataStream);

    Root.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    ModuleLoader.Result result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(member.namespace.getMembers().size(), result.namespaceMember.definition.getResolvedName().toNamespace().getMembers().size());
    assertEquals(member.definition.getResolvedName().toNamespace().getMembers().size(), result.namespaceMember.namespace.getMembers().size());
    assertEquals(member.namespace.getDefinition("D").getType(), result.namespaceMember.namespace.getDefinition("D").getType());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeIndexedDataTest() throws IOException {
    NamespaceMember member = typeCheckClass("\\static \\data D (n : Nat) | D zero => con0 | D (suc n) => con1 (n = n)");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    NameModuleID moduleID = (NameModuleID) member.definition.getResolvedName().getModuleID();
    ModuleSerialization.writeStream(moduleID, dataStream);

    Root.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    ModuleLoader.Result result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(member.namespace.getMembers().size(), result.namespaceMember.definition.getResolvedName().toNamespace().getMembers().size());
    assertEquals(member.definition.getResolvedName().toNamespace().getMembers().size(), result.namespaceMember.namespace.getMembers().size());
    assertEquals(member.namespace.getDefinition("D").getType(), result.namespaceMember.namespace.getDefinition("D").getType());
    assertEquals(0, errorReporter.getErrorList().size());
    DataDefinition newDef = (DataDefinition) result.namespaceMember.namespace.getDefinition("D");
    Constructor con0 = newDef.getConstructor("con0");
    assertTrue(con0.getPatterns() != null);
    assertEquals(con0.getPatterns().getPatterns().size(), 1);
    assertTrue(con0.getPatterns().getPatterns().get(0).getPattern() instanceof ConstructorPattern);
    assertTrue(((ConstructorPattern) con0.getPatterns().getPatterns().get(0).getPattern()).getConstructor() == Preprelude.ZERO);
    assertTrue(con0.getPatterns().getParameters() == EmptyDependentLink.getInstance());
    Constructor con1 = newDef.getConstructor("con1");
    assertTrue(con1.getPatterns() != null);
    assertEquals(con1.getPatterns().getPatterns().size(), 1);
    assertTrue(con1.getPatterns().getPatterns().get(0).getPattern() instanceof ConstructorPattern);
    assertTrue(((ConstructorPattern) con1.getPatterns().getPatterns().get(0).getPattern()).getConstructor() == Preprelude.SUC);
    assertTrue(con1.getPatterns().getParameters() instanceof TypedDependentLink);
  }

  @Test
  public void serializeFunctionTest() throws IOException {
    NamespaceMember member = typeCheckClass(
        " \\static \\function\n" +
        " f : Nat => g\n" +
        " \\where \\static \\function\n" +
        "        g : Nat => 0\n" +
        "\n");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    NameModuleID moduleID = (NameModuleID) member.definition.getResolvedName().getModuleID();
    ModuleSerialization.writeStream(moduleID, dataStream);

    Root.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    ModuleLoader.Result result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(1, result.namespaceMember.namespace.getMembers().size());
    Definition definition = result.namespaceMember.namespace.getDefinition("f");
    assertNotNull(definition);
    assertEquals(1, member.namespace.getMembers().size());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeNestedTest() throws IOException {
    NamespaceMember member = typeCheckClass("\\class A { \\class B { \\class C { } } }");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    NameModuleID moduleID = (NameModuleID) member.definition.getResolvedName().getModuleID();
    ModuleSerialization.writeStream(moduleID, dataStream);

    Root.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    ModuleLoader.Result result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), moduleID);
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
  }
}