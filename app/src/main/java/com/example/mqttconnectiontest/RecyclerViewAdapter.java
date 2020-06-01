package com.example.mqttconnectiontest;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<String> picturePaths = new ArrayList<String>();
    private ArrayList<String> attributes = new ArrayList<String>();
    private Context context;

    //constructor
    public RecyclerViewAdapter(ArrayList<String> picturePaths, ArrayList<String> attributes, Context context){
        picturePaths = picturePaths;
        attributes = attributes;
        context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.status_listitems, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Bitmap bitmap = BitmapFactory.decodeFile(picturePaths.get(position));
        holder.image.setImageBitmap(bitmap);
        holder.attribute.setText(attributes.get(position));
    }

    @Override
    public int getItemCount() {
        return attributes.size();
    }

    //create references to views
    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView attribute;
        RelativeLayout parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.status_image);
            attribute = itemView.findViewById(R.id.attribute);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}
