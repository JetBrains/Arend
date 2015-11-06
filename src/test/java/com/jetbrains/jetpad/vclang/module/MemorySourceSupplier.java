package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.DummySource;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.definition.ResolvedName.toPath;

public class MemorySourceSupplier implements SourceSupplier {
  static class MemorySourceEntry {
    final List<String> children;
    final String source;
    final long lastModified;

    private MemorySourceEntry(String source, List<String> children) {
      this.source = source;
      this.children = children;
      this.lastModified = System.nanoTime();
    }
  }

  private final ModuleLoader myModuleLoader;
  private final ErrorReporter myErrorReporter;
  private final Map<List<Name>, MemorySourceEntry> myMap = new HashMap<>();

  public MemorySourceSupplier(ModuleLoader moduleLoader, ErrorReporter errorReporter) {
    myModuleLoader = moduleLoader;
    myErrorReporter = errorReporter;
    add(moduleName(), null);
  }

  public void add(List<Name> module, String source) {
    MemorySourceEntry oldEntry = myMap.get(module);
    myMap.put(module, new MemorySourceEntry(source, oldEntry == null ? new ArrayList<String>() : oldEntry.children));
    if (!module.isEmpty()) {
      myMap.get(module.subList(0, module.size() - 1)).children.add(module.get(module.size() - 1).name);
    }
   }

  public void touch(List<Name> module) {
    add(module, myMap.get(module).source);
  }

  public static List<Name> moduleName(String... module) {
    List<Name> result = new ArrayList<>();
    for (String aPath : module) {
      result.add(new Name(aPath));
    }
    return result;
  }

  @Override
  public Source getSource(ResolvedName module) {
    MemorySourceEntry entry = myMap.get(toPath(module));
    return entry != null ? new MemorySource(myModuleLoader, myErrorReporter, module, entry): new DummySource();
  }
}
