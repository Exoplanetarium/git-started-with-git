import java.io.IOException;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class GitWrapper {

    /**
     * Initializes a new Git repository.
     * This method creates the necessary directory structure
     * and initial files (index, HEAD) required for a Git repository.
     */
    public void init() {
        // to-do: implement functionality here
        Git git = new Git();
        git.init();
    }

    /**
     * Stages a file for the next commit.
     * This method adds a file to the index file.
     * If the file does not exist, it throws an IOException.
     * If the file is a directory, it throws an IOException.
     * If the file is already in the index, it does nothing.
     * If the file is successfully staged, it creates a blob for the file.
     * @param filePath The path to the file to be staged.
     */
    public void add(String filePath) {
        // to-do: implement functionality here
        Git git = new Git();
        git.addFile(filePath);
    }

    /**
     * Creates a commit with the given author and message.
     * It should capture the current state of the repository by building trees based on the index file,
     * writing the tree to the objects directory,
     * writing the commit to the objects directory,
     * updating the HEAD file,
     * and returning the commit hash.
     *
     * The commit should be formatted as follows:
     * tree: <tree_sha>
     * parent: <parent_sha>
     * author: <author>
     * date: <date>
     * message: <message>
     *
     * @param author  The name of the author making the commit.
     * @param message The commit message describing the changes.
     * @return The SHA1 hash of the new commit.
     */
    public String commit(String author, String message) {
        // to-do: implement functionality here
        Git git = new Git();
        return git.commit(author, message);
    }

    /**
     * Checks out a specific commit given its hash.
     * This method should read the HEAD file to determine the "checked out" commit.
     * Then it should update the working directory to match the
     * state of the repository at that commit by tracing through the root tree and
     * all its children.
     *
     * @param commitHash The SHA1 hash of the commit to check out.
     */
    public void checkout(String commitHash) {
        try {
            BufferedReader indexReader = new BufferedReader(new FileReader("git/index"));
            String line;
            while ((line = indexReader.readLine()) != null) {
                File file = new File(line.split(" ")[1].substring(21));
                if (file.exists()) {
                    file.delete();
                }
                String[] fileParents = line.split(" ")[1].substring(21).split("/");
                String path = "";
                for (String dirPath : fileParents) {
                    File dir = new File(path + dirPath);
                    if (dir.isDirectory() && dir.list().length == 0) {
                        dir.delete();
                    }
                    path += dirPath + "/";
                }
            }
            indexReader.close();
            FileWriter headWriter = new FileWriter("git/HEAD");
            headWriter.write(commitHash);
            headWriter.close();
            Git git = new Git();
            git.regenerateIndex();

            BufferedReader indexReader2 = new BufferedReader(new FileReader("git/index"));
            String line2;
            while ((line2 = indexReader2.readLine()) != null) {
                String[] dirsToMake = line2.split(" ")[1].substring(21).split("/");
                String trackDirPath = "";
                for (int i = 0; i < dirsToMake.length; i++) {
                    String dirPath = dirsToMake[i];
                    File file = new File(trackDirPath + dirPath);
                    if (i != dirsToMake.length - 1) {
                        file.mkdir();
                        trackDirPath = dirPath + "/";
                    } else {
                        file.createNewFile();
                        String fileHash = line2.split(" ")[0];
                        BufferedReader fileReader = new BufferedReader(new FileReader("git/objects/" + fileHash));
                        StringBuilder fileContents = new StringBuilder();
                        String line3;
                        boolean first = false;
                        while ((line3 = fileReader.readLine()) != null) {
                            if (first) {
                                fileContents.append("\n");
                            }
                            fileContents.append(line3);
                            first = true;
                        }

                        fileReader.close();

                        FileWriter fileWriter = new FileWriter(file.getPath());
                        fileWriter.write(fileContents.toString());
                        fileWriter.close();
                        trackDirPath = "";
                    }
                    
                }
            }
            indexReader2.close();
        } catch (IOException e) {
            return;
        }
        
    }
}