#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <ESP8266mDNS.h>
#include <WiFiUdp.h>
#include <ArduinoOTA.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <SoftwareSerial.h>


const char* ssid = "Ubilabs";//路由器ssid
const char* password = "googleiot";//路由器密码
const char* mqtt_server = "123.206.127.199";//服务器的地址


//全局变量区域上界


unsigned int HighLen = 0; 
unsigned int LowLen  = 0; 
unsigned int Len_mm  = 0; 
unsigned long serialSpeed = 9600;
SoftwareSerial mySerial(D6, D7); // RX, TX
long lastMsg_async[10] = {0};//存放时间的变量 
int async[10]={0};
bool op[10]={0};
int threshold=200;//车辆检测阈值
int distance=0;




//全局变量区域下界

WiFiClient espClient;
PubSubClient client(espClient);

int OTA=0;
int OTAS=0;
long lastMsg = 0;//存放时间的变量 
char msg[200];//存放要发的数据
String load;


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
    if (client.connect("OTADEMO"+randnum)) {//接入时的用户名，尽量取一个很不常用的用户名
      client.subscribe("OTAlisten");//接收外来的数据时的intopic
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
      load +=(char)payload[i];//串口打印出接收到的数据
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


void setup() {
  //setup代码区域上界


  mySerial.begin(serialSpeed);
  Serial.begin(9600);
  async[0]=1;
  op[0]=1;
  //填写自己的逻辑代码



  //setup代码区域下界

   setup_wifi();//自动连WIFI接入网络
  client.setServer(mqtt_server, 1883);//1883为端口号
  client.setCallback(callback); //用于接收服务器接收的数据
}

void loop() {
        if(OTA){
          OTAsetup();
        ArduinoOTA.handle();
       }
       else{
        reconnect();//确保连上服务器，否则一直等待。
        client.loop();//MUC接收数据的主循环函数。
        //loop代码上界
        us100();
        trigger(1000);//每隔1秒触发有效

        //自己的逻辑代码



        //loop代码下界   
         long now = millis();//记录当前时间
        if (now - lastMsg > 100) {//每隔100毫秒秒发一次数据

          lastMsg = now;//刷新上一次发送数据的时间
        }
       }
}

void encodeJson(){
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root1 = jsonBuffer.createObject();
  //发送数据区上界


  //添加其他要发送的JSON包就像这样下面这句代码
  root1["back"] = "OTA";


  //发送数据区下界
  root1.printTo(msg);
  }
void us100(){
     int soft_time=100;
     long now1 = millis();
    if (now1 - lastMsg_async[0] > soft_time&&async[1]==1) {
      lastMsg_async[0]=now1;
      lastMsg_async[1]=now1;
      async[0]=1;
      async[1]=0;
      mySerial.flush();     // 清空串口接收缓冲区 
      mySerial.write(0X55); // 发送0X55，触发US-100开始测距    
    }
    if (now1 - lastMsg_async[1] > soft_time&&async[0]==1) {
      lastMsg_async[1]=now1;
      lastMsg_async[0]=now1;
      async[1]=1;
      async[0]=0;
        if(mySerial.available() >= 2)            //当串口接收缓冲区中数据大于2字节        
        { 
          HighLen = mySerial.read();                   //距离的高字节 
          LowLen  = mySerial.read();                   //距离的低字节          
          Len_mm  = HighLen*256 + LowLen;             //计算距离值          
          if((Len_mm > 1) && (Len_mm < 10000))    //有效的测距的结果在1mm到100m之间          
          {              
            distance=Len_mm;
          }
        }  
    }
}
void trigger(int filtration_time){
    long now2 = millis();
      if(distance<threshold&&op[1]==1&&now2 - lastMsg_async[2] > filtration_time){
      op[0]=1;
      op[1]=0;
      
      lastMsg_async[2]=now2;
     encodeJson();
     client.publish("OTAback",msg);//以OTA为TOPIC对外发送MQTT消息
    }
    if(distance>=threshold&&op[0]==1){
      op[0]=0;
      op[1]=1;
    }
}
