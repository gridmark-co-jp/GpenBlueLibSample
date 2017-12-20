package mydomain.jp.co.gpenbluelibsample;

import android.bluetooth.BluetoothDevice;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import gridmark.jp.co.gpenbluelibrary.GPenBlueLib;
import gridmark.jp.co.gpenbluelibrary.GPenBlueListener;
import gridmark.jp.co.gpenbluelibrary.GPenBlueNotify;

public class MainActivity extends AppCompatActivity implements GPenBlueListener{

    // ----定数--------------------------------------------------
    // GPenBlueの固有値(GPenBlueならば全個体が共通の値を持つパラメータ)
    final private String DEVICE_NAME = "Smart Pen-01";  // デバイスの名前
    final private String SERVICE_UUID = "0783b03e-8535-b5a0-7140-a304d2495cb7"; // サービスUUID
    final private String CHARACTERISTIC_UUID = "0783b03e-8535-b5a0-7140-a304d2495cb8";  // キャラスタリクティックUUID

    // GPenBlue１本１本異なる値（この値は適宜変更すること）
    final private String DEVICE_ADDRESS = "DD:11:05:09:01:8E";  // デバイスのアドレス

    // 変数保存キー
    final private static String SAVE_TEXT_INFO = "SAVE_TEXT_INFO";
    final private static String SAVE_TEXT_DATA = "SAVE_TEXT_DATA";

    // ----変数--------------------------------------------------
    // クラス
    private Common mCommon;
    private GPenBlueLib mGPenBlueLib;   // ライブラリクラス

    // BTデバイス関連
    private BluetoothDevice mBluetoothDevice;   // BTデバイス

    // コンポーネント
    private TextView mTextViewInfo; // 画面上のテキストビュー(上)
    private TextView mTextViewData; // 画面上のテキストビュー(下)

    // ----イベント--------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 変数初期化
        mCommon = new Common();
        mGPenBlueLib = new GPenBlueLib(this.getApplication(), new GPenBlueNotify(), this);  // ライブラリクラス初期化
        mTextViewInfo = (TextView)findViewById(R.id.textView1);
        mTextViewData = (TextView)findViewById(R.id.textView2);

        SetText("", 0);
        SetText("", 1);

        // button1をタップしたときのイベント
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ScanBtDevices();    // 手順１：GPenBlueを取得
            }
        });

        // button2をタップしたときのイベント
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectBtDevice();  // 手順２：GPenBlueを接続
            }
        });

        // button3をタップしたときのイベント
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DisConnectBtDevice();   // 手順３：GPenBTの接続を解除
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        mGPenBlueLib.FromOnPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Activity破棄時（主に画面回転時）に表示中のtextView1&2文字列を保存
        outState.putString(SAVE_TEXT_INFO, mTextViewInfo.getText().toString());
        if(mTextViewData.getVisibility() == View.VISIBLE)
            outState.putString(SAVE_TEXT_DATA, mTextViewData.getText().toString());
        else
            outState.putString(SAVE_TEXT_DATA, "");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // 保存しておいたtextView1&2文字列を戻す
        SetText(savedInstanceState.getString(SAVE_TEXT_INFO), 0);
        SetText(savedInstanceState.getString(SAVE_TEXT_DATA), 1);
    }

    // スキャン処理が完了したときのイベント
    public void ScanComplated(ArrayList<BluetoothDevice> devices){
        if(devices.size() <= 0) {
            SetText("BTデバイスが見つかりませんでした / スキャンを中止しました", 0);
            return;
        }
        for(BluetoothDevice device:devices){
            if(device.getName().equals(DEVICE_NAME) && device.getAddress().equals(DEVICE_ADDRESS)){
                mBluetoothDevice = device;  // デバイス情報を１つ記憶する
                SetText("BTデバイスを取得しました: " + "name=" + mBluetoothDevice.getName() + ", address=" + mBluetoothDevice.getAddress(), 0);
                return;
            }
        }
    }

    // 接続処理が完了したときのイベント
    public void ConnectComplated(int error){
        if(error == 0){
            SetText("BTデバイスの接続が完了しました。\r\nドットコードをタッチしてください。", 0);
            SetText("", 1);
        }else{
            SetText("BTデバイスと接続できませんでした エラーコード=" + String.format("%d", error), 0);
        }
    }

    // 接続解除処理が完了したときのイベント
    public void DisConnectComplated(){
        SetText("BTデバイスの接続を解除しました", 0);
        SetText("", 1);
        mTextViewData.setVisibility(View.INVISIBLE);
    }

    // GPenBlueが発信するバイトデータを受け取るイベント
    public void RecieveGpenData(byte[] data){
        // 受信データ解析
        if(data != null) {
            // 変数初期化
            int dotcode = -1;   // アクティブコード
            Point xy = new Point(-1, -1);   // XYコード
            StringBuilder output = new StringBuilder(); // 画面上に表示する文字列

            // 通常フォーマット
            if(data.length == 7){
                if(data[0] == (byte)0xFF && data[1] == (byte)0xFD){
                    // 解析OK
                    if(mCommon.ByteIndex(data[2], 7)) {
                        // Activeコード
                        if (!mCommon.ByteIndex(data[2], 3)) {
                            // Activeコード取得(data:3,4,5,6)
                            if(data[3] >= 0){
                                if(data[3] >= 64 && data[3] <= 127)
                                    data[3] -= 0x40;    // パリティ削除
                            }else{
                                if(data[3] < -64)
                                    data[3] += 0x80;    // パリティ削除
                                else
                                    data[3] += 0x40;    // パリティ削除
                            }
                            dotcode = ((data[3] & 0xFF) << (8 * 3)) + ((data[4] & 0xFF) << (8 * 2)) + ((data[5] & 0xFF) << (8 * 1)) + ((data[6] & 0xFF) << (8 * 0));
                            output.append("dotcode = " + dotcode);
                            SetText(output.toString(), 1);
                            // 前回取得したドットコードと異なる
                            if (!mCommon.ByteIndex(data[2], 4))
                                DispatchActiveCode(dotcode);
                        }
                        // XYコード(通常版)
                        else {
                            // Activeコード取得(data:3)
                            dotcode = data[3] & 0xFF;
                            output.append("dotcode = " + dotcode);
                            // 座標値（整数部分）(data:4,5)
                            xy.set(data[4] & 0xFF, data[5] & 0xFF);
                            // 座標値（小数部分）(data:6)
                            int xf = ((data[6] >> 4) & 0x0F) & 0xFF;
                            int yf = (data[6] & 0x0F) & 0xFF;
                            // 座標値（整数部分と小数部分を合成）
                            xy.set((xy.x << 4) + xf, (xy.y << 4) + yf);
                            output.append(", (x, y) = (" + xy.x + ", " + xy.y + ")");
                            SetText(output.toString(), 1);
                        }
                    }
                }
                // 解析NG
                else{
                }
            }
            // 短縮フォーマット
            else if(data.length == 4){
                // XYコード(短縮版)
                if((data[0] & 0xFF) == 0xFF) {
                    // 座標値（整数部分）(data:1,2)
                    xy.set(data[1] & 0xFF, data[2] & 0xFF);
                    // 座標値（小数部分）(data:3)
                    int xf = ((data[3] >> 4) & 0x0F) & 0xFF;
                    int yf = (data[3] & 0x0F) & 0xFF;
                    // 座標値（整数部分と小数部分を合成）
                    xy.set((xy.x << 4) + xf, (xy.y << 4) + yf);
                    output.append("(x, y) = (" + xy.x + ", " + xy.y + ")");
                    SetText(output.toString(), 1);
                }
            }
        }
    }

    // ライブラリ側で例外が発生したときのイベント
    public void RecieveExceptionMessage(String msg){
        Log.d("TAG", "例外を検知しました message = " + msg);
    }

    // ----メソッド--------------------------------------------------
    // テキストビューに文字列を表示する
    private void SetText(final String msg, final int id){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(id == 0)
                    mTextViewInfo.setText(msg);
                else if(id == 1)
                    mTextViewData.setText(msg);
            }
        });
    }

    // 周辺をスキャンしてBTデバイスを取得する
    private void ScanBtDevices(){
        SetText("BTデバイスを検索中...", 0);
        int result = mGPenBlueLib.ScanBtDevices(5000);  // 5000ミリ秒周辺をスキャンする
        if(result != 0)
            SetText(String.format("スキャンを開始できませんでした エラーコード=%d", result), 0);
    }

    // スキャンを中止する
    private void StopScanBtDevices(){
        int result = mGPenBlueLib.StopScanBtDevices();
    }

    // BTペンを接続する
    private void ConnectBtDevice(){
        if(mBluetoothDevice == null){
            SetText("デバイスが選択されていません", 0);
        }else {
            mTextViewData.setVisibility(View.VISIBLE);
            int result = mGPenBlueLib.ConnectBtDevice(mBluetoothDevice, SERVICE_UUID, CHARACTERISTIC_UUID);
            if (result == 0) {
                SetText("BTデバイスを接続中...", 0);
                SetText("", 1);
            }
            else
                SetText(String.format("接続できませんでした エラーコード=%d", result), 0);
        }
    }

    // BTペンの接続を解除する
    public void DisConnectBtDevice(){
        int result = mGPenBlueLib.DisconnectBtDevice();
        if(result != 0)
            SetText(String.format("BTデバイスとの接続を解除できませんでした エラーコード=%d", result), 0);
    }

    // アクティブコードに対応付けた自作機能を実行する
    private void DispatchActiveCode(int dotcode){
        // ex)画像を表示する、動画を再生する、BTペンの接続を解除する など
        if(dotcode == 1){
            // ex)画像Aを表示する
        }else if(dotcode == 2){
            // ex)画像Bを表示する
        }
    }
}
