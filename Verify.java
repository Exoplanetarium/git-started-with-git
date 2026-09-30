public class Verify {
    public static void main(String[] args) {
        Git git = new Git();
        git.init();
        git.add("helloworld.txt");
        git.add("helloworld2.txt");
    }
}
