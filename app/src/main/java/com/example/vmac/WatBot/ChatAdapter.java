package com.example.vmac.WatBot;

/**
 * Created by VMac on 17/11/16.
 */

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;

import java.util.ArrayList;


public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


  private int SELF = 100;
  public static final int BOT = 101;
  public static final int CATEGORY_VIEW = 102;
  public static final int GIF_VIEW = 103;

  private ArrayList<Message> messageArrayList;
  private RequestManager requestManager;


  public ChatAdapter(ArrayList<Message> messageArrayList, RequestManager requestManager) {
    this.messageArrayList = messageArrayList;
    this.requestManager = requestManager;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView;

    // view type is to identify where to render the chat message
    // left or right
    if (viewType == CATEGORY_VIEW){
      itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_category, parent, false);

      return new ViewHolderCategory(itemView);
    } else if (viewType == GIF_VIEW) {
      itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_image_watson, parent, false);
      return new ViewHolderImage(itemView);
    } else {
      if (viewType == SELF) {
        // self message
        itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item_self, parent, false);
      } else {
        // WatBot message
        itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item_watson, parent, false);
      }
      return new ViewHolderText(itemView);
    }
  }

  @Override
  public int getItemViewType(int position) {
    Message message = messageArrayList.get(position);

    if (message.getId() != null && message.getId().equals("1")) {
      return SELF;
    }

    if (message.getId() != null && message.getId().equals("2")) {
      return BOT;
    }

    if (message.getId() != null && message.getId().equals("4")) {
      return GIF_VIEW;
    }

    return CATEGORY_VIEW;
  }

  @Override
  public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
      Message message = messageArrayList.get(position);
      if (holder.getItemViewType() == CATEGORY_VIEW) {

        ((ViewHolderCategory)holder).message.setText(message.message);
        if (message.secondMessage != null) {
          ((ViewHolderCategory)holder).message2.setVisibility(View.VISIBLE);
          ((ViewHolderCategory)holder).message2.setText(message.secondMessage);
        } else {
          ((ViewHolderCategory)holder).message2.setVisibility(View.INVISIBLE);
        }
      } else if (holder.getItemViewType() == GIF_VIEW) {
          int drawableId = message.drawableId;
          requestManager
                  .load(drawableId)
                  .into(((ViewHolderImage)holder).messageImage);
      } else {
      message.setMessage(message.getMessage());
      ((ViewHolderText) holder).message.setText(message.getMessage());
    }
  }

  @Override
  public int getItemCount() {
    return messageArrayList.size();
  }

  public class ViewHolderText extends RecyclerView.ViewHolder {
    TextView message;

    public ViewHolderText(View view) {
      super(view);
      message = itemView.findViewById(R.id.message);

      //TODO: Uncomment this if you want to use a custom Font
            /*String customFont = "Montserrat-Regular.ttf";
            Typeface typeface = Typeface.createFromAsset(itemView.getContext().getAssets(), customFont);
            message.setTypeface(typeface);*/

    }
  }

  public class ViewHolderImage extends RecyclerView.ViewHolder {
    ImageView messageImage;

    public ViewHolderImage(View view) {
      super(view);
      messageImage = itemView.findViewById(R.id.message_img);
    }
  }

    public class ViewHolderCategory extends RecyclerView.ViewHolder {
        TextView message;
        TextView message2;

        public ViewHolderCategory(View view) {
            super(view);
            message = itemView.findViewById(R.id.message_text);
            message2 = itemView.findViewById(R.id.message_text2);
        }
    }


}