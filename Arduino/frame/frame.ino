#include <ArduinoJson.h>
#include <WiFi.h>
#include <ESPmDNS.h>
#include <WiFiUdp.h>
#include <ArduinoOTA.h>
#include <PubSubClient.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <String.h>
#include <stdio.h>
#include "driver/pcnt.h"

const char* ssid = "********";
const char* password = "*******";
const char* mqtt_server = "***.***.***.***";//服务器的地址 

BLECharacteristic *pCharacteristic;
bool deviceConnected = false;
uint8_t txValue = 0;
long BLElastMsg = 0;//存放时间的变量 
String rxload="a1234b1234c1234d1234e1234f1234";
int BLE_receive [50];
bool BLE_receive_lock=0;
bool motor_stop=1;

pcnt_config_t pcnt_config_r;
pcnt_config_t pcnt_config_l;

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

//全局变量区域上界
int BLEPubInterval =1000;//蓝牙每1000毫秒发送一次数据
long last[10]={0};//存放时间，与计时器配合，代替delay效果
int BLE_decode_result [10];
char BLE_decode_result_char[9];
int16_t count_l = 0;     
int16_t count_r = 0;     
int16_t count_l_real = 0;     
int16_t count_r_real = 0;
int L_speed[4]={0};
int R_speed[4]={0};
bool direction_R=0;
bool direction_L=0;
bool control_mode=0;//0为PWM模式,1为转速模式
int target_PWM_L=0;//目标左轮PWM值
int target_PWM_R=0;//目标右轮PWM值
int target_SP_L=0;//目标左轮转速值
int target_SP_R=0;//目标右轮转速值

double kp=10;
double ki=10;
double kd=10;

double ITerm_L,ITerm_R,lastInput_L,lastInput_R;
double timeChange=1;
double outMin=1024;
double outMax=4096;
int R,L;
double Output_L=0;
double Output_R=0;
//全局变量区域下界

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      if (rxValue.length() > 0) {
        rxload="";
        BLE_receive_lock=1;
        for(int t=0;t<50;t++){
          BLE_receive[t]=0;
        }
//        memset(BLE_receive,0,sizeof(BLE_receive));
         BLE_receive[0]=(int)rxValue[0]-30;
        for (int i = 1; i < rxValue.length(); i++)
         {
          BLE_decode_result_char[i-1]=(char)rxValue[i];
            BLE_receive[i]=(int)rxValue[i]-30;
//          Serial.print(BLE_receive[i]);
         }
//        Serial.println("ok");
      }
    }
};



WiFiClient espClient;
PubSubClient client(espClient);

int OTA=0;
int OTAS=0;
long lastMsg = 0;//存放时间的变量 
char msg[200];//存放要发的数据
String load;

void setupBLE(String BLEName){
  const char *ble_name=BLEName.c_str();
  BLEDevice::init(ble_name);
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX,BLECharacteristic::PROPERTY_NOTIFY);
  pCharacteristic->addDescriptor(new BLE2902());
  BLECharacteristic *pCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX,BLECharacteristic::PROPERTY_WRITE);
  pCharacteristic->setCallbacks(new MyCallbacks());
  pService->start();
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");
}
void setup_wifi() {//自动连WIFI接入网络
  delay(10);
    WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
}

void reconnect() {//等待，直到连接上服务器
  while (!client.connected()) {//如果没有连接上
    int randnum = random(0, 999); 
    if (client.connect("OTADEMO"+randnum)) {//接入时的用户名
      client.subscribe("testin");
    } else {
      Serial.print("failed, rc=");//连接失败
      Serial.print(client.state());//重新连接
      Serial.println(" try again in 5 seconds");//延时5秒后重新连接
      delay(5000);
    }
  }
}

void callback(char* topic, byte* payload, unsigned int length) {//用于接收服务器接收的数据
  load="";
  for (int i = 0; i < length; i++) {
      load +=(char)payload[i];
  }
   decodeJson();
}

void  decodeJson() {
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.parseObject(load);
   OTA = root["OTA"];
   OTAS =OTA;
   //接收数据json处理区上界

   //添加其他自己的JSON收听处理方式就像这样  int Activity=root["ACT"];

   //接收数据json处理区下界
}

void OTAsetup(){
   if(OTAS){
  ArduinoOTA.onStart([]() {
    Serial.println("Start");
  });
  ArduinoOTA.onEnd([]() {
    Serial.println("\nEnd");
  });
  ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
    Serial.printf("Progress: %u%%\r", (progress / (total / 100)));
  });
  ArduinoOTA.onError([](ota_error_t error) {
    Serial.printf("Error[%u]: ", error);
    if (error == OTA_AUTH_ERROR) Serial.println("Auth Failed");
    else if (error == OTA_BEGIN_ERROR) Serial.println("Begin Failed");
    else if (error == OTA_CONNECT_ERROR) Serial.println("Connect Failed");
    else if (error == OTA_RECEIVE_ERROR) Serial.println("Receive Failed");
    else if (error == OTA_END_ERROR) Serial.println("End Failed");
  });
  ArduinoOTA.begin();
  Serial.println("Ready");
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());
  OTAS=0;
   }
}

void execute(bool execute_l,bool execute_r,int l_speed,int r_speed)//电机执行函数，execute_l为1，左电机前进，为0则退；execute_r同理。l_speed表示左电机的PWM值；r_speed同理。
{
  digitalWrite(27,0+execute_r);
  digitalWrite(14,!(0+execute_r));
  ledcWrite(0, r_speed);
  digitalWrite(21,0+execute_l);
  digitalWrite(33,!(0+execute_l));
  ledcWrite(1, l_speed);
}

void setup() 
{  
  pcnt_example_init();
  Serial.begin(9600);
  ledcAttachPin(17, 0); 
  ledcAttachPin(16, 1);
  ledcSetup(0, 312500, 12); // 312500Hz PWM, 12-bit 分辨率
  ledcSetup(1, 312500, 12);
  pinMode(27, OUTPUT);
  pinMode(14, OUTPUT);
  pinMode(21, OUTPUT);
  pinMode(33, OUTPUT);
  //setup代码区域上界

  //填写自己的逻辑代码

  //setup代码区域下界
  setupBLE("BW-AICar 321");
  delay(500);

}

void loop()
{

  //loop代码上界
    BLEdecode();
    if(control_mode){
        L= (int)Output_L;
        R= (int)Output_R;
        detection_count(50);
        execute(direction_L,direction_R,L,R);
    }
  build_BLE_MSG();
  //loop代码下界   
  mqttPub();
  BLEPub();
 
}




void build_BLE_MSG(){
  rxload="";
  char MsgTemp[20];
  snprintf (MsgTemp, 75, "a%db%dc%dd%d", L,R,L_speed[2],R_speed[2]);
  for(int i=0;i<20;i++){
    rxload+=MsgTemp[i];
  }
}

static void detection_count(int Delta_T){
   long speed_detection_time_now=millis();
   double Delta_T_Real=speed_detection_time_now-last[0];
      timeChange=Delta_T_Real;
     if(Delta_T_Real>Delta_T){
        pcnt_get_counter_value(PCNT_UNIT_0, &count_l_real);//取数据
        pcnt_get_counter_value(PCNT_UNIT_1, &count_r_real);//取数据
        R_speed[1]=-count_l_real;
        L_speed[1]=-count_r_real;
        R_speed[2]=R_speed[1]*Delta_T/(Delta_T_Real);  
        L_speed[2]=L_speed[1]*Delta_T/(Delta_T_Real);
        pcnt_counter_clear(PCNT_UNIT_0);
        pcnt_counter_clear(PCNT_UNIT_1);
        last[0]=speed_detection_time_now;
        Compute_L();
        Compute_R();
     }
}

void Compute_L()
{  
   if(target_SP_L==0){Output_L=0;return;}
   double error = target_SP_L - L_speed[2];
   ITerm_L += (ki * error);
   if(ITerm_L> outMax) ITerm_L= outMax;
   else if(ITerm_L< outMin) ITerm_L= outMin;
   double dInput = (L_speed[2] - lastInput_L);
   Output_L = kp * error + ITerm_L - kd * dInput;
   if(Output_L > outMax) Output_L = outMax;
   else if(Output_L < outMin) Output_L = outMin;
   lastInput_L = L_speed[2];
}

void Compute_R()
{  
   if(target_SP_R==0){Output_R=0;return;}
   double error = target_SP_R - R_speed[2];
   ITerm_R += (ki * error);
   if(ITerm_R> outMax) ITerm_R= outMax;
   else if(ITerm_R< outMin) ITerm_R= outMin;
   double dInput = (R_speed[2] - lastInput_R);
   Output_R = kp * error + ITerm_R - kd * dInput;
   if(Output_R > outMax) Output_R = outMax;
   else if(Output_R < outMin) Output_R = outMin;
   lastInput_R = R_speed[2];
}

void BLEdecode(){
  if(BLE_receive_lock){
    for(int b=0;b<10;b++){
      BLE_decode_result[b]=0;
    }
//    memset(BLE_decode_result,0,sizeof(BLE_decode_result));
    int result_count=0;
    int index=0;
      while(index<=19)
      {
          BLE_decode_result[result_count]=BLE_receive[index]*100+BLE_receive[index+1];
          if(BLE_decode_result[result_count]>9999){ BLE_receive_lock=0;return;}
          index=index+2;
          result_count++;
        }
        BLE_receive_lock=0;
      if(BLE_decode_result[9]!=4321){BLE_receive_lock=0;return;}
      if(BLE_decode_result[1]>4097||BLE_decode_result[2]>4097||BLE_decode_result[0]>9999){BLE_receive_lock=0;return;}
      if(BLE_decode_result[0]>=0&&BLE_decode_result[0]<=7)//方向与控制模式
      {
        bool direction_case[8]={0,1,1,0,0,1,1,0};
        direction_L=BLE_decode_result[0]%2;
        direction_R=direction_case[BLE_decode_result[0]];
        control_mode=BLE_decode_result[0]/4;
        if(control_mode){
          target_SP_L=BLE_decode_result[1];
          target_SP_R=BLE_decode_result[2];
        }else{
          target_PWM_L=BLE_decode_result[1];
          target_PWM_R=BLE_decode_result[2];
          
        }
      }

      if(BLE_decode_result[0]==9999)//进入OTA模式
      {
        OTA=1;
        OTAS=1;
      }
      execute(direction_L,direction_R,target_PWM_L,target_PWM_R);
    }
    else{
      //Do nothing
    }
 }

static void mqttPub(){
   long now = millis();//记录当前时间
  if (now - lastMsg > 1000) {//每隔1000毫秒秒发一次数据
     encodeJson();
     client.publish("testout",msg);
     lastMsg = now;//刷新上一次发送数据的时间
  }
}

static void pcnt_example_init(void){
 pcnt_config_l.pulse_gpio_num = 15;// 脉冲脚
 pcnt_config_l.ctrl_gpio_num = -15;  //方向脚
 pcnt_config_l.channel = PCNT_CHANNEL_1;
 pcnt_config_l.unit = PCNT_UNIT_0;
 pcnt_config_l.pos_mode = PCNT_COUNT_INC;
 pcnt_config_l.neg_mode = PCNT_COUNT_DIS;
 pcnt_config_l.lctrl_mode = PCNT_MODE_REVERSE;
 pcnt_config_l.hctrl_mode = PCNT_MODE_KEEP;
 pcnt_config_l.counter_h_lim = 32767;
 pcnt_config_l.counter_l_lim = -32767;
 pcnt_unit_config(&pcnt_config_l);
 pcnt_set_filter_value(PCNT_UNIT_0, 1023);//设定检测阈值
 pcnt_filter_enable(PCNT_UNIT_0);
 pcnt_counter_pause(PCNT_UNIT_0);

 pcnt_config_r.pulse_gpio_num = 13;// 脉冲脚
 pcnt_config_r.ctrl_gpio_num = -15;  //方向脚
 pcnt_config_r.channel = PCNT_CHANNEL_0;
 pcnt_config_r.unit = PCNT_UNIT_1;
 pcnt_config_r.pos_mode = PCNT_COUNT_INC;
 pcnt_config_r.neg_mode = PCNT_COUNT_DIS;
 pcnt_config_r.lctrl_mode = PCNT_MODE_REVERSE;
 pcnt_config_r.hctrl_mode = PCNT_MODE_KEEP;
 pcnt_config_r.counter_h_lim = 32767;
 pcnt_config_r.counter_l_lim = -32767;
 pcnt_unit_config(&pcnt_config_r);
 pcnt_set_filter_value(PCNT_UNIT_1, 1023);//设定检测阈值
 pcnt_filter_enable(PCNT_UNIT_1);
 pcnt_counter_pause(PCNT_UNIT_1);
 pcnt_counter_clear(PCNT_UNIT_1);
 pcnt_counter_resume(PCNT_UNIT_1);
 pcnt_counter_clear(PCNT_UNIT_0);
 pcnt_counter_resume(PCNT_UNIT_0);
}

static void BLEPub(){
   long BLEnow = millis();//记录当前时间
    if (BLEnow - BLElastMsg > BLEPubInterval) 
    {//每隔BLEPubInterval毫秒发一次信号
        if (deviceConnected&&rxload.length()>0) {
          String str=rxload;
          const char *newValue=str.c_str();
          pCharacteristic->setValue(newValue);
          pCharacteristic->notify();
        }
      BLElastMsg = BLEnow;//刷新上一次发送数据的时间
    }
}

void encodeJson(){
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root1 = jsonBuffer.createObject();
  //发送数据区上界
  root1["R2"] = R_speed[2];
  root1["L2"] = L_speed[2];
  root1["PL"]=L;
  root1["PR"]=R;
  root1["SL"]=target_SP_L;
  root1["SR"]=target_SP_R;
  //发送数据区下界
  root1.printTo(msg);
  }
