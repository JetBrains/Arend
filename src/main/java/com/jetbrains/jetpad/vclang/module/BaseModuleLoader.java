package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.error.CycleError;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.output.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.module.output.OutputSupplier;
import com.jetbrains.jetpad.vclang.module.source.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;

public abstract class BaseModuleLoader implements ModuleLoader {
  private final List<ResolvedName> myLoadingModules = new ArrayList<>();

  // TODO: extract deserialization
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

  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;
  private final boolean myRecompile;

  public BaseModuleLoader(boolean recompile) {
    mySourceSupplier = DummySourceSupplier.getInstance();
    myOutputSupplier = DummyOutputSupplier.getInstance();
    myRecompile = recompile;
  }

  public void setSourceSupplier(SourceSupplier sourceSupplier) {
    mySourceSupplier = sourceSupplier;
  }

  public void setOutputSupplier(OutputSupplier outputSupplier) {
    myOutputSupplier = outputSupplier;
  }

  @Override
  public void save(ResolvedName module) {
    Output output = myOutputSupplier.getOutput(module);
    if (output.canWrite()) {
      try {
        output.write();
      } catch (IOException e) {
        savingError(new GeneralError(module, GeneralError.ioError(e)));
      }
    }
  }

  private boolean deserializeStubs(ResolvedName name, List<ResolvedName> deserializedModules) {
    if (name.toDefinition() != null || myDeserializingModules.get(name).source != null && myDeserializingModules.get(name).source.isContainer())
      return true;
    try {
      myDeserializingModules.get(name).output.readStubs();
    } catch (IOException e) {
      for (ResolvedName rn : deserializedModules) {
        myDeserializingModules.remove(rn);
        rn.parent.removeMember(rn.toNamespaceMember());
      }
      loadingError(new GeneralError(name, GeneralError.ioError(e)));
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

  private boolean tryDeserialize(ResolvedName module) {
    return tryDeserialize(module, new HashSet<ResolvedName>()) != null;
  }

  private Set<ResolvedName> tryDeserialize(ResolvedName module, Set<ResolvedName> visiting) {
    Source source = mySourceSupplier.getSource(module);
    Output output = myOutputSupplier.getOutput(module);
    if (!output.canRead()) {
      output = myOutputSupplier.locateOutput(module);
    }

    if (output == null || !output.canRead()) {
      if (source.isContainer()) {
        module.parent.getChild(module.name);
        myDeserializingModules.put(module, new DeserializingModuleInfo(
            new Output.Header(Collections.<List<String>>emptyList(), Collections.<String>emptyList()), null, source));
        myDeserializingModules.get(module).result = new ModuleLoadingResult(module.toNamespaceMember(), false, 0);
        return Collections.emptySet();
      } else {
        return null;
      }
    }

    if (source.isAvailable() && source.lastModified() > output.lastModified())
      return null;

    Output.Header header;
    try {
      header = output.getHeader();
    } catch (IOException e) {
      loadingError(new GeneralError(module, GeneralError.ioError(e)));
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
          Set<ResolvedName> nestedVisitngDeps = tryDeserialize(depName, visiting);
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
            processLoaded(nestedModule, nestedResult);
            myDeserializingModules.get(nestedModule).result = nestedResult;
          } catch (IOException e) {
            for (ResolvedName loaded : deserializedModules) {
              myDeserializingModules.remove(loaded);
              loaded.parent.removeMember(loaded.toNamespaceMember());
            }
            loadingError(new GeneralError(nestedModule, GeneralError.ioError(e)));
            return null;
          }
        }
    }

    return visitingDeps;
  }

  private void processLoaded(ResolvedName module, ModuleLoadingResult result) {
    if (result == null || result.errorsNumber != 0) {
      GeneralError error = new GeneralError(module.parent.getResolvedName(), result == null ? "cannot load module '" + module.name + "'" : "module '" + module.name + "' contains " + result.errorsNumber + (result.errorsNumber == 1 ? " error" : " errors"));
      error.setLevel(GeneralError.Level.INFO);
      loadingError(error);
    } else {
      if (result.namespaceMember != null && (result.namespaceMember.abstractDefinition != null || result.namespaceMember.definition != null)) {
        loadingSucceeded(module, result.namespaceMember, result.compiled);
      }
    }

    if (result != null && result.namespaceMember != null) {
      module.parent.addMember(result.namespaceMember);
    }
  }

  @Override
  public ModuleLoadingResult load(ResolvedName module, boolean tryLoad) {
    NamespaceMember member = module.parent.getMember(module.name.name);
    if (member != null && (member.abstractDefinition != null || member.definition != null)) {
      return null;
    }

    int index = myLoadingModules.indexOf(module);
    if (index != -1) {
      loadingError(new CycleError(new ArrayList<>(myLoadingModules.subList(index, myLoadingModules.size()))));
      return null;
    }

    try {
      if (!myRecompile && tryDeserialize(module)) {
        return myDeserializingModules.get(module).result;
      }

      Source source = mySourceSupplier.getSource(module);
      if (!source.isAvailable()) {
        if (!tryLoad) {
          loadingError(new ModuleNotFoundError(module));
        }
        return null;
      }

      try {
        myLoadingModules.add(module);

        ModuleLoadingResult result = source.load();
        processLoaded(module, result);

        return result;
      } finally {
        myLoadingModules.remove(myLoadingModules.size() - 1);
      }
    } catch (EOFException e) {
      loadingError(new GeneralError(module, "Incorrect format: Unexpected EOF"));
    } catch (ModuleDeserialization.DeserializationException e) {
      loadingError(new GeneralError(module, e.toString()));
    } catch (IOException e) {
      loadingError(new GeneralError(module, GeneralError.ioError(e)));
    }
    return null;
  }
}
