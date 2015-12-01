package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.ReportingModuleLoader;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static org.junit.Assert.*;

public class ModuleSerializationTest {
  ListErrorReporter errorReporter;
  ReportingModuleLoader moduleLoader;

  @Before
  public void initialize() {
    RootModule.initialize();
    errorReporter = new ListErrorReporter();
    moduleLoader = new ReportingModuleLoader(errorReporter, false);
  }

  @Test
  public void serializeExprTest() throws IOException {
    ClassDefinition def = typeCheckClass("\\static \\function f => \\lam {x y : Nat} (z : Nat -> Nat) => \\Pi (A : \\Type0) {x : A} -> A");
    Namespace namespace = def.getResolvedName().toNamespace();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(new ResolvedName(def.getParentNamespace(), def.getName()), dataStream);

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, "test"));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) namespace.getDefinition("f")).getTerm(), ((FunctionDefinition) result.namespaceMember.namespace.getDefinition("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeElimTest() throws IOException {
    ClassDefinition def = typeCheckClass("\\static \\function\n" +
        " f (x y : Nat) : Nat <= \\elim x, y\n" +
        "  | _, zero => 0\n" +
        "  | x, suc zero => x\n" +
        "  | _, suc (suc x) => suc x");
    Namespace namespace = def.getResolvedName().toNamespace();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getResolvedName(), dataStream);

    RootModule.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, "test"));
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, "test"));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(CompareVisitor.CMP.EQUALS, compare(((FunctionDefinition) namespace.getDefinition("f")).getTerm(), ((FunctionDefinition) result.namespaceMember.namespace.getDefinition("f")).getTerm(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeDataTest() throws IOException {
    ClassDefinition def = typeCheckClass("\\data D (A : \\Type0) | con1 A | con2 Nat A");
    Namespace namespace = def.getResolvedName().toNamespace();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(new ResolvedName(def.getParentNamespace(), def.getName()), dataStream);

    RootModule.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, "test"));
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, "test"));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(def.getStatements().size(), ((ClassDefinition) result.namespaceMember.definition).getStatements().size());
    assertEquals(def.getResolvedName().toNamespace().getMembers().size(), result.namespaceMember.namespace.getMembers().size());
    assertEquals(CompareVisitor.CMP.EQUALS, compare(namespace.getDefinition("D").getType(), result.namespaceMember.namespace.getDefinition("D").getType(), new ArrayList<CompareVisitor.Equation>(0)).isOK());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeFunctionTest() throws IOException {
    ClassDefinition def = typeCheckClass(
        " \\static \\function\n" +
        " f : Nat => g\n" +
        " \\where \\static \\function\n" +
        "        g : Nat => 0\n" +
        "\n");
    Namespace namespace = def.getResolvedName().toNamespace();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getResolvedName(), dataStream);

    RootModule.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, def.getName()));
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, def.getName()));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
    assertEquals(1, result.namespaceMember.namespace.getMembers().size());
    Definition definition = result.namespaceMember.namespace.getDefinition("f");
    assertNotNull(definition);
    assertEquals(1, namespace.getMembers().size());
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void serializeNestedTest() throws IOException {
    ClassDefinition def = typeCheckClass("\\class A { \\class B { \\class C { } } }");
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    DataOutputStream dataStream = new DataOutputStream(stream);
    ModuleSerialization.writeStream(def.getResolvedName(), dataStream);

    RootModule.initialize();

    ModuleDeserialization moduleDeserialization = new ModuleDeserialization();
    ModuleDeserialization.readStubsFromStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, new Name("test")));
    ModuleLoadingResult result = moduleDeserialization.readStream(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())), new ResolvedName(RootModule.ROOT, new Name("test")));
    assertNotNull(result);
    assertNotNull(result.namespaceMember);
    assertTrue(result.namespaceMember.definition instanceof ClassDefinition);
    assertEquals(0, result.errorsNumber);
  }
}