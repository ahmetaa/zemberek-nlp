package zemberek.apps;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public abstract class ConsoleApp {

  public abstract String description();

  public abstract void run() throws Exception;

  public void execute(String... args) {
    JCommander commander = JCommander.newBuilder()
        .addObject(this)
        .build();
    try {
      commander.parse(args);
      run();
    } catch (ParameterException e) {
      System.out.println(e.getMessage());
      System.out.println("Description: ");
      System.out.println(ApplicationRunner.wrap(description(), 80));
      System.out.println();
      commander.usage();
    } catch (Exception e) {
      e.printStackTrace();
      commander.usage();
    }
  }
}
