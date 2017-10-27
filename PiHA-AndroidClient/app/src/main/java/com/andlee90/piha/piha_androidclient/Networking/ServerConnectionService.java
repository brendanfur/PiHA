package com.andlee90.piha.piha_androidclient.Networking;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.andlee90.piha.piha_androidclient.Database.ServerItem;
import com.andlee90.piha.piha_androidclient.UI.Controls.MainActivity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;

import CommandObjects.Command;
import DeviceObjects.Device;
import DeviceObjects.DeviceList;
import UserObjects.User;

/**
 * Manages all network activities via a series of AsyncTasks.
 */
public class ServerConnectionService extends Service
{
    // Map a server's id to its object streams
    private volatile static Hashtable<Integer, ObjectOutputStream> sOutputTable = new Hashtable<>();
    private volatile static Hashtable<Integer, ObjectInputStream> sInputTable = new Hashtable<>();

    // Binds this service to an activity
    private final IBinder mBinder = new ServerConnectionBinder();

    public class ServerConnectionBinder extends Binder
    {
        public ServerConnectionService getService()
        {
            return ServerConnectionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Execution method for connecting to a server and receiving it's devices.
     */
    public void establishConnection(ServerItem server) throws ExecutionException, InterruptedException
    {
        new EstablishConnectionTask(server).execute();
    }

    /**
     * Execution method for issuing a command to a specific device and receiving changes made to it
     * by it's hosting server.
     */
    public void issueCommand(Device device, Command command) throws ExecutionException, InterruptedException
    {
        new IssueCommandTask(device, command).execute();
    }

    /**
     * Background task for connecting to a server and receiving it's devices.
     *
     * Performs the following actions:
     *
     * 1) Establish socket connection to server.
     * 2) Create object streams and store them in the corresponding table.
     * 3) Write user to server and receive updated user. Send user broadcast.
     * 4) If authenticated, continue.
     * 5) Write empty device list and receive back server's devices. Send device list broadcast.
     */
    private class EstablishConnectionTask extends AsyncTask<Void, Void, Void>
    {
        private ServerItem server;

        EstablishConnectionTask(ServerItem s)
        {
            this.server = s;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            User user = new User(0, server.getUsername(), server.getPassword(), "", "", "", "");
            DeviceList devices = new DeviceList();

            try
            {
                Socket socket = new Socket(server.getAddress(), server.getPort());

                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                sOutputTable.put(server.getId(), outputStream);

                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                sInputTable.put(server.getId(), inputStream);

                outputStream.writeObject(user);
                user = (User) inputStream.readObject();

                Intent userIntent = new Intent(MainActivity.RECEIVE_USER);
                userIntent.putExtra("user", user);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(userIntent);

                if(user.getUserId() != 0)
                {
                    outputStream.writeObject(devices);
                    devices = (DeviceList) inputStream.readObject();

                    for(Device device: devices.getDevices())
                    {
                        device.setHostServerId(server.getId());
                    }

                    Intent devicesIntent = new Intent(MainActivity.RECEIVE_DEVICE_LIST);
                    devicesIntent.putExtra("devices", devices);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(devicesIntent);
                }
            }
            catch (IOException | ClassNotFoundException e)
            {
                e.printStackTrace();
            }

            return null;
        }
    }

    /**
     * Background task for issuing a command to a specific device and receiving changes made to it
     * by it's hosting server.
     *
     * Performs the following actions:
     *
     * 1) Get object streams from the corresponding table.
     * 2) Write device to server to initialize the device's controller.
     * 3) Write command to server and receive back updated device. Send device broadcast.
     */
    private class IssueCommandTask extends AsyncTask<Void, Void, Void>
    {
        Device device;
        Command command;

        IssueCommandTask(Device device, Command command)
        {
            this.device = device;
            this.command = command;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                ObjectOutputStream outputStream = sOutputTable.get(device.getHostServerId());
                ObjectInputStream inputStream = sInputTable.get(device.getHostServerId());

                outputStream.writeObject(device);
                outputStream.writeObject(command);
                device = (Device) inputStream.readObject();

                Intent deviceIntent = new Intent(MainActivity.RECEIVE_DEVICE);
                deviceIntent.putExtra("device", device);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(deviceIntent);
            }
            catch (IOException | ClassNotFoundException e)
            {
                e.printStackTrace();
            }

            return null;
        }
    }
}
