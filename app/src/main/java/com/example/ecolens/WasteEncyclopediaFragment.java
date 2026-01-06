package com.example.ecolens;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WasteEncyclopediaFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private ImageButton btnSearch;
    private WasteAdapter adapter;
    private List<WasteItem> items = new ArrayList<>();
    private List<WasteItem> filteredItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waste_encyclopedia, container, false);

        recyclerView = view.findViewById(R.id.rv_waste_items);
        etSearch = view.findViewById(R.id.et_search);
        btnSearch = view.findViewById(R.id.btn_search);

        loadWasteItems();
        filteredItems.addAll(items);

        adapter = new WasteAdapter(getContext(), filteredItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filter(etSearch.getText().toString());
                hideKeyboard(v);
                return true;
            }
            return false;
        });

        btnSearch.setOnClickListener(v -> {
            filter(etSearch.getText().toString());
            hideKeyboard(v);
        });

        return view;
    }

    private void filter(String text) {
        String query = text.toLowerCase().trim();
        filteredItems.clear();
        if (query.isEmpty()) {
            filteredItems.addAll(items);
        } else {
            for (WasteItem item : items) {
                if (item.name.toLowerCase().contains(query)) {
                    filteredItems.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void loadWasteItems() {
        try {
            InputStream is = getResources().openRawResource(R.raw.waste_items);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String name = obj.getString("name");
                String prep = obj.getString("preparation");
                boolean hazardous = obj.getBoolean("hazardous");
                String image = obj.getString("image");
                boolean recyclable = obj.getBoolean("recyclable");
                items.add(new WasteItem(name, prep, hazardous, image, recyclable));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class WasteItem {
        String name, preparation, image;
        boolean hazardous, recyclable;

        WasteItem(String name, String preparation, boolean hazardous, String image, boolean recyclable) {
            this.name = name;
            this.preparation = preparation;
            this.hazardous = hazardous;
            this.image = image;
            this.recyclable = recyclable;
        }
    }

    static class WasteAdapter extends RecyclerView.Adapter<WasteAdapter.ViewHolder> {
        Context context;
        List<WasteItem> items;

        WasteAdapter(Context context, List<WasteItem> items) {
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_waste, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WasteItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvPrep.setText(item.preparation);
            holder.imgHazard.setVisibility(item.hazardous ? View.VISIBLE : View.GONE);
            holder.imgRecycle.setVisibility(item.recyclable ? View.VISIBLE : View.GONE);

            // Load image dynamically by resource name
            int resId = context.getResources().getIdentifier(item.image, "drawable", context.getPackageName());
            holder.imgItem.setImageResource(resId);

            holder.itemView.setOnClickListener(v -> {
                showPopup(item);
            });
        }

        private void showPopup(WasteItem item) {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.popup_waste_item);

            ImageView popupImg = dialog.findViewById(R.id.popup_img_item);
            TextView popupName = dialog.findViewById(R.id.popup_tv_item_name);
            TextView popupPrep = dialog.findViewById(R.id.popup_tv_item_prep);
            Button closeButton = dialog.findViewById(R.id.popup_btn_close);

            popupName.setText(item.name);
            popupPrep.setText(item.preparation);

            int resId = context.getResources().getIdentifier(item.image, "drawable", context.getPackageName());
            popupImg.setImageResource(resId);

            closeButton.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgItem, imgHazard, imgRecycle;
            TextView tvName, tvPrep;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgItem = itemView.findViewById(R.id.img_item);
                imgHazard = itemView.findViewById(R.id.img_hazard);
                imgRecycle = itemView.findViewById(R.id.img_recycle);
                tvName = itemView.findViewById(R.id.tv_item_name);
                tvPrep = itemView.findViewById(R.id.tv_item_prep);
            }
        }
    }
}
