#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <SoftwareSerial.h>
unsigned int HighLen = 0; 
unsigned int LowLen  = 0; 
unsigned int Len_mm  = 0; 
unsigned long serialSpeed = 9600;
SoftwareSerial mySerial(D6, D7); // RX, TX
long lastMsg_async[10] = {0};//存放时间的变量 
int async[10]={0};
bool op[10]={0};
int threshold;
int distance=0;

void setup(){ 
  mySerial.begin(serialSpeed);
  Serial.begin(9600);
  async[0]=1;
  op[0]=1;
  threshold=200;//车辆检测阈值距离
}




void loop(){
    us100();//采用async原理，保证loop不停顿。
    trigger(1000);//可以过滤数据，1000毫秒内的触发无效
    
}


void trigger(int filtration_time){
    long now2 = millis();
      if(distance<threshold&&op[1]==1&&now2 - lastMsg_async[2] > filtration_time){
      op[0]=1;
      op[1]=0;
      Serial.println("ok");
      lastMsg_async[2]=now2;
    }
    if(distance>=threshold&&op[0]==1){
      op[0]=0;
      op[1]=1;
    }
}

void us100(){
     int soft_time=100;//超声波测量时间间隔的一半
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

