package org.arend.frontend;

public class ConsoleMain extends BaseCliFrontend {
  public static void main(String[] args) {
    var main = new ConsoleMain();
    if (main.run(args) == null || main.isExitWithError())
      System.exit(1);
  }
}
