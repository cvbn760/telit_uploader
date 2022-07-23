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
            if (data.contains("OK") | data.contains(">>>")) {
                System.out.println(data + "\r\n");
                return true;
            }
            if (data.contains("ERROR") | data.contains("NO CARRIER") ) {
                System.out.println(data + "\r\n");
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

    public static boolean hashBinUpload(String sourceAddr) throws SerialPortException, IOException, InterruptedException {         // разрешение на запуск hash.bin
        if (!sendFile("hash.bin", sourceAddr, "/data/azc/mod/")) return false; // загружаем hash.bin
        silentCmd();
        if (!sendAtCmd("AT#M2MRUN=1,\"hash.bin\"\r")) return false;                           // разрешение на запуск hash.bin
        silentCmd();
        if (!sendAtCmd("AT+M2M=1\r")) return false;                                             // перезагрузка

        String data = "";
        // Ожидаем пока перезагрузится
        System.out.println("Waiting reboot module...");
        serialPort.closePort();
        Thread.sleep(30000);
        System.out.println(1);
        int cnt = 0;
        down:
        while (true) end:{
            Thread.sleep(500);
            try {
                openConnection(port);
//                if (!serialPort.openPort()) break end;
                data = serialPort.readString();
                if (data == null) data = "";
                System.out.println(data);
                if (data.contains("hash.bin_started")){
                    if (data.contains("crc32=811B90A2")){
                        System.out.println(data);
                        return true;
                    }
                }

                if (cnt >= 600) {
                    deleteFileModen("hash.bin", "/data/azc/mod/");
                }
                cnt++;
                serialPort.closePort();
            }
            catch (Exception exc){
                break end;
            }
        }
    }


    private static String port = "";
    private static String sourceAddr = "";

    public static void main(String[] args) throws SerialPortException, InterruptedException, IOException {

//        String port = args[0];
//        String sourceAddr = args[1];
        port = "29";
        sourceAddr = "C:/8450110539_DSP/";

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
//        int cmd = 0;
//        if(sendAtCmd("AT$GPSP=0\r")) cmd++;           // 1
//        if(sendAtCmd("AT$GPSSAV\r")) cmd++;           // 2
//        if(sendAtCmd("AT#V24CFG=3,1,1\r")) cmd++;     // 3
//        if(sendAtCmd("AT#V24=3,1\r")) cmd++;          // 4
//        if(sendAtCmd("AT#DIALMODE=2\r")) cmd++;       // 5
//        if(sendAtCmd("AT#ECONLY=2\r")) cmd++;         // 6
//        if(sendAtCmd("AT#VAUX=1,1\r")) cmd++;         // 7
//        if(sendAtCmd("AT#V24CFG=0,2,1\r")) cmd++;     // 8
//        if(sendAtCmd("AT#V24CFG=2,2,1\r")) cmd++;     // 9
//        if(sendAtCmd("AT#V24CFG=4,2,1\r")) cmd++;     // 10
//        if(sendAtCmd("AT#SIMDET=1\r")) cmd++;         // 11
//        if(sendAtCmd("AT&W\r")) cmd++;                // 12
//        if(sendAtCmd("AT&P\r")) cmd++;                // 13
//        if(sendAtCmd("AT#ECALLNWTMR=120,2\r")) cmd++; // 14
//        System.out.println(cmd + " out of " + 14 + " commands sent successfully");
//        if (cmd != 14){
//            serialPort.closePort();
//            System.out.println("Not all AT commands were executed.");
//            System.out.println("The firmware is stopped.");
//            System.out.println("Download LE910C1-EU_25.21.222-B008_CUST_136_perf_TFI.exe and try again.");
//            return;
//        }

        // Загрузка hash.bin
        int hashCnt = 0;
        while (!hashBinUpload(sourceAddr)){
            hashCnt++;
            if (hashCnt == 5) {
                serialPort.closePort();
                System.out.println("The firmware is stopped.");
                System.out.println("Download LE910C1-EU_25.21.222-B008_CUST_136_perf_TFI.exe and try again.");
                return;
            }
        }


//         Загрузка файлов
//        if (!sendFile("era.bin", sourceAddr, "/data/azc/mod/")) return;
//        if (!sendFile("factory.cfg", sourceAddr, "/data/azc/mod/")) return;
//        if (!sendFile("softdog.bin", sourceAddr, "/data/azc/mod/")) return;
//        if (!sendFile("adb_credentials", sourceAddr, "/var/run/")) return;


        serialPort.closePort();
        System.out.println("Firmware completed successfully");
        System.out.println("Port: COM" + port + " was closed");
    }
}
