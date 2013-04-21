package server;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Ticker;
import javax.microedition.midlet.*;

/**
 * @author user
 */
public class server_Midlet extends MIDlet implements Runnable, CommandListener{
    Display display = null;
    LocalDevice device = null;
    RemoteDevice rdevice = null;
    StreamConnectionNotifier server = null;
    StreamConnection connection = null;
    InputStream in;
    OutputStream out;
    List list = new List("JBA",List.IMPLICIT);
    List show = new List("Connected to:",List.IMPLICIT);
    Thread server_thread = null;
    ServiceRecord servrec;// = null;
    String url;
    UUID uuid = null;
    DataElement dataelement = null;
    Canvas can;
    //Commands...
    Command EXIT_CMD = new Command("Exit", Command.EXIT, 1);
    Command START_CMD = new Command("Start", Command.SCREEN, 2);
    Command OK_CMD = new Command("OK", Command.OK, 1);
    Command SEND_CMD = new Command("Send", Command.SCREEN, 2);

    boolean ready = false;    //state of server.
    //int menu = 0;


    public void startApp() {
        display = Display.getDisplay(this);
        //mainmenu(null);
        readytheserver();
        if(ready == true)
            startserver();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional){
        clear();
        if(server_thread != null) //server_thread.isAlive())
        {
            try{
                server_thread.join();
            }catch(InterruptedException ex){
                ex.printStackTrace();
            }
        }
        server_thread = null;
        notifyDestroyed();
        System.err.print("asdaf");
    }

    public void run() {
        Alert alt = new Alert("Error","", null, AlertType.ERROR);
        Alert alt1 = new Alert("Connected To:","", null, AlertType.INFO);
        alt1.setTimeout(1500);
        Ticker ticker = new Ticker("Waiting for the client to connect...");
        list.setTicker(ticker);
        display.setCurrent(list);
        try {
            //set the notifier(server) to accept the client connections.
            connection = server.acceptAndOpen();
        } catch (IOException ex) {
            alt.setString("Error opening the server");
            display.setCurrent(alt, list);
        }
        String bthname, bthadd;
        try {
            rdevice = RemoteDevice.getRemoteDevice(connection);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            bthname = rdevice.getFriendlyName(false);
        } catch (IOException ex) {
            bthname = "******";
        }
        try{
            bthadd = rdevice.getBluetoothAddress();
        }catch(Exception e){
            bthadd = rdevice.getBluetoothAddress();
        }
        alt1.setString(bthname + "(bth address:" + bthadd + ")");
        show.append("Name: "+bthname, null);
        show.append("Address: "+bthadd, null);
        show.addCommand(OK_CMD);
        show.setCommandListener(this);
        display.setCurrent(alt1,show);
        try{
            in = connection.openInputStream();
            out = connection.openOutputStream();
        }catch(IOException ioe){
            alt.setString("Error in opening in/out stream...");

            display.setCurrent(alt, list);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if(c == EXIT_CMD){
            destroyApp(true);
        }

        if(c == OK_CMD){
            connectedto();
        }
    }

    public void startserver(){
        list.addCommand(EXIT_CMD);
        list.setCommandListener(this);
        display.setCurrent(list);
        server_thread = new Thread(this);
        server_thread.start();
    }

    public void readytheserver(){
        ready = true;
        servrec = null;
        Alert alt = new Alert("Error:", "", null, AlertType.ERROR);
        alt.setTimeout(Alert.FOREVER);
        try{
            device = LocalDevice.getLocalDevice();
            device.setDiscoverable(DiscoveryAgent.GIAC);
        }catch(BluetoothStateException bse){
            alt.setString("Error in Starting device...");
            display.setCurrent(alt, list);
            bse.printStackTrace();
            ready = false;
            return;
        }
        uuid = new UUID("f3d68400b4c711d880a2000bdb544cb1", false);
        url = "btspp://localhost:"+uuid+";name=JBA server;authorize=false";
        try {
            server = (StreamConnectionNotifier) Connector.open(url);
        }catch(IOException ex){
            ex.printStackTrace();
            ready = false;
            return;
        }
        servrec = device.getRecord(server); //get the service record from server
        //set the attributes of the service record...
        //make service public browseable.
        dataelement = new DataElement(DataElement.DATSEQ);      //data element sequence.
        dataelement.addElement(new DataElement(DataElement.UUID, new UUID(0x1002)));
        servrec.setAttributeValue(0x0005, dataelement);
        //set description for the service
        dataelement = new DataElement(DataElement.STRING, "JBA Service");
        servrec.setAttributeValue(0x101, dataelement);
        //set name for the service
        dataelement = new DataElement(DataElement.STRING, "JBA Server");
        servrec.setAttributeValue(0x102, dataelement);
        //set service availability
        dataelement = new DataElement(DataElement.U_INT_4,255);
        servrec.setAttributeValue(0x0008, dataelement);
        try {
            device.updateRecord(servrec);
        } catch (ServiceRegistrationException ex) {
            ex.printStackTrace();
            ready = false;
            return;
        }

    }

    public void connectedto(){
        show.setTitle("Send Servicelist To:");
        show.removeCommand(OK_CMD);
        show.addCommand(EXIT_CMD);
        show.addCommand(SEND_CMD);
        show.setCommandListener(this);
        display.setCurrent(show);
    }

    public void send_servicelist(){

    }

    public void clear(){
        if(connection != null){
            try {
                connection.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if(server != null){
            try {
                server.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try{
            if(in != null)
                in.close();
            if(out != null)
                out.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}