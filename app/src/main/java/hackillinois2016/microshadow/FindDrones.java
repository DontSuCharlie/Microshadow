package hackillinois2016.microshadow;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.List;

/*
 * Created by drago (Charlie Su) on 2/20/2016.
 * This class abstracts away the messy af code needed to find drones in the local vicinity
 * What this class ultimately returns is a Device object, which represents the drone you want to control.
 */

/*
 * HOW TO USE
 * ARDiscoveryDevice device = FindDrones.findDrones();
 *
 */

/*
    How this code works:
    Our goal is to connect with a Parrot AR Drone.
    1) They have provided something called the ARDiscoveryService, which will call onServicesDevicesListUpdated() whenever a drone has been detected
    2) To use the ARDiscoveryService, we have to initialize a few things
    3)

    so
    1) initialize the discovery service
    2) start the discovery service
    3) register the callback function
    4) decide what to do with the callback
    5) now that you have an object representing the connection between the app and drone, you turn it into a device object
    6) clean up
*/
public class FindDrones extends Activity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {
    private boolean droneDiscovered = false;
    private ARDiscoveryService discoveryService;//an object for discovering the AR drone
    private ServiceConnection discoveryServiceConnection;//an object representing the connection between the AR drone and the this application
    private ARDiscoveryDeviceService deviceService;//an object that will be converted into a device
    private static final String TAG = MainActivity.class.getSimpleName();
    private ARDiscoveryServicesDevicesListUpdatedReceiver receiver;//some long ass variable


    //initializes the discoveryServiceConnection and discoveryService objects
    private void initDiscoveryService()
    {//we need to establish a connection first to start discovery service for some reason
        //if the discoveryServiceConnection was null, we do the following
        if(discoveryServiceConnection == null)
        {
            discoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    discoveryService = ((ARDiscoveryService.LocalBinder) service).getService();
                    startDiscovery();
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {
                    discoveryService = null;
                }
            };
        }
        //if it was discoveryService that was null, we do the following
        if(discoveryService == null)
        {
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, discoveryServiceConnection, Context.BIND_AUTO_CREATE);
        }
        else
            startDiscovery();
    }

    //once the discoveryService has been initialized, we start discovery (looking for drones)
    private void startDiscovery()
    {
        if(discoveryService != null)
            discoveryService.start();
    }

    //we want to register the callback function
    private void registerReceivers()
    {
        receiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.registerReceiver(receiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    //now we want to implement the callback function
    @Override
    public void onServicesDevicesListUpdated()
    {
       // Log.d(TAG, "onServicesDevicesListUpdated...");
        if(discoveryService!=null)
        {
            List<ARDiscoveryDeviceService> deviceList = discoveryService.getDeviceServicesArray();
            droneDiscovered = true;
            //if it turned out that we didn't really discover anything, we say droneDiscovered = false again
            //otherwise we set deviceSErvice as the first element of the list
            if(deviceList.size() > 0)
                deviceService = deviceList.get(0);
            else
                droneDiscovered = false;
        }
    }

    //once I have the ARService, I need to transform it into an ARDiscoveryDevice
    private ARDiscoveryDevice createDiscoveryDevice(ARDiscoveryDeviceService service)
    {
        ARDiscoveryDevice device = null;
        if ((service != null) && (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.equals(ARDiscoveryService.getProductFromProductID(service.getProductID()))))
        {
            try
            {
                device = new ARDiscoveryDevice();
                ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();
                device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        return device;
    }

    private void unregisterReceivers()
    {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.unregisterReceiver(receiver);
    }

    private void closeServices()
    {
        if(discoveryService!=null)
        {
            new Thread(new Runnable(){
                @Override
                public void run()
                {
                    discoveryService.stop();
                    getApplicationContext().unbindService(discoveryServiceConnection);
                    discoveryService = null;
                }
            }).start();
        }
    }
    public ARDiscoveryDevice findDrone()
    {
        registerReceivers();
        initDiscoveryService();
        while(!droneDiscovered)
        {
            try
            {
                Thread.sleep(100);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        unregisterReceivers();
        closeServices();
        return createDiscoveryDevice(deviceService);
    }
}