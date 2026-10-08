import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public class Git {
    public static void main(String[] args) {
        Git git = new Git();
        git.init();
        git.addFile("helloworld.txt");
        git.addFile("helloworld2.txt");
        git.addFile("helloworld.txt");
        git.addFile("testFolder/helloworld.txt");
        git.addFile("helloworldcopy.txt");
        git.addFile("testFolder/testFolder2/testfile1.txt");
        System.out.println(git.tree("testFolder"));
        System.out.println(git.indexTree());
    }

    // Initializes repository structure in ./git/: Objects/, index, and HEAD
    public void init() {
        File git = new File("./git");
        File objects = new File("./git/objects");
        File index = new File("./git/index");
        File HEAD = new File("./git/HEAD");

        try {
            boolean repoCreated = false;
            repoCreated = (git.mkdirs() || repoCreated);
            repoCreated = (objects.mkdirs() || repoCreated);
            repoCreated = (index.createNewFile() || repoCreated);
            repoCreated = (HEAD.createNewFile() || repoCreated);

            if (repoCreated) {
                System.out.println("Git Repository Created");
            } else {
                System.out.println("Git Repository Already Exists");
            }

        } catch (IOException e) {
            System.out.println("File creation exception: " + e);
        }
    }

    // Hashes file contents using SHA-1
    public String hashFile(String filePath) {
        try {
            byte[] fileContents = Files.readAllBytes(Path.of(filePath));
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(fileContents);
            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            System.out.println("File hashing exception:" + e);
        }
        return null;
    }

    public String hashString(String str) {
        try {
            byte[] fileContents = str.getBytes();
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(fileContents);
            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            System.out.println("File hashing exception:" + e);
        }
        return null;
    }

    // Turns file into BLOB with hash name and inserts into git/objects/, and records in git/index
    public void addFile(String filePath) {
        if (!Files.isRegularFile(Path.of(filePath))) {
            return;
        }

        String fileHash = this.hashFile(filePath);
        String blobPathString = "git/objects/" + fileHash;
        File blob = new File(blobPathString);

        try {
            blob.createNewFile();
            byte[] contents = Files.readAllBytes(Path.of(filePath));
            Files.write(Path.of(blobPathString), contents);
        } catch (Exception e) {
            System.out.println("File adding exception:" + e);
        }


        String indexPathString = "git/index";
        String indexEntry = "\n" + fileHash + " " + "git-started-with-git/" + filePath;

        // see if file was already indexed
        try {
            List<String> indexLines = Files.readAllLines(Path.of(indexPathString));
            boolean lineReplaced = false;
            String entryToWrite = "";
            int i = 0;
            ArrayList<String> linesToWrite = new ArrayList<>();
            if (!indexLines.contains(indexEntry.substring(1))) {
                // false means rewrite the index instead of appending
                if (Files.size(Path.of("git/index")) != 0) {
                    for (String indexLine : indexLines) {
                        String[] lineParts = indexLine.split(" ", 2);
                        if (lineParts.length == 2 && lineParts[1].substring(21).equals(filePath)) {
                            entryToWrite = indexEntry;
                            lineReplaced = true;
                        } else {
                            if (i == 0) {
                                entryToWrite = indexLine;
                            } else {
                                entryToWrite = "\n" + indexLine;
                            }
                        }
                        linesToWrite.add(entryToWrite);
                        i++;
                    }
                    if (!lineReplaced) {
                        linesToWrite.add(indexEntry);
                    }
                } else {
                    linesToWrite.add(indexEntry.substring(1));
                }

                try (FileWriter indexWriter = new FileWriter(indexPathString, false)) {
                    for (String lineToWrite : linesToWrite) {
                        indexWriter.write(lineToWrite);
                    }
                    indexWriter.close();
                }

            }

        } catch (IOException e) {
            System.out.println("Index writing exception: " + e);
        }
    }

    public String tree(String directoryPath) {
        ArrayList<String> tree = new ArrayList<>();
        File path = new File(directoryPath);
        File[] files = path.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                tree.add("blob " + hashFile(files[i].getPath()) + " " + files[i].getPath());
            } else {
                tree.add("tree " + hashString(tree(files[i].getPath())) + " " + files[1].getPath());

            }
        }
        StringBuilder toBeReturned = new StringBuilder();
        for (String str : tree) {
            toBeReturned.append(str + "\n");
        }
        return hashString(toBeReturned.substring(0, toBeReturned.length() - 1));
    }

    public String indexTree() {
        try {
            ArrayList<String> workingList = new ArrayList<>();
            BufferedReader indexReader = new BufferedReader(new FileReader("git/index"));
            while (indexReader.ready()) {
                workingList.add("blob " + indexReader.readLine());
            }
            ArrayList<String[]> splitEntries = new ArrayList<>();
            for (String listEntry : workingList) {
                splitEntries.add(listEntry.split("/"));
            }
            while (splitEntries.size() > 1) {
                splitEntries = sort(splitEntries);
                StringBuilder folderEntries = new StringBuilder();
                ArrayList<Integer> entriesToRemove = new ArrayList<>();
                String folderName = "";
                int indexOfEntry = 0;
                int indexOfFolder = 0;
                boolean isFirst = true;
                for (int i = 0; i < splitEntries.size(); i++) {
                    String[] splitEntry = splitEntries.get(i);
                    if (isFirst) {
                        folderName = splitEntry[splitEntry.length - 2];
                        indexOfFolder = splitEntry.length - 2;
                        indexOfEntry = i;
                        folderEntries.append(splitEntry[0].substring(0, 46));
                        folderEntries.append(splitEntry[splitEntry.length - 1]);
                        folderEntries.append("\n");
                        entriesToRemove.add(i);
                        isFirst = false;
                    } else {
                        if (splitEntry.length <= indexOfFolder) {
                            break;
                        } else {
                            if (splitEntry[indexOfFolder].equals(folderName) || indexOfFolder == 0) {
                                folderEntries.append(splitEntry[0].substring(0, 46));
                                folderEntries.append(splitEntry[splitEntry.length - 1]);
                                folderEntries.append("\n");
                                entriesToRemove.add(i);
                            }
                        }
                    }
                }
                String folderString = folderEntries.toString();
                folderString = folderString.substring(0, folderString.length() - 1);

                // finding the path of the folder to add to new entry
                StringBuilder folderPath = new StringBuilder();
                String[] folderArray = splitEntries.get(indexOfEntry);
                folderPath.append("git-started-with-git/");
                for (int entryIndex = 1; entryIndex < folderArray.length - 1; entryIndex++) {
                    String entry = folderArray[entryIndex];
                    folderPath.append(entry + "/");
                }
                // folderPath.append(folderArray[folderArray.length - 1]);
                String folderPathString = folderPath.toString();
                String newEntry = "tree " + hashString(folderString) + " "
                        + folderPathString.substring(0, folderPathString.length() - 1);
                for (int entryToRemove =
                        entriesToRemove.size() - 1; entryToRemove >= 0; entryToRemove--) {
                    splitEntries.remove((int) entriesToRemove.get(entryToRemove));
                }
                splitEntries.add(newEntry.split("/"));
            }

            indexReader.close();
            String ret = "";
            for (String part : splitEntries.get(0)) {
                ret += part + "/";
            }
            return "tree " + hashString(ret.substring(0, ret.length() - 1));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<String[]> sort(ArrayList<String[]> input) {
        ArrayList<String[]> output = new ArrayList<>();
        while (input.size() != 0) {
            int max = 0;
            int savedIndex = 0;
            for (int i = 0; i < input.size(); i++) {
                String[] array = input.get(i);
                if (array.length > max) {
                    max = array.length;
                    savedIndex = i;
                }
            }
            output.add(input.get(savedIndex));
            input.remove(savedIndex);
        }
        return output;
    }


}


