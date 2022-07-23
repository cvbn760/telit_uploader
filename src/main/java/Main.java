import jssc.*;

import java.io.*;

public class Main {
    private static SerialPort serialPort;
    private static boolean currentTask = false;
    private static InputStream is = null;
    private static OutputStream os = null;

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
        serialPort.writeString(cmd + "\r");
        System.out.println(cmd + "\r");
        Thread.sleep(500);
        String data = serialPort.readString();
        if(data.contains("OK")){
            System.out.println("OK");
            return true;
        }else {
            System.out.println("ERROR");
            return false;
        }
    }

    private static void sendFile(String source, String destenation) throws IOException, SerialPortException, InterruptedException {
        serialPort.writeString(destenation + "\r");
        System.out.println(destenation + "\r");
        Thread.sleep(500);
        String data = serialPort.readString();
        System.out.println(data);
        if(data.contains(">>>")){
//            System.out.println(">>>");
        }else {
//            System.out.println("ERROR");
            return;
        }

        try {
            is = new FileInputStream(source);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                serialPort.writeBytes(buffer);
            }
            Thread.sleep(500);
            data = serialPort.readString();
            System.out.println(data);
        } finally {
            is.close();
        }
    }

    public static void main(String[] args) throws SerialPortException, InterruptedException, IOException {

//        String port = args[0];
        String port = "29";

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
            System.out.println("COM" + port + " was opened\r\n");
        }

        // Загрузка АТ команд
        if(!sendAtCmd("AT$GPSP=1")) return;
        if(!sendAtCmd("AT$GPSSAV")) return;
        if(!sendAtCmd("AT#V24CFG=3,1,1")) return;
        if(!sendAtCmd("AT#V24=3,1")) return;
        if(!sendAtCmd("AT#DIALMODE=2")) return;
        if(!sendAtCmd("AT#ECONLY=2")) return;
        if(!sendAtCmd("AT#VAUX=1,1")) return;
        if(!sendAtCmd("AT#V24CFG=0,2,1")) return;
        if(!sendAtCmd("AT#V24CFG=2,2,1")) return;
        if(!sendAtCmd("AT#V24CFG=4,2,1")) return;
        if(!sendAtCmd("AT#SIMDET=1")) return;
        if(!sendAtCmd("AT&W")) return;
        if(!sendAtCmd("AT&P")) return;
        if(!sendAtCmd("AT#ECALLNWTMR=120,2")) return;
        System.out.println("All cmds upload");

//         Загрузка файлов
        sendFile("C:/8450110539_DSP/era.bin", "\r\nAT#M2MWRITE=\"/data/azc/mod/era.bin\",572458");
        Thread.sleep(500);
        sendFile("C:/8450110539_DSP/factory.cfg", "\r\nAT#M2MWRITE=\"/data/azc/mod/factory.cfg\",46");
        Thread.sleep(500);
        sendFile("C:/8450110539_DSP/softdog.bin", "\r\nAT#M2MWRITE=\"/data/azc/mod/softdog.bin\",13096");
        Thread.sleep(500);
        sendFile("C:/8450110539_DSP/adb_credentials", "\r\nAT#M2MWRITE=\"/var/run/adb_credentials\",18");
        Thread.sleep(500);

    }
}
