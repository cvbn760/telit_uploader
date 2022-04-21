import java.util.Arrays;
import java.util.List;

public class MainClass {
    public static void main(String...args){
        Arrays.stream(args).toList().stream().forEach(System.out::println);
        System.out.println("DOWNLOADER: hello");
    }
}
