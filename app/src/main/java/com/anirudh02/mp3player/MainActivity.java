  package com.anirudh02.mp3player;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Runnable {
    ListView listViewMP3;

    ArrayList<String> mp3List;
    String selectedMP3;
    TextView name;
    TextView time;
    MediaPlayer music;
    Handler handler = new Handler();
    SeekBar seekbar;
    Thread thread;
    Button btn;

    //Set the path to the Download folder in the device external storage
    String mp3Path = Environment.getExternalStorageDirectory().getPath() + "/Download/";
    int curr = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("MP3 Player");
        //If the user has not already allowed the app to access their external storage to retrieve the MP3 files,
        //ask the user for permission to access external storage. The app will not open unless this permission is granted.
        String p1 = Manifest.permission.READ_EXTERNAL_STORAGE;
        String p2 = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int permissionReadStorage = ContextCompat.checkSelfPermission(this, p1);
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, p2);
        if(permissionReadStorage != PackageManager.PERMISSION_GRANTED
                || permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(getApplicationContext(), error.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        } else {
            File directory = new File(mp3Path);
            if(!directory.exists()) {
                directory.mkdirs();
            }
        }
        mp3List = new ArrayList<String>();
        name = (TextView)findViewById(R.id.select);
        time = (TextView)findViewById(R.id.time);
        seekbar = (SeekBar)findViewById(R.id.pos);
        btn = (Button)findViewById(R.id.play);
        //Retrieve the mp3 files from the Download folder and store them in an ArrayList
        File[] listFiles = new File(mp3Path).listFiles();
        if(listFiles == null) {
            Intent i = new Intent(this, error.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }
        String fileName, extName;
        for (File file : listFiles) {
            fileName = file.getName();
            extName = fileName.substring(fileName.length() - 3);
            if (extName.equals((String) "mp3"))
                mp3List.add(fileName);
        }
        Log.e("Size", mp3List.size() + "");
        Log.e("Path", mp3Path);
        //If the Download folder has no mp3 files, open up a new activity that tells the user to upload
        //mp3 files to the Download folder and then restart the app.
        //The flag is there to ensure the back button from the other activity closes the app and doesn't bring the
        //user back to this activity.
        if(mp3List.size() == 0) {
            Intent i = new Intent(this, error.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return;
        }

        //Populate the ListView that was created in the layout resource file for this activity with
        //the names of mp3 files in the Download folder and select the first mp3 file in the list by default
        listViewMP3 = (ListView) findViewById(R.id.listViewMP3);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_activated_1, mp3List);
        listViewMP3.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listViewMP3.setAdapter(adapter);
        listViewMP3.setItemChecked(0, true);
        //Set the timestamp to 0:00 by default to start.
        time.setText("Time: " + "00:00");
        //Whenever the user clicks on another mp3 file in the ListView, store the position of that file
        //in the list of mp3 files in an integer variable, curr, and call the click method, which will update the
        //app to play the newly selected mp3 file
        listViewMP3
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                                            int arg2, long arg3) {
                        curr = arg2;
                        click();
                    }
                });
        //By default, the first mp3 file, at index 0, is selected, so curr will be set to 0.
        curr = 0;
        //The selected mp3 variable will store the name of the mp3 file
        selectedMP3 = mp3List.get(curr);
        name.setText("Selected Music: " + selectedMP3);
        //The onselect method is called to setup the app to play the selected mp3 file
        onSelect(selectedMP3);
    }

    public void onSelect(String selectedMP3) {
        //Store the path of the mp3 file to be played in a string
        String path = mp3Path + selectedMP3;
        //Set the button text to "Play" since the music (at least when the app is first launched) isn't playing
        btn.setText(R.string.play);
        //Create a new instance of the MediaPlayer class to play the selected mp3 file, with looping set to false
        music = MediaPlayer.create(this, Uri.parse(Environment.getExternalStorageDirectory().getPath() + "/Download/" + selectedMP3));
        music.setLooping(false);
        //Create a listener that checks to see when the music is finished playing
        //When the music is finished playing, call the "Next" method.
        music.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                next();
            }
        });
        //Set the seekbar's length to the music's duration
        seekbar.setMax(music.getDuration());
        //Create a seekbar change listener
        // If the user moves the seekbar themselves, update the timestamp based on the location of the seekbar
        //and forward/rewind the music to that position
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    music.seekTo(i);
                    name.setText("Selected Music: " + selectedMP3);
                    int secs = (i/1000)%60;
                    int mins = ((i/1000) / 60) % 60;
                    int hrs = ((i/1000)/60)/60;
                    if(hrs > 0 && hrs < 10 && secs < 10 && mins< 10) {
                        time.setText("Time: 0" + hrs + ":0" + mins + ":0" + secs);
                    }
                    else if(hrs > 0 && hrs < 10 && secs >= 10 && mins < 10) {
                        time.setText("Time: 0" + hrs + ":0" + mins + ":" + secs);
                    }
                    else if(hrs > 0 && hrs < 10 && secs < 10 && mins >= 10) {
                        time.setText("Time: 0" + hrs + ":" + mins + ":0" + secs);
                    }
                    else if(hrs > 0 && hrs < 10 && secs >= 10 && mins >= 10) {
                        time.setText("Time: 0" + hrs + ":" + mins + ":" + secs);
                    }
                    else if(hrs > 0 && hrs >= 10 && secs < 10 && mins >= 10) {
                        time.setText("Time: " + hrs + ":" + mins + ":0" + secs);
                    }
                    else if(hrs > 0 && hrs >= 10 && secs >= 10 && mins <10) {
                        time.setText("Time: " + hrs + ":0" + mins + ":" + secs);
                    }
                    else if(hrs > 0 && hrs >= 10 && secs >= 10 && mins >= 10) {
                        time.setText("Time: " + hrs + ":" + mins + ":" + secs);
                    }
                    else if(hrs > 0 && hrs >- 10 && secs < 10 && mins < 10) {
                        time.setText("Time: " + hrs + ":0" + mins + ":0" + secs);
                    }
                    else if(hrs == 0 && secs < 10 && mins < 10) {
                        time.setText("Time: 0" + mins + ":0" + secs);
                    }
                    else if(hrs == 0 && secs >= 10 && mins < 10) {
                        time.setText("Time: 0" + mins + ":" + secs);
                    }
                    else if(hrs == 0 && secs < 10 && mins >= 10) {
                        time.setText("Time: " + mins + ":0" + secs);
                    }
                    else {
                        time.setText("Time: " + mins + ":" + secs);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });
    }
    //Next method that will be called when a mp3 file is finished playing.
    //If the last file in the list of mp3 files was played, the first mp3 file in the list will be played.
    //Otherwise, the next mp3 file in the list will be played.
    public void next() {
        if(curr == mp3List.size() - 1) {
            curr = 0;
        } else {
            curr++;
        }
        click();
    }
    //Whenever new music is about to be played, this method will first be called.
    //It will get the name of the mp3 file, reset the timestamp, select the mp3 file in the ListView,
    //call the SelectStop method which stops the current music,
    //call the onSelect method to continue to setup the app to play the new music,
    //and then call the Play method to play the music.
    public void click() {
        selectedMP3 = mp3List.get(curr);
        time.setText("Time: "+ "00:00");
        name.setText("Selected Music: ");
        selectStop();
        onSelect(selectedMP3);
        listViewMP3.setItemChecked(curr, true);
        play(null);
    }
    //Method to play the music when the app has been setup to play it, or when the music was previously paused.
    public void play(View view) {
        //If the music is currently playing, pause it.
        if(music.isPlaying()) {
            btn.setText("PLAY");
            music.pause();
        }
        //Otherwise, play the music from the position of the seekbar.
        else {
            name.setText("Selected Music: "+ selectedMP3);
            music.start();
            music.seekTo(seekbar.getProgress());
            //Create a new thread and start it
            thread = new Thread(this);
            thread.start();
            btn.setText("PAUSE");
        }
    }

    //This method stops the music being played when the stop button is pressed.
    public void stop(View view) {
        music.stop();
        btn.setText("PLAY");
        try {
            music.prepare();
        } catch(IllegalStateException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
        music.seekTo(0);
        seekbar.setProgress(0);
        time.setText("Time: 00:00");
    }
    //This method stops the music from being played when a new mp3 file is about to start playing
    //This method is similar to the Stop method, except that the name of the previously selected music
    //gets cleared.
    public void selectStop() {
        music.stop();
        btn.setText("PLAY");
        try {
            music.prepare();
        } catch(IllegalStateException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
        music.seekTo(0);
        seekbar.setProgress(0);
        time.setText("Time: 00:00");
        name.setText("Selected Music: ");
    }

    //This method is automatically executed while the thread is running and the music is playing.
    //It consistently updates the seekbar position as the music progresses and updates the timestamp accordingly.
    public void run() {
        while(music.isPlaying()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    seekbar.setProgress(music.getCurrentPosition());
                     int secs = (music.getCurrentPosition()/1000)%60;
                    int mins = ((music.getCurrentPosition()/1000) / 60) % 60;
                    int hrs = ((music.getCurrentPosition()/1000)/60)/60;
                    if(hrs > 0 && hrs < 10 && secs < 10 && mins< 10) {
                        time.setText("Time: 0" + hrs + ":0" + mins + ":0" + secs);
                    }
                    else if(hrs > 0 && hrs < 10 && secs >= 10 && mins < 10) {
                        time.setText("Time: 0" + hrs + ":0" + mins + ":" + secs);
                    }
                    else if(hrs > 0 && hrs < 10 && secs < 10 && mins >= 10) {
                        time.setText("Time: 0" + hrs + ":" + mins + ":0" + secs);
                    }
                    else if(hrs > 0 && hrs < 10 && secs >= 10 && mins >= 10) {
                        time.setText("Time: 0" + hrs + ":" + mins + ":" + secs);
                    }
                    else if(hrs > 0 && hrs >= 10 && secs < 10 && mins >= 10) {
                        time.setText("Time: " + hrs + ":" + mins + ":0" + secs);
                    }
                    else if(hrs > 0 && hrs >= 10 && secs >= 10 && mins <10) {
                        time.setText("Time: " + hrs + ":0" + mins + ":" + secs);
                    }
                    else if(hrs > 0 && hrs >= 10 && secs >= 10 && mins >= 10) {
                        time.setText("Time: " + hrs + ":" + mins + ":" + secs);
                    }
                    else if(hrs > 0 && hrs >- 10 && secs < 10 && mins < 10) {
                        time.setText("Time: " + hrs + ":0" + mins + ":0" + secs);
                    }
                    else if(hrs == 0 && secs < 10 && mins < 10) {
                        time.setText("Time: 0" + mins + ":0" + secs);
                    }
                    else if(hrs == 0 && secs >= 10 && mins < 10) {
                        time.setText("Time: 0" + mins + ":" + secs);
                    }
                    else if(hrs == 0 && secs < 10 && mins >= 10) {
                        time.setText("Time: " + mins + ":0" + secs);
                    }
                    else {
                        time.setText("Time: " + mins + ":" + secs);
                    }
                }
            });
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}