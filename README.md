Git Project:

Methods and Functions:
- init() --- initializes folder structure: git/, objects/, HEAD, index.
- hash(String filePath) --- Hashes file contents using SHA-1
- add(String filePath) --- Turns file into BLOB with hash name and inserts into git/objects/, and records in git/INDEX

No bugs to fix.

GitWrapper example tester:
    GitWrapper git = new GitWrapper();
    git.init();
    git.addFile("helloworld.txt");
    git.addFile("testFolder/helloworld.txt");
    String commitHash = git.commit("Me", "testing first commit");
    git.checkout(commitHash);