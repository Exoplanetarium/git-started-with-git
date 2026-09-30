import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

public class Git {
    public static void main(String[] args) {
        Git git = new Git();
        git.init();

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
        String indexEntry = fileHash + " " + filePath + "\n";

        // see if file was already indexed
        try {
            List<String> indexLines = Files.readAllLines(Path.of(indexPathString));
            boolean lineReplaced = false;

            // false means rewrite the index instead of appending
            try (FileWriter indexWriter = new FileWriter(indexPathString, false)) {
                for (String indexLine : indexLines) {
                    String[] lineParts = indexLine.split(" ", 2);

                    if (lineParts.length == 2 && lineParts[1].equals(filePath)) {
                        indexWriter.write(indexEntry);
                        lineReplaced = true;
                    } else {
                        indexWriter.write(indexLine + "\n");
                    }
                }

                // if path wasn't already indexed do this
                if (!lineReplaced) {
                    indexWriter.write(indexEntry);
                }
            }
        } catch (IOException e) {
            System.out.println("Index writing exception: " + e);
        }
    }
}


