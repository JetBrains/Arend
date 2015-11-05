package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.module.output.OutputSupplier;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

import java.io.IOException;
import java.util.*;

public class DeserializingLoader {
  private final List<ResolvedName> myLoadingModules;
  private final ModuleLoader myModuleLoader;
  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;

  public DeserializingLoader(List<ResolvedName> loadingModules, ModuleLoader moduleLoader, SourceSupplier sourceSupplier, OutputSupplier outputSupplier) {
    this.myLoadingModules = loadingModules;
    this.myModuleLoader = moduleLoader;
    this.mySourceSupplier = sourceSupplier;
    this.myOutputSupplier = outputSupplier;
  }

  private final static class DeserializingModuleInfo {
    public final Output.Header header;
    public final Output output;
    public final Source source;
    public ModuleLoadingResult result;


    private DeserializingModuleInfo(Output.Header header, Output output, Source source) {
      this.header = header;
      this.output = output;
      this.source = source;
    }
  }

  private final Map<ResolvedName, DeserializingModuleInfo> myDeserializingModules = new HashMap<>();

  private boolean deserializeStubs(ResolvedName name, List<ResolvedName> deserializedModules) {
    DeserializingModuleInfo info = myDeserializingModules.get(name);

    if (name.toDefinition() != null || info.output.isContainer())
      return true;

    try {
      info.output.readStubs();
      if (info.source.isAvailable())
        info.source.load(true);
    } catch (IOException e) {
      for (ResolvedName rn : deserializedModules) {
        myDeserializingModules.remove(rn);
        rn.parent.removeMember(rn.toNamespaceMember());
      }
      myModuleLoader.loadingError(new GeneralError(name, GeneralError.ioError(e)));
      return false;
    }

    deserializedModules.add(name);
    for (List<String> dependency : myDeserializingModules.get(name).header.dependencies) {
      ResolvedName depName = new ResolvedName(null, "\\root");
      for (String aPath : dependency) {
        depName = new ResolvedName(depName.toNamespace(), aPath);
        if (!deserializeStubs(depName, deserializedModules)) {
          return false;
        }
      }
    }

    return true;
  }

  public ModuleLoadingResult load(ResolvedName module) {
    if (load(module, new HashSet<ResolvedName>()) == null)
      return null;
    return myDeserializingModules.get(module).result;
  }

  private Set<ResolvedName> load(ResolvedName module, Set<ResolvedName> visiting) {
    Source source = mySourceSupplier.getSource(module);
    Output output = myOutputSupplier.getOutput(module);
    if (!output.canRead()) {
      output = myOutputSupplier.locateOutput(module);
    }
    if (!output.canRead()) {
      return null;
    }

    if (output.isContainer() && (source.isContainer() || !source.isAvailable())) {
      try {
        myDeserializingModules.put(module, new DeserializingModuleInfo(
            new Output.Header(Collections.<List<String>>emptyList(), Collections.<String>emptyList()), output, source));
        myDeserializingModules.get(module).result = new ModuleLoadingResult(module.toNamespaceMember(), false, 0);
        output.readStubs();
        if (source.isAvailable())
          source.load(true);
        return Collections.emptySet();
      } catch (IOException e) {
        myModuleLoader.loadingError(new GeneralError(module, GeneralError.ioError(e)));
        module.parent.removeMember(module.toNamespaceMember());
        return null;
      }
    }

    if (source.isAvailable() && (source.isContainer() != output.isContainer() || source.lastModified() > output.lastModified()))
      return null;

    Output.Header header;
    try {
      header = output.getHeader();
    } catch (IOException e) {
      myModuleLoader.loadingError(new GeneralError(module, GeneralError.ioError(e)));
      return null;
    }

    Set<ResolvedName> visitingDeps = new HashSet<>();
    myDeserializingModules.put(module, new DeserializingModuleInfo(header, output, source));
    visiting.add(module);

    boolean failed = false;
    dependencies_loop:
    for (List<String> dependency : header.dependencies) {
      ResolvedName depName = new ResolvedName(null, "\\root");
      boolean isContainer = false;
      for (String aPath : dependency) {
        if (myDeserializingModules.containsKey(depName) && myDeserializingModules.get(depName).header.provided.contains(aPath)) {
          break;
        }
        depName = new ResolvedName(depName.toNamespace(), aPath);
        if (depName.toAbstractDefinition() != null || myLoadingModules.contains(depName)) {
          failed = true;
          break dependencies_loop;
        }
        if (!myDeserializingModules.containsKey(depName)) {
          Set<ResolvedName> nestedVisitngDeps = load(depName, visiting);
          if (nestedVisitngDeps == null) {
            failed = true;
            break dependencies_loop;
          }
          visitingDeps.addAll(nestedVisitngDeps);
        }
        DeserializingModuleInfo nestedInfo = myDeserializingModules.get(depName);
        if (nestedInfo.source.isAvailable() && !nestedInfo.source.isContainer() && nestedInfo.source.lastModified() > output.lastModified()) {
          failed = true;
          break dependencies_loop;
        }
        isContainer = nestedInfo.source.isContainer();
      }
      if (isContainer) {
        failed = true;
        break;
      }
    }
    visiting.remove(module);
    if (failed) {
      myDeserializingModules.remove(module);
      return null;
    }

    visitingDeps.remove(module);

    if (visitingDeps.isEmpty()) {
      List<ResolvedName> deserializedModules = new ArrayList<>();
      if (!deserializeStubs(module, deserializedModules)) {
        return null;
      }
        for (ResolvedName nestedModule : deserializedModules) {
          try {
            ModuleLoadingResult nestedResult = myDeserializingModules.get(nestedModule).output.read();
            ModuleLoader.Helper.processLoaded(myModuleLoader, nestedModule, nestedResult);
            myDeserializingModules.get(nestedModule).result = nestedResult;
          } catch (IOException e) {
            for (ResolvedName loaded : deserializedModules) {
              myDeserializingModules.remove(loaded);
              loaded.parent.removeMember(loaded.toNamespaceMember());
            }
            myModuleLoader.loadingError(new GeneralError(nestedModule, GeneralError.ioError(e)));
            return null;
          }
        }
    }

    return visitingDeps;
  }

  public void setSourceSupplier(SourceSupplier sourceSupplier) {
    mySourceSupplier = sourceSupplier;
  }

  public void setOutputSupplier(OutputSupplier outputSupplier) {
    myOutputSupplier = outputSupplier;
  }
}
