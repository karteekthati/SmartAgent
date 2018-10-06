package com.smartagent.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.smartagent.Bean.Bean1;
import com.smartagent.R;

import java.util.ArrayList;

public class SmartAdapter  extends  RecyclerView.Adapter<SmartAdapter.ViewHolder> {

    private ArrayList<Bean1> data;
    private Context mCtx;

    public SmartAdapter(ArrayList<Bean1> data, Context mCtx) {
        this.data = data;
        this.mCtx = mCtx;
    }

    @NonNull
    @Override
    public SmartAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.cardlayout, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SmartAdapter.ViewHolder viewHolder, int i) {
        Bean1 obj = data.get(i);

        viewHolder.TvId.setText(String.valueOf(obj.getid()));
        viewHolder.TvName.setText(String.valueOf(obj.getname()));
        viewHolder.TvType.setText(String.valueOf(obj.gettype()));
        viewHolder.TvSize.setText(String.valueOf(obj.getsize()));
        viewHolder.TvPath.setText(String.valueOf(obj.getpath()));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView TvId,TvName,TvType,TvSize, TvPath;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            TvId = (TextView)itemView.findViewById(R.id.Tvid);
            TvName = (TextView)itemView.findViewById(R.id.Tvname);
            TvType = (TextView)itemView.findViewById(R.id.Tvtype);
            TvSize = (TextView)itemView.findViewById(R.id.Tvsize);
            TvPath = (TextView)itemView.findViewById(R.id.Tvpath);
        }
    }
}
