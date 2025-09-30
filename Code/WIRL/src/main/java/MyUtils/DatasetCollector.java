package MyUtils;
import edu.lu.uni.serval.utils.FileHelper;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class DatasetCollector {
    public static void main(String [] args){
        try {
            String javaRepoPath = "The path to the java repository";
            String targetPath = "The output path";
            String urlPath = "Original dataset from Zhang et al.";
            List<Map<String, Set<String>>> reposInfoList = new ArrayList<>();
            List<String> repoNames = new ArrayList<>();
            identifyTopProjects(urlPath, repoNames, reposInfoList);
//            FileOutputStream fos = new FileOutputStream(new File("E:/CodeAdaptation/Dataset/AllRepoNames.txt"));
//            PrintStream ps = new PrintStream(fos);
//            System.setOut(ps);
            int finishedRepoNum = 1;
            int repoCnt = 1;
            List<Map<String, Set<String>>> reposInfoListTop10 = reposInfoList.subList(finishedRepoNum, reposInfoList.size());
//            List<Map<String, Set<String>>> reposInfoListTop10 = reposInfoList;
            for (int i=0;i<reposInfoListTop10.size();i++) {
                Map<String, Set<String>> repo = reposInfoListTop10.get(i);
//                System.out.println(userAndRepoName);
//                System.out.println(repo);
                String userNameAndRepo = repoNames.get(i+finishedRepoNum);
                String repoName = userNameAndRepo.split("/")[1];
                String repoPath = javaRepoPath + repoName;
                Map<RevCommit, Set<String>> repoInfo = new HashMap<>();
                List<RevCommit> commitList = new ArrayList<>();
                for (Map.Entry<String, Set<String>> entry : repo.entrySet()) {
                    String filePath = entry.getKey();
                    Set<String> soUrls = entry.getValue();
                    System.out.println("File: " + filePath);
                    if(!filePath.contains(userNameAndRepo)){
                        continue;
                    }
                    String [] split = filePath.split(userNameAndRepo + "/blob/");
                    if(split.length != 2){
                        continue;
                    }
                    String localFilePath = split[1];
                    localFilePath = localFilePath.substring(localFilePath.indexOf("/") + 1);
                    for(String soUrl : soUrls){
                        System.out.println("SO URL: " + soUrl);
//                        String firstCommit = GitContentFinder.findFirstCommit(repoPath + "/" + localFilePath,soUrl);
//                        RevCommit firstCommit = JGitContentFinder.findContentFirstCommit (repoPath, repoPath + "/" + localFilePath,soUrl);
                        String urlNumber = soUrl.substring(soUrl.lastIndexOf("/") + 1);
                        RevCommit firstCommit = JGitContentFinder.findContentFirstCommit (repoPath, localFilePath, urlNumber);

                        if (firstCommit != null) {
                            if(repoInfo.containsKey(firstCommit)){
                                Set<String> tempList = repoInfo.get(firstCommit);
                                tempList.add(urlNumber +"#" + localFilePath);
                                repoInfo.put(firstCommit, tempList);
                            }
                            else{
                                Set<String> tempList = new HashSet<>();
                                tempList.add(urlNumber +"#" + localFilePath);
                                repoInfo.put(firstCommit, tempList);
                            }
                            commitList.add(firstCommit);
                        }
                        else {
                            System.out.println("not found the first commit");
                        }
                    }
                }
                HashSet<String> deduplicateSet = new HashSet<>();
                int pairNum = 0;
//                int commitNum=0;
                String firstDirName = (finishedRepoNum+ repoCnt) + "-" + repoName;
                if(!commitList.isEmpty()) repoCnt++;
                if(FileHelper.isValidPath(targetPath + firstDirName)){
                    continue;
                }
                for(RevCommit commit : commitList){
                    boolean result = ResetGitRepos.rollBackPreRevision(repoPath + "/.git", commit.getId());
                    if(!result) continue;
                    Set<String> fileAndSOUrl = repoInfo.get(commit);
                    for(String s: fileAndSOUrl){
                        String []entry1 = s.split("#");
                        String soUrl = entry1[0];
                        String filePath = entry1[1];
                        String dedepulicateKey = commit + "-" + soUrl + "-" + filePath;
                        if(!deduplicateSet.contains(dedepulicateKey)){
                            deduplicateSet.add(dedepulicateKey);
                        }
                        else{
                            continue;
                        }
                        FileUtil.copy(repoPath + "/" + filePath,targetPath + firstDirName + "/"  + pairNum + "-" + soUrl + "-"  + commit.getName().substring(0,7) + "/" + filePath.substring(filePath.lastIndexOf("/")+1));
                        List<File> allTxtFiles = FileHelper.getAllFiles(urlPath + soUrl, ".txt");
                        for(File file : allTxtFiles){
                            String fileName = file.getName();
                            String targetFilePath = targetPath + firstDirName + "/"  + pairNum + "-" + soUrl +"-" + commit.getName().substring(0,7) + "/" +  fileName;
                            FileUtil.copy(file.getAbsolutePath(),targetFilePath);
                        }
                        pairNum++;
                    }
//                    commitNum++;
//                    if(commitNum  == repoInfo.size()){
//                        ResetGitRepos.pullMaster(repoPath + "/.git");
//                    }
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Identify GitHub projects that reference the most StackOverflow
     */
    public static void identifyTopProjects(String rootPath,List<String> repoNames, List<Map<String, Set<String>>> repoInfoList) throws IOException {
        /*
            set the output path
         */
//        FileOutputStream fos = new FileOutputStream(new File("E:/CodeAdaptation/Dataset/AllRepositoriesJava2SO.txt"));
//        FileOutputStream fos = new FileOutputStream(new File("E:/CodeAdaptation/Dataset/Top100RepositoriesJava2SO.txt"));
//        PrintStream ps = new PrintStream(fos);
//        System.setOut(ps);

        Map<String, Set<String>> soToGithubRepos = new HashMap<>();
        Map<String, Set<String>> gitHub2SO = new HashMap<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(rootPath))) {
            for (Path dirPath : directoryStream) {
                if (Files.isDirectory(dirPath)) {
                    processDirectory(dirPath, soToGithubRepos,gitHub2SO);
                }
            }
        }

        Map<String, Integer> repoReferenceCount = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : gitHub2SO.entrySet()) {
            repoReferenceCount.put(entry.getKey(), entry.getValue().size());
        }

//        List<Map.Entry<String, Integer>> topRepos = repoReferenceCount.entrySet().stream()
//                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
////                .limit(20)
//                .collect(Collectors.toList());
        int topk=100;
//        int topk= repoReferenceCount.size();
        List<Map.Entry<String, Integer>> topRepos = repoReferenceCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
//                .limit(topk)
                .collect(Collectors.toList());
        HashMap<String,Integer> Map4Deduplication = new HashMap<>();
        int cnt = 0;
        int validRepoNum = 0;
        for (int i = 0; i < topRepos.size(); i++) {
            Map.Entry<String, Integer> entry = topRepos.get(i);
            Map<String, Set<String>> eachRepoJava2SO = new HashMap<>();
            String userAndRepoName = entry.getKey();
            for (String s : gitHub2SO.get(userAndRepoName)){
//                System.out.println(s + "-----------------");
                Set<String> strings = soToGithubRepos.get(s);
                for(String s2 : strings){
                    if(s2.contains(userAndRepoName)){
                        eachRepoJava2SO.computeIfAbsent(s2, k -> new HashSet<>()).add(s);
                    }
                }
            }
            String repoName = userAndRepoName.split("/")[1];
            if(Map4Deduplication.containsKey(repoName)){
                int value = Map4Deduplication.get(repoName);
                if(value < entry.getValue()){
                    Map4Deduplication.put(repoName,entry.getValue());
                }
                else{
                    continue;
                }
            }
            else{
                Map4Deduplication.put(repoName,entry.getValue());
            }
            validRepoNum++;
//            if(validRepoNum > topk) break;
            System.out.println((validRepoNum) + ". " + userAndRepoName + " - reference number: " + entry.getValue());
            System.out.println("involved file number："+ eachRepoJava2SO.size());

            for(Map.Entry<String, Set<String>> entry1 : eachRepoJava2SO.entrySet()){
                System.out.println(entry1.getKey() + " : " );
                for(String s : entry1.getValue()){
                    System.out.println(s);
                }
            }

            repoInfoList.add(eachRepoJava2SO);
            repoNames.add(userAndRepoName);
            cnt +=entry.getValue();
        }

    }

    /**
     * 处理单个目录，读取gh_repo_urls.txt和so_url.txt文件
     */
    private static void processDirectory(Path dirPath, Map<String, Set<String>> soToGithubRepos, Map<String, Set<String>> githubRepoToSo) {
        Path ghRepoFile = dirPath.resolve("gh_repo_urls.txt");
        Path soUrlFile = dirPath.resolve("so_url.txt");

        if (Files.exists(ghRepoFile) && Files.exists(soUrlFile)) {
            try {
                String soUrl = Files.readAllLines(soUrlFile).get(0).trim();

                List<String> ghRepoLines = Files.readAllLines(ghRepoFile);
                for (String line : ghRepoLines) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 2) {
                        String repoUrl = parts[1].trim();
                        String repoName = extractRepoName(repoUrl);

                        soToGithubRepos.computeIfAbsent(soUrl, k -> new HashSet<>()).add(repoUrl);
                        githubRepoToSo.computeIfAbsent(repoName, k -> new HashSet<>()).add(soUrl);
                    }
                }
            } catch (IOException e) {
                System.err.println("处理目录时出错: " + dirPath + " - " + e.getMessage());
            }
        }
    }

    /**
     * 从GitHub URL中提取仓库名称
     */
    private static String extractRepoName(String repoUrl) {
        if (repoUrl.contains("github.com")) {
            String[] parts = repoUrl.split("github\\.com/");
            if (parts.length > 1) {
                String userAndRepo = parts[1].split("/blob")[0];
//                return userAndRepo.split("/")[1];
                return userAndRepo;
            }
        }
        return repoUrl;
    }
}
