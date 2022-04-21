import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Pattern;

public class MainClass {
    private static final String GET_CMD = "cmd.exe";
    private static final String GET_LIST_DEVICES = "adb devices";
    private static final String MAIN_DISK = "/C";

    private static final String IS_DEVICE_REG_EX = "^.+[\\s]+(device)[\\s]*$";

    private static Process prcs;


    public static Process startNewProcces() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(GET_CMD, MAIN_DISK, GET_LIST_DEVICES);
        builder.redirectErrorStream(true);
        return builder.start();
    }


    public static boolean checkingConnectedDevices(Process prcs) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(prcs.getInputStream()));
        String line;
        int cnt = 0;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }
            cnt = Pattern.compile(IS_DEVICE_REG_EX).matcher(line).matches() ?  cnt + 1 : cnt;
            System.out.println(line);
        }
        if (cnt < 1){
            return false;
        }
        return true;
    }

    public static void main(String...args) throws IOException {
        Arrays.stream(args).toList().stream().forEach(System.out::println);
        System.out.println("DOWNLOADER: has been started.");

        try {
            prcs = startNewProcces();
        } catch (IOException e) {
            System.out.println("DOWNLOADER: unable to use command line.");
            return;
        }

        if (!checkingConnectedDevices(prcs)){
            System.out.println("DOWNLOADER: no connected devices found.");
            return;
        }
    }
}
