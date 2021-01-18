
import drop.*;
import test.*;

SDrop drop;

PImage im;

float threshold = 0x80;
int sLength = 100000;
float oscillation = 200;
float filter = 95;

float[] mins, maxs; 

ArrayList<Slider> sliders = new ArrayList<Slider>();

boolean allowProcess = false;

boolean detectionInverted = false;

void setup() {
  size(800, 600);
  drop = new SDrop(this);
  Slider thS = new Slider();
  thS.label = "threshold";
  thS.min=0;
  thS.max=0xFF;
  thS.value = threshold;
  thS.pos = new PVector(20, 20);
  sliders.add(thS);
  Slider slS = new Slider();
  slS.label = "sample length";
  slS.min=10;
  slS.max=1000000;
  slS.value = sLength;
  slS.pos = new PVector(20, 50);
  sliders.add(slS);
  Slider osS = new Slider();
  osS.label = "oscillation";
  osS.min=1;
  osS.max=1000;
  osS.value = oscillation;
  osS.pos = new PVector(20, 80);
  sliders.add(osS);
  Slider fiS = new Slider();
  fiS.label = "filter";
  fiS.min=0;
  fiS.max=100;
  fiS.value = filter;
  fiS.pos = new PVector(20, 110);
  sliders.add(fiS);
}

void draw() {
  background(0xFF);
  if (allowProcess) {
    for (Slider s : sliders) s.update();
    threshold = sliders.get(0).value;
    sLength = ceil(sliders.get(1).value);
    oscillation = sliders.get(2).value;
    filter = sliders.get(3).value;
    process();
    image(im, 0, 0, width, height);
    stroke(0, 0x80, 0, 0x80);
    noFill();
    for (int x=0; x<mins.length; x++) line((float)x*width/mins.length, map(mins[x], -1, 1, height, 0), (float)x*width/mins.length, map(maxs[x], -1, 1, height, 0));
    for (Slider s : sliders) s.draw();
    text("CTRL : invert detection", 20, 140);
    text("TAB : export", 20, 170);
  } else {
    fill(0);
    textAlign(CENTER, CENTER);
    text("drag and drop a picture", width/2, height/2);
  }
}

void dropEvent(DropEvent theDropEvent) {
  allowProcess = false;
  im = loadImage(theDropEvent.file().toString());
  allowProcess = true;
}

void process() {
  mins = new float[im.width];
  maxs = new float[im.width];
  for (int x=0; x<im.width; x++) {
    mins[x]=1;
    maxs[x]=-1;
    boolean valueFound = false;
    for (int y=0; y<im.height; y++) {
      float thisValue = map(y, 0, im.height-1, 1, -1);
      float thisBrightness = brightness(im.get(x, y));
      if (thisBrightness<threshold^detectionInverted) {
        mins[x] = min(mins[x], thisValue);
        maxs[x] = max(maxs[x], thisValue);
        valueFound = true;
      }
    }
    if (!valueFound) {
      mins[x]=maxs[x]=0;
    }
  }
}

void keyPressed() {
  if (keyCode==TAB) export();
  if (keyCode==CONTROL) detectionInverted^=true;
}

void export() {
  double[] resultWaveform = new double[sLength];
  double currentValue = 0;
  boolean direction = false;// true = up;
  float switchDirectionEvery = oscillation;
  float switchDirectionCounter = 0;
  double prevValue = 0;
  for (int i=0; i<resultWaveform.length; i++) {
    int pixelX = floor((float)i*im.width/resultWaveform.length);
    if (direction) currentValue = maxs[pixelX];
    else currentValue = mins[pixelX];
    currentValue = currentValue*((double)1-(filter/100))+prevValue*filter/100;
    prevValue = currentValue;
    resultWaveform[i] = currentValue;
    switchDirectionCounter++;
    if (switchDirectionCounter>=switchDirectionEvery) {
      switchDirectionCounter-=switchDirectionEvery;
      direction^=true;
    }
  }
  double[][] resultWaveformStereo = new double[2][resultWaveform.length];
  for (int i=0; i<resultWaveform.length; i++) resultWaveformStereo[0][i] = resultWaveformStereo[1][i] = resultWaveform[i];
  new WavFile(sketchPath("result.wav"), resultWaveformStereo);
}

class Slider {
  String label;
  PVector pos;
  PVector size = new PVector(200, 20);
  float min;
  float max;
  float value;
  void update() {
    if (mousePressed) {
      if (mouseX>=pos.x&&mouseY>=pos.y&&mouseX<=pos.x+size.x&&mouseY<=pos.y+size.y) {
        value = ((float)mouseX-pos.x)*(max-min)/size.x+min;
      }
    }
  }
  void draw() {
    stroke(0x50, 0, 0x90);
    line(pos.x+(value-min)*size.x/(max-min), pos.y, pos.x+(value-min)*size.x/(max-min), pos.y+size.y);
    stroke(0xC0, 0, 0xC0);
    noFill();
    rect(pos.x, pos.y, size.x, size.y);
    fill(0xC0, 0, 0xC0);
    textAlign(LEFT, TOP);
    text(label+" : "+floor(value), pos.x+size.x+10, pos.y);
  }
}
