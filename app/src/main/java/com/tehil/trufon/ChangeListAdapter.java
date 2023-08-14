package com.tehil.trufon;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

class ChangeListAdapter extends ArrayAdapter<String> {
    //Arranges the list of medication history so that the headings are bold
    public ChangeListAdapter(Context context, List<String> dataArrayList) {
        super(context, android.R.layout.simple_list_item_1, dataArrayList);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) super.getView(position, convertView, parent);
        String item = getItem(position);

        if (item != null) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(item);

            if (item.contains("ITime")) {
                spannableStringBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#3F51B5")), 0, item.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableStringBuilder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, item.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                if (item.contains("Medication")) {
                    int startIndex = item.indexOf("Medication");
                    int endIndex = startIndex + "Medication".length();
                    if (startIndex >= 0 && endIndex <= item.length()) {
                   spannableStringBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#3F51B5")), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannableStringBuilder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                if (item.contains("Date:")) {
                    int startIndex = item.indexOf("Date:");
                    int endIndex = startIndex + "Date:".length();
                    if (startIndex >= 0 && endIndex <= item.length()) {
                        spannableStringBuilder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                if (item.contains("| Took Medicine:")) {
                    int startIndex = item.indexOf("| Took Medicine:");
                    int endIndex = startIndex + "| Took Medicine:".length();
                    if (startIndex >= 0 && endIndex <= item.length()) {
                        spannableStringBuilder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                if (item.contains("| Time: ")) {
                    int startIndex = item.indexOf("| Time: ");
                    int endIndex = startIndex + "| Time: ".length();
                    if (startIndex >= 0 && endIndex <= item.length()) {
                        spannableStringBuilder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }

            textView.setText(spannableStringBuilder);
        }

        return textView;
    }
}
