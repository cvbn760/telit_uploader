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
            serialPort.setParams(SerialPort.BAUDRATE_115200,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            //Включаем аппаратное управление потоком
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
                    SerialPort.FLOWCONTROL_RTSCTS_OUT);
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
            return false;
        }
        return true;
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
            if (data.contains("ERROR")) {
                System.out.println(data + "\r\n");
                return false;
            }
        }
    }

    private boolean deleteFile(String fileName, String destenation){
        // Составляем команду для удаления файла
        StringBuffer sb = new StringBuffer();

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
        if(!sendAtCmd(sb.toString())) return false;

        sb.delete(0, sb.length());
        // Передаем файл
        try {
            is = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                serialPort.writeBytes(buffer);
            }
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
                    return true;
                }
                if (data.contains("ERROR")) {
                    System.out.println(data + "\r\n");
                    is.close();
                    return false;
                }
            }
        }catch (Exception exc){
            is.close();
            exc.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) throws SerialPortException, InterruptedException, IOException {

//        String port = args[0];
//        String sourceAddr = args[1];
        String port = "29";
        String sourceAddr = "C:/8450110539_DSP/";

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
        if(!sendAtCmd("AT$GPSP=1\r"));
        if(!sendAtCmd("AT$GPSSAV\r")) return;
        if(!sendAtCmd("AT#V24CFG=3,1,1\r")) return;
        if(!sendAtCmd("AT#V24=3,1\r")) return;
        if(!sendAtCmd("AT#DIALMODE=2\r")) return;
        if(!sendAtCmd("AT#ECONLY=2\r")) return;
        if(!sendAtCmd("AT#VAUX=1,1\r")) return;
        if(!sendAtCmd("AT#V24CFG=0,2,1\r")) return;
        if(!sendAtCmd("AT#V24CFG=2,2,1\r")) return;
        if(!sendAtCmd("AT#V24CFG=4,2,1\r")) return;
        if(!sendAtCmd("AT#SIMDET=1\r")) return;
        if(!sendAtCmd("AT&W\r")) return;
        if(!sendAtCmd("AT&P\r")) return;
        if(!sendAtCmd("AT#ECALLNWTMR=120,2\r")) return;
        System.out.println("All cmds upload");

//         Загрузка файлов
        if (!sendFile("era.bin", sourceAddr, "/data/azc/mod/")) return;
        if (!sendFile("factory.cfg", sourceAddr, "/data/azc/mod/")) return;
        if (!sendFile("softdog.bin", sourceAddr, "/data/azc/mod/")) return;
        if (!sendFile("adb_credentials", sourceAddr, "/var/run/")) return;


        serialPort.closePort();
        System.out.println("Port: COM" + port + " was closed");
        System.out.println("Firmware completed successfully");
    }
}
