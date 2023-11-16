package com.example.nfcalarm.ui.alarm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.nfcalarm.AlarmReceiver;
import com.example.nfcalarm.R;
import com.example.nfcalarm.databinding.FragmentAlarmBinding;

import java.util.Calendar;
import java.util.Date;

public class AlarmFragment extends Fragment {

    private FragmentAlarmBinding binding;
    AlertDialog setAlarmDialog;
    Button setAlarmBtn;
    LinearLayout alarmLayout0, alarmLayout1, alarmLayout2;
    AlarmViewModel alarmViewModel;
    SharedPreferences sharedPreferences;
    TextView alarmTimeTxt;
    NfcAdapter mNfcAdapter;
    AlarmManager alarmManager;
    Calendar calendar, calendar_now;
    int hour, minute;
    PendingIntent pendingIntent;
    AlarmReceiver alarmReceiver = new AlarmReceiver();
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String KEY_TAG = "tagID";
    public static final String KEY_TIME = "alarmTime";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sharedPreferences = getContext().getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);

        Log.i("DEBUG", "onCreate: " + ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS));

        binding = FragmentAlarmBinding.inflate(inflater, container, false);
        alarmLayout0 = binding.alarmStatus0View;
        alarmLayout1 = binding.alarmStatus1View;
        alarmLayout2 = binding.alarmStatus2View;

        alarmViewModel =
                new AlarmViewModel(alarmLayout0, alarmLayout1, alarmLayout2);

        View root = binding.getRoot();
        buildSetAlarmDialog();
        setAlarmBtn = binding.setAlarmBtn;
        alarmTimeTxt = binding.alarmTimeTxt;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(super.getActivity());

        setAlarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAlarmDialog.show();
            }
        });

        checkViewStatus();

        return root;
    }

    void checkViewStatus(){
        String tagID = sharedPreferences.getString(KEY_TAG, null);
        String alarmTime = sharedPreferences.getString(KEY_TIME, null);
        if (tagID == null){
            alarmViewModel.changeAlarmPageStatus(0);
//            tagStatus = 0;
        } else if (alarmTime == null){

            alarmViewModel.changeAlarmPageStatus(1);
//            tagStatus = 1;
        } else {
            alarmTimeTxt.setText(alarmTime);
            alarmViewModel.changeAlarmPageStatus(2);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    @Override
    public void onResume() {
        super.onResume();

        if(mNfcAdapter!= null) {
            Bundle options = new Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            mNfcAdapter.enableReaderMode(super.getActivity(),
                    this::onTagDiscovered,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if(mNfcAdapter!= null) {
            mNfcAdapter.disableReaderMode(super.getActivity());
            mNfcAdapter.disableForegroundDispatch(super.getActivity());
        }
    }


    public void onTagDiscovered(Tag tag) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (alarmViewModel != null){
            if (alarmViewModel.getAlarmPageStatus() == 2){
                if (sharedPreferences.getString(KEY_TAG, null).equals(bytesToHexString(tag.getId())) ){

                    editor.remove(KEY_TIME);
                    editor.apply();

                    alarmReceiver.stopAlarm();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            alarmViewModel.changeAlarmPageStatus(1);
                        }
                    });

                    Toast.makeText(getContext(), "Alarm Turned Off", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getContext(), "Tag Not Recognized", Toast.LENGTH_SHORT).show();
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        checkViewStatus();
                    }
                });
            }
        }
    }


    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length == 0) {
            return null;
        }

        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }

        return stringBuilder.toString();
    }

    private void buildSetAlarmDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.register_card_dialog, null);

        TimePicker timePickerAlarm = view.findViewById(R.id.timePickerAlarm);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        builder.setView(view);
        builder.setTitle("Set Alarm")
                .setPositiveButton(
                        "Set Alarm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                hour = timePickerAlarm.getHour();
                                minute = timePickerAlarm.getMinute();
                                editor.putString(KEY_TIME, (hour < 10 ? "0" + hour : hour) + ":" + (minute < 10 ? "0" + minute : minute));
                                editor.apply();

                                setAlarm();
                                createNotificationChannel();


                                checkViewStatus();
                            }
                        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        setAlarmDialog = builder.create();
    }



    @SuppressLint("ScheduleExactAlarm")
    void setAlarm(){
        alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Date date = new Date();

        calendar_now = Calendar.getInstance();
        calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar_now.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.before(calendar_now)){
            calendar.add(Calendar.DATE, 1);
        }

        Intent intent = new Intent(getContext(), alarmReceiver.getClass());
        pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_MUTABLE);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

       Toast.makeText(getContext(), "Alarm Created", Toast.LENGTH_SHORT).show();
    }


    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "Alarm Reminder";
            String description = "Tap NFC to turn off alarm";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel("AlarmNotif", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


}

