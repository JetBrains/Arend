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
import java.util.ArrayList;
import java.util.List;

public abstract class BaseModuleLoader implements ModuleLoader {
  private final List<ResolvedName> myLoadingModules = new ArrayList<>();
  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;
  private final boolean myRecompile;

  public BaseModuleLoader(boolean recompile) {
    mySourceSupplier = DummySourceSupplier.getInstance();
    myOutputSupplier = DummyOutputSupplier.getInstance();
    myRecompile = true; // recompile; // TODO: Fix serialization.
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

  private boolean areDependenciesValid(Output output, boolean tryLoad) {
    if (!output.canRead())
      return false;
    for (List<String> dependency : output.getDependencies()) {
      ResolvedName name = new ResolvedName(null, "\\root");
      for (String aPath : dependency) {
        name = new ResolvedName(name.toNamespace(), aPath);
        Source source = mySourceSupplier.getSource(name);
        if (source != null && source.lastModified() > output.lastModified()) {
          return false;
        }
        load(name, tryLoad, true);
        if (name.toDefinition() == null)
          return false;
      }
    }
    return true;
  }

  @Override
  public ModuleLoadingResult load(ResolvedName module, boolean tryLoad) {
    return load(module, tryLoad, false);
  }

  public ModuleLoadingResult load(ResolvedName module, boolean tryLoad, boolean forceNoCompile) {
    NamespaceMember member = module.parent.getMember(module.name.name);
    if (member != null && (member.abstractDefinition != null || member.definition != null)) {
      return null;
    }

    int index = myLoadingModules.indexOf(module);
    if (index != -1) {
      loadingError(new CycleError(new ArrayList<>(myLoadingModules.subList(index, myLoadingModules.size()))));
      return null;
    }

    Source source = mySourceSupplier.getSource(module);
    Output output = myOutputSupplier.getOutput(module);
    boolean compile;
    if (source.isAvailable()) {
      compile = myRecompile || source.lastModified() > output.lastModified() || areDependenciesValid(output, tryLoad);
    } else {
      output = myOutputSupplier.locateOutput(module);
      if (!output.canRead() || !areDependenciesValid(output, tryLoad)) {
        if (!tryLoad && !forceNoCompile) {
          loadingError(new ModuleNotFoundError(module));
        }
        return null;
      }
      compile = false;
    }

    if (forceNoCompile && compile)
      return null;

    myLoadingModules.add(module);
    try {
      ModuleLoadingResult result;
      if (compile) {
        result = source.load();
      } else {
        result = output.read();
      }

      if (result == null || result.errorsNumber != 0) {
        GeneralError error = new GeneralError(module.parent.getResolvedName(), result == null ? "cannot load module '" + module.name + "'" : "module '"+ module.name + "' contains " + result.errorsNumber + (result.errorsNumber == 1 ? " error" : " errors"));
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
      return result;
    } catch (EOFException e) {
      loadingError(new GeneralError(module, "Incorrect format: Unexpected EOF"));
    } catch (ModuleDeserialization.DeserializationException e) {
      loadingError(new GeneralError(module, e.toString()));
    } catch (IOException e) {
      loadingError(new GeneralError(module, GeneralError.ioError(e)));
    } finally {
      myLoadingModules.remove(myLoadingModules.size() - 1);
    }

    return null;
  }
}
