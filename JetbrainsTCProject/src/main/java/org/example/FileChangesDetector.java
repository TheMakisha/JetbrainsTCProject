package org.example;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.util.*;

public class FileChangesDetector {

  private final GitCmdExec gitCmd;
  private final GithubClient githubClient;

  public FileChangesDetector(GithubClient githubClient, GitCmdExec gitCmd) {
    this.githubClient = githubClient;
    this.gitCmd = gitCmd;
  }

  public Set<String> detectFileChanges(String owner, String repository, String localRepoPath, String branchA, String branchB) {
    var independentlyModifiedFiles = new HashSet<String>();

    List<String> command = new LinkedList<>();
    command.add("git");
    command.add("merge-base");
    command.add(branchB);
    command.add("origin/" + branchA);

    File localRepo = new File(localRepoPath);

    var result = gitCmd.runCommand(localRepo, command);
    String commitSHA = result.output().trim();
    System.out.println(commitSHA);
    command.clear();
    command.add("git");
    command.add("diff");
    command.add("--name-only");
    command.add("--diff-filter=M");
    command.add(commitSHA);
    command.add(branchB);

    result = gitCmd.runCommand(localRepo, command);
    var localFileNames = new HashSet<String>(Arrays.asList(result.output().split("\n")));

    var request = GHRequestBuilder.getCommit(owner, repository, commitSHA).build();
    var commitResponse = githubClient.send(request, new TypeReference<GHGetCommitResponse>() {});
    System.out.println(commitResponse.response.getData().commit);
    var commitDate = commitResponse.response.getData().commit.author.date;

    request = GHRequestBuilder.listCommits(owner, repository)
            .sha(commitSHA)
            .since(commitDate)
            .build();

    var listCommitsResponse = githubClient.send(request, new TypeReference<List<GHListCommitObject>>(){}).response;
    GHPage<GHGetCommitResponse> commitInfoResponse;

    while (true) {
      var listCommits = listCommitsResponse.getData();
      for (var commit: listCommits) {
        var r = GHRequestBuilder.getCommit(owner, repository, commit.sha).build();
        commitInfoResponse = githubClient.send(r, new TypeReference<GHGetCommitResponse>() {}).response;
        while (true) {
           var commitInfo = commitInfoResponse.getData();
              if (commitInfo.files != null) {
                for (var file: commitInfo.files) {
                  if(file.status == "modified" && localFileNames.contains(file.fileName)) {
                independentlyModifiedFiles.add(file.fileName);
              }
            }
          }
          if (commitInfoResponse.hasNext()) {
            commitInfoResponse.nextPage();
          }
          else {
            break;
          }
        }
      }
      if (listCommitsResponse.hasNext()) {
        listCommitsResponse.nextPage();
      }
      else {
        break;
      }
    }
    return independentlyModifiedFiles;
  }
}

