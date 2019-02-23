import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import gab.opencv.*; 
import processing.video.*; 
import java.awt.*; 
import javax.swing.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class HeadUp2 extends PApplet {






int TIME_LIMIT_SECONDS = 10;
int CHECKS_PER_SECOND = 1;
float MAX_OPACITY = 0.95f;

PImage mind;
PFrame f; // create frame for webcam image
String[] list = new String[2]; // list for settings
int ypos; // the height 
int rSig; // the distance
int almTimer;
int trigHeight = 0; // height limit
int trigDist = 0; // distance limit
boolean alm; // alarm
boolean pause;
Capture video;
OpenCV opencv;
float opacity = 0.1f;

public void setup() {
  frameRate(CHECKS_PER_SECOND);
  
  mind = this.loadImage("mind.png");
  //The next lines are for intitializing the camera and OpenCV
  video = new Capture(this, 640, 480);
  opencv = new OpenCV(this, 640, 480);
  opencv.loadCascade(OpenCV.CASCADE_FRONTALFACE);  
  video.start();
  
  File localFile = new File(dataPath("settings.txt")); // manage settings file
  if(localFile.exists()){ // check for existing file
    println("Loading Settings...");
    loadsettings("settings.txt");
  }
  else{ // create new file
    savesettings("settings.txt");
  }
}

public void draw() {
  getDistance();  // run the OpenCV routine
  if (pause) {
    alm = false;
  }
  if (trigHeight!=0 && trigDist!=0 && !pause) {
    if (rSig > trigDist || ypos > trigHeight) alm = true; //compare values to limits 
    else alm = false;
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
  if(mousePressed && mouseOver(10, 200, 100, 30)) {
    trigDist = rSig + 3; // set trigger distance plus a little extra distance
    savesettings("settings.txt");
    fill(0, 255, 0);
  }
  rect(10, 200, 100, 30);  
  
  noFill();  
  if(mousePressed && mouseOver(120, 200, 100, 30)) {
    trigHeight = ypos+3; // set trigger height
    savesettings("settings.txt");
    fill(0, 255, 0);
  }
  rect(120, 200, 100, 30);
  
  noFill();
  if(pause) fill(0, 255, 0); // this part draws the pause switch
  rect(230, 200, 80, 30);
 
}

public void getDistance() { //OPenCV functions
  //pushmatrix and popmatrix prevents the buttons from beeing scaled with the video
  pushMatrix(); 
  scale(0.5f); // scales the video to the window size
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
    int delta = trigDist-faces[i].height;
    //the following line draws a second box with the limit distance
    if (trigDist!=0) 
    rect(faces[i].x-delta/2, faces[i].y-delta/2, faces[i].width+delta, trigDist);
  }
  //This draws a line at the limit height:
  if (trigHeight!=0) line(0, trigHeight, width*2, trigHeight);
  popMatrix();
}

public void captureEvent(Capture c) { //important OpenCV stuff
  c.read();
}

public void mouseReleased(){ //check if mouse was released so the switch gets triggered only once
  if(mouseOver(230, 200, 80, 30)) pause = !pause;
}

public boolean mouseOver(int xpos, int ypos, int rwidth, int rheight){ 
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
    opacity += 0.02f;
    this.frameRate(60); //increase frame rate so opacity comes in clean
  } else {
    opacity = 0;
    this.frameRate(CHECKS_PER_SECOND);
  }

  if (f != null) {
    f.setVisible(popEnable);
  }
}

public void loadsettings(String paramString){ // load settings from file
  String[] arrayOfString = loadStrings(dataPath(paramString));
  trigHeight = Integer.parseInt(arrayOfString[0]);
  trigDist = Integer.parseInt(arrayOfString[1]);
}
  
public void savesettings(String paramString){ // save settings to file
  list[0] = str(trigHeight);
  list[1] = str(trigDist);
  saveStrings(dataPath(paramString), list);
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
  
  public void draw() {
    textSize(32);
    text("word", 10, 30); 
    fill(0, 102, 153);
    text("word", 10, 60);
    fill(0, 102, 153, 51);
    text("word", 10, 90); 
  }
}
  public void settings() {  size(320, 240); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "HeadUp2" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
