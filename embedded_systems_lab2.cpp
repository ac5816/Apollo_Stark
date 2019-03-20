#include "SHA256.h"
#include "mbed.h"

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------//
//Photointerrupter input pins
#define I1pin D3
#define I2pin D6
#define I3pin D5

//Incremental encoder input pins
#define CHApin   D12
#define CHBpin   D11

//Motor Drive output pins   //Mask in output byte
#define L1Lpin D1           //0x01
#define L1Hpin A3           //0x02
#define L2Lpin D0           //0x04
#define L2Hpin A6          //0x08
#define L3Lpin D10           //0x10
#define L3Hpin D2          //0x20

#define PWMpin D9

//Motor current sense
#define MCSPpin   A1
#define MCSNpin   A0

//Timer
Timer t;
Ticker time_up;

//Initialise the serial port
RawSerial pc(SERIAL_TX, SERIAL_RX);

int8_t orState = 0;    //Rotot offset at motor state 0

//---------------------------------------------------Constants and variables for processing input command---------------------------------------------------------------//

#define MAX_COMMAND_LENGTH 18   // Longest command is 17 bits long plus a termination character 
Queue<void, 8> inCharQ;

//ISR to receive each incoming byte and place it in queue
void serialISR() {
    uint8_t newChar = pc.getc();        //Retrieves byte from serial port
    inCharQ.put((void*)newChar);
}

char character_array[MAX_COMMAND_LENGTH];

//--------------------------------------------------------------PWM variables----------------------------------------------------------------------------//

PwmOut motorTorqueControl(PWMpin);

//-----------------------------------------------------------Global variables for bitcoin mining------------------------------------------------------------------------//

uint8_t sequence[] = {0x45,0x6D,0x62,0x65,0x64,0x64,0x65,0x64,
                          0x20,0x53,0x79,0x73,0x74,0x65,0x6D,0x73,
                          0x20,0x61,0x72,0x65,0x20,0x66,0x75,0x6E,
                          0x20,0x61,0x6E,0x64,0x20,0x64,0x6F,0x20,
                          0x61,0x77,0x65,0x73,0x6F,0x6D,0x65,0x20,
                          0x74,0x68,0x69,0x6E,0x67,0x73,0x21,0x20,
                          0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                          0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

uint64_t* key = (uint64_t*)((int)sequence + 48);
uint64_t* nonce = (uint64_t*)((int)sequence + 56);
uint8_t hash[32];
int hashrate = 0;

//----------------------------------------------------Shared variables---------------------------------------------------------------//
uint64_t newKey;
Mutex newKey_mutex;

float newDutyCycle = 1.0;

//----------------------------------------------------------------------------------------------------------------------------------------------------------------------//

typedef struct {
    char*       command;
    int         counter;
}mail_t;

Mail<mail_t, 16> mail_box;

//Thread
Thread thread;
Thread decode_thread;
//Global Variables
int i;
float interval;

//Mapping from sequential drive states to motor phase outputs
/*
State   L1  L2  L3
0       H   -   L
1       -   H   L
2       L   H   -
3       L   -   H
4       -   L   H
5       H   L   -
6       -   -   -
7       -   -   -
*/
//Drive state to output table
const int8_t driveTable[] = {0x12,0x18,0x09,0x21,0x24,0x06,0x00,0x00};

//Mapping from interrupter inputs to sequential rotor states. 0x00 and 0x07 are not valid
const int8_t stateMap[] = {0x07,0x05,0x03,0x04,0x01,0x00,0x02,0x07};  
//const int8_t stateMap[] = {0x07,0x01,0x03,0x02,0x05,0x00,0x04,0x07}; //Alternative if phase order of input or drive is reversed

//Phase lead to make motor spin
const int8_t lead = -2;  //2 for forwards, -2 for backwards

//Status LED
DigitalOut led1(LED1);

//Photointerrupter inputs
//DigitalIn I1(I1pin);
//DigitalIn I2(I2pin);
//DigitalIn I3(I3pin);
InterruptIn I1(I1pin);
InterruptIn I2(I2pin);
InterruptIn I3(I3pin);

//Motor Drive outputs
DigitalOut L1L(L1Lpin);
DigitalOut L1H(L1Hpin);
DigitalOut L2L(L2Lpin);
DigitalOut L2H(L2Hpin);
DigitalOut L3L(L3Lpin);
DigitalOut L3H(L3Hpin);

//Set a given drive state
void motorOut(int8_t driveState){
    
    //Lookup the output byte from the drive state.
    int8_t driveOut = driveTable[driveState & 0x07];
      
    //Turn off first
    if (~driveOut & 0x01) L1L = 0;
    if (~driveOut & 0x02) L1H = 1;
    if (~driveOut & 0x04) L2L = 0;
    if (~driveOut & 0x08) L2H = 1;
    if (~driveOut & 0x10) L3L = 0;
    if (~driveOut & 0x20) L3H = 1;
    
    //Then turn on
    if (driveOut & 0x01) L1L = 1;
    if (driveOut & 0x02) L1H = 0;
    if (driveOut & 0x04) L2L = 1;
    if (driveOut & 0x08) L2H = 0;
    if (driveOut & 0x10) L3L = 1;
    if (driveOut & 0x20) L3H = 0;
    }
    
    //Convert photointerrupter inputs to a rotor state
inline int8_t readRotorState(){
    return stateMap[I1 + 2*I2 + 4*I3];
    }

//Basic synchronisation routine    
int8_t motorHome() {
    //Put the motor in drive state 0 and wait for it to stabilise
    motorOut(0);
    wait(2.0);
    
    //Get the rotor state
    return readRotorState();
}

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------//

//Poll the rotor state and set the motor outputs accordingly to spin the motor
void interrupt() {
    int8_t intState = 0;
    //int8_t intStateOld = 0;
    
    intState = readRotorState();
        
    motorOut((intState-orState+lead+6)%6); //+6 to make sure the remainder is positive
        //pc.printf("%d\n\r",intState);
}

//---------------------------------------------------------------------------------------------------------------------------------------------------------------------//

void putMessage (char* character_array) {
       mail_t *mail = mail_box.alloc();
       mail->command = character_array;
       mail->counter = hashrate;
       mail_box.put(mail);
} 

//-----------------------------------------------------Thread to receive commands--------------------------------------------------------------------------------------//

void receive_message_thread (void) {
    int i=0;
    
    pc.attach(&serialISR);
    while(1) {
        osEvent newEvent = inCharQ.get();
        uint8_t newChar = (uint8_t)newEvent.value.p;
        if (newChar != '\r' && i < MAX_COMMAND_LENGTH) {
            character_array[i] = newChar;
            i++;
        }
        else {
            character_array[i] = '\0';
            putMessage(character_array);
            i = 0;
        }
        wait(1);
    }
}

//----------------------------------------------------------------------------------------------------------------------------------------------------------------------//

//-----------------------------------------Thread to Decode received commands-------------------------------------------------------------------------------------------//

void decode_command(void) {    
    while (true) {
        osEvent evt = mail_box.get();
        if (evt.status == osEventMail) {
            mail_t *mail = (mail_t*)evt.value.p;
            
            // Received variables
            uint64_t receivedKey;
            float receivedDutyCycle;

            //printf("\nNonce: %.2f V\r\n"   , mail->voltage);
            if (mail->command[0]=='K') {
                sscanf(character_array, "K%llx", &receivedKey); // hex values will always be capital ('x' for lower case)
                newKey_mutex.lock();                          // so no one else changes the mutex when we are changing it
                newKey = receivedKey;
                newKey_mutex.unlock();
            }

            else if (mail->command[0]=='M') {
                sscanf(character_array, "M%f", &receivedDutyCycle);
                newKey_mutex.lock();
                newDutyCycle = receivedDutyCycle;
                newKey_mutex.unlock(); 
            }

            mail_box.free(mail);

        }   
    }
}


//----------------------------------------------------------------------------------------------------------------------------------------------------------------------//

//Main
int main() {
    
    float pwm_duty_cycle = 1.0;

    pc.printf("Hello\n\r");
    
    //Thread
    thread.start(receive_message_thread);
    decode_thread.start(decode_command);

    // PWM variables
    motorTorqueControl.period(0.002);
    motorTorqueControl.write(1.0);

    // Run the motor synchronisation
    orState = motorHome();
    pc.printf("Rotor origin: %x \n\r", orState);
    SHA256 sha256;
    int counter = 0;
    
    interrupt();
    
    I1.rise(&interrupt);
    I1.fall(&interrupt);
    I2.rise(&interrupt);
    I2.fall(&interrupt);
    I3.rise(&interrupt);
    I3.fall(&interrupt);
    
    t.start();
    int start_time = t.read_ms();
    
    while (1) {
        
        newKey_mutex.lock();
        *key = newKey;
        pwm_duty_cycle = newDutyCycle;
        pc.printf("New Duty Cycle: %f\n\r", pwm_duty_cycle);
        newKey_mutex.unlock();
        
        //sequence now contains new key
        sha256.computeHash(hash, sequence, 64);
        counter++;
        
        motorTorqueControl.write(newDutyCycle);

        if (t.read_ms() - start_time >= 1000) {
            hashrate = counter;
            pc.printf("The hash rate is: %d \r\n", counter);
            start_time = t.read_ms();
            counter = 0;           
        }
    }
}