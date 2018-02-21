package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.util.FileUtils;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class ConsoleMain extends BaseCliFrontend {
  public ConsoleMain(Path libDir, Path sourceDir, Path outDir) {
  }

  @Override
  protected String displaySource(ModulePath module, boolean modulePathOnly) {
    StringBuilder builder = new StringBuilder();
    builder.append(module);
    if (!modulePathOnly) {
      if (source.source1 != null) {
        builder.append(" (").append(source.source1).append(")");
      } else if (source.source2 != null && source.source2.source1 != null) {
        builder.append(" (").append(source.source2.source1).append(")");
      }
    }
    return builder.toString();
  }

  private static CommandLine parseArgs(String[] args) {
    try {
      Options cmdOptions = new Options();
      cmdOptions.addOption("h", "help", false, "print this message");
      cmdOptions.addOption(Option.builder("L").longOpt("libs").hasArg().argName("libdir").desc("directory containing libraries").build());
      cmdOptions.addOption(Option.builder("s").longOpt("source").hasArg().argName("srcdir").desc("project source directory").build());
      cmdOptions.addOption(Option.builder("o").longOpt("output").hasArg().argName("outdir").desc("directory for project-specific binary files (relative to srcdir)").build());
      cmdOptions.addOption(Option.builder().longOpt("recompile").desc("recompile files").build());
      CommandLine cmdLine = new DefaultParser().parse(cmdOptions, args);

      if (cmdLine.hasOption("h")) {
        new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
        return null;
      } else {
        return cmdLine;
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      return null;
    }
  }

  public static void main(String[] args) throws IOException {
    CommandLine cmdLine = parseArgs(args);
    if (cmdLine == null) {
      return;
    }

    String libDirStr = cmdLine.getOptionValue("L");
    Path libDir = libDirStr != null ? Paths.get(libDirStr) : null;

    String sourceDirStr = cmdLine.getOptionValue("s");
    Path sourceDir = Paths.get(sourceDirStr == null ? System.getProperty("user.dir") : sourceDirStr);

    String cacheDirStr = cmdLine.getOptionValue("o");
    Path outDir = sourceDir.resolve(cacheDirStr != null ? cacheDirStr : ".output");

    if (cmdLine.hasOption("recompile")) { // TODO[cache]
      deleteOutput(outDir);
    }

    new ConsoleMain(libDir, sourceDir, outDir).run(sourceDir, cmdLine.getArgList());
  }

  private static void deleteOutput(Path cacheDir) throws IOException {
    if (!Files.exists(cacheDir)) {
      return;
    }

    Files.walkFileTree(cacheDir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (file.getFileName().toString().endsWith(FileUtils.SERIALIZED_EXTENSION)) {
          try {
            Files.delete(file);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
