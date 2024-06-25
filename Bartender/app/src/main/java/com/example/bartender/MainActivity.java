package com.example.bartender;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 3;
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 2;
    private BluetoothAdapter mbtAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice DispositivoSeleccionado;
    private ConnectedThread MyConexionBT;
    private ArrayList<String> mNameDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;

    Button btnVodka,btnRon,btnSearch,btnConnect,btnDisconnect;
    Spinner cmbBt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        requestBluetoothConnectPermission();
        requestLocationPermission();

        btnVodka = findViewById(R.id.btnVodka);
        btnRon = findViewById(R.id.btnRon);
        cmbBt = findViewById(R.id.cmbBt);
        btnSearch = findViewById(R.id.btnSearch);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        cmbBt = findViewById(R.id.cmbBt);


        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,mNameDevices);
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cmbBt.setAdapter(deviceAdapter);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DispositivosVinculados();
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConectarDispBt();
            }
        });


        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btSocket!= null){
                    try{btSocket.close();}
                    catch (IOException e)
                    {
                        Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();
                    }
                }
                finish();
            }
        });


        btnRon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, "Se preparo su Vodka", Toast.LENGTH_SHORT).show();;
                try {
                    MyConexionBT.write('a');
                }
                catch (Exception e){
                    showToast("Bartender no encontrado.");
                }
            }
        });


        btnVodka.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(MainActivity.this, "Se preparo su Ron", Toast.LENGTH_SHORT).show();
                try{
                    MyConexionBT.write('b');
                }
                catch (Exception e){
                    showToast("Bartender no encontrado.");
                }
            }
        });



        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == MainActivity.REQUEST_ENABLE_BT){
                        Log.d(TAG, "Actividad Registrada");
                    }
                }
            });

    public void DispositivosVinculados(){
        mbtAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mbtAdapter == null){
            showToast("Bluetooth no disponible en este dispositivo");
            finish();
            return;
        }
        if(!mbtAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED){
                return;
            }
            someActivityResultLauncher.launch(enableBtIntent);
        }

        cmbBt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id){
                DispositivoSeleccionado = getBluetoothDeviceByName(mNameDevices.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView){
                DispositivoSeleccionado = null;
            }
        });

        Set<BluetoothDevice> pairedDevices = mbtAdapter.getBondedDevices();
        if(pairedDevices.size()>0){
            for(BluetoothDevice device : pairedDevices) {
                mNameDevices.add(device.getName());
            }
            deviceAdapter.notifyDataSetChanged();
        }
        else{
            showToast("No hay dispositivos emparejados");
        }
    }
    private void ConectarDispBt(){
        if(DispositivoSeleccionado == null){
            showToast("Selecciona un dispositivo Bluetooth");
            return;
        }
        try{
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED){
                return;
            }
            btSocket = DispositivoSeleccionado.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
            btSocket.connect();
            MyConexionBT = new ConnectedThread(btSocket);
            MyConexionBT.start();
            showToast("Conexion exitosa");
        }
        catch (IOException e){
            showToast("Error al conectar el dispositivo");
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            Log.d(TAG, "Permiso concedidio");
        }
        else{
            Log.d(TAG, "Permiso denegado");
        }
    }

    private BluetoothDevice getBluetoothDeviceByName(String name){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED){
            Log.d(TAG, "======> ActivityCompat.checkselfPermission");
        }
        Set<BluetoothDevice> pairedDevices = mbtAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices){
            if(device.getName().equals(name)){
                return device;
            }
        }
        return null;
    }

    private void requestBluetoothConnectPermission(){
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
    }

    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_FINE_LOCATION_PERMISSION);
    }

    private class ConnectedThread extends Thread {
        private final OutputStream mmOutStream;
        ConnectedThread(BluetoothSocket socket){
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e){
                showToast("Error al crear el flujo de datos");
            }
            mmOutStream = tmpOut;
        }
        public void write(char input){
            try{
                mmOutStream.write((byte)input);
            }
            catch (IOException e){
                Toast.makeText(getBaseContext(), "La conexion fallo", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void showToast(final String message){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}