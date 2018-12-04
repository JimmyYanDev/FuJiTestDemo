package com.qmyan.fujitestdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.qmyan.fujitestdemo.common.DataFrameStructure;
import com.qmyan.fujitestdemo.common.SerialPortHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";

    private int oldSN = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        jusDoIt();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        jusDoIt();
    }

    private void jusDoIt() {
        final DataFrameStructure lyfDataFrameStructure = new DataFrameStructure(new int[]{204}, 1, 2, 4);
        final DataFrameStructure aucmaDataFrameStructure = new DataFrameStructure(new int[]{254, 85, 239}, 1, 3, 4);
        final DataFrameStructure fujiDataFrameStructure = new DataFrameStructure(new int[]{2}, 2, 1, 3);
        boolean startResult = SerialPortHelper.start("/dev/ttymxc2"
                , 9600
                , fujiDataFrameStructure
                , new SerialPortHelper.IDataCheckCallBack() {
                    @Override
                    public boolean check(List<Integer> data) {
                        return true;
                    }
                }
                , new SerialPortHelper.IDataReceivedCallBack() {
                    @Override
                    public List<Integer> onReceived(List<Integer> data) {
                        printCMD("onReceived: ", data);

                        return handleData(data);
                    }
                });
        if (startResult) {
            Log.e(TAG, "串口打开成功");
        }
    }

    private void printCMD(String tag, List<Integer> data) {
        Log.e(TAG, tag + data.toString());
        StringBuffer stringBuffer = new StringBuffer();
        for (Integer i : data) {
            String s = Integer.toHexString(i).toUpperCase();
            if (s.length() == 1) {
                stringBuffer.append(0);
            }
            stringBuffer.append(s);
            stringBuffer.append(" ");
        }
        Log.e(TAG, tag + stringBuffer.toString());
    }

    private List<Integer> handleData(List<Integer> data) {
        // 获取数据帧指令类型
        int cmd = data.get(3);
        // 获取该条数据帧序号
        int sn = data.get(4);
        // 存放pc的响应指令
        List<Integer> response = new ArrayList<Integer>();
        switch (cmd) {
            case 160:
                // [ACK]肯定回答
                isACK = true;
                Log.e(TAG, "handleData: [ACK]肯定回答");
                break;
            case 161:
                // [NAK]否定回答
                isACK = false;
                Log.e(TAG, "handleData: [NAK]否定回答");
                break;
            case 162:
                // [POLL]状态查询
                Log.e(TAG, "handleData: [POLL]状态查询");
                if (flag && isStartedComplete) {
                    response = vendOut(sn, 1, 17);
//                    response = configVMCTime(sn);
//                    response = getStatus(sn, 1);
                    flag = false;
                }
                if (!flag && isACK && isStartedComplete) {
                    response = vendOut(sn, 2, 23);
                    isACK = false;
                }
                break;
            case 224:
                // [Service_RPT]VMC维护状态报告
                int serviceReportType = data.get(5);
                if (serviceReportType == 1) {
                    // VMC开关门事件
                    int doorSwitch = data.get(6);
                    if (doorSwitch == 0) {
                        Log.e(TAG, "handleData: 门关闭");
                    }
                    if (doorSwitch == 1) {
                        Log.e(TAG, "handleData: 门开启");
                    }
                }
                if (serviceReportType == 2) {
                    // 进入/退出维护模式
                    int serviceMode = data.get(6);
                    if (serviceMode == 0) {
                        Log.e(TAG, "handleData: 退出维护模式");
                    }
                    if (serviceMode == 1) {
                        Log.e(TAG, "handleData: 进入维护模式");
                    }
                }
                if (serviceReportType == 3) {
                    // 维护按键报告
                    int buttonNum = data.get(6);
                    Log.e(TAG, "handleData: 按下按键" + buttonNum);
                }
                break;
            case 225:
                // [Action_RPT]VMC行为报告
                int actionReportType = data.get(5);
                if (actionReportType == 1) {
                    // VMC启动状态报告
                    int standBy = data.get(6);
                    if (standBy == 0) {
                        Log.e(TAG, "handleData: VMC启动中");
                    }
                    if (standBy == 1) {
                        isStartedComplete = true;
                        Log.e(TAG, "handleData: VMC启动完成");
                    }
                }
                if (actionReportType == 2) {
                    // VMC出货结果报告
                    // 货道号
                    int column = data.get(6);
                    // 商品ID
                    String spid = Integer.toHexString(data.get(7)) + Integer.toHexString(data.get(8));
                    int status = data.get(9);
                    if (status == 1) {
                        // 出货成功
                        Log.e(TAG, "handleData: 出货成功-" + column + "-" + spid);
                    } else {
                        // 出货失败
                        Log.e(TAG, "handleData: 出货失败-" + column + "-" + spid);
                    }
                }
                break;
            case 226:
                // [Info_RPT]VMC信息报告
                int infoReportType = data.get(5);
                switch (infoReportType) {
                    case 1:
                        // VMC系统配置报告
                        // 售货机机种
                        int machineType = data.get(6);
                        String machineTypeDecode = machineType == 1 ? "饮料机" : (machineType == 2 ? "咖啡机" : "Twistar");
                        Log.e(TAG, "handleData: 售货机机种-" + machineTypeDecode);
                        // 售货机实际货道数
                        int columnNum = data.get(21);
                        Log.e(TAG, "handleData: 售货机实际货道数-" + columnNum);
                        break;
                    case 2:
                        // VMC能力
                        // 支持的离线数据条数
                        int offlineDataMaxNum = data.get(6) << 3 * 4
                                + data.get(7) << 2 * 4
                                + data.get(8) << 4
                                + data.get(9);
                        Log.e(TAG, "handleData: 支持的离线数据条数-" + offlineDataMaxNum);
                        break;
                    case 3:
                        // VMC现在时间
                        String dateTime = parseTime(data, 6);
                        Log.e(TAG, "handleData: VMC现在时间-" + dateTime);
                        break;
                    case 4:
                        // 货道冷热模式
                        // 各货道冷热模式
                        for (int i = 7; i < (data.size() - 2); i++) {
                            Log.e(TAG, "handleData: 货道冷热模式"
                                    + (i - 6)
                                    + "-"
                                    + (data.get(i) == 1 ? "制冷" : "加热"));
                        }
                        break;
                    case 5:
                        // 货道现金单价
                        // 各货道现金单价
                        for (int i = 7; i < (data.size() - 2); i += 2) {
                            Log.e(TAG, "handleData: 货道现金单价"
                                    + (i - 6)
                                    + "-"
                                    + (data.get(i) << 4 + data.get(i + 1)));
                        }
                        break;
                    case 6:
                        // 货道非现金单价
                        // 各货道非现金单价
                        for (int i = 7, count = 1; i < (data.size() - 2); i += 2, count++) {
                            Log.e(TAG, "handleData: 货道非现金单价"
                                    + (count)
                                    + "-"
                                    + (data.get(i) << 4 + data.get(i + 1)));
                        }
                        break;
                    case 7:
                        // 货道商品编码
                        // 各货道商品编码
                        for (int i = 7, count = 1; i < (data.size() - 2); i += 2, count++) {
                            Log.e(TAG, "handleData: 货道商品编码"
                                    + (count)
                                    + "-"
                                    + (data.get(i) << 4 + data.get(i + 1)));
                        }
                        break;
                    case 8:
                        // 连续购买个数
                        int saleMaxNum = data.get(6);
                        Log.e(TAG, "handleData: 连续购买个数-" + saleMaxNum);
                        break;
                    case 10:
                        // 智能控制盒软件版本
                        StringBuffer softwareVersion = new StringBuffer();
                        for (int i = 6; i < (data.size() - 2); i++) {
                            softwareVersion.append(data.get(i));
                        }
                        Log.e(TAG, "handleData: 智能控制盒软件版本-" + softwareVersion.toString());
                        break;
                    case 13:
                        // 机器编号
                        StringBuffer machineID = new StringBuffer();
                        StringBuffer nc = new StringBuffer();
                        for (int i = 6; i < 14; i++) {
                            machineID.append(data.get(i));
                        }
                        Log.e(TAG, "handleData: 机器编号-" + machineID.toString());
                        for (int i = 14; i < 18; i++) {
                            nc.append(data.get(i));
                        }
                        Log.e(TAG, "handleData: NC-" + nc.toString());
                        break;
                    case 19:
                        // 库内冷热模式
                        int room1HCCode = data.get(6);
                        int room2HCCode = data.get(7);
                        int room3HCCode = data.get(8);
                        Log.e(TAG, "handleData: 库内冷热模式:1号库-" + (room1HCCode == 1 ? "加热" : "制冷"));
                        Log.e(TAG, "handleData: 库内冷热模式:2号库-" + (room2HCCode == 1 ? "加热" : "制冷"));
                        Log.e(TAG, "handleData: 库内冷热模式:3号库-" + (room3HCCode == 1 ? "加热" : "制冷"));
                        break;
                    case 20:
                        // 累计销售信息
                        StringBuffer aggNum = new StringBuffer();
                        for (int i = 6; i < (data.size() - 10); i += 4) {
                            int count = data.get(i) << 3 * 4
                                    + data.get(i + 1) << 2 * 4
                                    + data.get(i + 2) << 4
                                    + data.get(i + 3);
                            aggNum.append(count);
                            aggNum.append(" ");
                        }
                        Log.e(TAG, "handleData: 累计销售个数、累计销售金额" +
                                "、累计现金销售个数、累计现金销售金额" +
                                "、累计非现金销售个数、累计非现金销售金额" +
                                "、期间合计销售个数和期间合计销售金额分别为：" + aggNum.toString());
                        break;
                    case 21:
                        // 各货道合计销售个数
                        for (int i = 7, count = 1; i < (data.size() - 2); i += 3, count++) {
                            Log.e(TAG, "handleData: 货道合计销售个数"
                                    + (count)
                                    + "-"
                                    + (data.get(i) << 8 + data.get(i + 1) << 4 + data.get(i + 2)));
                        }
                        break;
                    case 22:
                        // 各货道合计销售金额
                        for (int i = 7, count = 1; i < (data.size() - 2); i += 3, count++) {
                            Log.e(TAG, "handleData: 货道合计销售金额"
                                    + (count)
                                    + "-"
                                    + (data.get(i) << 8 + data.get(i + 1) << 4 + data.get(i + 2)));
                        }
                        break;
                    case 29:
                        // 主控软件版本
                        StringBuffer masterVersion = new StringBuffer();
                        for (int i = 6; i < (data.size() - 2); i++) {
                            masterVersion.append(data.get(i));
                        }
                        Log.e(TAG, "handleData: 主控软件版本-" + masterVersion);
                        break;
                    case 30:
                        // 照明动作模式
                        int lampMode = data.get(6);
                        Log.e(TAG, "handleData: 照明动作模式-" + (lampMode == 0 ? "OFF" : (lampMode == 1 ? "ON" : "AUTO")));
                        break;
                    case 34:
                        // 加热节电时间带
                        StringBuffer heaterTime = new StringBuffer();
                        heaterTime.append("开始时间：");
                        heaterTime.append(data.get(6));
                        heaterTime.append(":");
                        heaterTime.append(data.get(6));
                        heaterTime.append(" 结束时间：");
                        heaterTime.append(data.get(7));
                        heaterTime.append(":");
                        heaterTime.append(data.get(8));
                        Log.e(TAG, "handleData: 加热节电时间带-" + heaterTime.toString());
                        break;
                    case 35:
                        // 制冷节电时间带
                        StringBuffer compressorTime = new StringBuffer();
                        compressorTime.append("开始时间：");
                        compressorTime.append(data.get(6));
                        compressorTime.append(":");
                        compressorTime.append(data.get(6));
                        compressorTime.append(" 结束时间：");
                        compressorTime.append(data.get(7));
                        compressorTime.append(":");
                        compressorTime.append(data.get(8));
                        Log.e(TAG, "handleData: 制冷节电时间带-" + compressorTime.toString());
                        break;
                    case 37:
                        // 加热库内ON/OFF温度
                        // 加热库内的库号
                        int roomNo = data.get(6);
                        // 加热库的ON/OFF温度
                        int roomTemp = data.get(7) << 4 + data.get(8);
                        Log.e(TAG, "handleData: 加热库内ON/OFF温度-" + roomNo + ":" + roomTemp);
                        break;
                    case 50:
                        // 货道按钮配置状态
                        // 各货道按钮的配置状态
                        for (int i = 7, count = 1; i < (data.size() - 2); i++, count++) {
                            Log.e(TAG, "handleData: 货道按钮配置状态" + count + ":" + (data.get(i) == 1 ? "未配置按钮" : "已配置按钮"));
                        }
                        break;
                    default:
                        break;
                }
                break;
            case 227:
                // [Status_RPT]VMC系统状态报告
                int statusReportType = data.get(5);
                switch (statusReportType) {
                    case 1:
                        // VMC系统状态报告
                        // VMC销售状态
                        int saleStatus = data.get(6);
                        // 工作模式
                        int workPattern = data.get(7);
                        // 开关门状态
                        int doorSwitch = data.get(8);
                        // 硬币器连接状态
                        int coinConnection = data.get(9);
                        // 纸币器连接状态
                        int billConnection = data.get(10);
                        // 硬币5角缺币状态
                        int coinType5Lack = data.get(11);
                        // 硬币1元缺币状态
                        int coinType1Lack = data.get(14);
                        // 纸币器停用状态
                        int billStatus = data.get(15);
                        Log.e(TAG, "handleData: VMC系统状态报告-VMC销售状态:" + (saleStatus == 0 ? "销售停止" : "可销售"));
                        Log.e(TAG, "handleData: VMC系统状态报告-工作模式:" + (workPattern == 1 ? "待机可销售模式" : (workPattern == 2 ? "投币后等待操作模式" : (workPattern == 4 ? "维护模式" : "售货机销售暂停状态"))));
                        Log.e(TAG, "handleData: VMC系统状态报告-开关门状态:" + (doorSwitch == 0 ? "门关" : "门开"));
                        Log.e(TAG, "handleData: VMC系统状态报告-硬币器连接状态:" + (coinConnection == 0 ? "未连接" : "连接中"));
                        Log.e(TAG, "handleData: VMC系统状态报告-纸币器连接状态:" + (billConnection == 0 ? "未连接" : "连接中"));
                        Log.e(TAG, "handleData: VMC系统状态报告-硬币5角缺币状态:" + (coinType5Lack == 0 ? "不缺币" : "缺币"));
                        Log.e(TAG, "handleData: VMC系统状态报告-硬币1元缺币状态:" + (coinType1Lack == 0 ? "不缺币" : "缺币"));
                        Log.e(TAG, "handleData: VMC系统状态报告-纸币器停用状态:" + (billStatus == 0 ? "未停用" : "停用"));
                        break;
                    case 2:
                        //  VMC现在故障状态
                        // 故障个数
                        int errorNum = data.get(6);
                        // 故障代码
                        StringBuffer errorCodes = new StringBuffer();
                        for (int i = 7; i < (data.size() - 2); i += 2) {
                            int errorCode = data.get(i) << 4 + data.get(i + 1);
                            errorCodes.append(errorCode);
                            errorCodes.append(";");
                        }
                        Log.e(TAG, "handleData: VMC现在故障状态-故障个数:" + errorNum + ",故障代码:" + errorCodes.toString());
                        break;
                    case 3:
                        // 货道售空状态
                        // 各货道售空状态
                        for (int i = 7, count = 1; i < (data.size() - 2); i++, count++) {
                            Log.e(TAG, "handleData: 货道售空状态-" + count + (data.get(i) == 0 ? "未售空" : "售空"));
                        }
                        break;
                    case 4:
                        // 用户投币余额
                        // 用户余额
                        int value = data.get(6) << 4 + data.get(7);
                        Log.e(TAG, "handleData: 用户投币余额-" + value);
                        break;
                    case 5:
                        // 库内温度
                        // 仓室 1 库内温度
                        int room1Temp = data.get(6);
                        // 仓室 2 库内温度
                        int room2Temp = data.get(7);
                        // 仓室 3 库内温度
                        int room3Temp = data.get(8);
                        Log.e(TAG, "handleData: 仓室 1 库内温度" + room1Temp);
                        Log.e(TAG, "handleData: 仓室 2 库内温度" + room2Temp);
                        Log.e(TAG, "handleData: 仓室 3 库内温度" + room3Temp);
                        break;
                    case 6:
                        // 货道可销售状态
                        // 各货道可销售状态
                        for (int i = 7, count = 1; i < (data.size() - 2); i++, count++) {
                            Log.e(TAG, "handleData: 货道可销售状态:" + count + ":" + (data.get(i) == 0 ? "可销售" : "不可销售"));
                        }
                        break;
                    default:
                        break;
                }
                break;
            case 228:
                // [Button_RPT]VMC按键报告
                int buttonReportType = data.get(5);
                switch (buttonReportType) {
                    case 1:
                        // 用户按键报告
                        // 用户选择的选择按钮对应的货道
                        int column = data.get(6);
                        // 用户选择的选择按钮对应的商品ID
                        int spid = data.get(7) << 4 + data.get(8);
                        Log.e(TAG, "handleData: 用户按键报告-对应货道" + column + ":" + spid);
                        break;
                    case 2:
                        // 退币开关报告
                        Log.e(TAG, "handleData: 退币开关报告-用户选择退币操作");
                        break;
                    case 3:
                        // 扩展按键报告
                        int button = data.get(6);
                        Log.e(TAG, "handleData: 扩展按键报告-按键号:" + button);
                        break;
                    default:
                        break;
                }
                break;
            case 229:
                // [Vendout_RPT]VMC出货报告
                // 流水号
                long number = data.get(5) << 3 * 4 + data.get(6) << 2 * 4 + data.get(7) << 4 + data.get(8);
                // 交易时间
                String time = parseTime(data, 9);
                // VMC交易种类
                int type = data.get(25);
                // 现金扣费金额（售货机按设定的现金价格扣费）
                int cashCost = data.get(26) << 4 + data.get(27);
                // 非现金扣费金额（售货机按设定的非现金价格扣费统计，与 PC 端实际扣款金额无关）
                int cardCost = data.get(28) << 4 + data.get(29);
                // 出货结果
                int status = data.get(30);
                // 货道号
                int column = data.get(31);
                // 商品编码
                int spid = data.get(32) << 4 + data.get(33);
                Log.e(TAG, "handleData: VMC出货报告-" + (status == 1 ? "出货成功" : "出货失败:" + status));
                break;
            case 230:
                // [Get_Confdata]VMC请求 PC 的配置数据
                int getConfDataType = data.get(5);
                switch (getConfDataType) {
                    case 1:
                        // 预留
                        break;
                    case 2:
                        // 请求系统时间
                        Log.e(TAG, "handleData: VMC请求PC的配置数据-请求系统时间");
                        break;
                    case 3:
                        // 请求货道现金单价信息
                        Log.e(TAG, "handleData: VMC请求PC的配置数据-请求货道现金单价信息");
                        break;
                    case 4:
                        // 请求货道非现金单价信息
                        Log.e(TAG, "handleData: VMC请求PC的配置数据-请求货道非现金单价信息");
                        break;
                    case 5:
                        // 请求货道商品编码信息
                        Log.e(TAG, "handleData: VMC请求PC的配置数据-请求货道商品编码信息");
                        break;
                    default:
                        break;
                }
                break;
            case 231:
                // [Get_Update]VMC请求固件升级文件
                int getUpdateType = data.get(5);
                if (getUpdateType == 1) {
                    // 生成无需升级的响应指令
                    // 添加默认的数据帧起始标记
                    response.add(2);
                    // 设置有效计算范围内的字节数
                    response.add(0);
                    response.add(108);
                    // 设置数据帧指令类型为[Updata_DAT]PC返回升级文件
                    response.add(245);
                    // 设置该条数据帧的序号
                    response.add(sn);
                    // 设置数据内容
                    // 设置type为1
                    response.add(getUpdateType);
                    // 不需要升级时，全为0
                    for (int i = 0; i < 103; i++) {
                        response.add(0);
                    }
                    // 计算有效计算字节内所有的字节累加和
                    checkSum(response);
                }
                break;
            case 236:
                // [Cost_RPT]VMC扣款报告
                int costType = data.get(5);
                int machine = data.get(6);
                int costColumn = data.get(7);
                // 扣费金额
                int value = data.get(8) << 4 + data.get(9);
                Log.e(TAG, "handleData: VMC扣款报告-扣费金额:" + value);
                break;
            default:
                // 其他默认响应为ACK
                // generateACKCMD(sn, response);
                break;
        }
        if (response.size() <= 0) {
            generateACKCMD(sn, response);
        }
        return response;
    }

    private boolean flag = true;
    private boolean isStartedComplete = false;
    private boolean isACK = false;

    /**
     * 将长度为16个字节的ASCII码格式的时间
     * 解析为人类可读的字符串。
     *
     * @param data     完整的一帧数据
     * @param startPos 表示时间的起始位置
     * @return eg:  2015-01-15 09：30：01 星期四
     */
    private String parseTime(List<Integer> data, int startPos) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 14; i++) {
            sb.append(data.get(startPos + i));
            if (i == 3 || i == 5) {
                sb.append("-");
            } else if (i == 7 || i == 13) {
                sb.append(" ");
            } else if (i == 9 || i == 11) {
                sb.append(":");
            }
        }
        int day = data.get(startPos + 15);
        switch (day) {
            case 1:
                sb.append("星期日");
                break;
            case 2:
                sb.append("星期一");
                break;
            case 3:
                sb.append("星期二");
                break;
            case 4:
                sb.append("星期三");
                break;
            case 5:
                sb.append("星期四");
                break;
            case 6:
                sb.append("星期五");
                break;
            case 7:
                sb.append("星期六");
                break;
            default:
                break;
        }
        return sb.toString();
    }

    private void generateACKCMD(int sn, List<Integer> response) {
        // 生成ACK指令
        // 添加默认的数据帧起始标记
        response.add(2);
        // 设置有效计算范围内的字节数
        response.add(0);
        response.add(4);
        // 设置数据帧指令类型为[ACK]肯定回答
        response.add(160);
        // 设置该条数据帧的序号
        response.add(sn);
        checkSum(response);
    }

    /**
     * 计算有效计算字节内所有的字节累加和，并且累计和只保留最低位2个字节
     *
     * @param data
     */
    private void checkSum(List<Integer> data) {
        int sum = 0;
        for (int i = 1; i < data.size(); i++) {
            sum += data.get(i);
        }
        String hexString = Integer.toHexString(sum);
        // Log.e(TAG, "checkSum: hexString-" + hexString);
        // Log.e(TAG, "checkSum: hexString-" + (sum >>> 8 & 0xFF));
        // Log.e(TAG, "checkSum: hexString-" + (sum & 0xFF));
        data.add(sum >>> 8 & 0xFF);
        data.add(sum & 0xFF);
    }

    /**
     * 设置VMC时间
     */
    private List<Integer> configVMCTime(int sn) {
        ArrayList<Integer> cmd = new ArrayList<>();
        // 添加默认的数据帧起始标记
        cmd.add(2);
        // 设置有效计算范围内的字节数
        cmd.add(0);
        cmd.add(21);
        // 设置数据帧指令类型为[Config_IND]PC请求配置售货机对应的参数
        cmd.add(240);
        // 设置该条数据帧的序号
        cmd.add(sn);
        // 设置数据内容
        // 设置type为2,对应PC请求配置售货机VMC时间
        cmd.add(2);
        // 设置时间
        String dateTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-E").format(new Date());
        Log.e(TAG, "configVMCTime: dateTime" + dateTime);
        String[] split = dateTime.split("-");
        cmd.add(Integer.valueOf(split[0].substring(0, 1)));
        cmd.add(Integer.valueOf(split[0].substring(1, 2)));
        cmd.add(Integer.valueOf(split[0].substring(2, 3)));
        cmd.add(Integer.valueOf(split[0].substring(3, 4)));
        cmd.add(Integer.valueOf(split[1].substring(0, 1)));
        cmd.add(Integer.valueOf(split[1].substring(1, 2)));
        cmd.add(Integer.valueOf(split[2].substring(0, 1)));
        cmd.add(Integer.valueOf(split[2].substring(1, 2)));
        cmd.add(Integer.valueOf(split[3].substring(0, 1)));
        cmd.add(Integer.valueOf(split[3].substring(1, 2)));
        cmd.add(Integer.valueOf(split[4].substring(0, 1)));
        cmd.add(Integer.valueOf(split[4].substring(1, 2)));
        cmd.add(Integer.valueOf(split[5].substring(0, 1)));
        cmd.add(Integer.valueOf(split[5].substring(1, 2)));
        switch (split[6]) {
            case "周日":
                cmd.add(1);
                break;
            case "周一":
                cmd.add(2);
                break;
            case "周二":
                cmd.add(3);
                break;
            case "周三":
                cmd.add(4);
                break;
            case "周四":
                cmd.add(5);
                break;
            case "周五":
                cmd.add(6);
                break;
            case "周六":
                cmd.add(7);
                break;
            default:
                cmd.add(1);
                break;
        }
        checkSum(cmd);
        printCMD("设置VMC时间: ", cmd);
        return cmd;
    }

    /**
     * 通知VMC出货
     *
     * @param sn
     * @param mode
     * @param column
     * @return
     */
    private List<Integer> vendOut(int sn, int mode, int column) {
        return vendOut(sn, mode, 2, 50, column, 0, 0);
    }

    /**
     * 通知VMC出货
     *
     * @param sn
     * @param mode
     * @param method
     * @param type
     * @param column
     * @param spid
     * @param cost
     * @return
     */
    private List<Integer> vendOut(int sn, int mode, int method, int type, int column, int spid, int cost) {
        ArrayList<Integer> cmd = new ArrayList<>();
        // 添加默认的数据帧起始标记
        cmd.add(2);
        // 设置有效计算范围内的字节数
        cmd.add(0);
        cmd.add(42);
        // 设置数据帧指令类型为[Vendout_IND]PC请求VMC出货
        cmd.add(244);
        // 设置该条数据帧的序号
        cmd.add(sn);
        // 设置数据内容
        cmd.add(mode);
        cmd.add(method);
        cmd.add(type);
        cmd.add(column);
        cmd.add(spid >>> 4);
        cmd.add(spid & 0xFF);
        cmd.add(00);
        cmd.add(69);
        for (int i = 0; i < 30; i++) {
            cmd.add(0);
        }
        checkSum(cmd);
        printCMD("出货指令：", cmd);
        return cmd;
    }

    private List<Integer> getInfo(int sn, int type) {
        ArrayList<Integer> cmd = new ArrayList<>();
        // 添加默认的数据帧起始标记
        cmd.add(2);
        // 设置有效计算范围内的字节数
        cmd.add(0);
        cmd.add(5);
        // 设置数据帧指令类型为[Get_Status]PC请求VMC上报对应的Status_RPT
        cmd.add(243);
        // 设置该条数据帧的序号
        cmd.add(sn);
        // 设置数据内容
        cmd.add(type);
        checkSum(cmd);
        printCMD("PC请求VMC上报对应的Status_RPT: " + type + "\n", cmd);
        return cmd;
    }

    private List<Integer> getStatus(int sn, int type) {
        ArrayList<Integer> cmd = new ArrayList<>();
        // 添加默认的数据帧起始标记
        cmd.add(2);
        // 设置有效计算范围内的字节数
        cmd.add(0);
        cmd.add(5);
        // 设置数据帧指令类型为[Get_Status]PC请求VMC上报对应的Status_RPT
        cmd.add(243);
        // 设置该条数据帧的序号
        cmd.add(sn);
        // 设置数据内容
        cmd.add(type);
        checkSum(cmd);
        printCMD("PC请求VMC上报对应的Status_RPT: " + type + "\n", cmd);
        return cmd;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SerialPortHelper.stop();
    }

}
