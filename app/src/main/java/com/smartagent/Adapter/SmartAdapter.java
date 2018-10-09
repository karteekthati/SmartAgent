package com.smartagent.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.smartagent.Bean.Bean1;
import com.smartagent.R;

import java.io.File;
import java.util.ArrayList;

import id.zelory.compressor.Compressor;

public class SmartAdapter  extends  RecyclerView.Adapter<SmartAdapter.ViewHolder> {

    private ArrayList<Bean1> data;
    private Context mCtx;
    String TAG = "TAG";

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


        try{
            if(String.valueOf(obj.gettype()).equalsIgnoreCase("IMAGE")){
                viewHolder.ImgView.setVisibility(View.VISIBLE);
                viewHolder.Videoview.setVisibility(View.GONE);
                //setting image to imageview
                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                File newFile = new File(root + "/SmartAgent/test.jpg");
                if(newFile.exists()){
                    Bitmap bitmap = new Compressor(mCtx).compressToBitmap(newFile);
                    viewHolder.ImgView.setImageBitmap(rotateImage(bitmap, 0));
                    viewHolder.ImgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }
            else{
                viewHolder.ImgView.setVisibility(View.GONE);
                viewHolder.Videoview.setVisibility(View.VISIBLE);
                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                File newFile = new File(root + "/SmartAgent/video.mp4");

                if(newFile.exists()){
                    Uri uri = Uri.fromFile(newFile);
                    viewHolder.Videoview.setVideoURI(uri);
                    viewHolder.Videoview.start();
                }

            }
        }
        catch (Exception e){
            Log.d(TAG, e.toString());
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix,
                true);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView TvId,TvName,TvType,TvSize, TvPath;
        ImageView ImgView;
        VideoView Videoview;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            TvId = (TextView)itemView.findViewById(R.id.Tvid);
            TvName = (TextView)itemView.findViewById(R.id.Tvname);
            TvType = (TextView)itemView.findViewById(R.id.Tvtype);
            TvSize = (TextView)itemView.findViewById(R.id.Tvsize);
            TvPath = (TextView)itemView.findViewById(R.id.Tvpath);

            ImgView = (ImageView)itemView.findViewById(R.id.ImgView);
            Videoview = (VideoView)itemView.findViewById(R.id.Videoview);

        }
    }
}
