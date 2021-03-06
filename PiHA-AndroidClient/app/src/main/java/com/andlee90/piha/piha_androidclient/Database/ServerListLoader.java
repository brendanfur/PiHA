package com.andlee90.piha.piha_androidclient.Database;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class ServerListLoader extends AsyncTaskLoader<List<ServerItem>>
{
    private Context mContext;

    public ServerListLoader(Context context)
    {
        super(context);
        mContext = context;
    }

    @Override
    public List<ServerItem> loadInBackground()
    {
        ServerDbHelper dbServerDbHelper = new ServerDbHelper(mContext);
        Cursor cursor = dbServerDbHelper.getAllServers();

        ArrayList<ServerItem> servers = new ArrayList<>();

        while (cursor.moveToNext())
        {
            servers.add(new ServerItem(cursor.getInt(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("server_name")),
                    cursor.getString(cursor.getColumnIndex("server_address")),
                    cursor.getInt(cursor.getColumnIndex("server_port")),
                    cursor.getString(cursor.getColumnIndex("server_username")),
                    cursor.getString(cursor.getColumnIndex("server_password"))));
        }

        return servers;
    }
}