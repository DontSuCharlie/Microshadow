package hackillinois2016.microshadow;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;
import com.parrot.arsdk.ARSDK;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {

    //lol
    private BandClient client = null;
    private Button btnConnect;
    private Button btnLaunch;
    private TextView txtStatus;
    ARDiscoveryDevice device;
    //private FindDrones finder;
    private DroneCommands commander;

    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {
                appendToUI(String.format(" X = %.3f \n Y = %.3f\n Z = %.3f", event.getAccelerationX(),
                        event.getAccelerationY(), event.getAccelerationZ()));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ARSDK.loadSDKLibs();
        txtStatus = (TextView) findViewById(R.id.textStatus);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("");
                new AccelerometerSubscriptionTask().execute();
            }
        });
        this.findDrone();

        btnLaunch = (Button) findViewById(R.id.btnLaunch);
        btnLaunch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                findDrone();
                DroneCommands d = new DroneCommands(device);
                d.takeoff();
            }
        });
        //finder = new FindDrones();
        //commander = new DroneCommands(this.findDrone());
        //commander.takeoff();
    }

    @Override
    protected void onResume() {
        super.onResume();
        txtStatus.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {
            try {
                client.getSensorManager().unregisterAccelerometerEventListener(mAccelerometerEventListener);
            } catch (BandIOException e) {
                appendToUI(e.getMessage());
            }
        }
    }

    private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS128);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(string);
            }
        });
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }



    private boolean droneDiscovered = false;
    private ARDeviceController deviceController;
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
        if(discoveryService != null) {
            System.err.println("Discovery Service started");
            discoveryService.start();
        }
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
            System.out.println("function called");
            List<ARDiscoveryDeviceService> deviceList = discoveryService.getDeviceServicesArray();
            droneDiscovered = true;
            System.out.println("A DRONE HAS BEEN DISCOVERED");
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
    public void findDrone()
    {
        initDiscoveryService();
        registerReceivers();
        //System.out.println("Success! A drone has been found!");
        unregisterReceivers();
        closeServices();
        device = createDiscoveryDevice(deviceService);
        try
        {
            deviceController = new ARDeviceController(device);
        }
        catch (ARControllerException e)
        {
            System.out.println("Device not found.");
        }

    }
}
