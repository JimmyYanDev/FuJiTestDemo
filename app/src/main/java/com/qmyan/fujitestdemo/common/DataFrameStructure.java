package com.qmyan.fujitestdemo.common;

/**
 * @author MichealYan
 * @date 2018/11/27
 * Email: 956462326@qq.com
 * Describe: 数据帧的结构
 **/
public class DataFrameStructure {
    /**
     * 数据帧帧头
     */
    private int[] mFrameHeaders;
    /**
     * 有效数据长度所占字节数(数据长度：指有效计算范围内的字节数)
     */
    private int mEffectiveDataLengthBytes;
    /**
     * 有效数据长度的起始位置
     */
    private int mEffectiveDataLengthPosition;
    /**
     * 有效数据长度
     */
    private int mEffectiveDataLength;
    /**
     * 辅助数据长度（除有效数据外，其他数据的总长度）
     */
    private int mAuxiliaryDataLength;
    /**
     * 数据总长度
     */
    private int mTotalDataLength;

    public DataFrameStructure(int[] mFrameHeaders
            , int mEffectiveDataLengthBytes
            , int mEffectiveDataLengthPosition
            , int mAuxiliaryDataLength) {
        this.mFrameHeaders = mFrameHeaders;
        this.mEffectiveDataLengthBytes = mEffectiveDataLengthBytes;
        this.mEffectiveDataLengthPosition = mEffectiveDataLengthPosition;
        this.mAuxiliaryDataLength = mAuxiliaryDataLength;
    }

    public int[] getmFrameHeaders() {
        return mFrameHeaders;
    }

    public int getmEffectiveDataLengthBytes() {
        return mEffectiveDataLengthBytes;
    }

    public int getmEffectiveDataLengthPosition() {
        return mEffectiveDataLengthPosition;
    }

    public int getmEffectiveDataLength() {
        return mEffectiveDataLength;
    }

    public void setmEffectiveDataLength(int mEffectiveDataLength) {
        this.mEffectiveDataLength = mEffectiveDataLength;
        this.mTotalDataLength = mEffectiveDataLength + mAuxiliaryDataLength;
    }

    public int getmTotalDataLength() {
        return mTotalDataLength;
    }
}
