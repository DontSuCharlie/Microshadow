package hackillinois2016.microshadow;

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
public class FindDrones implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {
    private List<String>
    private static final String TAG = MainActivity.class.getSimpleName();
    private ARDiscoveryServicesDevicesListUpdatedReceiver receiver;//some long ass variable
    private ARDiscoveryService discoveryService;//an object for discovering the AR drone
    private ServiceConnection discoveryServiceConnection;//an object representing the connection between the AR drone and the thin

    //if there is no service, we start discovery service for some reason
    private void startDiscovery()
    {
        if(discoveryService != null)
            discoveryService.start();
    }


    private void initDiscoveryService()
    {
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
            }
        }
        if(discoveryService == null)
        {
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, discoveryServiceConnection, Context.BIND_AUTO_CREATE);
        }
        else
            startDiscovery();
    }

    private void registerReceivers()
    {
        receiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.registerReceiver(receiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    @Override
    public void onServicesDevicesListUpdated()
    {
        Log.d(TAG, "onServicesDevicesListUpdated...");
        if(discoveryService!=null)
        {
            List<ARDiscoveryDeviceService> deviceList = discoveryService.getDeviceServicesArray();
            //do what I want with the list? What's the list event of?
        }
    }

    //once I have the ARService, I need to transform it into an ARDiscoveryDevice
    private ARDiscoveryDevice createDiscoveryDevice(ARDiscoveryDeviceService service)
    {
        ARDiscoveryDevice device = null;
        if ((service != null) && (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.equals(ARDiscoveryService.getProductFromProductID(service.getProductID()))))
        {

        }

    }

}
