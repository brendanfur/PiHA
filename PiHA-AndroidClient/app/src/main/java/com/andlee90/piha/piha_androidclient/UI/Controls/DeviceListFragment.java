package com.andlee90.piha.piha_androidclient.UI.Controls;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.andlee90.piha.piha_androidclient.R;
import com.andlee90.piha.piha_androidclient.UI.Controls.ViewHolders.LedViewHolder;
import com.andlee90.piha.piha_androidclient.UI.Controls.ViewHolders.RelayModuleViewHolder;
import com.andlee90.piha.piha_androidclient.UI.Controls.ViewHolders.RgbLedViewHolder;
import com.andlee90.piha.piha_androidclient.UI.Controls.ViewHolders.StepperMotorViewHolder;
import com.andlee90.piha.piha_androidclient.UI.Controls.ViewHolders.ViewHolder;
import com.thebluealliance.spectrum.SpectrumDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import CommandObjects.LedCommand;
import CommandObjects.RgbLedCommand;
import DeviceObjects.Device;
import DeviceObjects.Led;
import DeviceObjects.RelayModule;
import DeviceObjects.RgbLed;
import DeviceObjects.StepperMotor;

public class DeviceListFragment extends ListFragment
{
    private Context mContext;
    private DeviceListArrayAdapter mAdapter;
    private ArrayList<Device> mDevices = new ArrayList<>();

    public static DeviceListFragment newInstance()
    {
        return new DeviceListFragment();
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_device_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new DeviceListArrayAdapter(mContext,
                android.R.layout.simple_list_item_1, mDevices);
        setListAdapter(mAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        RelativeLayout rl = v.findViewById(R.id.hidden_view);

        if(rl.getVisibility() == View.GONE)
        {
            rl.setVisibility(View.VISIBLE);

                for(int i = 0; i < l.getCount(); i++)
                {
                    View otherView = l.getChildAt(i);
                    if(otherView != null && otherView != v && position != i)
                    {
                        RelativeLayout rl2 = otherView.findViewById(R.id.hidden_view);
                        rl2.setVisibility(View.GONE);
                    }
                }
        }
        else
        {
            rl.setVisibility(View.GONE);
        }
    }

    public void setListView(ArrayList<Device> devices)
    {
        mDevices.addAll(devices);
        mAdapter.notifyDataSetChanged();
    }

    private class DeviceListArrayAdapter extends ArrayAdapter<Device>
    {
        private LayoutInflater mInflater;
        private List<Device> devices = null;
        private Map<Integer, View> views = new HashMap<Integer, View>();


        DeviceListArrayAdapter(Context context, int resourceId, List<Device> devices)
        {
            super(context, resourceId, devices);

            this.devices = devices;
            mInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent)
        {
            final ViewHolder viewHolder;
            View view = views.get(position);

            if (view == null) {
                Device device = devices.get(position);

                switch (device.getDeviceType()) {
                    case LED:
                        view = mInflater.inflate(R.layout.list_led_items, null);
                        viewHolder = new LedViewHolder(view);
                        viewHolder.getDeviceName().setText(device.getDeviceName());
                        viewHolder.getServerId().setText("" + device.getHostServerId());

                        if (device.getDeviceMode() == Led.LedMode.OFF)
                            viewHolder.getDeviceSwitch().setChecked(false);
                        else viewHolder.getDeviceSwitch().setChecked(true);

                        viewHolder.getDeviceSwitch().setOnClickListener(view1 -> {
                            try {
                                if (viewHolder.getBlink().isChecked()) {
                                    if (!viewHolder.getDeviceSwitch().isChecked()) {
                                        ((MainActivity) getActivity()).mService.issueCommand(device,
                                                new LedCommand(LedCommand.LedCommandType.TOGGLE));
                                    } else {
                                        ((MainActivity) getActivity()).mService.issueCommand(device,
                                                new LedCommand(LedCommand.LedCommandType.BLINK));
                                    }
                                } else {
                                    ((MainActivity) getActivity()).mService.issueCommand(device,
                                            new LedCommand(LedCommand.LedCommandType.TOGGLE));
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                        views.put(position, view);
                        break;

                    case RGB_LED:
                        view = mInflater.inflate(R.layout.list_rgb_led_items, null);
                        viewHolder = new RgbLedViewHolder(view);
                        viewHolder.getDeviceName().setText(device.getDeviceName());
                        viewHolder.getServerId().setText("" + device.getHostServerId());

                        if (device.getDeviceMode() == RgbLed.RgbLedMode.OFF)
                            viewHolder.getDeviceSwitch().setChecked(false);
                        else viewHolder.getDeviceSwitch().setChecked(true);

                        viewHolder.getDeviceSwitch().setOnClickListener(view1 -> {
                            try {
                                int color = viewHolder.getColorSelectButton().getCurrentTextColor();
                                RgbLedCommand.RgbLedCommandType ct;
                                if (viewHolder.getBlink().isChecked()) {
                                    if (!viewHolder.getDeviceSwitch().isChecked()) {
                                        if(getColorCommand(false, color) != null) {
                                            ct = getColorCommand(false, color);
                                            ((MainActivity) getActivity()).mService.issueCommand(device,
                                                    new RgbLedCommand(ct));
                                        } else {
                                            ((MainActivity) getActivity()).mService.issueCommand(device,
                                                    new RgbLedCommand(RgbLedCommand.RgbLedCommandType.TOGGLE_WHITE));
                                        }
                                    } else {
                                        if(getColorCommand(true, color) != null) {
                                            ct = getColorCommand(true, color);
                                            ((MainActivity) getActivity()).mService.issueCommand(device,
                                                    new RgbLedCommand(ct));
                                        } else {
                                            ((MainActivity) getActivity()).mService.issueCommand(device,
                                                    new RgbLedCommand(RgbLedCommand.RgbLedCommandType.BLINK_WHITE));
                                        }
                                    }
                                } else {
                                    if(getColorCommand(false, color) != null) {
                                        ct = getColorCommand(false, color);
                                        ((MainActivity) getActivity()).mService.issueCommand(device,
                                                new RgbLedCommand(ct));
                                    } else {
                                        ((MainActivity) getActivity()).mService.issueCommand(device,
                                                new RgbLedCommand(RgbLedCommand.RgbLedCommandType.TOGGLE_WHITE));
                                    }
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });

                        viewHolder.getColorSelectButton().setOnClickListener(view12 -> new SpectrumDialog.Builder(getContext())
                                .setColors(R.array.colors_array)
                                .setDismissOnColorSelected(true)
                                .setOutlineWidth(1)
                                .setOnColorSelectedListener((positiveResult, color) -> {
                                    if(positiveResult)
                                    {
                                        viewHolder.getColorSelectButton().setTextColor(color);
                                    }
                                }).build().show(getFragmentManager(), "color_picker"));
                        views.put(position, view);
                        break;

                    case RELAY_MOD:
                        view = mInflater.inflate(R.layout.list_relay_mod_items, null);
                        viewHolder = new RelayModuleViewHolder(view);

                        viewHolder.getDeviceName().setText(device.getDeviceName());
                        viewHolder.getServerId().setText("" + device.getHostServerId());

                        if (device.getDeviceMode() == RelayModule.RelayModuleMode.OFF)
                            viewHolder.getDeviceSwitch().setChecked(false);
                        else viewHolder.getDeviceSwitch().setChecked(true);

                        /*viewHolder.getDeviceSwitch().setOnClickListener(view1 -> {
                            try {
                                if(viewHolder.getBlink().isChecked()) {
                                    if(!viewHolder.getDeviceSwitch().isChecked()) {
                                        ((MainActivity)getActivity()).mService.issueCommand(device,
                                                new LedCommand(LedCommand.LedCommandType.TOGGLE));
                                    } else {
                                        ((MainActivity)getActivity()).mService.issueCommand(device,
                                                new LedCommand(LedCommand.LedCommandType.BLINK));
                                    }
                                } else {
                                    ((MainActivity)getActivity()).mService.issueCommand(device,
                                            new LedCommand(LedCommand.LedCommandType.TOGGLE));
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });*/
                        views.put(position, view);
                        break;

                    case STEP_MOTOR:
                        view = mInflater.inflate(R.layout.list_stepper_items, null);
                        viewHolder = new StepperMotorViewHolder(view);

                        viewHolder.getDeviceName().setText(device.getDeviceName());
                        viewHolder.getServerId().setText("" + device.getHostServerId());

                        if (device.getDeviceMode() == StepperMotor.StepperMotorMode.OFF)
                            viewHolder.getDeviceSwitch().setChecked(false);
                        else viewHolder.getDeviceSwitch().setChecked(true);

                        /*viewHolder.getDeviceSwitch().setOnClickListener(view1 -> {
                            try {
                                if(viewHolder.getBlink().isChecked()) {
                                    if(!viewHolder.getDeviceSwitch().isChecked()) {
                                        ((MainActivity)getActivity()).mService.issueCommand(device,
                                                new LedCommand(LedCommand.LedCommandType.TOGGLE));
                                    } else {
                                        ((MainActivity)getActivity()).mService.issueCommand(device,
                                                new LedCommand(LedCommand.LedCommandType.BLINK));
                                    }
                                } else {
                                    ((MainActivity)getActivity()).mService.issueCommand(device,
                                            new LedCommand(LedCommand.LedCommandType.TOGGLE));
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });*/
                        views.put(position, view);
                        break;
                }
            }
            return view;
        }

        private RgbLedCommand.RgbLedCommandType getColorCommand(boolean blink, int color)
        {
            RgbLedCommand.RgbLedCommandType ct  = null;
            if(blink) {
                if(color == getResources().getColor(R.color.red))
                    ct = RgbLedCommand.RgbLedCommandType.BLINK_RED;
                else if(color == getResources().getColor(R.color.magenta))
                    ct = RgbLedCommand.RgbLedCommandType.BLINK_MAGENTA;
                else  if(color == getResources().getColor(R.color.blue))
                    ct = RgbLedCommand.RgbLedCommandType.BLINK_BLUE;
                else if(color == getResources().getColor(R.color.green))
                    ct = RgbLedCommand.RgbLedCommandType.BLINK_GREEN;
                else if(color == getResources().getColor(R.color.yellow))
                    ct = RgbLedCommand.RgbLedCommandType.BLINK_YELLOW;
                else if(color == getResources().getColor(R.color.cyan))
                    ct = RgbLedCommand.RgbLedCommandType.BLINK_CYAN;
                else if(color == getResources().getColor(R.color.white))
                    ct = RgbLedCommand.RgbLedCommandType.BLINK_WHITE;
            } else {
                if(color == getResources().getColor(R.color.red))
                    ct = RgbLedCommand.RgbLedCommandType.TOGGLE_RED;
                else if(color == getResources().getColor(R.color.magenta))
                    ct = RgbLedCommand.RgbLedCommandType.TOGGLE_MAGENTA;
                else  if(color == getResources().getColor(R.color.blue))
                    ct = RgbLedCommand.RgbLedCommandType.TOGGLE_BLUE;
                else if(color == getResources().getColor(R.color.green))
                    ct = RgbLedCommand.RgbLedCommandType.TOGGLE_GREEN;
                else if(color == getResources().getColor(R.color.yellow))
                    ct = RgbLedCommand.RgbLedCommandType.TOGGLE_YELLOW;
                else if(color == getResources().getColor(R.color.cyan))
                    ct = RgbLedCommand.RgbLedCommandType.TOGGLE_CYAN;
                else if(color == getResources().getColor(R.color.white))
                    ct = RgbLedCommand.RgbLedCommandType.TOGGLE_WHITE;
            } return ct;
        }
    }
}
