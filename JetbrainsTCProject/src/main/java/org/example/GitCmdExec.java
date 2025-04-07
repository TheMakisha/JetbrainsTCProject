package org.example;

import java.io.*;
import java.io.File;
import java.util.List;

public class GitCmdExec {

  public GitExecResult runCommand(File gitDirectory, List<String> command) {
    try {
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.directory(gitDirectory);
      Process p = pb.start();

      int exitCode = p.waitFor();
      String output = readInputStreamToTheEnd(p.getInputStream());
      String error = readInputStreamToTheEnd(p.getErrorStream());

      if (exitCode != 0) {
        throw new GitCommandException(
                "Git command failed: " + String.join(" ", command) + "\nError: " + error
        );
      }

      return new GitExecResult(output, error, exitCode);

    } catch (IOException | InterruptedException e) {
      throw new GitCommandException("Exception running git command: " + String.join(" ", command), e);
    }
  }

  private String readInputStreamToTheEnd(InputStream is) throws IOException {
    StringBuilder output = new StringBuilder();

    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);

    String line;
    while ( (line = br.readLine()) != null) {
      output.append(line + "\n");
    }
    return output.toString();
  }
}
