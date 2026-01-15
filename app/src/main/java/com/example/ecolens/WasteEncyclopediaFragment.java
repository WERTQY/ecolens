package com.example.ecolens;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import androidx.navigation.Navigation;
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

        // --- FIX 1: Correct ID for the back button ---
        // Was "btn_back_manual", changed to "btn_back_map" based on your XML
        View backButton = view.findViewById(R.id.btn_back_map);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Navigation.findNavController(view).navigateUp();
            });
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        loadWasteItemsFromFirestore();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (adapter != null) filter(etSearch.getText().toString());
                hideKeyboard(v);
                return true;
            }
            return false;
        });

        btnSearch.setOnClickListener(v -> {
            if (adapter != null) filter(etSearch.getText().toString());
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
                if (item.getItemName() != null && item.getItemName().toLowerCase().contains(query)) {
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

    // --- FIX 2: Updated Data Model to use imageSource ---
    public static class WasteItem {
        private String itemName;
        private String preparation;
        private String imageSource; // Renamed from imageName to match your Firebase URL field
        private boolean hazardous;
        private boolean recyclable;

        public WasteItem() {}

        public String getItemName() { return itemName; }
        public String getPreparation() { return preparation; }
        public String getImageSource() { return imageSource; } // Updated Getter
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

            // --- FIX 3: Use Simplified Loader ---
            loadImage(viewContext, item.getImageSource(), holder.imgItem);

            holder.itemView.setOnClickListener(v -> showPopup(item, v.getContext()));
        }

        // --- NEW HELPER METHOD ---
        private void loadImage(Context context, String url, ImageView target) {
            // 1. Clear old image (Prevents "Wrong Image" recycling bug)
            Glide.with(context).clear(target);

            // 2. Load URL directly
            if (url != null && !url.isEmpty()) {
                Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.ic_recycle) // Show while loading
                        .error(R.drawable.ic_recycle)       // Show if error
                        .dontAnimate()                      // Prevent list flickering
                        .into(target);
            } else {
                Glide.with(context)
                        .load(R.drawable.ic_recycle)
                        .dontAnimate()
                        .into(target);
            }
        }

        private void showPopup(WasteItem item, Context viewContext) {
            final Dialog dialog = new Dialog(viewContext);
            dialog.setContentView(R.layout.popup_waste_item);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            ImageView popupImg = dialog.findViewById(R.id.popup_img_item);
            TextView popupName = dialog.findViewById(R.id.popup_tv_item_name);
            TextView popupPrep = dialog.findViewById(R.id.popup_tv_item_prep);
            TextView popupRecyclable = dialog.findViewById(R.id.popup_tv_recyclable);
            TextView popupHazardous = dialog.findViewById(R.id.popup_tv_hazardous);
            Button closeButton = dialog.findViewById(R.id.popup_btn_close);

            popupName.setText(item.getItemName());
            popupPrep.setText(item.getPreparation());

            // Logic for Recyclable Text
            if(item.isRecyclable()) {
                popupRecyclable.setText("Recyclable: Yes");
                popupRecyclable.setTextColor(Color.parseColor("#2E7D32")); // Green
            } else {
                popupRecyclable.setText("Recyclable: No");
                popupRecyclable.setTextColor(Color.RED);
            }

            // Logic for Hazardous Text
            if(item.isHazardous()) {
                popupHazardous.setText("Hazardous: Yes");
                popupHazardous.setTextColor(Color.RED);
            } else {
                popupHazardous.setText("Hazardous: No");
                popupHazardous.setTextColor(Color.parseColor("#2E7D32")); // Green
            }

            // Use the same helper for popup image
            loadImage(viewContext, item.getImageSource(), popupImg);

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