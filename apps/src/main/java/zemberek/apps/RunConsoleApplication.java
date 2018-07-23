package zemberek.apps;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public class RunConsoleApplication {

  public static void main(String[] args) {
    ServiceLoader<ConsoleApp> loader = ServiceLoader.load(ConsoleApp.class);
    List<ConsoleApp> apps = Lists.newArrayList(loader);
    if (apps.size() == 0) {
      System.out.println("No applications found.");
      System.exit(0);
    }
    if (args.length == 0) {
      listApplications(apps);
      System.exit(0);
    }
    String className = args[0];
    for (ConsoleApp app : apps) {
      if (app.getClass().getSimpleName().contains(className)) {
        app.execute(Arrays.copyOfRange(args, 1, args.length));
        System.exit(0);
      }
    }
    System.out.println("Cannot find application for :" + className);
    listApplications(apps);
  }

  private static void listApplications(List<ConsoleApp> apps) {
    System.out.println("List of available applications:");
    System.out.println("-------------------------------");
    for (ConsoleApp app : apps) {
      System.out.println(app.getClass().getSimpleName());
      System.out.println();
      String wrapped = wrap(app.description(), 80);
      System.out.println(wrapped);
      System.out.println();
    }
    System.exit(0);
  }

  static String wrap(String s, int lineLength) {
    if (s == null) {
      return "";
    }
    List<String> lines = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    for (String token : Splitter.on(" ").splitToList(s)) {
      sb.append(token).append(" ");
      if (sb.length() >= lineLength) {
        lines.add(sb.toString().trim());
        sb = new StringBuilder();
      }
    }
    if(sb.length()>0) {
      lines.add(sb.toString().trim());
    }
    return String.join("\n", lines);
  }
}