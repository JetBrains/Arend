package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.module.FileModuleID;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.source.FileSource;
import com.jetbrains.jetpad.vclang.module.utils.FileOperations;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.*;
import com.jetbrains.jetpad.vclang.naming.oneshot.OneshotNameResolver;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.ConcreteResolveListener;
import com.jetbrains.jetpad.vclang.term.Prelude;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class PreludeTest {
  public static final ModuleNamespaceProvider DEFAULT_MODULE_NS_PROVIDER = new SimpleModuleNamespaceProvider();
  public static final StaticNamespaceProvider DEFAULT_STATIC_NS_PROVIDER = new SimpleStaticNamespaceProvider();
  public static final DynamicNamespaceProvider DEFAULT_DYNAMIC_NS_PROVIDER = new SimpleDynamicNamespaceProvider();
  public static final NameResolver DEFAULT_NAME_RESOLVER = new NameResolver(DEFAULT_MODULE_NS_PROVIDER, DEFAULT_STATIC_NS_PROVIDER, DEFAULT_DYNAMIC_NS_PROVIDER);
  public static Abstract.Definition PRELUDE_DEFINITION;

  @BeforeClass
  public static void initializePrelude() throws IOException {
    Prelude.moduleID = new FileModuleID(new ModulePath("Prelude"));
    ErrorReporter errorReporter = new ListErrorReporter();
    ModuleLoader.Result result = new FileSource(errorReporter, Prelude.moduleID, FileOperations.getFile(new File(Paths.get("").toAbsolutePath().toFile(), "lib"), Prelude.moduleID.getModulePath(), FileOperations.EXTENSION)).load();
    OneshotNameResolver oneshotNameResolver = new OneshotNameResolver(errorReporter, DEFAULT_NAME_RESOLVER, new ConcreteResolveListener(), DEFAULT_STATIC_NS_PROVIDER, DEFAULT_DYNAMIC_NS_PROVIDER);
    oneshotNameResolver.visitModule(result.abstractDefinition);
    Prelude.PRELUDE = (SimpleNamespace) DEFAULT_STATIC_NS_PROVIDER.forDefinition(result.abstractDefinition);
    PRELUDE_DEFINITION = result.abstractDefinition;
  }
}
