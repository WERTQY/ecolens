package com.example.ecolens;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WasteEncyclopediaFragment extends Fragment {

    private static final String TAG = "WasteEncyclopedia";
    private RecyclerView recyclerView;
    private EditText etSearch;
    private WasteAdapter adapter;
    private final List<WasteItem> items = new ArrayList<>();
    private final List<WasteItem> filteredItems = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waste_encyclopedia, container, false);

        recyclerView = view.findViewById(R.id.rv_waste_items);
        etSearch = view.findViewById(R.id.et_search);
        ImageButton btnSearch = view.findViewById(R.id.btn_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        loadWasteItemsFromFirestore();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (adapter != null) {
                    filter(etSearch.getText().toString());
                }
                hideKeyboard(v);
                return true;
            }
            return false;
        });

        btnSearch.setOnClickListener(v -> {
            if (adapter != null) {
                filter(etSearch.getText().toString());
            }
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
                if (item.getItemName().toLowerCase().contains(query)) {
                    filteredItems.add(item);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void loadWasteItemsFromFirestore() {
        db.collection("encyclopedia")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && getContext() != null) {
                        items.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            WasteItem item = document.toObject(WasteItem.class);
                            items.add(item);
                        }
                        filteredItems.clear();
                        filteredItems.addAll(items);

                        adapter = new WasteAdapter(filteredItems);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    public static class WasteItem {
        private String itemName, preparation, imageName;
        private boolean hazardous, recyclable;

        public WasteItem() {}

        public String getItemName() { return itemName; }
        public String getPreparation() { return preparation; }
        public String getImageName() { return imageName; }
        public boolean isHazardous() { return hazardous; }
        public boolean isRecyclable() { return recyclable; }
    }

    static class WasteAdapter extends RecyclerView.Adapter<WasteAdapter.ViewHolder> {
        private final List<WasteItem> items;

        WasteAdapter(List<WasteItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_waste, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WasteItem item = items.get(position);
            Context viewContext = holder.itemView.getContext();

            holder.tvName.setText(item.getItemName());
            holder.tvPrep.setText(item.getPreparation());

            String imageName = item.getImageName();
            if (imageName != null && !imageName.isEmpty()) {
                String drawableName = imageName.trim().toLowerCase();
                if (drawableName.contains(".")) {
                    drawableName = drawableName.substring(0, drawableName.lastIndexOf('.'));
                }
                int resId = viewContext.getResources().getIdentifier(drawableName, "drawable", viewContext.getPackageName());
                if (resId != 0) {
                    Glide.with(viewContext)
                            .load(resId)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_warning)
                            .into(holder.imgItem);
                } else {
                    holder.imgItem.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                holder.imgItem.setImageResource(R.drawable.ic_launcher_background);
            }

            holder.itemView.setOnClickListener(v -> showPopup(item, v.getContext()));
        }

        private void showPopup(WasteItem item, Context viewContext) {
            final Dialog dialog = new Dialog(viewContext);
            dialog.setContentView(R.layout.popup_waste_item);

            ImageView popupImg = dialog.findViewById(R.id.popup_img_item);
            TextView popupName = dialog.findViewById(R.id.popup_tv_item_name);
            TextView popupPrep = dialog.findViewById(R.id.popup_tv_item_prep);
            TextView popupRecyclable = dialog.findViewById(R.id.popup_tv_recyclable);
            TextView popupHazardous = dialog.findViewById(R.id.popup_tv_hazardous);
            Button closeButton = dialog.findViewById(R.id.popup_btn_close);

            popupName.setText(item.getItemName());
            popupPrep.setText(item.getPreparation());
            popupRecyclable.setText("Recyclable: " + (item.isRecyclable() ? "Yes" : "No"));
            popupHazardous.setText("Hazardous: " + (item.isHazardous() ? "Yes" : "No"));


            String imageName = item.getImageName();
            if (imageName != null && !imageName.isEmpty()) {
                String drawableName = imageName.trim().toLowerCase();
                if (drawableName.contains(".")) {
                    drawableName = drawableName.substring(0, drawableName.lastIndexOf('.'));
                }
                int resId = viewContext.getResources().getIdentifier(drawableName, "drawable", viewContext.getPackageName());
                if (resId != 0) {
                    Glide.with(viewContext)
                            .load(resId)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_warning)
                            .into(popupImg);
                } else {
                    popupImg.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                popupImg.setImageResource(R.drawable.ic_launcher_background);
            }

            closeButton.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgItem;
            TextView tvName, tvPrep;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgItem = itemView.findViewById(R.id.img_item);
                tvName = itemView.findViewById(R.id.tv_item_name);
                tvPrep = itemView.findViewById(R.id.tv_item_prep);
            }
        }
    }
}
