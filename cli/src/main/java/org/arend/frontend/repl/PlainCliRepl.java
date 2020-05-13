package org.arend.frontend.repl;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;

public class PlainCliRepl extends CommonCliRepl {
  public PlainCliRepl() {
    super();
    prompt = "\u03bb ";
  }

  @Override
  public void printlnOpt(Object anything, boolean toError) {
    (toError ? System.err : System.out).println(anything);
  }

  @Override
  public void eprintln(Object anything) {
    System.err.println(anything);
    System.err.flush();
  }

  @Override
  public void println(Object anything) {
    System.out.println(anything);
  }

  @Override
  public void println() {
    System.out.println();
  }

  @Override
  public void print(Object anything) {
    System.out.print(anything);
    System.out.flush();
  }

  public void runRepl(@NotNull InputStream inputStream) {
    var scanner = new Scanner(inputStream);
    print(prompt());
    while (scanner.hasNext()) {
      if (repl(scanner.nextLine(), scanner::nextLine)) break;
      print(prompt());
    }
  }

  public static void main(String... args) {
    main(Collections.emptyList());
  }

  public static void main(@NotNull Collection<? extends Path> libDirs) {
    var repl = new PlainCliRepl();
    repl.println(ASCII_BANNER);
    repl.println();
    repl.println("Note: you're using the plain REPL.");
    repl.addLibraryDirectories(libDirs);
    repl.initialize();
    repl.runRepl(System.in);
  }
}
