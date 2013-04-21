/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Ticker;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 *
 * @author user
 */

public class client_Midlet extends MIDlet implements CommandListener, DiscoveryListener, Runnable {
    LocalDevice Device = null;
    // Keep the discovery agent reference.
    private DiscoveryAgent agent;
    private Display display = null;
    //Collects the remote devices and services found during a search.
    private Vector devicesfound = null;
    private Vector servicesfound = null;
    //list for devices and services.
    private List device_list = null;
    private List service_list = null;
    private List info_list = new List("", List.IMPLICIT);
    private ServiceRecord services[];
    private ServiceRecord servrec;
    private RemoteDevice[] devices = null;
    private StreamConnection connection = null;
    private Thread connect = null;
    DataOutputStream out = null;
    DataInputStream in = null;
    // Soft button for exiting the demo
    private Command EXIT_CMD = new Command("Exit", Command.EXIT, 1);
    // Soft button for searching devices and services.
    private Command SEARCH_CMD = new Command("Search", Command.SCREEN, 2);
    // Soft button for searching devices and services.
    private Command BROWSE_CMD = new Command("Browse", Command.SCREEN, 2);
    // Soft buttons.....
    private Command BACK_CMD = new Command("Back", Command.BACK, 1);
    private Command CANCEL_CMD = new Command("Cancel",Command.CANCEL,1);
    //menu number...
    int currentmenu = 0;
    //////////////
    //int serviceSearch = 0;  //to track if any search is going on.
    boolean devicesearch = false;
    boolean servicesearch = false;

    protected void startApp() throws MIDletStateChangeException {
        display = Display.getDisplay(this);
        Alert alt = null;
        currentmenu = 1;
        try {
            //create a local device and discovery agent
            Device = LocalDevice.getLocalDevice();
            //Device.setDiscoverable(DiscoveryAgent.GIAC);
            agent = Device.getDiscoveryAgent();
        } catch (Exception e) {
            System.err.println("Can't initialize bluetooth: " + e);
            alt = new Alert("Bluetooth Error:","",null,AlertType.ERROR);
            alt.setTimeout(Alert.FOREVER);
            alt.addCommand(EXIT_CMD);
            alt.setCommandListener(this);
        }
        try {
            if(!agent.startInquiry(DiscoveryAgent.GIAC,this)) {
                System.err.println("Inquiry not started: ");
            }
            }catch(BluetoothStateException bse) {
                System.err.println(bse);
        }
        mainmenu(alt);

    }

    protected void pauseApp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        clear();
        try {
            if (connect != null){
                connect.join();
            }
        }catch(InterruptedException ex){
            ex.printStackTrace();
        }
        notifyDestroyed();
    }

    public void commandAction(Command c, Displayable d) {
        if(c == EXIT_CMD){
            try {
                destroyApp(true);
            } catch (MIDletStateChangeException ex) {
                ex.printStackTrace();
            }
        }
        else if(c == SEARCH_CMD){
            if(currentmenu == 1 && devicesearch == false){
                startdevicesearch();
            }
        }
        else if(c == CANCEL_CMD){
            if(devicesearch)
            {
                devicesearch = !agent.cancelInquiry(this);
            }
        }
        else if(c == BROWSE_CMD){
            //browse_device();
        }
        else if(c == List.SELECT_COMMAND){
            List list = (List) display.getCurrent();
            int index = list.getSelectedIndex();
            if(currentmenu == 2 || currentmenu == 1){
                if(!devicesfound.isEmpty() && !servicesearch)
                    startservicesearch((RemoteDevice)devicesfound.elementAt(index));
            }
            if(currentmenu == 3){
                if(!servicesfound.isEmpty() && !servicesearch)
                    connect();
            }
        }
        else if(c == BACK_CMD){
            if(currentmenu == 2 || currentmenu == 3){
                mainmenu(null);
            }
            if(currentmenu == 4){
                clear();
                showservices(null);
            }
        }
    }

    public void connect(){
        connect = new Thread(this);
        connect.start();
    }

    public void run(){
        Alert alt = new Alert("Error:", "", null, AlertType.ERROR);
        alt.setTimeout(Alert.FOREVER);
        String url = servrec.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
        try{
            connection = (StreamConnection)Connector.open(url);
        }catch(IOException ioe){
            alt.setString("Cannot create connection...");
            mainmenu(alt);
            clear();
            return;
        }
        RemoteDevice rdevice = servrec.getHostDevice();
        String name, add;
        try{
            name = rdevice.getFriendlyName(false);
            add = rdevice.getBluetoothAddress();
        }catch(IOException ioe){
            name = "Unknown";
            add = "Unknown";
        }
        connectedto(name, add);
        //open streams for connection...
        try{
            out = connection.openDataOutputStream();
            in = connection.openDataInputStream();
        }catch(IOException ioe){
            alt.setString("Error in connection streams.");
            mainmenu(alt);
            clear();
            return;
        }
    }

    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) {
        devicesfound.addElement(remoteDevice);
        device_list.append(remoteDevice.getBluetoothAddress(), null);
    }

    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        services= servRecord;
        servrec = servRecord[0];
        DataElement dataelement = null;
        synchronized(servicesfound){
            for(int i =0; i <services.length; i++){
                dataelement = services[i].getAttributeValue(0x100);
                servicesfound.addElement(services[i]);
                service_list.append((String) dataelement.getValue(), null);
            }
        }
    }

    public void serviceSearchCompleted(int transID, int respCode) {
        servicesearch = false;
        Alert alt = new Alert("Service search status...","", null, AlertType.INFO);
        alt.setTimeout(750);
        switch(respCode) {
            case DiscoveryListener.SERVICE_SEARCH_COMPLETED:
                alt.setString("Search Completed...");
                if(!servicesfound.isEmpty()){
                    System.err.print("Show services\n");
                    showservices(alt);
                }
                else{
                    alt.setString("No services found...");
                    showdevices(alt);
                }
                break;
            case DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
                alt.setString("Device not reachable...");
                showdevices(alt);
                break;
            case DiscoveryListener.SERVICE_SEARCH_ERROR:
                alt.setType(AlertType.ERROR);
                alt.setTitle("Error in search...");
                showdevices(alt);
                break;
            case DiscoveryListener.SERVICE_SEARCH_NO_RECORDS:
                alt.setString("No service records found...");
                showdevices(alt);
                break;
            case DiscoveryListener.SERVICE_SEARCH_TERMINATED:
                alt.setString("You terminated the search...");
                if(!servicesfound.isEmpty())
                    showservices(alt);
                else{
                    alt.setString("No services found...");
                    showdevices(alt);
                }
                break;
        }
    }

    public void inquiryCompleted(int discType) {
        devicesearch = false;
        Alert alt = new Alert("Device Inquiry status","", null, AlertType.INFO);
        alt.setTimeout(500);
        switch (discType) {
            case DiscoveryListener.INQUIRY_COMPLETED:   //Inquiry completed normally
                alt.setString("Search Completed...");
                if(devicesfound.size() > 0){
                    get_names();
                    alt.setString(devicesfound.size()+ "devices found...");
                }
                else if(devicesfound.isEmpty()){
                    device_list.append("EMPTY...", null);
                }
                showdevices(alt);
                break;
            case DiscoveryListener.INQUIRY_ERROR:
                alt.setType(AlertType.ERROR);
                if(devicesfound.isEmpty()){
                    alt.setString("Error in search...");
                    mainmenu(alt);
                }
                else{
                    alt.setString("ERROR..., but" + devicesfound.size() + "devices found...");
                    get_names();
                    showdevices(alt);
                }
                break;
            case DiscoveryListener.INQUIRY_TERMINATED:
                alt.setString("Search terminated");
                if(devicesfound.size() > 0){
                    get_names();
                    showdevices(alt);
                }
                else if(devicesfound.isEmpty()){
                    device_list.append("Empty", null);
                    showdevices(alt);
                }
                break;
        }
    }

    //Search for the services provided...
    private void startservicesearch(RemoteDevice rdevice){
        servicesearch = true;
        Alert alt;
        servicesfound = new Vector();
        service_list = new List("", List.IMPLICIT);
        try{
            service_list.setTitle(rdevice.getFriendlyName(false));
        }catch(Exception e){
            service_list.setTitle(rdevice.getBluetoothAddress());
        }
        //attributes for service_name, servicedescription, providername.
        int attributes[] = {0x100,0x101,0x102};
        UUID[] uuid = new UUID[1];
        //uuid for services that are public browseable.
        uuid[0] = new UUID(0x1002);
        try {
            agent.searchServices(attributes, uuid, rdevice, this);
        }catch (BluetoothStateException e) {
            alt = new Alert("Error in bth service.", "Couldnt initiate service search", null, AlertType.ERROR);
            servicesearch = false;
            showdevices(alt);
        }
        alt = new Alert("Service Status", "Search initiated....", null, AlertType.INFO);
        showservices(alt);
    }

    private void startdevicesearch(){
        Alert alt = new Alert ("Search status", null, null, AlertType.INFO);
        alt.setTimeout(500);
        if(agent == null)
            agent = Device.getDiscoveryAgent();
        // remove older devices from list if any
        devicesfound.removeAllElements();
        device_list = null;
        device_list = new List("Devices Discovered",List.IMPLICIT);
        devicesearch = true;
        try{
            devicesearch = agent.startInquiry(DiscoveryAgent.GIAC, this);    //no pairing required
        }catch(BluetoothStateException bse){
            alt.setType(AlertType.ERROR);
            alt.setString("Error in search initiation...");
            mainmenu(alt);
        }
        if(devicesearch){
            alt.setString("Search Started....");
            showdevices(alt);
        }
        else{
            alt.setType(AlertType.ERROR);
            alt.setString("Error in searching...");
            device_list = null; //no search implies no devices found.
            mainmenu(alt);
        }
    }

    private void get_names(){
        String name = null;
        for(int i =devicesfound.size()-1; i >= 0; i--){
            try{
                name = ((RemoteDevice)devicesfound.elementAt(i)).getFriendlyName(false);
            }catch(IOException ioe){
                continue;   //if no friendlyname then simply continue with bth address.
            }
            if(!name.equals("")){
                device_list.set(i, name, null);
            }
        }
    }

    private void mainmenu(Alert a) {
        List knownDevices = new List("Known Devices",List.IMPLICIT);
        if (devicesfound == null) devicesfound = new Vector();
        if (agent == null) agent = Device.getDiscoveryAgent();
        String name = null;
        // get preknown devices and add them to the Vector.
        devices = agent.retrieveDevices(DiscoveryAgent.CACHED);
        if(devices != null){
            synchronized(devicesfound){
                for(int i = 0; i < devices.length; i++){
                    try{
                        name = devices[i].getFriendlyName(false);
                    }catch(IOException ioe){
                        name = devices[i].getBluetoothAddress();
                    }
                    if(name.equals(""))
                        name = devices[i].getBluetoothAddress();
                    knownDevices.insert(0, name, null);
                }
            }
        }

        if(devicesfound.isEmpty()){
            knownDevices.append("Empty", null); // Displays Empty on the screen to indicate devicelist is empty
        }
        knownDevices.addCommand(EXIT_CMD);
        knownDevices.addCommand(SEARCH_CMD);
        knownDevices.setCommandListener(this);
        if(a == null)
            display.setCurrent(knownDevices);
        else
            display.setCurrent(a, knownDevices);
        currentmenu = 1;
    }

    private void showdevices(Alert a){
        //  if in main menu..
        if(currentmenu == 1 || currentmenu == 2){
            if(devicesearch){
                device_list.setTitle("Searching........");
                device_list.addCommand(CANCEL_CMD);

            }
            else if(!devicesearch){
                device_list.setTitle("Devices Discovered:");
                device_list.removeCommand(CANCEL_CMD);
                device_list.addCommand(EXIT_CMD);
                device_list.addCommand(BACK_CMD);
            }
        }
        device_list.setCommandListener(this);
        if(a == null)
            display.setCurrent(device_list);
        else
            display.setCurrent(a, device_list);
        currentmenu = 2;
    }

    private void showservices(Alert a){
        if(currentmenu == 2 || currentmenu == 1){
            service_list.addCommand(EXIT_CMD);
            service_list.addCommand(BACK_CMD);
            service_list.setCommandListener(this);
        }
        if(a == null)
            display.setCurrent(service_list);
        else
            display.setCurrent(a, service_list);
       currentmenu = 3;
    }

    private void connectedto(String name, String add){
        Ticker tic = new Ticker("Waiting for the Servicelist from "+name);
        info_list.setTitle("Connected To:");
        info_list.setTicker(tic);
        info_list.append("Name: "+name, null);
        info_list.append("Address: "+add, null);
        info_list.addCommand(BACK_CMD);
        info_list.setCommandListener(this);
        display.setCurrent(info_list);
        currentmenu = 4;
    }

    private void browse_device(){
        
    }

    private void clear() {
        try{
            if(out != null){
                out.close();
            }
            if(in != null)
                in.close();
            if(connection != null)
                connection.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}