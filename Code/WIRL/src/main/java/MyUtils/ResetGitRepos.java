package MyUtils;

import edu.lu.uni.serval.git.exception.GitRepositoryNotFoundException;
import edu.lu.uni.serval.git.exception.NotValidGitRepositoryException;
import edu.lu.uni.serval.git.travel.GitRepository;
import edu.lu.uni.serval.utils.FileHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResetGitRepos {
    public static void main(String [] args) throws GitAPIException, IOException, NotValidGitRepositoryException, GitRepositoryNotFoundException {
//        String gitRoot = javaBasePath + eachProjectName + "/.git";
//        StringBuilder result = new StringBuilder();
//        try{
//            GitRepository gitRepository = ResetGitRepos.openRepository(javaBasePath,eachProjectName,gitRoot);
//            List<RevCommit> commits = gitRepository.getAllCommits(false);
////            System.out.println(commits.size());
//            ArrayList<String> commitIDs = new ArrayList<>();
//            for(RevCommit commit:commits){
//                commitIDs.add(commit.getName());
//            }
//            ResetGitRepos.removeGitLock(eachProjectName, javaBasePath);
//            ResetGitRepos.rollReposToLatestCommit(gitRoot, commits.get(0).getName(), commitIDs, commits);
//        }
//        catch (Exception e){
//            continue;
//        }
    }
    public static void removeGitLock(String gitRoot) {
    /*
        check if index file is locked, if it is, delete index.lock
    */
        String lockFile = gitRoot + "/" + "index.lock";
        if (FileHelper.isValidPath(lockFile)) {
            FileHelper.deleteFile(lockFile);
            System.out.println("delete lockFile:" + lockFile);
        }
    }

    public static GitRepository openRepository(String basePath,String projectName,String gitRoot) throws NotValidGitRepositoryException, GitRepositoryNotFoundException, IOException {
        String revisedPath = basePath +"\\" + projectName + File.separator + "Rev";
        String previousPath = basePath +"\\" + projectName + File.separator + "Pre";
        FileHelper.createDirectory(revisedPath);
        FileHelper.createDirectory(previousPath);
        GitRepository gitRepository = new GitRepository(gitRoot,revisedPath,previousPath);
        gitRepository.open();
        return gitRepository;
    }
    public static void rollReposToLatestCommit(String gitRoot, String commitID,List<String> commitIDs, List<RevCommit>commits) throws NotValidGitRepositoryException, GitRepositoryNotFoundException, IOException, GitAPIException {

        Boolean isRollBack;
        int index = commitIDs.indexOf(commitID);
        System.out.println("roll back to the "+index+"-th commit");
        if(index!=-1){
            RevCommit commit = commits.get(index);
//            System.out.println("commit.getId():"+commit.getId());
            isRollBack = rollBackPreRevision(gitRoot, commit.getId());
            System.out.println(isRollBack);
        }
    }

    public static void rollReposToSpecificCommit(String gitRoot, String commitID,List<String> commitIDs, List<RevCommit>commits) throws NotValidGitRepositoryException, GitRepositoryNotFoundException, IOException, GitAPIException {

        Boolean isRollBack;
        int index = commitIDs.indexOf(commitID);
//        System.out.println("roll back to the "+index+"-th commit");
        if(index!=-1){
            RevCommit commit = commits.get(index);
//            System.out.println("commit.getId():"+commit.getId());
            isRollBack = rollBackPreRevision(gitRoot, commit.getId());
//            System.out.println(isRollBack);
        }
    }
    public static void rollReposToSpecificBeforeCommit(String gitRoot, String commitID,List<String> commitIDs, List<RevCommit>commits) throws NotValidGitRepositoryException, GitRepositoryNotFoundException, IOException, GitAPIException {

        Boolean isRollBack;
        int index = commitIDs.indexOf(commitID);
        System.out.println("roll back to the "+index+"-th commit");
        if(index!=-1){
            RevCommit commit = commits.get(index);
            System.out.println("commit.getId():"+commit.getId());
            RevCommit beforeCommit = commit.getParent(0);
            System.out.println("beforeCommit.getId():"+beforeCommit.getId());
            isRollBack = rollBackPreRevision(gitRoot, beforeCommit.getId());
            System.out.println(isRollBack);
        }
    }

    public static String rollReposToSpecificCommitAndGetID(String gitRoot, String commitID,List<String> commitIDs, List<RevCommit>commits) throws NotValidGitRepositoryException, GitRepositoryNotFoundException, IOException, GitAPIException {

        Boolean isRollBack;
        int index = commitIDs.indexOf(commitID);
        RevCommit parentCommit = null;
        System.out.println(index);
        if(index!=-1){
            RevCommit commit = commits.get(index);
            System.out.println("commit.getId():"+commit.getId());
            parentCommit = commit.getParent(0);
            System.out.println("parent commit.getId():"+parentCommit.getId());
            isRollBack = rollBackPreRevision(gitRoot, parentCommit.getId());
            System.out.println(isRollBack);
        }
        if(parentCommit!=null)
            return parentCommit.getName();
        else
            return "";

    }


    public static boolean rollBackPreRevision(String gitRoot, ObjectId revision) throws IOException, GitAPIException {
        try{
            removeGitLock(gitRoot);
            Git git = Git.open(new File(gitRoot));

            Repository repository = git.getRepository();

            RevWalk walk = new RevWalk(repository);
            RevCommit revCommit = walk.parseCommit(revision);
//        String preVision = revCommit.getParent(0).getName();
            String thisVision = revCommit.getName();
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef(thisVision).call();
            repository.close();
            return true;
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }

    }

}
