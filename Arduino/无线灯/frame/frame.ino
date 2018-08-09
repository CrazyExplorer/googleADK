#include <WiFi.h>
#include <ESPmDNS.h>
#include <ArduinoOTA.h>
#include <PubSubClient.h>
#include "EEPROM.h"
#include <stdio.h>  
#include <ArduinoJson.h>
#include "driver/pcnt.h"  
#define EEPROM_SIZE 3


const char* ssid = "Li-501";//路由器ssid
const char* password = "blackwalnut";//路由器密码
const char* mqtt_server = "123.206.127.199";//服务器的地址


int id=0;
int require_0[3]={0,0,0};
int require_1=0;


WiFiClient espClient;
PubSubClient client(espClient);

int OTA=0;
int OTAS=0;
long lastMsg = 0;//存放时间的变量 
char msg[200];//存放要发的数据
String load;
int output [6]={0,0,0,0,0,0};
long lastMsg_async[10] = {0};//存放时间的变量 
int async[10]={0};
int deaddot=0;//重启限度

void(* resetFunc) (void) = 0; //制造重启命令


void reconnect() {//等待，直到连接上服务器
  while (!client.connected()) {//如果没有连接上
    int randnum = random(0, 9999); 
    if (client.connect("1233DA2C16EA8F16"+randnum)) {
      client.subscribe("BW/1233DA2C16EA8F16/WSC");//接收外来的数据时的intopic
    } else {
      Serial.print("failed, rc=");//连接失败
      Serial.print(client.state());//重新连接
      Serial.println(" try again in 5 seconds");//延时5秒后重新连接
      resetFunc();  
    }
  }
}

void  decodeJson() {
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.parseObject(load);
   OTA = root["OTAd"];
   OTAS =OTA;
   //接收数据json处理区上界
   if(root["sa"][0]!=0){
    Serial.println(load);
    EEPROM.write(1, root["sa"][0]);
    EEPROM.commit();
    id=root["sa"][0];
   }
   else if(root["c"][1]==id&&root["c"][1]!=0){
       if(root["c"][0]==0){
        int coil_id=root["c"][2];
        output[coil_id]=root["c"][3];
//        debug();
       }
       else if(root["c"][0]==1){
        for(int i=1;i<6;i++){
          output[i]=root["c"][i+1];
        }
//        debug();
       }
   }
   else if(root["r"][1]==id&&root["r"][1]!=0){
       if(root["r"][0]==0){
        require_0[1]=root["r"][2];
        require_0[0]=1;
       }
       else if(root["r"][0]==1){
        require_1=1;
       }
   }
   
   
   //接收数据json处理区下界
}




void setup() {
  //setup代码区域上界
    Serial.begin(9600);
  if (!EEPROM.begin(EEPROM_SIZE))
  {
    Serial.println("failed to initialise EEPROM"); 
  }
  id=byte(EEPROM.read(1));
  output_init();
  //填写自己的逻辑代码
  async[4]=1;

  //setup代码区域下界

   setup_wifi();//自动连WIFI接入网络
  client.setServer(mqtt_server, 1883);//1883为端口号
  delay(100);
  client.setCallback(callback); //用于接收服务器接收的数据
}

void loop() {
        reconnect();//确保连上服务器，否则一直等待。
        client.loop();//MUC接收数据的主循环函数。
        //loop代码上界
        
         execute_map();
         if(require_0[0]==1){
          encodeJsonSingleBack(require_0[0]);
          client.publish("BW/1233DA2C16EA8F16/WSF",msg);
          require_0[0]=0;
         }
         if(require_1==1){
           encodeJson();
           client.publish("BW/1233DA2C16EA8F16/WSF",msg);
          require_1=0;
         }

        //loop代码下界   
         long now = millis();
        if (now - lastMsg > 2000) {
           encodeJson();
           client.publish("BW/1233DA2C16EA8F16/WSF",msg);//以OTA为TOPIC对外发送MQTT消息
          lastMsg = now;//刷新上一次发送数据的时间
        }
       
}

void encodeJson(){
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root1 = jsonBuffer.createObject();
  JsonArray& digitalout = root1.createNestedArray("d"); 
  digitalout.add(1); 
  digitalout.add(id); 
  digitalout.add(output[1]); 
  digitalout.add(output[2]); 
  digitalout.add(output[3]); 
  digitalout.add(output[4]); 
  digitalout.add(output[5]); 
  //发送数据区下界
  root1.printTo(msg);
}

void encodeJsonSingleBack(int num){
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root1 = jsonBuffer.createObject();
  JsonArray& digitalout = root1.createNestedArray("d"); 
  digitalout.add(0); 
  digitalout.add(id); 
  digitalout.add(num); 
  digitalout.add(output[num]); 
  root1.printTo(msg);
}


void execute_map(){
     int soft_time=30;
     long now1 = millis();
     long now2 = millis();
     long now3 = millis();
     long now4 = millis();
     long now5 = millis();
    if (now1 - lastMsg_async[0] > soft_time&&async[4]==1) {
      digitalWrite(23,output[4]);
      lastMsg_async[0]=now1;
      lastMsg_async[1]=now1;
      async[0]=1;
      async[4]=0;
//      Serial.println(1);
    }
    if (now2 - lastMsg_async[1] > soft_time&&async[0]==1) {
      digitalWrite(22,output[5]);
      lastMsg_async[1]=now2;
      lastMsg_async[2]=now2;
      async[0]=0;
      async[1]=1;
//      Serial.println(2);
    }
    if (now3 - lastMsg_async[2] > soft_time&&async[1]==1) {
      digitalWrite(18,output[3]);
      lastMsg_async[2]=now3;
      lastMsg_async[3]=now3;
      async[1]=0;
      async[2]=1;
//      Serial.println(3);
    }
    if (now4 - lastMsg_async[3] > soft_time&&async[2]==1) {
      digitalWrite(19,output[2]);
      lastMsg_async[3]=now4;
      lastMsg_async[4]=now4;
      async[2]=0;
      async[3]=1;
//      Serial.println(4);
    }
    if (now5 - lastMsg_async[4] > soft_time&&async[3]==1) {
      digitalWrite(21,output[1]);
      lastMsg_async[4]=now5;
      lastMsg_async[0]=now5;
      async[3]=0;
      async[4]=1;
//      Serial.println(5);
    }
}

void output_init(){
  pinMode(21,OUTPUT);
  pinMode(19,OUTPUT);
  pinMode(18,OUTPUT);
  pinMode(22,OUTPUT);
  pinMode(23,OUTPUT);
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


void callback(char* topic, byte* payload, unsigned int length) {//用于接收服务器接收的数据
  load="";
  for (int i = 0; i < length; i++) {
      load +=(char)payload[i];
  }
   decodeJson();

}
void setup_wifi() {//自动连WIFI接入网络
  delay(100);
    WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    
    
    delay(500);
    Serial.println(".");
  }
}





void debug(){
  for(int i=1;i<6;i++){
    Serial.print(output[i]);
  }
  Serial.println("");
}

