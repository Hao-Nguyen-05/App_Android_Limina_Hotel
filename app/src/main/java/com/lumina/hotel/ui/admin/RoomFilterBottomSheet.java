package com.lumina.hotel.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.lumina.hotel.R;
import com.lumina.hotel.data.dao.RoomDao;
import com.lumina.hotel.databinding.BottomSheetFilterRoomBinding;
import com.lumina.hotel.model.RoomFilter;
import com.lumina.hotel.model.RoomType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomFilterBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "RoomFilterBottomSheet";
    private static final String ARG_FILTER = "arg_filter";

    public interface OnFilterAppliedListener {
        void onFilterApplied(RoomFilter filter);
    }

    private BottomSheetFilterRoomBinding binding;
    private ExecutorService executor;
    private RoomFilter working;

    public static RoomFilterBottomSheet newInstance(RoomFilter current) {
        RoomFilterBottomSheet sheet = new RoomFilterBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_FILTER, current.serialize());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetFilterRoomBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String raw = getArguments() != null ? getArguments().getString(ARG_FILTER, "") : "";
        working = RoomFilter.parse(raw);

        binding.btnFilterReset.setOnClickListener(v -> {
            binding.cgFilterType.clearCheck();
            binding.cgFilterFloor.clearCheck();
        });
        binding.btnFilterApply.setOnClickListener(v -> applyAndClose());

        loadOptions();
    }

    private void loadOptions() {
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                RoomDao dao = new RoomDao(requireContext());
                List<RoomType> types = dao.getTypeOptions();
                List<Integer> floors = dao.getFloorOptions();
                runOnUi(() -> {
                    buildTypeChips(types);
                    buildFloorChips(floors);
                });
            } catch (Exception e) {
                runOnUi(() -> {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(),
                                R.string.admin_room_filter_options_error,
                                Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void runOnUi(Runnable action) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (binding != null) action.run();
        });
    }

    private void buildTypeChips(List<RoomType> types) {
        binding.cgFilterType.removeAllViews();
        for (RoomType type : types) {
            Chip chip = createChip(binding.cgFilterType);
            chip.setTag(type.getId());
            chip.setText(type.getTypeName());
            chip.setChecked(working.getTypeIds().contains(type.getId()));
            binding.cgFilterType.addView(chip);
        }
    }

    private void buildFloorChips(List<Integer> floors) {
        binding.cgFilterFloor.removeAllViews();
        for (Integer floor : floors) {
            Chip chip = createChip(binding.cgFilterFloor);
            chip.setTag(floor);
            chip.setText(getString(R.string.admin_room_filter_floor_format, floor));
            chip.setChecked(working.getFloors().contains(floor));
            binding.cgFilterFloor.addView(chip);
        }
    }

    private Chip createChip(ChipGroup parent) {
        Chip chip = (Chip) getLayoutInflater()
                .inflate(R.layout.chip_filter_option, parent, false);
        chip.setId(View.generateViewId());
        return chip;
    }

    private void applyAndClose() {
        RoomFilter result = RoomFilter.parse("");
        for (int i = 0; i < binding.cgFilterType.getChildCount(); i++) {
            Chip chip = (Chip) binding.cgFilterType.getChildAt(i);
            if (chip.isChecked()) result.getTypeIds().add((Integer) chip.getTag());
        }
        for (int i = 0; i < binding.cgFilterFloor.getChildCount(); i++) {
            Chip chip = (Chip) binding.cgFilterFloor.getChildAt(i);
            if (chip.isChecked()) result.getFloors().add((Integer) chip.getTag());
        }

        result.save(requireContext());

        Fragment parent = getParentFragment();
        if (parent instanceof OnFilterAppliedListener) {
            ((OnFilterAppliedListener) parent).onFilterApplied(result);
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        binding = null;
    }
}