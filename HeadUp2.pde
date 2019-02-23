import gab.opencv.*;
import processing.video.*;
import java.awt.*;
import javax.swing.*;

int TIME_LIMIT_SECONDS = 10;
int CHECKS_PER_SECOND = 1;
float MAX_OPACITY = 0.95;
String SETTINGS_FILE_NAME = "settings.txt";

PImage mind;
PFrame f; // create frame for webcam image
String[] list = new String[2]; // list for settings
int ypos; // the height 
int yValueMiddleOfBox;
int rSig; // the distance
int almTimer;
int trigHeight = 0; // height limit
int trigDist = 0; // distance limit
boolean alm; // alarm
boolean pause;
Capture video;
OpenCV opencv;
float opacity = 0.1;

void setup() {
  frameRate(CHECKS_PER_SECOND);
  size(320, 240);
  mind = this.loadImage("mind.png");
  //The next lines are for intitializing the camera and OpenCV
  video = new Capture(this, 640, 480);
  opencv = new OpenCV(this, 640, 480);
  opencv.loadCascade(OpenCV.CASCADE_FRONTALFACE);  
  video.start();
  
  File localFile = new File(dataPath(SETTINGS_FILE_NAME));
  if(localFile.exists()) {
    println("Loading Settings...");
    loadsettings();
  }
  else {
    savesettings();
  }
}

void draw() {
  getDistance();  // run the OpenCV routine
  if (pause) {
    alm = false;
  }
  if (trigHeight!=0 && trigDist!=0 && !pause) {
    if (rSig > trigDist) {
      alm = true;
      println(System.currentTimeMillis() + " distance");
    } else if (yValueMiddleOfBox > trigHeight) {
      alm = true;
      println(System.currentTimeMillis() + " height");
    } else {
      alm = false;
    }
  } else {
    alm = false;
  }
  
  int timeMillis = TIME_LIMIT_SECONDS * 1000;

  if (!alm){
    almTimer = millis() + timeMillis;  //reset alarm timer if alarm is off
    setPopup(false); // close popup
  } else if ((millis() > almTimer)){ // && (millis() - timeMillis < almTimer)
    setPopup(true); // open popup
  }
  
//The following part draws the 2 buttons and checks if they were pressed
  
  textSize(14);
  fill(0, 255, 0);
  text("set distance", 18, 220);
  text("set height", 136, 220);
  text("pause", 248, 220);
  
  stroke(0, 255, 0);
  
  noFill();
  if (mousePressed && mouseOver(10, 200, 100, 30)) {
    trigDist = rSig + 3; // set trigger distance plus a little extra distance
    savesettings();
    fill(0, 255, 0);
  }
  rect(10, 200, 100, 30);  
  
  noFill();  
  if (mousePressed && mouseOver(120, 200, 100, 30)) {
    trigHeight = yValueMiddleOfBox + 3; // set trigger height
    savesettings();
    fill(0, 255, 0);
  }
  rect(120, 200, 100, 30);
  
  noFill();
  if(pause) fill(0, 255, 0); // this part draws the pause switch
  rect(230, 200, 80, 30);
 
}

void getDistance() { //OPenCV functions
  //pushmatrix and popmatrix prevents the buttons from beeing scaled with the video
  pushMatrix(); 
  scale(0.5); // scales the video to the window size
  opencv.loadImage(video);
  
  image(video, 0, 0 ); // this draws the webcam image

  noFill();
  if (alm) stroke(255, 0, 0); //draw all lines red if alarm is active
  else stroke(0, 255, 0);
  strokeWeight(4);
  Rectangle[] faces = opencv.detect();
  for (int i = 0; i < faces.length; i++) {
    //println(faces[i].x + "," + faces[i].y);
    rect(faces[i].x, faces[i].y, faces[i].width, faces[i].height);
    rSig = faces[i].height;
    ypos = faces[i].y;
    yValueMiddleOfBox = faces[i].y + faces[i].height / 2;  
    int delta = trigDist - faces[i].height;
    //the following line draws a second box with the limit distance
    if (trigDist != 0) { 
      rect(faces[i].x - delta / 2, faces[i].y - delta / 2, faces[i].width + delta, trigDist);
      line(faces[i].x, yValueMiddleOfBox, faces[i].x + faces[i].width, yValueMiddleOfBox);
    }
  }
  //This draws a line at the limit height:
  if (trigHeight != 0) {
    line(0, trigHeight, width*2, trigHeight);
  }
  popMatrix();
}

void captureEvent(Capture c) { //important OpenCV stuff
  c.read();
}

void mouseReleased(){ //check if mouse was released so the switch gets triggered only once
  if(mouseOver(230, 200, 80, 30)) pause = !pause;
}

boolean mouseOver(int xpos, int ypos, int rwidth, int rheight){ 
  //return true if mouse is over a given rectangle
  if(mouseX > xpos && mouseX < xpos+rwidth && 
      mouseY > ypos && mouseY < ypos+rheight) return true;
  else return false;
}

public void setPopup(boolean popEnable){
  if (popEnable) {
    if (f == null) {
      f = new PFrame();
    }
    f.setOpacity(Math.min(MAX_OPACITY, opacity));
    opacity += 0.02;
    this.frameRate(60); //increase frame rate so opacity comes in clean
  } else {
    opacity = 0;
    this.frameRate(CHECKS_PER_SECOND);
  }

  if (f != null) {
    f.setVisible(popEnable);
  }
}

public void loadsettings() {
  String[] arrayOfString = loadStrings(dataPath(SETTINGS_FILE_NAME));
  trigHeight = Integer.parseInt(arrayOfString[0]);
  trigDist = Integer.parseInt(arrayOfString[1]);
}
  
public void savesettings() {
  list[0] = str(trigHeight);
  list[1] = str(trigDist);
  saveStrings(dataPath(SETTINGS_FILE_NAME), list);
}

public class PFrame extends JFrame { // configure popup window and position
  public PFrame() {
    int width = displayWidth; //(int) Math.floor(displayWidth * 0.8);
    int height = displayHeight; //(int) Math.floor(displayHeight * 0.8);
    setBounds(displayWidth / 2 - width / 2, displayHeight / 2 - height / 2, width, height);
    this.setFocusableWindowState(false);
    this.setUndecorated(true);
    this.setAlwaysOnTop(true);
    JLabel label = new JLabel("CHECK YOUR POSTURE!", SwingConstants.CENTER);
    label.setFont(new Font("Sans", Font.PLAIN, 150));
    add(label);
  }
}
