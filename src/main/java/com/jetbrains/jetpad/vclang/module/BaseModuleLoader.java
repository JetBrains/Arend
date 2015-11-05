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

  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;

  private final DeserializingLoader myDeserializingLoader;
  private final boolean myRecompile;

  public BaseModuleLoader(boolean recompile) {
    mySourceSupplier = DummySourceSupplier.getInstance();
    myOutputSupplier = DummyOutputSupplier.getInstance();
    myRecompile = recompile;
    myDeserializingLoader = new DeserializingLoader(myLoadingModules, this, mySourceSupplier, myOutputSupplier);
  }

  public void setSourceSupplier(SourceSupplier sourceSupplier) {
    mySourceSupplier = sourceSupplier;
    myDeserializingLoader.setSourceSupplier(sourceSupplier);
  }

  public void setOutputSupplier(OutputSupplier outputSupplier) {
    myOutputSupplier = outputSupplier;
    myDeserializingLoader.setOutputSupplier(outputSupplier);
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
      if (!myRecompile) {
        ModuleLoadingResult result = myDeserializingLoader.load(module);
        if (result != null)
          return result;
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

        ModuleLoadingResult result = source.load(false);
        Helper.processLoaded(this, module, result);

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
