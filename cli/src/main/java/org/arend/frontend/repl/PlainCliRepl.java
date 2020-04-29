package org.arend.frontend.repl;

import org.arend.prelude.GeneratedVersion;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Scanner;

public class PlainCliRepl extends CommmonCliRepl {
  public PlainCliRepl() {
    super();
  }

  public void runRepl(@NotNull InputStream inputStream) {
    var scanner = new Scanner(inputStream);
    print(prompt());
    while (scanner.hasNext()) {
      if (repl(scanner::nextLine, scanner.nextLine())) break;
      print(prompt());
    }
  }

  public static void main(String... args) {
    var repl = new PlainCliRepl();
    repl.println(APP_NAME + " (plain) " + GeneratedVersion.VERSION_STRING + ": https://arend-lang.github.io   :? for help");
    repl.initialize();
    repl.runRepl(System.in);
  }
}
