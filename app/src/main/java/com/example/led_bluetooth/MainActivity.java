package com.example.led_bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private TextView tvState;
    private Spinner modeLed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvState = findViewById(R.id.tvState);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        Button btnScan = findViewById(R.id.btnScan);
        ListView listViewDevices = findViewById(R.id.listViewDevices);
        SeekBar seekBarBrightness = findViewById(R.id.seekBarBrightness);
        modeLed = findViewById(R.id.modeSpinner);
        initSpinner();

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    sendCommand("!m=" + (position + 2) + ";");
                }
                catch (Exception exception){
                    Toast.makeText(getApplicationContext(), "Произошла ошибка: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        tvState.setOnClickListener(v -> {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                showMessageBox(MainActivity.this,
                        "Подтверждение",
                        "Разорвать соединение с устройством?");
            } else {
                Toast.makeText(MainActivity.this,
                        "Нет активного соединения",
                        Toast.LENGTH_SHORT).show();
            }
        });
        modeLed.setOnItemSelectedListener(itemSelectedListener);

        btnScan.setOnClickListener(v -> {
            if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
                Toast.makeText(MainActivity.this, "Включите Bluetooth перед началом сканирования", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                pairedDevices = bluetoothAdapter.getBondedDevices();
                ArrayList<String> deviceList = new ArrayList<>();
                for (BluetoothDevice device : pairedDevices) {
                    deviceList.add(device.getName() + "\n" + device.getAddress());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
                listViewDevices.setVisibility(View.VISIBLE);
                listViewDevices.setAdapter(adapter);

                listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
                    BluetoothDevice device = (BluetoothDevice) pairedDevices.toArray()[position];
                    if (connectToDevice(device))
                        listViewDevices.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {sendCommand("!b=" + seekBar.getProgress() + ";");}
        });


    }
    public void showMessageBox(Context context, String title, String message) {
        if (context == null || ((Activity) context).isFinishing()) {
            return;
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Да", (dialog, which) -> {
                    closeConnection();
                    tvState.setText("отключено");
                })
                .setNegativeButton("Нет", null)
                .show();
    }
    // Подключение к устройству
    private boolean connectToDevice(BluetoothDevice device) {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Стандартный UUID SPP
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return false;
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(this, "Подключено к " + device.getName(), Toast.LENGTH_SHORT).show();
            tvState.setText("подключено");
            return true;
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка подключения, убедитесь что устройство включено", Toast.LENGTH_SHORT).show();
            tvState.setText("отключено");
            return false;
        }
    }
    private void sendCommand(String command) {
        try {
            if (outputStream != null) {
                outputStream.write(command.getBytes());
            }
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
            closeConnection();
        }
    }
    void closeConnection() {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                outputStream.close();
                bluetoothSocket.close();
                outputStream = null;
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            Log.e("Bluetooth", "Ошибка при закрытии соединения", e);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnection();
    }
    private void initSpinner(){
        String[] modes = { "Радуга (вся лента)", "Радуга (от начала к концу)",
                "Случайная смена цветов",
                "Бегающий светодиод (один)",  // возможность извенить цвет
                "Бегающий светодиод (много)", // возможность извенить цвет
                "Мигалки (вращение)",
                "Мигалки (на половину)",
                "Стробоскоп",  // задержку починить
                "Пульсация одним цветом", // возможность извенить цвет
                "Пульсация со сменой цветов", // удалить
                "Плавная смена яркости по вертикали (для кольца)",
                "Безумие красных светодиодов", // возможность извенить цвет
                "Безумие случайных цветов", // изменение скорости
                "Белый синий красный бегут по кругу", // изменение скорости
                "Пульсирует значок радиации",
                "Красный светодиод бегает по кругу", // вылетает и изменение цвета
                "Бело синий градиент",
                "Тоже хрень какая то",
                "Красные вспышки спускаются вниз", // изменение цвета
                "Полумесяц", // delete
                "Эффект пламени",
                "Радуга в вертикаьной плоскости",
                "Пакман", // delete
                "Безумие случайных вспышек",
                "Полицейская мигалка",
                "RGB пропеллер", // speed
                "Случайные вспышки красного в вертикаьной плоскости", // color
                "Зелёненькие бегают по кругу случайно", // color
                "Крутая плавная вращающаяся радуга", // speed
                "Крутая плавная вращающаяся радуга (в обратную сторону)", // delete
                "Чёт сломалось", // delete
                "Плавное заполнение цветом", // color
                "бегающие светодиоды", // color
                "линейный огонь", // color?
                "беготня секторов круга (не работает)", // delete
                "очень плавная вращающаяся радуга", // speed
                "случайные разноцветные включения",
                "бегущие огни", // color
                "случайные вспышки белого цвета", // speed, fix delay
                "случайные вспышки белого цвета на белом фоне",
                "бегущие каждые 3 (ЧИСЛО СВЕТОДИОДОВ ДОЛЖНО БЫТЬ НЕЧЁТНОЕ)", // delete
                "бегущие каждые 3 радуга (ЧИСЛО СВЕТОДИОДОВ ДОЛЖНО БЫТЬ КРАТНО 3)", // delete
                "бегущие каждые 3 радуга (ЧИСЛО СВЕТОДИОДОВ ДОЛЖНО БЫТЬ КРАТНО 3)", // delete
                "стробоскоп", // color, speed
                "прыгающие мячики", // delete
                "прыгающие мячики цветные" // delete
        };
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, modes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeLed.setAdapter(adapter);
    }
}