package org;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class FileDetectorChangesTest {
  private GithubClient githubClient;
  private GitCmdExec gitCmd;
  private FileChangesDetector detector;

  private final String owner = "testOwner";
  private final String repository = "testRepo";
  private final String localRepoPath = "/path/to/repo";
  private final String branchA = "main";
  private final String branchB = "feature-branch";
  private final String commonCommitSha = "abc123def456";

  @BeforeEach
  void setUp() {
    gitCmd = Mockito.mock(GitCmdExec.class);
    githubClient = Mockito.mock(GithubClient.class);
    detector = new FileChangesDetector(githubClient, gitCmd);
  }

  @Test
  void shouldReturnEmptySetOfFileChanges() {
    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("merge-base") && cmd.contains(branchB) && cmd.contains("origin/" + branchA)))).thenReturn(new GitExecResult(commonCommitSha, "", 0));
    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd ->
            cmd.contains("diff") && cmd.contains("--name-only"))
            ))
            .thenReturn(new GitExecResult("", "", 0));

    mockGitHubCommitResponse(commonCommitSha);
    mockGitHubListCommitsResponse(List.of());
    var modifiedFiles = detector.detectFileChanges(owner, repository, localRepoPath, branchA, branchB);

    assertTrue(modifiedFiles.isEmpty());
  }

  @Test
  void shouldReturnSingleModifiedFile() {
    String modifiedFile = "src/main/java/example/App.java";

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("merge-base"))))
            .thenReturn(new GitExecResult(commonCommitSha + "\n", "", 0));

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("diff"))))
            .thenReturn(new GitExecResult(modifiedFile + "\n", "", 0));

    mockGitHubCommitResponse(commonCommitSha);

    GHListCommitObject commitObject = new GHListCommitObject();
    commitObject.sha = "newer123commit";

    mockGitHubListCommitsResponse(List.of(commitObject));

    mockGitHubGetCommitResponse(commitObject.sha, modifiedFile, "modified");

    Set<String> modifiedFiles = detector.detectFileChanges(owner, repository, localRepoPath, branchA, branchB);
    assertEquals(1, modifiedFiles.size());
    assertTrue(modifiedFiles.contains(modifiedFile));
  }

  @Test
  void shouldReturnMultipleModifiedFiles() {
    List<String> localModifiedFiles = Arrays.asList(
            "src/main/java/org/example/App.java",
            "src/main/java/org/example/Config.java",
            "pom.xml"
    );

    Map<String, String> githubModifiedFiles = Map.of(
            "src/main/java/org/example/App.java", "modified",
            "src/main/java/org/example/Utils.java", "modified",
            "pom.xml", "modified"
    );

    Set<String> expectedFiles = new HashSet<>(Arrays.asList(
            "src/main/java/org/example/App.java",
            "pom.xml"
    ));

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("merge-base"))))
            .thenReturn(new GitExecResult(commonCommitSha + "\n", "", 0));

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("diff"))))
            .thenReturn(new GitExecResult(String.join("\n", localModifiedFiles) + "\n", "", 0));

    mockGitHubCommitResponse(commonCommitSha);

    GHListCommitObject commitObject = new GHListCommitObject();
    commitObject.sha = "newer123commit";

    mockGitHubListCommitsResponse(List.of(commitObject));

    mockGitHubGetCommitResponse(commitObject.sha, githubModifiedFiles);

    Set<String> modifiedFiles = detector.detectFileChanges(owner, repository, localRepoPath, branchA, branchB);

    assertEquals(expectedFiles.size(), modifiedFiles.size());
    assertEquals(expectedFiles, modifiedFiles);
  }

  @Test
  void shouldReturnMultipleModifiedFilesWithMultipleCommits() {
    String file1 = "src/main/java/example/App.java";
    String file2 = "pom.xml";
    String file3 = "README.md";

    List<String> localModifiedFiles = Arrays.asList(file1, file2, file3);

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("merge-base"))))
            .thenReturn(new GitExecResult(commonCommitSha + "\n", "", 0));

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("diff"))))
            .thenReturn(new GitExecResult(String.join("\n", localModifiedFiles) + "\n", "", 0));

    GHListCommitObject commit1 = new GHListCommitObject();
    commit1.sha = "commit1sha";

    GHListCommitObject commit2 = new GHListCommitObject();
    commit2.sha = "commit2sha";

    mockGitHubCommitResponse(commonCommitSha);
    mockGitHubListCommitsResponse(Arrays.asList(commit1, commit2));

    mockGitHubGetCommitResponse(commit1.sha, Map.of(file1, "modified"));

    mockGitHubGetCommitResponse(commit2.sha, Map.of(file3, "modified"));

    Set<String> modifiedFiles = detector.detectFileChanges(owner, repository, localRepoPath, branchA, branchB);

    assertEquals(2, modifiedFiles.size());
    assertTrue(modifiedFiles.contains(file1));
    assertTrue(modifiedFiles.contains(file3));
    assertFalse(modifiedFiles.contains(file2));
  }

  @Test
  void shouldDetectModifiedFilesOnlyAndIgnoreAddedAndDeletedFiles() {
    String modifiedFile = "src/main/java/example/App.java";
    String addedFile = "src/main/java/example/NewFile.java";
    String removedFile = "src/main/java/example/OldFile.java";

    List<String> localModifiedFiles = List.of(modifiedFile);

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("merge-base"))))
            .thenReturn(new GitExecResult(commonCommitSha + "\n", "", 0));

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("diff"))))
            .thenReturn(new GitExecResult(String.join("\n", localModifiedFiles) + "\n", "", 0));

    GHListCommitObject commit = new GHListCommitObject();
    commit.sha = "commitsha";

    mockGitHubCommitResponse(commonCommitSha);
    mockGitHubListCommitsResponse(List.of(commit));

    Map<String, String> githubFiles = Map.of(
            modifiedFile, "modified",
            addedFile, "added",
            removedFile, "removed"
    );

    mockGitHubGetCommitResponse(commit.sha, githubFiles);

    Set<String> modifiedFiles = detector.detectFileChanges(owner, repository, localRepoPath, branchA, branchB);

    assertEquals(1, modifiedFiles.size());
    assertTrue(modifiedFiles.contains(modifiedFile));
    assertFalse(modifiedFiles.contains(addedFile));
    assertFalse(modifiedFiles.contains(removedFile));
  }

  @Test
  void shouldThrowGitCommandException() {
    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("merge-base"))))
            .thenThrow(new GitCommandException("Command failed"));

    assertThrows(GitCommandException.class, () ->
            detector.detectFileChanges(owner, repository, localRepoPath, branchA, branchB));
  }

  @Test
  void shouldThrowGitApiException() {
    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("merge-base"))))
            .thenReturn(new GitExecResult(commonCommitSha + "\n", "", 0));

    when(gitCmd.runCommand(any(java.io.File.class), argThat(cmd -> cmd.contains("diff"))))
            .thenReturn(new GitExecResult("file.txt\n", "", 0));

    when(githubClient.send(any(GHRequest.class), any(TypeReference.class)))
            .thenThrow(new GHApiException("API call failed"));

    assertThrows(GHApiException.class, () ->
            detector.detectFileChanges(owner, repository, localRepoPath, branchA, branchB));
  }

  void mockGitHubCommitResponse(String commitSHA) {
    GHGetCommitResponse commitResponse = new GHGetCommitResponse();
    commitResponse.sha = commitSHA;
    commitResponse.commit = new Commit();
    commitResponse.commit.author = new User();
    commitResponse.commit.author.date = "2023-01-01T12:00:00Z";

    GHPage<GHGetCommitResponse> page = (GHPage<GHGetCommitResponse>)mock(GHPage.class);
    when(page.getData()).thenReturn(commitResponse);
    when(page.hasNext()).thenReturn(false);

    var response = new GHResponse<>(page);
    doReturn(response).when(githubClient)
            .send(argThat(req -> req.toString().contains("/commits/" + commitSHA)), any(TypeReference.class));
  }

  private void mockGitHubListCommitsResponse(List<GHListCommitObject> commits) {
    GHPage<List<GHListCommitObject>> page = (GHPage<List<GHListCommitObject>>) mock(GHPage.class);
    when(page.getData()).thenReturn(commits);
    when(page.hasNext()).thenReturn(false);

    GHResponse<List<GHListCommitObject>> response = new GHResponse<>(page);
    doReturn(response).when(githubClient)
            .send(argThat(req -> req.toString().contains("/commits?")), any(TypeReference.class));
  }

  private void mockGitHubGetCommitResponse(String commitSha, String fileName, String status) {
    mockGitHubGetCommitResponse(commitSha, Map.of(fileName, status));
  }

  private void mockGitHubGetCommitResponse(String commitSha, Map<String, String> fileStatuses) {
    GHGetCommitResponse commitData = new GHGetCommitResponse();
    commitData.sha = commitSha;

    List<File> files = new ArrayList<>();
    for (Map.Entry<String, String> entry : fileStatuses.entrySet()) {
      File file = new File();
      file.fileName = entry.getKey();
      file.status = entry.getValue();
      files.add(file);
    }

    commitData.files = files;

    GHPage<GHGetCommitResponse> page = mock(GHPage.class);
    when(page.getData()).thenReturn(commitData);
    when(page.hasNext()).thenReturn(false);

    GHResponse<GHGetCommitResponse> response = new GHResponse<>(page);
    doReturn(response).when(githubClient).send(argThat(req -> req.toString().contains("/commits/" + commitSha)),
            any(TypeReference.class));
  }
}
