import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class SerarchEngine {
    private static SerialPort serialPort;

    private static SerarchEngine serarchEngine;

    private SerarchEngine() throws SerialPortException {
        if (serialPort != null){
            serialPort.closePort();
        }
        serialPort = new SerialPort("COM50");
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
        //Устанавливаем ивент лисенер и маску
        serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
        //Отправляем запрос устройству
        serialPort.writeString("AT#M2MLIST");
    }

    public static SerarchEngine getNewInstance() throws SerialPortException {
       if (serarchEngine == null){
           serarchEngine = new SerarchEngine();
       }
       return serarchEngine;
    }

    private static class PortReader implements SerialPortEventListener {

        public void serialEvent(SerialPortEvent event) {
            if(event.isRXCHAR() && event.getEventValue() > 0){
                try {
                    //Получаем ответ от устройства, обрабатываем данные и т.д.
                    String data = serialPort.readString(event.getEventValue());
                    //И снова отправляем запрос
//                    serialPort.writeString("AT#M2MLIST");
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
}
