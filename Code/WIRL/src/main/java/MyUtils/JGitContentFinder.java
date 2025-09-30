package MyUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class JGitContentFinder {
    public static RevCommit getLatestCommit(String repoPath) throws IOException {
        Repository repo = openRepository(repoPath);
        try (Git git = new Git(repo)) {
            return git
                    .log()
                    .setMaxCount(1)
                    .call()
                    .iterator()
                    .next();
        } catch (NoHeadException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
    public static RevCommit findContentFirstCommit(String repoPath, String filePath, String targetContent) {

        try (Repository repo = openRepository(repoPath)) {
            Iterable<RevCommit> commits = getFileHistory(repo, filePath);
            List<RevCommit> commitList = new ArrayList<>();
            for (RevCommit commit : commits) {
                commitList.add(commit);
            }
            Collections.reverse(commitList); // reversed the order from old to new

            int mark = 0;
            for (RevCommit commit : commitList) {
                if (fileExistsInCommit(repo, commit, filePath)) {
                    mark = 1;
                    String content = getFileContent(repo, commit, filePath);
                    if (content.contains(targetContent)) {
                        System.out.println("Target Content first committed in : " + commit.getName());
                        return commit;
                    }
                }
            }
            if(mark == 1){
                System.out.println("Target content not found, but file exists");
            }
            else {
                System.out.println("Target content not found, file not exist");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static Repository openRepository(String repoPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(new File(repoPath + "/.git"))
                .readEnvironment()
                .findGitDir()
                .build();
    }

    private static Iterable<RevCommit> getFileHistory(Repository repo, String filePath)
            throws GitAPIException {
        try (Git git = new Git(repo)) {
            LogCommand log = git.log();
            return log
                    .addPath(filePath)
                    .call();
        }
    }

    private static boolean fileExistsInCommit(Repository repo, RevCommit commit, String filePath)
            throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(repo, filePath, commit.getTree())) {
            return treeWalk != null;
        }
    }

    private static String getFileContent(Repository repo, RevCommit commit, String filePath)
            throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(repo, filePath, commit.getTree())) {
            if (treeWalk == null) return "";
            ObjectId blobId = treeWalk.getObjectId(0);
            byte[] bytes = repo.open(blobId).getBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}