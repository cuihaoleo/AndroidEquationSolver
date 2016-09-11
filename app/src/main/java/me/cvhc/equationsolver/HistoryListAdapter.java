package me.cvhc.equationsolver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class HistoryListAdapter extends BaseAdapter implements ListAdapter {
    private Context mContext;
    private ArrayList<String> mList = new ArrayList<>();
    private boolean[] mFavoriteMark;
    private int mIndexFavorite;
    private OnItemClickedListener mOnItemClickedListener;
    private OnItemPinningStateChangedListener mOnItemPinningStateChangedListener;

    public HistoryListAdapter(ArrayList<String> list, int idxFavorite, Context context) {
        mList.addAll(list);
        mIndexFavorite = idxFavorite;
        mContext = context;

        mFavoriteMark = new boolean[mList.size()];
        for (int i=0; i<idxFavorite; i++) {
            mFavoriteMark[i] = true;
        }
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        return mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View itemView = view;
        if (itemView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = inflater.inflate(R.layout.popup_window_list_item, null);
        }

        TextView textContent = (TextView) itemView.findViewById(R.id.textContent);
        ToggleButton toggleFavorite = (ToggleButton) itemView.findViewById(R.id.toggleFavorite);
        textContent.setText(mList.get(i));

        toggleFavorite.setOnCheckedChangeListener(null);
        toggleFavorite.setChecked(mFavoriteMark[i]);

        final int i_copy = i;
        textContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnItemClickedListener.onItemClicked(i_copy);
            }
        });

        toggleFavorite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mOnItemPinningStateChangedListener.onItemPinningStateChanged(i_copy, b);
                mFavoriteMark[i_copy] = b;
            }
        });

        return itemView;
    }

    public interface OnItemClickedListener {
        void onItemClicked(int position);
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
    }

    public interface OnItemPinningStateChangedListener {
        void onItemPinningStateChanged(int position, boolean pinned);
    }

    public void setOnItemPinningStateChangedListener(OnItemPinningStateChangedListener listener) {
        mOnItemPinningStateChangedListener = listener;
    }
}
