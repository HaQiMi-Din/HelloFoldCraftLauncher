package com.tungsten.hfcl.control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tungsten.hfcl.R;
import com.tungsten.hfcl.control.data.QuickInputTexts;
import com.tungsten.hfclcore.fakefx.collections.ObservableList;
import com.tungsten.hfcllibrary.component.FCLAdapter;
import com.tungsten.hfcllibrary.component.view.FCLImageButton;
import com.tungsten.hfcllibrary.component.view.FCLLinearLayout;
import com.tungsten.hfcllibrary.component.view.FCLTextView;

public class InputTextAdapter extends FCLAdapter {

    private final ObservableList<String> list;
    private final Callback callback;

    public InputTextAdapter(Context context, ObservableList<String> list, Callback callback) {
        super(context);
        this.list = list;
        this.callback = callback;
    }

    public interface Callback {
        void onTextInput(String string);
    }

    static class ViewHolder {
        FCLLinearLayout parent;
        FCLTextView textView;
        FCLImageButton delete;
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_input_text, null);
            viewHolder.parent = view.findViewById(R.id.parent);
            viewHolder.textView = view.findViewById(R.id.text);
            viewHolder.delete = view.findViewById(R.id.delete);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        String inputText = list.get(i);
        String[] split = inputText.split("&\\*&");
        viewHolder.textView.setText(split[0]);
        viewHolder.parent.setOnClickListener(v -> callback.onTextInput(split.length == 2 ? split[1] : split[0]));
        viewHolder.delete.setOnClickListener(v -> {
            QuickInputTexts.removeInputText(inputText);
            notifyDataSetChanged();
        });
        return view;
    }
}
