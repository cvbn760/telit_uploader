import jssc.SerialPortException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MainClass {
    private static final String GET_CMD = "cmd.exe";
    private static final String GET_LIST_DEVICES = "adb devices";
    private static final String ATTR = "/C";
    private static final String TELIT_MAIN_DIR = "/data/azc/mod/";
    private static final String TELIT_SOUND_DIR = "/data/aplay/";
    private static final String ADB_PUSH = "adb push";

    private static final String IS_DEVICE_REG_EX = "^.+[\\s]+(device)[\\s]*$";

    private static ProcessBuilder processBuilder;
    private static Process prcs;
    private static BufferedReader bufferedReader;
    private static BufferedWriter bufferedWriter;

    private static String directWithFiles;

    public static void startNewProcces(String dir, String cmd) throws IOException {
        processBuilder = new ProcessBuilder(GET_CMD, ATTR, cmd);
        processBuilder.redirectErrorStream(true);
        if (!dir.isEmpty()){
            processBuilder.directory(new File(dir));
        }
        prcs = processBuilder.start();
        bufferedReader = new BufferedReader(new InputStreamReader(prcs.getInputStream(),  StandardCharsets.US_ASCII));
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(prcs.getOutputStream()));
    }

    public static void releaseProcess() throws IOException {
        bufferedWriter.flush();
        bufferedWriter.close();
        bufferedReader.close();
        prcs = null;
        processBuilder = null;
    }

    public static List<String> readAnswer()  {
        List<String> content = new ArrayList<>();
        String line;
        while (true) {
            try {
                line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                content.add(line);
            } catch (Exception exc) {
                exc.printStackTrace();
                break;
            }
        }
        return content;
    }

    public static boolean checkingConnectedDevices() throws IOException {
        List<String> content = readAnswer().stream().filter(line -> Pattern.compile(IS_DEVICE_REG_EX).matcher(line).matches()).toList();
        if (content.size() < 1){
            return false;
        }
        System.out.println("DOWNLOADER: discovered devices");
        content.stream().map(st -> st.replaceAll("device", "").trim()).forEach(str -> System.out.println("->" + str));
        return true;
    }


    public static void main(String...args) throws IOException, InterruptedException, SerialPortException {
//        Arrays.stream(args).toList().stream().forEach(System.out::println);
//
//        String ADDR_PC = args[0];     // Полный путь к папке с файлами, которые будут загружаться в модем
//        String ADDR_TELIT = args[1];  // Дирректория в памяти модема куда будут складываться файлы
//        System.out.println("DOWNLOADER: has been started.");
//
//        // Пробуем использовать командную строку
//        try {
//            startNewProcces("","");
//            releaseProcess();
//        } catch (IOException e) {
//            System.out.println("DOWNLOADER: unable to use command line.");
//            return;
//        }
//
//        // Получаем список подключенных устройств
//        try {
//            startNewProcces("", GET_LIST_DEVICES);
//            if(!checkingConnectedDevices()){
//                System.out.println("DOWNLOADER: no connected devices found.");
//                return;
//            }
//        } catch (Exception exc) {
//            releaseProcess();
//        }
//        releaseProcess();
//
//        //Получаем список файлов в указанной дирректории
//        File dir = new File(ADDR_PC); //path указывает на директорию
//        File[] arrFiles = dir.listFiles();
//
////        System.out.println("DOWNLOADER: " + arrFiles.length + " files were found in the specified directory.");
//
//
//        // Загружаем файлы в модем
//        try {
//            StringBuffer buffer;
//            for (File f : arrFiles) {
//                buffer = new StringBuffer();
//                buffer.append(ADB_PUSH + " ").append(f.getName()).append(" " + ADDR_TELIT);
//                startNewProcces(ADDR_PC, buffer.toString());
//                readAnswer().stream().forEach(System.out::println);
//                System.out.println("->" + f.getName() + "...done");
//            }
//            System.out.println("DOWNLOADER: All files have been uploaded");
//        }
//        catch (Exception exc){
//            System.out.println("DOWNLOADER: An error occurred during loading");
//            exc.printStackTrace();
//        }


        SerarchEngine serarchEngine = SerarchEngine.getNewInstance();
    }
}
