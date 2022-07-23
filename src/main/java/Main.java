import jssc.*;

import java.io.*;

public class Main {
    private static SerialPort serialPort;
    private static boolean currentTask = false;
    private static InputStream is = null;

    private static boolean openConnection(String port){
        serialPort = new SerialPort("COM" + port);
        try {
            //Открываем порт
            serialPort.openPort();
            //Выставляем параметры
            serialPort.setParams(SerialPort.BAUDRATE_9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            //Включаем аппаратное управление потоком
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
                    SerialPort.FLOWCONTROL_RTSCTS_OUT);
        }
        catch (SerialPortException ex) {
//            System.out.println(ex);
            return false;
        }
        return true;
    }

    static void silentCmd() throws SerialPortException, InterruptedException {
        serialPort.writeString("AT\r");
        String data = "";

        while (true){ // Ждем ответ
            Thread.sleep(50);
            data = serialPort.readString();
            if(data == null) data = "";
            data = data.trim();
            if (!data.equals("")) return;
        }
    }

    static boolean sendAtCmd(String cmd) throws SerialPortException, InterruptedException {
        currentTask = false;
        //Устанавливаем ивент лисенер и маску
        //Отправляем запрос устройству
        serialPort.writeString(cmd);
        System.out.println(cmd);
        String data = "";

        while (true){ // Ждем ответ
            Thread.sleep(50);
            data = serialPort.readString();
            if(data == null) data = "";
            data = data.trim();
            if (data.contains("OK")) {
                System.out.println("OK\r\n");
                return true;
            }

            if (data.contains(">>>")) {
                System.out.println(">>>\r\n");
                return true;
            }
            if (data.contains("ERROR")) {
                System.out.println("ERROR\r\n");
                return false;
            }
            if (data.contains("NO CARRIER")) {
                System.out.println("NO CARRIER\r\n");
                return false;
            }
        }
    }

    public static boolean deleteFileModen(String fileName, String destenation) throws SerialPortException, InterruptedException {
        // Составляем команду для удаления файла
        StringBuffer sb = new StringBuffer();
        // Переходим в указанную дирректорию в модеме
        sb.append("AT#M2MCHDIR=");
        sb.append("\"");
        sb.append(destenation);
        sb.append("\"");
        sb.append("\r");
        sendAtCmd(sb.toString());

        // Удаляем файл
        sb.delete(0, sb.length());
        sb.append("AT#M2MDEL=");
        sb.append("\"");
        sb.append("\"");
        sb.append(fileName);
        sb.append("\r");
        return true;
    }

    private static boolean sendFile(String fileName, String source, String destenation) throws IOException, SerialPortException, InterruptedException {
        // Находим файл по указанному пути и определяем размер
        File file = new File(source + fileName);
        long size = file.length();
        System.out.println(source + fileName + "\r\nsize: " + size + " bytes");

        StringBuffer sb = new StringBuffer();
        sb.delete(0, sb.length());

        // Переходим в указанную дирректорию в модеме
        sb.append("AT#M2MCHDIR=");
        sb.append("\"");
        sb.append(destenation);
        sb.append("\"");
        sb.append("\r");
        if(!sendAtCmd(sb.toString()));

        // Составляем команду для отправки
        sb.delete(0, sb.length());
        sb.append("AT#M2MWRITE=");
        sb.append("\"");
        sb.append(fileName);
        sb.append("\",");
        sb.append(size);
        sb.append("\r");
        if(!sendAtCmd(sb.toString()));

        sb.delete(0, sb.length());
        // Передаем файл
        try {
            is = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                serialPort.writeBytes(buffer);
            }
            is.close();
            // После загрузки файла ожидаем ответ
            String data = "";
            while (true){ // Ждем ответ
                Thread.sleep(50);
                data = serialPort.readString();
                if(data == null) data = "";
                data = data.trim();
                if (data.contains("OK")) {
                    System.out.println(data + "\r\n");
                    is.close();
                    silentCmd();
                    return true;
                }
                if (data.contains("ERROR")) {
                    System.out.println(data + "\r\n");
                    is.close();
                    silentCmd();
                    return false;
                }
            }
        } catch (Exception exc){
            is.close();
            exc.printStackTrace();
            return false;
        }
    }




    private static String port = "";
    private static String sourceAddr = "";

    public static void main(String[] args) throws SerialPortException, InterruptedException, IOException {

        port = args[0];
        sourceAddr = args[1];
//        port = "29";
//        sourceAddr = "C:/8450110539_DSP/";

        // Показать доступные COM порты
        System.out.println("Ports available: ");
        String[] portNames = SerialPortList.getPortNames();
        for (String portName : portNames) {
            System.out.println(portName);
        }
        System.out.println("\r\n");

        // Подключиться к указанному COM порту
        if (!openConnection(port)){
            System.out.println("COM" + port + " unable to open\r\n");
            return;
        }
        else {
            System.out.println(">>>>>>>>>>>>>> COM" + port + " was opened\r\n");
        }

//        // Загрузка АТ команд
        int cmd = 0;
        if(sendAtCmd("AT$GPSP=1\r"));           // --
        if(sendAtCmd("AT$GPSSAV\r"))cmd++;            // 1
        if(sendAtCmd("AT#V24CFG=3,1,1\r")) cmd++;     // 2
        if(sendAtCmd("AT#V24=3,1\r")) cmd++;          // 3
        if(sendAtCmd("AT#DIALMODE=2\r")) cmd++;       // 4
        if(sendAtCmd("AT#ECONLY=2\r")) cmd++;         // 5
        if(sendAtCmd("AT#VAUX=1,1\r")) cmd++;         // 6
        if(sendAtCmd("AT#V24CFG=0,2,1\r")) cmd++;     // 7
        if(sendAtCmd("AT#V24CFG=2,2,1\r")) cmd++;     // 8
        if(sendAtCmd("AT#V24CFG=4,2,1\r")) cmd++;     // 9
        if(sendAtCmd("AT#SIMDET=1\r")) cmd++;         // 10
        if(sendAtCmd("AT&W\r")) cmd++;                // 11
        if(sendAtCmd("AT&P\r")) cmd++;                // 12
        if(sendAtCmd("AT#ECALLNWTMR=120,2\r")) cmd++; // 13
        System.out.println(cmd + " out of " + 13 + " commands sent successfully");
        if (cmd != 13){
            serialPort.closePort();
            System.out.println("Not all AT commands were executed.");
            System.out.println("The firmware is stopped.");
            System.out.println("Download LE910C1-EU_25.21.222-B008_CUST_136_perf_TFI.exe and try again.");
            return;
        }

        // Загрузка hash.bin
        // hash.bin отвечает только в UART0
        // использовать его при загрузке через USB нельзя никак

//         Загрузка файлов
        int fileCnt = 0;
        if (sendFile("hash.bin", sourceAddr, "/data/azc/mod/")) fileCnt++;    // 1
        if (sendFile("era.bin", sourceAddr, "/data/azc/mod/")) fileCnt++;     // 2
        if (sendFile("factory.cfg", sourceAddr, "/data/azc/mod/")) fileCnt++; // 3
        if (sendFile("softdog.bin", sourceAddr, "/data/azc/mod/")) fileCnt++; // 4
        if (sendFile("adb_credentials", sourceAddr, "/var/run/")) fileCnt++;  // 5
        System.out.println(fileCnt + " out of " + 5 + " main files sent successfully");
        if (fileCnt != 5){
            serialPort.closePort();
            System.out.println("Not all main files were uploaded.");
            System.out.println("The firmware is stopped.");
            System.out.println("Download LE910C1-EU_25.21.222-B008_CUST_136_perf_TFI.exe and try again.");
            return;
        }

        // Аудиофайлы
        int audCnt = 0;
        if (sendFile("by_ignition.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 1
        if (sendFile("by_ignition_off.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 2
        if (sendFile("by_service.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 3
        if (sendFile("by_sos.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 4
        if (sendFile("confirm_result.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 5
        if (sendFile("critical_error.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 6
        if (sendFile("ecall.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 7
        if (sendFile("end_conditions.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 8
        if (sendFile("end_test.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 9
        if (sendFile("end_test_call.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 10
        if (sendFile("error.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 11
        if (sendFile("md_test.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 12
        if (sendFile("no_conditions.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 13
        if (sendFile("no_satellites.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 14
        if (sendFile("no_test_call.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 15
        if (sendFile("ok.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 16
        if (sendFile("one_tone.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 17
        if (sendFile("play_phrase.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 18
        if (sendFile("say_phrase.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 19
        if (sendFile("self_test.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 20
        if (sendFile("start_test.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 21
        if (sendFile("stop_test.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 22
        if (sendFile("test_call.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 23
        if (sendFile("two_tones.wav", sourceAddr + "/audio_files/", "/data/aplay/")) audCnt++; // 24
        System.out.println(audCnt + " out of " + 24 + " audio files sent successfully");
        if (audCnt != 24){
            serialPort.closePort();
            System.out.println("Not all audio files were uploaded.");
            System.out.println("The firmware is stopped.");
            System.out.println("Download LE910C1-EU_25.21.222-B008_CUST_136_perf_TFI.exe and try again.");
            return;
        }

        // acdb файлы
        int acdbCnt = 0;
        if (sendFile("Bluetooth_cal.acdb", sourceAddr + "/dsp_files/", "/data/acdb/acdbdata/")) acdbCnt++;
        if (sendFile("General_cal.acdb", sourceAddr + "/dsp_files/", "/data/acdb/acdbdata/")) acdbCnt++;
        if (sendFile("Global_cal.acdb", sourceAddr + "/dsp_files/", "/data/acdb/acdbdata/")) acdbCnt++;
        if (sendFile("Handset_cal.acdb", sourceAddr + "/dsp_files/", "/data/acdb/acdbdata/")) acdbCnt++;
        if (sendFile("Hdmi_cal.acdb", sourceAddr + "/dsp_files/", "/data/acdb/acdbdata/")) acdbCnt++;
        if (sendFile("Headset_cal.acdb", sourceAddr + "/dsp_files/", "/data/acdb/acdbdata/")) acdbCnt++;
        if (sendFile("Speaker_cal.acdb", sourceAddr + "/dsp_files/", "/data/acdb/acdbdata/")) acdbCnt++;
        System.out.println(acdbCnt + " out of " + 7 + " ACDB files sent successfully");
        if (acdbCnt != 7){
            serialPort.closePort();
            System.out.println("Not all ACDB files were uploaded.");
            System.out.println("The firmware is stopped.");
            System.out.println("Download LE910C1-EU_25.21.222-B008_CUST_136_perf_TFI.exe and try again.");
            return;
        }

        if(sendAtCmd("AT#M2MRUN=1,\"hash.bin\"\r"));
        if(sendAtCmd("ATE0\r"));
        if(sendAtCmd("AT+M2M=1\r"));

        serialPort.closePort();
        System.out.println("Firmware completed successfully");
        System.out.println("Port: COM" + port + " was closed");
    }
}
