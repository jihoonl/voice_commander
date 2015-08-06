package com.github.rosjava.android_voice_commander.voice_commander;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;

import org.ros.android.MessageCallable;
import org.ros.android.NodeMainExecutorService;
import org.ros.android.view.RosTextView;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;

import java.io.IOException;
import java.lang.String;
import java.util.ArrayList;

import std_msgs.*;

public class VoiceCommander extends RosAppActivity implements View.OnClickListener
{
    public VoiceCommanderNode vc_node;
    public SpeechRecognizer mRecognizer;
    public VoiceCommander()
    {
        super("VoiceCommander", "VoiceCommander");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        setDefaultMasterName(getString(R.string.default_robot));
        setDashboardResource(R.id.top_bar);
        setMainWindowResource(R.layout.main);
        super.onCreate(savedInstanceState);

        findViewById(R.id.start_recog_btn).setOnClickListener(this);
        findViewById(R.id.robot_stop_btn).setOnClickListener(this);

        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(listener);

    }



    @Override
    protected void init(NodeMainExecutor nodeMainExecutor)
    {
        String voice_command_topic = remaps.get(getString(R.string.voice_command_topic));

        super.init(nodeMainExecutor);
        vc_node = new VoiceCommanderNode(voice_command_topic);
        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());
            nodeMainExecutorService.execute(
                    vc_node,
                    nodeConfiguration.setNodeName("voice_commander")
        );

        } catch (IOException e) {
            // Socket problem
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case 0:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {

            case R.id.start_recog_btn:
                Log.i("[VoiceCommander]", "tab: start_recog_btn");
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
                i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                mRecognizer.startListening(i);
                break;
            case R.id.robot_stop_btn:
                Log.i("[VoiceCommander]", "tab: robot_stop_btn");
                vc_node.command_publish("stop");
                break;
        }
    }

    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.i("[VoiceCommander]", "onReadyForSpeech");
            TextView recong_tv = (TextView)findViewById(R.id.google_recog_wrod_txt);
            recong_tv.setText("지금 말하세요.");
            TextView matched_tv = (TextView)findViewById(R.id.matched_word_txt);
            matched_tv.setText("");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.i("[VoiceCommander]", "onBeginningOfSpeech");
            TextView recong_tv = (TextView)findViewById(R.id.google_recog_wrod_txt);
            recong_tv.setText("인식 중....");
        }
        @Override
        public void onRmsChanged(float rmsdB) {
        }
        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            Log.i("[VoiceCommander]", "onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            Log.i("[VoiceCommander]", "onError: " + error);
            String strError = "";
            switch(error){
                case SpeechRecognizer.ERROR_AUDIO:
                case SpeechRecognizer.ERROR_CLIENT:
                    strError = getString(R.string.ERROR_AUDIO_MESSAGE);
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    strError = getString(R.string.ERROR_INSUFFICIENT_PERMISSIONS_MESSAGE);
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    strError = getString(R.string.ERROR_NETWORK_MESSAGE);
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    strError = getString(R.string.ERROR_NO_MATCH_MESSAGE);
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    strError = getString(R.string.ERROR_RECOGNIZER_BUSY_MESSAGE);
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    strError = getString(R.string.ERROR_SERVER_MESSAGE);
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    strError = getString(R.string.ERROR_SPEECH_TIMEOUT_MESSAGE);
                    break;
            }
            strError += getString(R.string.TRY_AGAIN);
            TextView recong_tv = (TextView)findViewById(R.id.google_recog_wrod_txt);
            recong_tv.setText(strError);
        }
        @Override
        public void onResults(Bundle results) {
            String key = "";
            key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = results.getStringArrayList(key);
            String[] rs = new String[mResult.size()];
            mResult.toArray(rs);
            Log.i("[VoiceCommander]", "Recong onResults: " + rs);
            Log.i("[VoiceCommander]", "Recong onResults: " + mResult.toString());

            TextView recong_tv = (TextView)findViewById(R.id.google_recog_wrod_txt);
            recong_tv.setText(rs[0]);
            TextView matched_tv = (TextView)findViewById(R.id.matched_word_txt);
            matched_tv.setText(matchKOR2ENG(rs[0]));
            vc_node.command_publish(matched_tv.getText().toString());
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            String key = "";
            key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = partialResults.getStringArrayList(key);
            String[] rs = new String[mResult.size()];
            mResult.toArray(rs);
            Log.i("[VoiceCommander]", "Recong onPartialResults: " + mResult.toString());
            TextView recong_tv = (TextView)findViewById(R.id.google_recog_wrod_txt);
            recong_tv.setText("인식 중...." + mResult.toString());
        }
        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };

    public String matchKOR2ENG(String input){
        Log.i("[VoiceCommander]", "matchKOR2ENG" + input);
        if(input.contains("앞으로")){
            return "forward";
        }

        else if(input.contains("앞쪽")){
            return "forward";
        }

        else if(input.contains("뒤로")){
            return "backward";
        }

        else if(input.contains("뒤쪽")){
            return "backward";
        }
        else if(input.contains("오른쪽")){
            return "right";
        }
        else if(input.contains("왼쪽")){
            return "left";
        }
        else if(input.contains("그만")){
            return "stop";
        }
        else if(input.contains("멈추어")){
            return "stop";
        }
        else if(input.contains("멈춰")){
            return "stop";
        }
        else if(input.contains("조심")){
            return "warn";
        }
        else if(input.contains("위험")){
            return "danger";
        }
        return "";
    }

    public class VoiceCommanderNode implements NodeMain {
        public final java.lang.String NODE_NAME = "voice_commander";
        private Publisher<std_msgs.String> voice_command_publiser;
        private String pub_voice_command_topic_name = "voice_command";

        public VoiceCommanderNode(String voice_command_topic){
            pub_voice_command_topic_name = voice_command_topic;
        }
        @Override
        public GraphName getDefaultNodeName() {
            return null;
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            voice_command_publiser = connectedNode.newPublisher(pub_voice_command_topic_name , std_msgs.String._TYPE);
            voice_command_publiser.setLatchMode(true);
        }

        @Override
        public void onShutdown(Node node) {
        }

        @Override
        public void onShutdownComplete(Node node) {
        }
        @Override
        public void onError(Node node, Throwable throwable) {

        }

        void command_publish (String data){
            if(voice_command_publiser != null){
                std_msgs.String command = voice_command_publiser.newMessage();
                command.setData(data);
                voice_command_publiser.publish(command);
            }
        }

    }
//    /**
//     * Call Toast on UI thread.
//     * @param message Message to show on toast.
//     */
//    private void showToast(final String message)
//    {
//        runOnUiThread(new Runnable()
//        {
//            @Override
//            public void run() {
//                if (lastToast != null)
//                    lastToast.cancel();
//
//                lastToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
//                lastToast.show();
//            }
//        });
//    }

}
