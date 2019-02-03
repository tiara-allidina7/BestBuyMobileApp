package com.example.vmac.WatBot;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.assistant.v2.Assistant;
import com.ibm.watson.developer_cloud.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.DialogRuntimeResponseGeneric;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageInput;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageResponse;
import com.ibm.watson.developer_cloud.assistant.v2.model.SessionResponse;
import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


  private RecyclerView recyclerView;
  private ChatAdapter mAdapter;
  private ArrayList<Message> messageArrayList;
  private EditText inputMessage;
  private ImageButton btnSend;
  private ImageButton btnRecord;
  StreamPlayer streamPlayer = new StreamPlayer();
  private boolean initialRequest;
  private boolean permissionToRecordAccepted = false;
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
  private static String TAG = "MainActivity";
  private static final int RECORD_REQUEST_CODE = 101;
  private boolean listening = false;
  private MicrophoneInputStream capture;
  private Context mContext;
  private MicrophoneHelper microphoneHelper;

  private Assistant watsonAssistant;
  private SessionResponse watsonAssistantSession;
  private SpeechToText speechService;
  private TextToSpeech textToSpeech;

  public final static String SELF_MSG_ID = "1";
  public final static String BOT_MSG_ID = "2";
  public final static String CATEGORY_MSG_ID = "3";
  public final static String IMG_MSG_ID = "4";

  private void createServices() {
    watsonAssistant = new Assistant("2018-11-08", new IamOptions.Builder()
            .apiKey(mContext.getString(R.string.assistant_apikey))
            .build());
    watsonAssistant.setEndPoint(mContext.getString(R.string.assistant_url));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mContext = getApplicationContext();

    inputMessage = findViewById(R.id.message);
    btnSend = findViewById(R.id.btn_send);
    btnRecord = findViewById(R.id.btn_record);
    String customFont = "Montserrat-Regular.ttf";
    Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
    inputMessage.setTypeface(typeface);
    recyclerView = findViewById(R.id.recycler_view);

    messageArrayList = new ArrayList<>();
    mAdapter = new ChatAdapter(messageArrayList, Glide.with(this));

    microphoneHelper = new MicrophoneHelper(this);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(mAdapter);


    this.inputMessage.setText("");
    this.initialRequest = true;


    btnSend.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (checkInternetConnection()) {
          sendMessage(null);
        }
      }
    });

    createServices();
    sendMessage(null);
  }

  public void submitPrice(View view){
    Message message = new Message();
    message.id = SELF_MSG_ID;
    message.message = "1000";
    sendMessage("1000");
  }

  // Sending a message to Watson Assistant Service
  private void sendMessage(String message) {

    message = message == null ? this.inputMessage.getText().toString().trim() : message;

    final String inputmessage = message;
    if (!this.initialRequest) {
      Message inputMessage = new Message();
      inputMessage.setMessage(inputmessage);
      inputMessage.setId(SELF_MSG_ID);
      messageArrayList.add(inputMessage);
    } else {
      Message inputMessage = new Message();
      inputMessage.setMessage(inputmessage);
      inputMessage.setId("100");
      this.initialRequest = false;
    }

    this.inputMessage.setText("");
    mAdapter.notifyDataSetChanged();

    Thread thread = new Thread(new Runnable() {
      public void run() {
        try {
          if (watsonAssistantSession == null) {
            ServiceCall<SessionResponse> call = watsonAssistant.createSession(new CreateSessionOptions.Builder().assistantId(mContext.getString(R.string.assistant_id)).build());
            watsonAssistantSession = call.execute();
          }

          MessageInput input = new MessageInput.Builder()
            .text(inputmessage)
            .build();
          MessageOptions options = new MessageOptions.Builder()
            .assistantId(mContext.getString(R.string.assistant_id))
            .input(input)
            .sessionId(watsonAssistantSession.getSessionId())
            .build();
          MessageResponse response = watsonAssistant.message(options).execute();
            Log.i(TAG, "run: "+response);
          final Message outMessage = new Message();
          if (response != null &&
                  response.getOutput() != null &&
                  !response.getOutput().getGeneric().isEmpty()){
            List<DialogRuntimeResponseGeneric> responseGenerics = response.getOutput().getGeneric();
            for (DialogRuntimeResponseGeneric responseGeneric : responseGenerics) {
              if ("text".equals(responseGeneric.getResponseType())){
                outMessage.setMessage(responseGeneric.getText());
                outMessage.setId(BOT_MSG_ID);
              }

              messageArrayList.add(outMessage);

              if (outMessage.message.contains("Hey Fiza")) {
                loadCategoryLabels();
              }

              if (outMessage.message.contains("budget")) {
                loadPrices();
              }

              if (outMessage.message.contains("found some things")) {
                loadCameraOptions();
              }

              if (outMessage.message.contains("Notifying")){
                loadSummonGif();
              }

              if (outMessage.message.contains("bag for your new camera")) {
                loadCameraBag();
              }

              runOnUiThread(new Runnable() {
                public void run() {
                  mAdapter.notifyDataSetChanged();
                  if (mAdapter.getItemCount() > 1) {
                    recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
                  }
                }
              });
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    thread.start();

    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
    View view = getCurrentFocus();
    if (view == null) {
      view = new View(this);
    }
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

  }

    private void loadCategoryLabels(){
        Message categoryMessage = new Message();
        categoryMessage.id = CATEGORY_MSG_ID;
        categoryMessage.message = "TV/Home Theatre";
        messageArrayList.add(categoryMessage);

        Message categoryMessage2 = new Message();
        categoryMessage2.id = CATEGORY_MSG_ID;
        categoryMessage2.message = "Computers & Accessories";
        messageArrayList.add(categoryMessage2);

        Message categoryMessage3 = new Message();
        categoryMessage3.id = CATEGORY_MSG_ID;
        categoryMessage3.message = "Headphones & Speakers";
        messageArrayList.add(categoryMessage3);

        Message categoryMessage4 = new Message();
        categoryMessage4.id = CATEGORY_MSG_ID;
        categoryMessage4.message = "Camera & Camcorders";
        messageArrayList.add(categoryMessage4);
    }

  private void loadPrices(){
    Message categoryMessage = new Message();
    categoryMessage.id = CATEGORY_MSG_ID;
    categoryMessage.message = "400-599";
    categoryMessage.secondMessage = "600-799";
    messageArrayList.add(categoryMessage);

    Message categoryMessage2 = new Message();
    categoryMessage2.id = CATEGORY_MSG_ID;
    categoryMessage2.message = "800-999";
    categoryMessage2.secondMessage = "  1000+  ";
    messageArrayList.add(categoryMessage2);
  }

  public void showMap(View view){
    Log.d("ChatAdapter", "load map gif");
    Message gifMessage = new Message();
    gifMessage.id = IMG_MSG_ID;
    gifMessage.drawableId = R.drawable.map;
    messageArrayList.add(gifMessage);
    mAdapter.notifyDataSetChanged();
    if (mAdapter.getItemCount() > 1) {
      recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
    }
  }

  public void loadSummonGif(){
    Log.d("ChatAdapter", "load summon gif");
    Message gifMessage = new Message();
    gifMessage.id = IMG_MSG_ID;
    gifMessage.drawableId = R.drawable.summon;
    messageArrayList.add(gifMessage);
    mAdapter.notifyDataSetChanged();
    if (mAdapter.getItemCount() > 1) {
      recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
    }
  }

  private void loadCameraOptions(){
    Message imageMessage = new Message();
    imageMessage.id = IMG_MSG_ID;
    imageMessage.drawableId = R.drawable.cannon;
    messageArrayList.add(imageMessage);

    Message imageMessage2 = new Message();
    imageMessage2.id = IMG_MSG_ID;
    imageMessage2.drawableId = R.drawable.nikon;
    messageArrayList.add(imageMessage2);

    Message imageMessage3 = new Message();
    imageMessage3.id = CATEGORY_MSG_ID;
    imageMessage3.message = "Show More Options";
    messageArrayList.add(imageMessage3);

    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
    View view = getCurrentFocus();
    if (view == null) {
      view = new View(this);
    }
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
  }

  private void loadCameraBag(){
    Message imageMessage = new Message();
    imageMessage.id = IMG_MSG_ID;
    imageMessage.drawableId = R.drawable.camera_bag;
    messageArrayList.add(imageMessage);

    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
    View view = getCurrentFocus();
    if (view == null) {
      view = new View(this);
    }
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
  }


  /**
   * Check Internet Connection
   *
   * @return
   */
  private boolean checkInternetConnection() {
    // get Connectivity Manager object to check connection
    ConnectivityManager cm =
      (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    boolean isConnected = activeNetwork != null &&
      activeNetwork.isConnectedOrConnecting();

    // Check for network connections
    if (isConnected) {
      return true;
    } else {
      Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
      return false;
    }

  }

  //Private Methods - Speech to Text
  private RecognizeOptions getRecognizeOptions(InputStream audio) {
    return new RecognizeOptions.Builder()
      .audio(audio)
      .contentType(ContentType.OPUS.toString())
      .model("en-US_BroadbandModel")
      .interimResults(true)
      .inactivityTimeout(2000)
      .build();
  }

}



