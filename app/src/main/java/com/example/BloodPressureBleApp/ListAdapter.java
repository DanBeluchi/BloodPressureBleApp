package com.example.BloodPressureBleApp;

import android.content.Context;
import android.util.Log;
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

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ItemViewHolder> {

    private List<BloodPressureMeasurement> mItems;
    private Context mContext;

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


    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView tvSystolic;
        private TextView tvDiastolic;
        private TextView tvPulse;
        private TextView tvDateTime;
        private LinearLayout llItem;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystolic = itemView.findViewById(R.id.tv_systolic_value);
            tvDiastolic = itemView.findViewById(R.id.tv_diastolic_value);
            tvPulse = itemView.findViewById(R.id.tv_pulse_value);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            itemView.setOnClickListener(this);
        }

        void bind(int index) {
            tvSystolic.setText(mItems.get(index).getmSystolic());
            tvDiastolic.setText(String.valueOf(mItems.get(index).getmDiastolic()));
            tvPulse.setText(String.valueOf(mItems.get(index).getmPulse()));
            tvDateTime.setText(String.valueOf(mItems.get(index).getmTimeStamp()));
        }

        @Override
        public void onClick(View view) {
            Toast.makeText(mContext, String.valueOf(mItems.get(getAdapterPosition())), Toast.LENGTH_LONG).show();
            int deletedRows = Database.mMeasurementsResultsDao.deleteMeasurementById(mItems.get(getAdapterPosition()).getmMeasurementId());
            if (deletedRows > 0) {
                mItems.remove(getAdapterPosition());
                notifyDataSetChanged();
            }
            Toast.makeText(mContext, deletedRows + "Item deleted = ", Toast.LENGTH_SHORT).show();
        }
    }
}
