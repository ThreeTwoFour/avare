/*
Copyright (c) 2012, Zubair Khan (governer@gmail.com) 
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    *
    *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ds.avare;


import java.util.List;

import com.ds.avare.position.Coordinate;
import com.ds.avare.utils.WeatherHelper;
import com.ds.avare.weather.Airep;
import com.ds.avare.weather.Metar;
import com.ds.avare.weather.Taf;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

/**
 * @author zkhan
 *
 */
public class PopoutAdapter extends BaseExpandableListAdapter {

    private Context mContext;
    private String[] mGroups;
    private String[] mChildrenText;
    private String[][] mChildren;
    
    private Metar mMetar;
    private Taf mTaf;
    private List<Airep> mAirep;

    private static final int GROUP_METAR = 0;
    private static final int GROUP_TAF = 1;
    private static final int GROUP_PIREP = 2;
    private static final int GROUP_TFR = 3;
    private static final int GROUP_NUM = 4;
    
    /**
     * @param context
     * @param textViewResourceId
     */
    public PopoutAdapter(Context context, StorageService service, String location, String info) {
        mContext = context;
        
        /*
         * Get all groups
         */
        mGroups = context.getResources().getStringArray(R.array.resGroupsPopout);
        /*
         * Assign children
         */
        mChildren = new String[GROUP_NUM][];
        mChildren[GROUP_METAR] = new String[1];
        mChildren[GROUP_TAF] = new String[1];
        mChildren[GROUP_PIREP] = new String[1];
        mChildren[GROUP_TFR] = new String[1];
        
        mChildrenText = new String[GROUP_NUM];

        /*
         * Show view
         */
        mChildrenText[GROUP_METAR] = mContext.getString(R.string.NotAvailable);
        mChildrenText[GROUP_TAF] = mContext.getString(R.string.NotAvailable);
        mChildrenText[GROUP_PIREP] = mContext.getString(R.string.NotAvailable);
        mChildrenText[GROUP_TFR] = info == null ? "" : info;

        if(service != null && location != null) {
            if(!location.contains("&")) {
                /*
                 * Not GPS
                 */
                /*
                 * Update weather etc.
                 */
                new ViewTask().execute(service, location);
            }
        }
    }

    /**
     * 
     * @author zkhan
     *
     */
    private class ViewTask extends AsyncTask<Object, Void, Boolean> {
        
        @Override
        protected Boolean doInBackground(Object... params) {
                       
            StorageService service = (StorageService)params[0];
            String location = (String)params[1];
            
            /*
             * Now find all about this airport 
             */
            mMetar = service.getInternetWeatherCache().getMetar(location);
            mTaf = service.getInternetWeatherCache().getTaf(location);
           
            Coordinate c = service.getDBResource().getCoordinate(location);
            if(null != c) {
                mAirep = service.getInternetWeatherCache().getAirep(c.getLongitude(), c.getLatitude());
            }
            
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(mMetar == null) {
                mChildrenText[GROUP_METAR] = mContext.getString(R.string.NotAvailable);
            }
            else {
                mChildrenText[GROUP_METAR] = WeatherHelper.formatWeather(mMetar.rawText);          
            }

            if(mTaf == null) {
                mChildrenText[GROUP_TAF] = mContext.getString(R.string.NotAvailable);
            }
            else {
                mChildrenText[GROUP_TAF] = WeatherHelper.formatWeather(mTaf.rawText);          
            }

            if(mAirep == null) {
                mChildrenText[GROUP_PIREP] = mContext.getString(R.string.NotAvailable);
            }
            else {
                /*
                 * All aireps/pireps
                 */
                String txt = "";
                for(int i = 0; i < mAirep.size(); i++) {
                    Airep a = mAirep.get(i);
                    txt += "\n" + a.reportType + "@ " + a.time + "\n" + a.rawText + "\n";
                }
                mChildrenText[GROUP_PIREP] = txt;
            }

            notifyDataSetChanged();            
        }
    }

    /**
     * Just update numbers.
     */
    public void refresh() {
        new ViewTask().execute();
    }


    /**
     * 
     */
    @Override
    public View getGroupView(int group, boolean isExpanded, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        View rowView = convertView;

        /*
         * Do not inflate if not needed. Speeds up things quite a bit
         */
        if(null == rowView) {
            rowView = inflater.inflate(R.layout.textview, parent, false);
        }
        
        TextView tv = (TextView)rowView.findViewById(R.id.textview_textview);
        tv.setTextColor(0xFFFFFFFF);
        
        
        /*
         * Set different values from different outputs.
         */
        switch(group) {
            case GROUP_METAR:
                String txt = (mMetar == null) ? "" : ("@ " + mMetar.time);
                int col = (mMetar == null) ? 0xFFFFFFFF : WeatherHelper.metarColor(mMetar.flightCategory);
                tv.setText(mGroups[group] + txt);
                tv.setTextColor(col);
                break;
            case GROUP_TAF:
                String txtf = (mTaf == null) ? "" : ("@ " + mTaf.time);
                tv.setText(mGroups[group] + txtf);
                break;
            case GROUP_PIREP:
                tv.setTextColor(0xFFFFFFFF);
                tv.setText(mGroups[group]);
                break;
            case GROUP_TFR:
                tv.setTextColor(0xFFFF0000);
                tv.setText(mGroups[group]);
                break;
        }
       
        /*
         * Inform with red color if any child is expired
         */
        return rowView;
    }
    
    /**
     * 
     */
    @Override
    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = convertView;

        if(null == rowView) {
            rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView tv = (TextView)rowView;
        tv.setTextColor(0xFFFFFFFF);
        tv.setText(mChildrenText[groupPosition]);
        
        return rowView;
    }

    /**
     * 
     */
    @Override
    public Object getChild(int arg0, int arg1) {
        return mChildren[arg0][arg1];
    }

    /**
     * 
     */
    @Override
    public long getChildId(int arg0, int arg1) {
        return arg1;
    }

    /**
     * 
     */
    @Override
    public int getChildrenCount(int arg0) {
        int count = 0;
        try {
            count = mChildren[arg0].length;
        } 
        catch (Exception e) {
        }

        return count;
    }

    /**
     * 
     */
    @Override
    public Object getGroup(int arg0) {
        return mGroups[arg0];
    }

    /**
     * 
     */
    @Override
    public int getGroupCount() {
        return mGroups.length;
    }

    /**
     * 
     */
    @Override
    public long getGroupId(int arg0) {
        return arg0;
    }

    /**
     * 
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * 
     */
    @Override
    public boolean isChildSelectable(int arg0, int arg1) {
        return true;
    }
}