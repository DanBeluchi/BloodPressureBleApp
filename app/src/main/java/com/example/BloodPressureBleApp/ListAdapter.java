package com.example.BloodPressureBleApp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.BloodPressureBleApp.Data.BloodPressureMeasurement;
import com.example.BloodPressureBleApp.Database.Database;

import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ItemViewHolder> {

    private List<BloodPressureMeasurement> mItems;
    private Context mContext;
    private boolean mItemSelected = false;

    public ListAdapter(List<BloodPressureMeasurement> mItems) {
        this.mItems = mItems;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        mContext = context;
        int layoutIdForListItem = R.layout.list_item;
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


    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        private TextView tvSystolic;
        private TextView tvDiastolic;
        private TextView tvPulse;
        private TextView tvDateTime;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystolic = itemView.findViewById(R.id.tv_systolic_value);
            tvDiastolic = itemView.findViewById(R.id.tv_diastolic_value);
            tvPulse = itemView.findViewById(R.id.tv_pulse_value);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            itemView.setOnCreateContextMenuListener(this);
            itemView.setBackgroundColor(Color.WHITE);
        }

        void bind(int index) {
            tvSystolic.setText(mItems.get(index).getmSystolic());
            tvDiastolic.setText(String.valueOf(mItems.get(index).getmDiastolic()));
            tvPulse.setText(String.valueOf(mItems.get(index).getmPulse()));
            tvDateTime.setText(String.valueOf(mItems.get(index).getmTimeStamp()));
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, final View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Option");

            MenuItem mShareItem = menu.add("Share");
            v.setBackgroundColor(mContext.getResources().getColor(R.color.selectedColor));
            mShareItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    String systolicText = mContext.getString(R.string.systolic_share);
                    String diastolicText = mContext.getString(R.string.diastolic_share);
                    String pulseSText = mContext.getString(R.string.pulse_share);
                    int clickedIndex = getAdapterPosition();
                    BloodPressureMeasurement clickedItem = mItems.get(clickedIndex);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, (mContext.getResources().getString(R.string.share_msg)) + clickedItem.getmTimeStamp() + System.lineSeparator() + systolicText + clickedItem.getmSystolic() + System.lineSeparator() + diastolicText + clickedItem.getmDiastolic() + System.lineSeparator() + pulseSText + clickedItem.getmPulse() + " ");
                    sendIntent.setType("text/plain");
                    Intent shareIntent = Intent.createChooser(sendIntent, null);
                    mContext.startActivity(shareIntent);
                    return true;
                }
            });


            MenuItem mDeleteItem = menu.add("Delete");
            mDeleteItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int deletedRows = Database.mMeasurementsResultsDao.deleteMeasurementById(mItems.get(getAdapterPosition()).getmMeasurementId());
                    if (deletedRows > 0) {
                        mItems.remove(getAdapterPosition());
                        notifyDataSetChanged();
                        Toast.makeText(mContext, "Data deleted", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
        }
    }
}
