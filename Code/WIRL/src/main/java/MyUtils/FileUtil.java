package MyUtils;

import edu.lu.uni.serval.utils.FileHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileUtil {

    public static boolean copy(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            if (!Files.exists(source)) {
                System.err.println("源文件不存在: " + sourcePath);
                return false;
            }

            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            return true;
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
            return false;
        }
    }
    public static String readFileToString(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int a = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    public static void main(String[] args) {
        List<File> allDirectories = FileHelper.getAllSubDirectories("E:\\CodeAdaptation\\Dataset\\CodeWiringCandidateData");
        List<String> stringFileNames = new ArrayList<>();
        for (File file : allDirectories) {
            String fileName = file.getName();
            stringFileNames.add(fileName);
        }
        Collections.sort(stringFileNames,(str1,str2) -> {
            String [] split1 = str1.split("-");
            String [] split2 = str2.split("-");
            return Integer.parseInt(split1[0]) - Integer.parseInt(split2[0]);
        });
        for(String fileName : stringFileNames){
            String[] split = fileName.split("-");
            int cnt = 0;
            String repoName = "";
            for(String sp : split){

                if (cnt != 0){
                    if(cnt>1){
                        repoName = repoName + "-" + sp;
                    }
                    else{
                        repoName+=sp;
                    }

                }

                cnt++;
            }
            System.out.println(repoName);
        }
    }
}