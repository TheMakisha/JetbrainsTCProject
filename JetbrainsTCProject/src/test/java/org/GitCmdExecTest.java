package org;

import org.example.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GitCmdExecTest {

  @TempDir
  File tempDir;
  private GitCmdExec gitCmdExec;

  @BeforeEach
  void setUp() {
    gitCmdExec = new GitCmdExec();
  }

  @Test
  void runCommandShouldSuccessfullyExecute() throws IOException {
    List<String> command = Arrays.asList("git", "status");
    String expectedOutput = "On branch main\nYour branch is up to date with 'origin/main'.\n";
    String expectedError = "";
    int expectedExitCode = 0;

    try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class, (mock, context) -> {
      Process processMock = mock(Process.class);
      when(mock.directory(any(File.class))).thenReturn(mock);
      when(mock.start()).thenReturn(processMock);
      when(processMock.waitFor()).thenReturn(expectedExitCode);
      when(processMock.getInputStream()).thenReturn(
              new ByteArrayInputStream(expectedOutput.getBytes(StandardCharsets.UTF_8)));
      when(processMock.getErrorStream()).thenReturn(
              new ByteArrayInputStream(expectedError.getBytes(StandardCharsets.UTF_8)));
    })) {

      GitExecResult result = gitCmdExec.runCommand(tempDir, command);

      assertEquals(expectedOutput, result.output());
      assertEquals(expectedError, result.error());
      assertEquals(expectedExitCode, result.exitCode());

      assertEquals(1, mocked.constructed().size());
      verify(mocked.constructed().get(0)).directory(tempDir);
      verify(mocked.constructed().get(0)).start();
    }
  }

  @Test
  void runCommandShouldReturnNonZeroExitCode() throws IOException, InterruptedException {
    List<String> command = Arrays.asList("git", "checkout", "non-existent-branch");
    String expectedOutput = "";
    String expectedError = "error: pathspec 'non-existent-branch' did not match any file(s) known to git";
    int expectedExitCode = 1;

    try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class, (mock, context) -> {
      Process processMock = mock(Process.class);
      when(mock.directory(any(File.class))).thenReturn(mock);
      when(mock.start()).thenReturn(processMock);
      when(processMock.waitFor()).thenReturn(expectedExitCode);
      when(processMock.getInputStream()).thenReturn(
              new ByteArrayInputStream(expectedOutput.getBytes(StandardCharsets.UTF_8)));
      when(processMock.getErrorStream()).thenReturn(
              new ByteArrayInputStream(expectedError.getBytes(StandardCharsets.UTF_8)));
    })) {
      GitCommandException exception = assertThrows(GitCommandException.class, () -> {
        gitCmdExec.runCommand(tempDir, command);
      });

      assertTrue(exception.getMessage().contains(expectedError));

      assertEquals(1, mocked.constructed().size());
      verify(mocked.constructed().get(0)).directory(tempDir);
      verify(mocked.constructed().get(0)).start();
    }
  }

  @Test
  void runCommandShouldThrowIoException() throws IOException {
    List<String> command = Arrays.asList("git", "status");
    IOException ioException = new IOException("IO error");

    try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class, (mock, context) -> {
      when(mock.directory(any(File.class))).thenReturn(mock);
      when(mock.start()).thenThrow(ioException);
    })) {
      GitCommandException exception = assertThrows(GitCommandException.class, () -> {
        gitCmdExec.runCommand(tempDir, command);
      });

      assertEquals(ioException, exception.getCause());

      assertEquals(1, mocked.constructed().size());
      verify(mocked.constructed().get(0)).directory(tempDir);
      verify(mocked.constructed().get(0)).start();
    }
  }

  @Test
  void runCommandShouldThrowInterruptedException() throws IOException, InterruptedException {
    List<String> command = Arrays.asList("git", "status");
    InterruptedException interruptedException = new InterruptedException("Interrupted");

    try (MockedConstruction<ProcessBuilder> mocked = mockConstruction(ProcessBuilder.class, (mock, context) -> {
      Process processMock = mock(Process.class);
      when(mock.directory(any(File.class))).thenReturn(mock);
      when(mock.start()).thenReturn(processMock);
      when(processMock.waitFor()).thenThrow(interruptedException);
    })) {
      GitCommandException exception = assertThrows(GitCommandException.class, () -> {
        gitCmdExec.runCommand(tempDir, command);
      });

      assertEquals(interruptedException, exception.getCause());

      assertEquals(1, mocked.constructed().size());
      verify(mocked.constructed().get(0)).directory(tempDir);
      verify(mocked.constructed().get(0)).start();
    }
  }

  @Test
  void runCommandShouldRunRealProcess() {
    if (!new File("/bin/echo").exists() && !new File("C:\\Windows\\System32\\cmd.exe").exists()) {
      return;
    }

    List<String> command;
    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      command = Arrays.asList("cmd.exe", "/c", "echo", "test");
    } else {
      command = Arrays.asList("/bin/echo", "test");
    }

    GitExecResult result = gitCmdExec.runCommand(tempDir, command);

    assertEquals(0, result.exitCode());
    assertEquals("test", result.output().trim());
    assertTrue(result.error().isEmpty());
  }

  @Test
  void readInputStreamToTheEndShouldHandleEmptyStream() throws Exception {
    java.lang.reflect.Method method = GitCmdExec.class.getDeclaredMethod("readInputStreamToTheEnd", InputStream.class);
    method.setAccessible(true);

    InputStream emptyStream = new ByteArrayInputStream("".getBytes());
    String result = (String) method.invoke(gitCmdExec, emptyStream);

    assertEquals("", result);
  }

  @Test
  void readInputStreamToTheEndShouldHandlesMultilineInput() throws Exception {
    java.lang.reflect.Method method = GitCmdExec.class.getDeclaredMethod("readInputStreamToTheEnd", InputStream.class);
    method.setAccessible(true);

    String input = "line1\nline2\nline3";
    InputStream stream = new ByteArrayInputStream(input.getBytes());
    String result = (String) method.invoke(gitCmdExec, stream);

    assertEquals("line1\nline2\nline3\n", result);
  }
}