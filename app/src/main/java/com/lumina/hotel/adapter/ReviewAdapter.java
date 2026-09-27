package com.lumina.hotel.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.lumina.hotel.R;
import com.lumina.hotel.databinding.ItemReviewBinding;
import com.lumina.hotel.model.Review;
import com.lumina.hotel.util.DateUtils;

import java.util.Locale;
import java.util.Objects;

/** Adapter cho danh sách đánh giá ở màn Chi tiết phòng (rvReviews). */
public class ReviewAdapter extends BaseAdapter<Review, ItemReviewBinding> {

    private static final DiffUtil.ItemCallback<Review> DIFF =
            new DiffUtil.ItemCallback<Review>() {
                @Override
                public boolean areItemsTheSame(@NonNull Review oldItem, @NonNull Review newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Review oldItem, @NonNull Review newItem) {
                    return oldItem.getRating() == newItem.getRating()
                            && Objects.equals(oldItem.getComment(), newItem.getComment());
                }
            };

    public ReviewAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    protected ItemReviewBinding inflateBinding(@NonNull LayoutInflater inflater,
                                               @NonNull ViewGroup parent) {
        return ItemReviewBinding.inflate(inflater, parent, false);
    }

    @Override
    protected void bind(@NonNull ItemReviewBinding b, @NonNull Review item, int position) {
        String name = item.getReviewerName() == null ? "" : item.getReviewerName();
        b.tvReviewerName.setText(name);
        b.tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT));
        b.tvReviewDate.setText(DateUtils.formatMonthYear(item.getCreatedAt()));
        b.tvReviewComment.setText(item.getComment());

        ImageView[] stars = {
                b.ivReviewStar1, b.ivReviewStar2, b.ivReviewStar3, b.ivReviewStar4, b.ivReviewStar5
        };
        int rating = item.getRating();
        for (int i = 0; i < stars.length; i++) {
            boolean filled = i < rating;
            stars[i].setImageResource(filled ? R.drawable.ic_star_filled : R.drawable.ic_star);
            stars[i].setAlpha(filled ? 1f : 0.25f);
        }
    }
}