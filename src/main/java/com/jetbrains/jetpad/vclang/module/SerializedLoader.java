package com.jetbrains.jetpad.vclang.module;

public class SerializedLoader {
  // FIXME[serial]
  /*
  private final List<ModuleID> myLoadingModules;
  private final ModuleLoader myModuleLoader;
  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;

  public SerializedLoader(List<ModuleID> loadingModules, ModuleLoader moduleLoader, SourceSupplier sourceSupplier, OutputSupplier outputSupplier) {
    this.myLoadingModules = loadingModules;
    this.myModuleLoader = moduleLoader;
    this.mySourceSupplier = sourceSupplier;
    this.myOutputSupplier = outputSupplier;
  }

  private final static class DeserializingModuleInfo {
    public final Output.Header header;
    public final Output output;
    public final Source source;
    public ModuleLoader.Result result;

    private DeserializingModuleInfo(Output.Header header, Output output, Source source) {
      this.header = header;
      this.output = output;
      this.source = source;
    }
  }

  private final Map<ModuleID, DeserializingModuleInfo> myDeserializingModules = new HashMap<>();

  private boolean deserializeStubs(ModuleID module, List<ModuleID> deserializedModules) {
    DeserializingModuleInfo info = myDeserializingModules.get(module);

    if (Root.getModule(module) != null)
      return true;

    try {
      info.output.readStubs();
    } catch (IOException e) {
      for (ModuleID deserializedModule : deserializedModules) {
        myDeserializingModules.remove(deserializedModule);
        Root.removeModule(deserializedModule);
      }
      myModuleLoader.loadingError(new ModuleLoadingError(module, GeneralError.ioError(e)));
      return false;
    }

    deserializedModules.add(module);
    for (ModuleID dependency : myDeserializingModules.get(module).header.dependencies) {
      if (!deserializeStubs(dependency, deserializedModules)) {
        return false;
      }
    }

    return true;
  }

  public ModuleLoader.Result load(ModuleID module) {
    if (load(module, new HashSet<ModuleID>()) == null)
      return null;
    return myDeserializingModules.get(module).result;
  }

  private Set<ModuleID> load(ModuleID module, Set<ModuleID> visiting) {
    Source source = mySourceSupplier.getSource(module);
    Output output = myOutputSupplier.getOutput(module);

    if (output == null || !output.canRead()) {
      return null;
    }

    if (source != null && source.lastModified() > output.lastModified())
      return null;

    Output.Header header;
    try {
      header = output.readHeader();
    } catch (IOException e) {
      myModuleLoader.loadingError(new ModuleLoadingError(module, GeneralError.ioError(e)));
      return null;
    }

    Set<ModuleID> visitingDeps = new HashSet<>();
    myDeserializingModules.put(module, new DeserializingModuleInfo(header, output, source));
    visiting.add(module);

    boolean failed = false;
    for (ModuleID dependency : header.dependencies) {
      if (Root.getModule(dependency) != null && Root.getModule(dependency).abstractDefinition != null || myLoadingModules.contains(dependency)) {
        failed = true;
        break;
      }
      if (!myDeserializingModules.containsKey(dependency)) {
        Set<ModuleID> nestedVisitingDeps = load(dependency, visiting);
        if (nestedVisitingDeps == null) {
          failed = true;
          break;
        }
        visitingDeps.addAll(nestedVisitingDeps);
      }

      DeserializingModuleInfo nestedInfo = myDeserializingModules.get(dependency);
      if (nestedInfo.source != null && nestedInfo.source.lastModified() > output.lastModified()) {
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
      List<ModuleID> deserializedModules = new ArrayList<>();
      if (!deserializeStubs(module, deserializedModules)) {
        return null;
      }

      for (ModuleID nestedModule : deserializedModules) {
        try {
          ModuleLoader.Result nestedResult = myDeserializingModules.get(nestedModule).output.read();
          //ModuleLoader.Helper.processLoaded(myModuleLoader, nestedModule, nestedResult);  // FIXME[serial]
          myDeserializingModules.get(nestedModule).result = nestedResult;
        } catch (IOException e) {
          for (ModuleID loaded : deserializedModules) {
            myDeserializingModules.remove(loaded);
            Root.removeModule(loaded);
          }
          myModuleLoader.loadingError(new ModuleLoadingError(nestedModule, GeneralError.ioError(e)));
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
  */
}
