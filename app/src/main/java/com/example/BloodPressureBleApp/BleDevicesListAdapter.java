package com.example.BloodPressureBleApp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.BloodPressureBleApp.Data.BloodPressureMeasurement;
import com.example.BloodPressureBleApp.Database.Database;

import java.util.List;

public class BleDevicesListAdapter extends RecyclerView.Adapter<BleDevicesListAdapter.ItemViewHolder> {

    private List<BluetoothDevice> mItems;
    private Context mContext;
    private BleListItemClickListener mListItemClickListener;

    public interface BleListItemClickListener {
        void onListItemClick(BluetoothDevice item);
    }

    public void setOnListItemClickListener(BleListItemClickListener listItemClickListener) {
        mListItemClickListener = listItemClickListener;
    }

    public BleDevicesListAdapter(List<BluetoothDevice> mItems) {
        this.mItems = mItems;
    }


    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        mContext = context;
        int layoutIdForListItem = R.layout.ble_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);


        View view = inflater.inflate(layoutIdForListItem, parent, false);

        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bind(position);
        Log.d("BINDLOG", String.valueOf(position));
    }

    @Override
    public int getItemCount() {
        return (mItems == null) ? 0 : mItems.size();
    }


    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView tvDeviceName;
        private TextView tvDeviceAddress;
        private LinearLayout llItem;


        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvDeviceAddress = itemView.findViewById(R.id.tv_device_address);
            llItem = itemView.findViewById(R.id.ble_list__item);
            itemView.setOnClickListener(this);
        }

        void bind(int index) {
            tvDeviceName.setText(mItems.get(index).getName());
            tvDeviceAddress.setText(String.valueOf(mItems.get(index).getAddress()));
        }

        @Override
        public void onClick(View view) {
            if (mListItemClickListener != null) {
                int clickedIndex = getAdapterPosition();
                BluetoothDevice clickedItem = mItems.get(clickedIndex);
                mListItemClickListener.onListItemClick(clickedItem);
            }
        }
    }

    public void clear() {
        int size = mItems.size();
        mItems.clear();
        notifyItemRangeRemoved(0, size);
        Log.d("BINDLOG", "Clearing recyclerview");
    }
}
