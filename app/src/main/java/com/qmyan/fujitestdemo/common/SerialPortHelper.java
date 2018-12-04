package com.qmyan.fujitestdemo.common;

import android.support.annotation.NonNull;
import android.util.Log;

import com.qmyan.fujitestdemo.serialport.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MichealYan
 * @date 2018/11/27
 * Email: 956462326@qq.com
 * Describe: 串口数据操作帮助类，实现串口数据的读写
 **/
public class SerialPortHelper {

    private static final String TAG = "SerialPortHelper";
    /**
     * 设备的串口
     */
    private static SerialPort mSerialPort;
    /**
     * 串口数据输入流
     */
    private static InputStream mInputStream;
    /**
     * 串口数据输出流
     */
    private static OutputStream mOutputStream;
    /**
     * 数据帧结构
     */
    private static DataFrameStructure mDataFrameStructure;
    /**
     * 是否停止读数串口数据（true停止，false继续）
     */
    private static boolean mStopped = false;

    /**
     * 已读取的数据帧数据的缓存
     */
    private static List<Integer> mDataFrameBuffer;

    /**
     * 数据校验回调
     */
    private static IDataCheckCallBack mDataCheckCallBack;
    /**
     * 数据接收回调
     */
    private static IDataReceivedCallBack mDataRecivedCallBack;
    /**
     * 执行串口读写操作的子线程
     */
    private static Runnable runnable;

    /**
     * 开始对串口数据进行处理
     *
     * @param deviceName          串口设备名称
     * @param baudrate            波特率
     * @param dataFrameStructure  串口每帧数据的数据结构
     * @param dataCheckCallBack   数据校验回调
     * @param dataRecivedCallBack 数据接收回调
     * @return 是否正常执行
     */
    public static boolean start(@NonNull String deviceName
            , int baudrate
            , @NonNull DataFrameStructure dataFrameStructure
            , @NonNull IDataCheckCallBack dataCheckCallBack
            , @NonNull IDataReceivedCallBack dataRecivedCallBack) {

        try {
            mSerialPort = new SerialPort(new File(deviceName), baudrate, 0);
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();
            mDataFrameStructure = dataFrameStructure;
            mDataCheckCallBack = dataCheckCallBack;
            mDataRecivedCallBack = dataRecivedCallBack;
            mDataFrameBuffer = new ArrayList<>();
            mStopped = false;
            runnable = new Runnable() {
                @Override
                public void run() {
                    readAndWrite();
                }
            };
            ThreadPoolManager.getInstance().execute(runnable);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "start: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 对串口设备进行读写
     */
    private static void readAndWrite() {
        while (!mStopped) {
            try {
                if (mInputStream.available() > 0) {
                    int read = mInputStream.read();
                    // 读取到一帧完整数据，进行响应
                    if (isAFrame(read)) {
                        List<Integer> temp = new ArrayList<>(mDataFrameBuffer);
                        mDataFrameBuffer.clear();
                        if (null != mDataRecivedCallBack) {
                            List<Integer> received = mDataRecivedCallBack.onReceived(temp);
                            if (received != null && received.size() > 0) {
                                byte[] bytes = integerListToByteArray(received);
                                mOutputStream.write(bytes);
                            }
                        } else {
                            Log.e(TAG, "readAndWrite: null == mDataRecivedCallBack");
                        }
                    }
                } else {
                    Thread.sleep(5L);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "readAndWrite: Caught an IOException when do mInputStream.available().Msg: \n" + e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "readAndWrite: Caught an InterruptedException when do Thread.sleep(5L).Msg: \n" + e.getMessage());
            }
        }
    }

    /**
     * 将整型集合转换为字节数组
     * @param list 整型集合
     * @return 字节数组
     */
    private static byte[] integerListToByteArray(List<Integer> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = (byte) list.get(i).intValue();
        }
        return bytes;
    }

    /**
     * 通过帧头和数据长度拆分出完整的一帧数据
     *
     * @return 如果刚好读取到一帧数据则返回True，否则返回False
     */
    private static boolean isAFrame(int read) {
        // 将读取的数据添加到本地的缓存中
        mDataFrameBuffer.add(read);

        // 数据帧头检测
        // 已读取数据不包含完整数据帧头，继续读取
        if (mDataFrameBuffer.size() < mDataFrameStructure.getmFrameHeaders().length) {
            return false;
        } else if (mDataFrameBuffer.size() == mDataFrameStructure.getmFrameHeaders().length) {
            // 已读取数据包含完整数据帧头，进行帧头校验
            int len = mDataFrameStructure.getmFrameHeaders().length;
            for (int i = 0; i < len; i++) {
                // 如果数据帧头中有部分对不上，则将从已经读取的数据帧数据缓存中移除第一位数据，并继续读取
                if (mDataFrameBuffer.get(i) != mDataFrameStructure.getmFrameHeaders()[i]) {
                    mDataFrameBuffer.remove(0);
                    Log.e(TAG, "isAFrame: 数据帧头检测失败：" + mDataFrameBuffer.get(i) + "::" + mDataFrameStructure.getmFrameHeaders()[i]);
                    return false;
                }
            }
        }

        // 获取有效数据长度
        if (mDataFrameBuffer.size() == (mDataFrameStructure.getmEffectiveDataLengthBytes() + mDataFrameStructure.getmEffectiveDataLengthPosition())) {
            int sum = 0;
            for (int i = 0; i < mDataFrameStructure.getmEffectiveDataLengthBytes(); i++) {
                sum += mDataFrameBuffer.get(mDataFrameStructure.getmEffectiveDataLengthPosition() + i);
            }
            Log.e(TAG, "isAFrame: 有效数据长度 " + sum);
            mDataFrameStructure.setmEffectiveDataLength(sum);
        }

        // 数据长度校验
        if (mDataFrameStructure.getmEffectiveDataLength() > 0 && mDataFrameBuffer.size() == mDataFrameStructure.getmTotalDataLength()) {
            // 校验值校验
            if (null != mDataCheckCallBack) {
                if (mDataCheckCallBack.check(mDataFrameBuffer)) {
                    return true;
                } else {
                    // 校验失败,清空缓存重新读取
                    mDataFrameBuffer.clear();
                    return false;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * 校验值校验回调接口
     */
    public interface IDataCheckCallBack {
        /**
         * 对接收的一帧数据进行校验，返回校验结果
         *
         * @param data 接收的数据
         * @return 校验结果
         */
        boolean check(List<Integer> data);
    }

    /**
     * 数据接收回调
     */
    public interface IDataReceivedCallBack {
        /**
         * 对接收的一帧数据进行处理，返回应答指令
         *
         * @param data 接收的数据
         * @return 应答指令
         */
        List<Integer> onReceived(List<Integer> data);
    }

    /**
     * 停止操作串口数据,释放回收资源
     */
    public static void stop() {
        mStopped = true;
        ThreadPoolManager.getInstance().remove(runnable);
        if (mDataFrameBuffer != null) {
            mDataFrameBuffer = null;
        }
        if (mDataFrameStructure != null) {
            mDataFrameStructure = null;
        }
        if (mDataCheckCallBack != null) {
            mDataCheckCallBack = null;
        }
        if (mDataRecivedCallBack != null) {
            mDataRecivedCallBack = null;
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInputStream = null;
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mOutputStream = null;
        }
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

}
