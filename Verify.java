public class Verify {
    public static void main(String[] args) {
        Git git = new Git();
        git.init();
        git.addFile("helloworld.txt");
        git.addFile("helloworld2.txt");
    }
}
