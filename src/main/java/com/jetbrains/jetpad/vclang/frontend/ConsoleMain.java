package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.storage.FileStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.CompositeSourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.CompositeStorage;
import com.jetbrains.jetpad.vclang.term.Abstract;
import org.apache.commons.cli.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ConsoleMain extends BaseCliFrontend<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> {
  private static final Options cmdOptions = new Options();
  static {
    cmdOptions.addOption("h", "help", false, "print this message");
    cmdOptions.addOption(Option.builder("s").longOpt("source").hasArg().argName("dir").desc("source directory").build());
    //cmdOptions.addOption(Option.builder("o").longOpt("output").hasArg().argName("dir").desc("output directory").build());
    //cmdOptions.addOption(Option.builder("L").hasArg().argName("dir").desc("add <dir> to the list of directories searched for libraries").build());
    cmdOptions.addOption(Option.builder().longOpt("recompile").desc("recompile files").build());
  }

  private final PreludeStorage preludeStorage;
  private final FileStorage fileStorage;
  private final CompositeStorage<PreludeStorage.SourceId, FileStorage.SourceId> compositeStorage;

  public ConsoleMain(Path sourceDir, boolean recompile) {
    this(new PreludeStorage(), new FileStorage(sourceDir), recompile);
  }

  private ConsoleMain(PreludeStorage preludeStorage, FileStorage fileStorage, boolean recompile) {
    this(preludeStorage, fileStorage, new CompositeStorage<>(preludeStorage, fileStorage, preludeStorage, fileStorage), recompile);
  }

  private ConsoleMain(PreludeStorage preludeStorage, FileStorage fileStorage, CompositeStorage<PreludeStorage.SourceId, FileStorage.SourceId> storage, boolean recompile) {
    super(storage, recompile);
    this.preludeStorage = preludeStorage;
    this.fileStorage = fileStorage;
    this.compositeStorage = storage;
  }

  @Override
  protected String displaySource(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId source, boolean modulePathOnly) {
    StringBuilder builder = new StringBuilder();
    builder.append(source.getModulePath());
    if (!modulePathOnly && source.source2 != null) {
      builder.append(" (").append(source.source2).append(")");
    }
    return builder.toString();
  }

  @Override
  protected PersistenceProvider<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> createPersistenceProvider() {
    return new MyPersistenceProvider();
  }

  class MyPersistenceProvider implements PersistenceProvider<CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId> {
    @Override
    public URL getUrl(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId sourceId) {
      try {
        final String root;
        final Path relPath;
        final String query;
        if (sourceId.source1 != null) {
          root = "prelude";
          relPath = Paths.get("");
          query = null;
        } else {
          root = "";
          relPath = sourceId.source2.getRelativeFilePath();
          query = "" + sourceId.source2.getLastModified();
        }
        return new URI("file", root, Paths.get("/").resolve(relPath).toUri().getPath(), query, null).toURL();
      } catch (URISyntaxException | MalformedURLException e) {
        throw new IllegalStateException();
      }
    }

    @Override
    public CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId getModuleId(URL sourceUrl) {
      if (sourceUrl.getAuthority() != null && sourceUrl.getAuthority().equals("prelude")) {
        if (sourceUrl.getPath().equals("/")) {
          return compositeStorage.idFromFirst(preludeStorage.preludeSourceId);
        } else {
          return null;
        }
      } else if (sourceUrl.getAuthority() == null) {
        try {
          Path path = Paths.get(new URI(sourceUrl.getProtocol(), null, sourceUrl.getPath(), null));
          ModulePath modulePath = FileStorage.modulePath(path.getRoot().relativize(path));
          if (modulePath == null) return null;

          final FileStorage.SourceId fileSourceId;
          if (sourceUrl.getQuery() == null) {
            fileSourceId = fileStorage.locateModule(modulePath);
          } else {
            long mtime = Long.parseLong(sourceUrl.getQuery());
            fileSourceId = fileStorage.locateModule(modulePath, mtime);
          }
          return fileSourceId != null ? compositeStorage.idFromSecond(fileSourceId) : null;
        } catch (URISyntaxException | NumberFormatException e) {
          return null;
        }
      } else {
        return null;
      }
    }

    @Override
    public String getIdFor(Abstract.Definition definition) {
      if (definition instanceof Concrete.Definition) {
        Concrete.Position pos = ((Concrete.Definition) definition).getPosition();
        if (pos != null) {
          return pos.line + ";" + pos.column;
        }
      }
      return null;
    }

    @Override
    public Abstract.Definition getFromId(CompositeSourceSupplier<PreludeStorage.SourceId, FileStorage.SourceId>.SourceId sourceId, String id) {
      Map<String, Abstract.Definition> sourceMap = definitionIds.get(sourceId);
      if (sourceMap == null) {
        return null;
      } else {
        return sourceMap.get(id);
      }
    }
  }


  public static void main(String[] args) {
    try {
      CommandLine cmdLine = new DefaultParser().parse(cmdOptions, args);

      if (cmdLine.hasOption("h")) {
        printHelp();
      } else {
        String sourceDirStr = cmdLine.getOptionValue("s");
        Path sourceDir = Paths.get(sourceDirStr == null ? System.getProperty("user.dir") : sourceDirStr);

        boolean recompile = cmdLine.hasOption("recompile");

        new ConsoleMain(sourceDir, recompile).run(sourceDir, cmdLine.getArgList());
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
    }
  }

  private static void printHelp() {
    new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
  }
}
