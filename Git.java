import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public class Git {
    public static void main(String[] args) {
        GitWrapper git = new GitWrapper();
        // git.add("helloworld2.txt");
        // git.add("testFolder/testfile.txt");
        // git.commit("Me", "Commit B");
        //git.checkout("7e76e6ef9e5c6d54c8d0ccbbe15e6029fb3e4fde");
        git.checkout("ac7cedee8370f0c1d21737ad0481363225db6b91");
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
        try {
            if (!Files.isRegularFile(Path.of(filePath))) {
                throw new IOException();
            }

            String fileHash = this.hashFile(filePath);
            String blobPathString = "git/objects/" + fileHash;
            File blob = new File(blobPathString);

            blob.createNewFile();
            byte[] contents = Files.readAllBytes(Path.of(filePath));
            Files.write(Path.of(blobPathString), contents);


            String indexPathString = "git/index";
            String indexEntry = fileHash + " " + "git-started-with-git/" + filePath;
            // see if file was already indexed
            List<String> indexLines = Files.readAllLines(Path.of(indexPathString));
            boolean lineReplaced = false;
            String entryToWrite = "";
            ArrayList<String> linesToWrite = new ArrayList<>();
            if (!indexLines.contains(indexEntry)) {
                // false means rewrite the index instead of appending
                if (Files.size(Path.of("git/index")) != 0) {
                    for (String indexLine : indexLines) {
                        String[] lineParts = indexLine.split(" ", 2);
                        if (lineParts.length == 2 && lineParts[1].substring(21).equals(filePath)) {
                            entryToWrite = indexEntry;
                            lineReplaced = true;
                        } else {
                            entryToWrite = indexLine;
                        }
                        linesToWrite.add(entryToWrite);
                    }
                    if (!lineReplaced) {
                        linesToWrite.add(indexEntry);
                    }
                } else {
                    linesToWrite.add(indexEntry);
                }

                try (FileWriter indexWriter = new FileWriter(indexPathString, false)) {
                    boolean isFirst = false;
                    for (String lineToWrite : linesToWrite) {
                        if (isFirst) {
                            indexWriter.write("\n");
                        }
                        indexWriter.write(lineToWrite);
                        isFirst = true;
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
                tree.add("blob " + hashFile(files[i].getPath()) + " " + files[i].getName());
            } else {
                tree.add("tree " + hashString(tree(files[i].getPath())) + " " + files[i].getName());

            }
        }
        StringBuilder toBeReturned = new StringBuilder();
        for (String str : tree) {
            toBeReturned.append(str + "\n");
        }
        String hash = hashString(toBeReturned.substring(0, toBeReturned.length() - 1));
        File treeFile = new File("./git/objects/" + hash);
        try {
            FileWriter wr = new FileWriter(treeFile.getPath());
            wr.write(toBeReturned.substring(0, toBeReturned.length() - 1));
            wr.close();
        } catch (IOException e) {
            return "";
        }

        return hash;
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

                // writes tree to objects
                String folderHash = hashString(folderString);
                FileWriter folderWriter = new FileWriter("git/objects/" + folderHash);
                folderWriter.write(folderString);
                folderWriter.close();

                String folderPathString = folderPath.toString();
                String newEntry = "tree " + folderHash + " "
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

            String potentialRootTreeString = ret.substring(0, ret.length() - 1);

            // fixing the bug where a single line is left in the working tree that is NOT the root
            // writes root tree to objects
            String rootHash = "";
            if (!potentialRootTreeString.split(" ")[2].equals("git-started-with-git")) {
                String blobHash = hashString(potentialRootTreeString);
                FileWriter blobWriter = new FileWriter("git/objects/" + blobHash);
                blobWriter.write(potentialRootTreeString);
                blobWriter.close(); 
                String actualRootTreeString = "tree " + blobHash + " git-started-with-git";
                rootHash = hashString(actualRootTreeString);
                FileWriter rootTreeWriter = new FileWriter("git/objects/" + rootHash);
                rootTreeWriter.write(actualRootTreeString);
                rootTreeWriter.close(); 
            } else {
                rootHash = hashString(potentialRootTreeString);
                FileWriter rootTreeWriter = new FileWriter("git/objects/" + rootHash);
                rootTreeWriter.write(ret.substring(0, ret.length() - 1));
                rootTreeWriter.close(); 
            }
           
            return rootHash;
        } catch (IOException e) {
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

    public String commit(String author, String message) {
        try {
            StringBuilder commitString = new StringBuilder();
            commitString.append("tree: " + indexTree() + "\n");

            BufferedReader br = new BufferedReader(new FileReader("git/HEAD"));
            String line;
            if ((line = br.readLine()) != null) {
                commitString.append("parent: " + line + "\n");
            }
            br.close();
            commitString.append("author: " + author + "\n");
            commitString.append("date: " + LocalDateTime.now() + "\n");
            commitString.append("message: " + message);

            String commitHash = hashString(commitString.toString());
            File commitFile = new File("git/objects/" + commitHash);

            FileWriter commitFileWriter = new FileWriter(commitFile.getPath());
            commitFileWriter.write(commitString.toString());
            commitFileWriter.close();
            
            FileWriter headWriter = new FileWriter("git/HEAD");
            headWriter.write(commitHash);
            headWriter.close();

            return commitHash;
        } catch (IOException e) {
            return "";
        }

    }

    public void regenerateIndex() {
        try {
            BufferedReader headReader = new BufferedReader(new FileReader("git/HEAD"));
            String commitHash = headReader.readLine();
            headReader.close();
            BufferedReader commitReader = new BufferedReader(new FileReader("git/objects/" + commitHash));
            String rootHash = "";
            String line;
            while ((line = commitReader.readLine()) != null) {
                if (line.split(" ")[0].equals("tree:")) {
                    rootHash = line.split(" ")[1];
                }
            }
            commitReader.close();
            BufferedReader rootReader = new BufferedReader(new FileReader("git/objects/" + rootHash));
            String startingHash = rootReader.readLine().split(" ")[1];
            rootReader.close();
            String temp = regenerateIndexHelper(startingHash, "git-started-with-git");
            String indexString = temp.substring(0, temp.length() - 1);
            FileWriter indexWriter = new FileWriter("git/index");
            indexWriter.write(indexString);
            indexWriter.close();
        } catch (IOException e) {
            return;
        }
    }

    public String regenerateIndexHelper(String treeHash, String path) {
        try {
            StringBuilder index = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader("git/objects/" + treeHash));
            ArrayList<String> treeList = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                treeList.add(line);
            }
            br.close();

            for (String tree : treeList) {
                String[] entry = tree.split(" ");
                if (entry[0].equals("tree")) {
                    String nextHash = entry[1];
                    index.append(regenerateIndexHelper(nextHash, path + "/" + entry[2]));
                } else {
                    if (entry[2].contains("git-started-with-git")) {
                        index.append(entry[1] + " " + path + "/" + entry[2].substring(21) + "\n");

                    } else {
                        index.append(entry[1] + " " + path + "/" + entry[2] + "\n");
                    }
                }
            }

            return index.toString();

        } catch (IOException e) {
            return "";
        }
        
    }

}


