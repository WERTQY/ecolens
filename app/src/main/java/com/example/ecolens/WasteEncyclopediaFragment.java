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
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

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

        // 1. Initialize Views
        recyclerView = view.findViewById(R.id.rv_waste_items);
        etSearch = view.findViewById(R.id.et_search);
        ImageButton btnSearch = view.findViewById(R.id.btn_search);
        View backButton = view.findViewById(R.id.btn_back_map);

        // 2. Setup Recycler
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Navigation
        backButton.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        // 4. Firebase
        db = FirebaseFirestore.getInstance();
        db = FirebaseFirestore.getInstance();
        loadWasteItemsFromFirestore();

        // 5. Search Logic
        setupSearchListeners(btnSearch);

        return view;
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

                        // Default: Show all
                        filteredItems.clear();
                        filteredItems.addAll(items);
                        adapter = new WasteAdapter(filteredItems);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    private void setupSearchListeners(ImageButton btnSearch) {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Handle "Enter" key on keyboard
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
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static class WasteItem {
        private String itemName;
        private String preparation;
        private String imageSource;   // e.g. "https://firebasestorage.googleapis.com url image"
        private boolean hazardous;
        private boolean recyclable;

        public WasteItem() {} // Empty constructor needed for Firestore

        public String getItemName() { return itemName; }
        public String getPreparation() { return preparation; }
        public String getImageSource() { return imageSource; }
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
            Context context = holder.itemView.getContext();

            holder.tvName.setText(item.getItemName());
            holder.tvPrep.setText(item.getPreparation());

            // Load Image for the List Row
            loadImage(context, item.getImageSource(), holder.imgItem);

            holder.itemView.setOnClickListener(v -> showPopup(item, v.getContext()));
        }

        private void loadImage(Context context, String imageSource, ImageView imageView) {
            CircularProgressDrawable loader = new CircularProgressDrawable(context);
            loader.setStrokeWidth(5f);
            loader.setCenterRadius(30f);
            loader.setColorSchemeColors(0xFF2E7D32);
            loader.start();
            Glide.with(context).clear(imageView);

            if (imageSource != null && imageSource.startsWith("http")) {
                Glide.with(context)
                        .load(imageSource)
                        .placeholder(R.drawable.ic_recycle)
                        .error(R.drawable.ic_recycle)
                        .dontAnimate()
                        .into(imageView);
            }
            else {// if there on URL img in firebase
                Glide.with(context)
                        .load(R.drawable.ic_recycle)
                        .dontAnimate()
                        .into(imageView);
            }
        }

        private void showPopup(WasteItem item, Context context) {
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.popup_waste_item);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            // Find Popup Views
            ImageView popupImg = dialog.findViewById(R.id.popup_img_item);
            TextView popupName = dialog.findViewById(R.id.popup_tv_item_name);
            TextView popupPrep = dialog.findViewById(R.id.popup_tv_item_prep);
            TextView popupRecyclable = dialog.findViewById(R.id.popup_tv_recyclable);
            TextView popupHazardous = dialog.findViewById(R.id.popup_tv_hazardous);
            Button closeButton = dialog.findViewById(R.id.popup_btn_close);

            // Set Text
            popupName.setText(item.getItemName());
            popupPrep.setText(item.getPreparation());

            // Logic: Recyclable (Green = Yes, Red = No)
            if (item.isRecyclable()) {
                popupRecyclable.setText("Recyclable: Yes");
                popupRecyclable.setTextColor(Color.parseColor("#2E7D32")); // Green
            } else {
                popupRecyclable.setText("Recyclable: No");
                popupRecyclable.setTextColor(Color.RED);
            }

            // Logic: Hazardous (Red = Yes, Green = No)
            if (item.isHazardous()) {
                popupHazardous.setText("Hazardous: Yes");
                popupHazardous.setTextColor(Color.RED);
            } else {
                popupHazardous.setText("Hazardous: No");
                popupHazardous.setTextColor(Color.parseColor("#2E7D32")); // Green
            }

            // Load Image into Popup
            loadImage(context, item.getImageSource(), popupImg);

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