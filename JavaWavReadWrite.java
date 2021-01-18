
import java.io.*;
import java.util.*;

class DataInputStreamLittleEndian
{
  private DataInputStream systemStream;
  public DataInputStreamLittleEndian(DataInputStream systemStream) {
    this.systemStream = systemStream;
  }

  public void close() throws IOException {
    this.systemStream.close();
  }

  public void read(byte[] byteBufferToReadInto) throws IOException {
    // no need to translate to little-endian here
    this.systemStream.read(byteBufferToReadInto);
  }

  public int readInt() throws IOException {
    byte[] bytesLittleEndian = new byte[4];
    this.systemStream.read(bytesLittleEndian);
    long returnValueAsLong = ((bytesLittleEndian[0] & 0xFF)
      | ((bytesLittleEndian[1] & 0xFF) << 8 )
      | ((bytesLittleEndian[2] & 0xFF) << 16)
      | ((bytesLittleEndian[3] & 0xFF) << 24));
    return (int)returnValueAsLong;
  }

  public short readShort() throws IOException {
    byte[] bytesLittleEndian = new byte[2];
    this.systemStream.read(bytesLittleEndian);
    int returnValueAsInt = ((bytesLittleEndian[0] & 0xFF)
      | ((bytesLittleEndian[1] & 0xFF) << 8 ));
    return (short)returnValueAsInt;
  }
}

class DataOutputStreamLittleEndian {
  private DataOutputStream systemStream;
  public DataOutputStreamLittleEndian(DataOutputStream systemStream) {
    this.systemStream = systemStream;
  }

  public void close() throws IOException {
    this.systemStream.close();
  }

  public void writeString(String stringToWrite) throws IOException {
    this.systemStream.writeBytes(stringToWrite);
  }

  public void writeBytes(byte[] bytesToWrite) throws IOException {        
    this.systemStream.write(bytesToWrite, 0, bytesToWrite.length);
  }

  public void writeInt(int intToWrite) throws IOException {
    byte[] intToWriteAsBytesLittleEndian = new byte[] {
      (byte)(intToWrite & 0xFF), 
      (byte)((intToWrite >> 8 ) & 0xFF), 
      (byte)((intToWrite >> 16) & 0xFF), 
      (byte)((intToWrite >> 24) & 0xFF)
      };
      this.systemStream.write(intToWriteAsBytesLittleEndian, 0, 4);
  }

  public void writeShort(short shortToWrite) throws IOException {
    byte[] shortToWriteAsBytesLittleEndian = new byte[] {
      (byte)shortToWrite, (byte)(shortToWrite >>> 8 & 0xFF)
      };
      this.systemStream.write(shortToWriteAsBytesLittleEndian, 0, 2);
  }
}

abstract class Sample {

  public abstract Sample buildFromBytes(byte[] valueAsBytes);
  public abstract Sample buildFromDouble(double valueAsDouble);
  public abstract byte[] convertToBytes();
  public abstract double convertToDouble();

  public static Sample[][] buildManyFromDoubles (SamplingInfo samplingInfo, double[][] input) {
    int numberOfChannels = input.length;
    Sample[][] returnSamples = new Sample[numberOfChannels][];
    int samplesPerChannel = input[0].length;
    for (int c = 0; c < numberOfChannels; c++) {
      returnSamples[c] = new Sample[samplesPerChannel];
    }
    Sample samplePrototype = samplingInfo.samplePrototype();
    for (int s = 0; s < samplesPerChannel; s++) {
      for (int c = 0; c < numberOfChannels; c++) {
        returnSamples[c][s] = samplePrototype.buildFromDouble(input[c][s]);
      }
    }
    return returnSamples;
  }

  public static Sample[][] buildManyFromBytes (SamplingInfo samplingInfo, byte[] bytesToConvert) {
    int numberOfBytes = bytesToConvert.length;
    int numberOfChannels = samplingInfo.numberOfChannels;
    Sample[][] returnSamples = new Sample[numberOfChannels][];
    int bytesPerSample = samplingInfo.bitsPerSample / 8;
    int samplesPerChannel = numberOfBytes / bytesPerSample / numberOfChannels;
    for (int c = 0; c < numberOfChannels; c++) {
      returnSamples[c] = new Sample[samplesPerChannel];
    }
    int b = 0;
    double halfMaxValueForEachSample = Math.pow(2, 8 * bytesPerSample - 1);
    Sample samplePrototype = samplingInfo.samplePrototype();
    byte[] sampleValueAsBytes = new byte[bytesPerSample];
    for (int s = 0; s < samplesPerChannel; s++) {            
      for (int c = 0; c < numberOfChannels; c++) {
        for (int i = 0; i < bytesPerSample; i++) {
          sampleValueAsBytes[i] = bytesToConvert[b];
          b++;
        }
        returnSamples[c][s] = samplePrototype.buildFromBytes(sampleValueAsBytes);
      }
    }
    return returnSamples;
  }

  public static Sample[] concatenateSets(Sample[][] setsToConcatenate) {
    int numberOfSamplesSoFar = 0;
    for (int i = 0; i < setsToConcatenate.length; i++) {
      Sample[] setToConcatenate = setsToConcatenate[i];
      numberOfSamplesSoFar += setToConcatenate.length;
    }
    Sample[] returnValues = new Sample[numberOfSamplesSoFar];
    int s = 0;
    for (int i = 0; i < setsToConcatenate.length; i++) {
      Sample[] setToConcatenate = setsToConcatenate[i];
      for (int j = 0; j < setToConcatenate.length; j++) {
        returnValues[s] = setToConcatenate[j];
        s++;
      }
    }
    return returnValues;
  }

  public static byte[] convertManyToBytes(Sample[][] samplesToConvert, SamplingInfo samplingInfo) {
    byte[] returnBytes = null;
    int numberOfChannels = samplingInfo.numberOfChannels;
    int samplesPerChannel = samplesToConvert[0].length;
    int bitsPerSample = samplingInfo.bitsPerSample;
    int bytesPerSample = bitsPerSample / 8;
    int numberOfBytes = numberOfChannels * samplesPerChannel * bytesPerSample;
    returnBytes = new byte[numberOfBytes];
    double halfMaxValueForEachSample = Math.pow(2, 8 * bytesPerSample - 1);
    int b = 0;
    for (int s = 0; s < samplesPerChannel; s++) {
      for (int c = 0; c < numberOfChannels; c++) {
        Sample sample = samplesToConvert[c][s];
        byte[] sampleAsBytes = sample.convertToBytes();
        for (int i = 0; i < bytesPerSample; i++) {
          returnBytes[b] = sampleAsBytes[i];
          b++;
        }
      }
    }
    return returnBytes;
  }

  public static Sample[] superimposeSets(Sample[][] setsToSuperimpose) {
    int maxSamplesSoFar = 0;
    for (int i = 0; i < setsToSuperimpose.length; i++) {
      Sample[] setToSuperimpose = setsToSuperimpose[i];
      if (setToSuperimpose.length > maxSamplesSoFar) {
        maxSamplesSoFar = setToSuperimpose.length;
      }
    }
    Sample[] returnValues = new Sample[maxSamplesSoFar];
    for (int i = 0; i < setsToSuperimpose.length; i++) {
      Sample[] setToSuperimpose = setsToSuperimpose[i];
      for (int j = 0; j < setToSuperimpose.length; j++) {
        Sample sampleToSuperimpose = setToSuperimpose[j];
        double sampleValueAsDouble = sampleToSuperimpose.convertToDouble();
        if (i > 0) sampleValueAsDouble += returnValues[i].convertToDouble();
        returnValues[i] = sampleToSuperimpose.buildFromDouble(sampleValueAsDouble);
      }
    }
    return returnValues;
  }
}

class Sample16 extends Sample
{
  public static int MAX_VALUE = (int)Math.pow(2, 15);
  public short value;

  public Sample16(short value) {
    this.value = value;
  }

  // Sample members
  public Sample buildFromBytes(byte[] valueAsBytes) {
    short valueAsShort = (short) (((valueAsBytes[0] & 0xFF)) | (short)((valueAsBytes[1] & 0xFF) << 8 ));
    return new Sample16(valueAsShort);
  }

  public Sample buildFromDouble(double valueAsDouble) {
    return new Sample16((short) (valueAsDouble * Short.MAX_VALUE));
  }

  public byte[] convertToBytes() {
    return new byte[] {
      (byte)((this.value) & 0xFF), (byte)((this.value >>> 8 ) & 0xFF),
    };
  }        

  public double convertToDouble() {
    return (double)this.value / MAX_VALUE;
  }
}

class Sample24 extends Sample
{
  public static int MAX_VALUE = (int)Math.pow(2, 23);
  public int value;

  public Sample24(int value) {
    this.value = value;
  }

  // Sample members
  public Sample buildFromBytes(byte[] valueAsBytes) {
    short valueAsShort = (short) (
    ((valueAsBytes[0] & 0xFF))
      | ((valueAsBytes[1] & 0xFF) << 8 )
      | ((valueAsBytes[2] & 0xFF) << 16));
    return new Sample24(valueAsShort);
  }

  public Sample buildFromDouble(double valueAsDouble) {
    return new Sample24((int)(valueAsDouble * MAX_VALUE));
  }

  public byte[] convertToBytes() {
    return new byte[] {
      (byte)((this.value) & 0xFF), 
      (byte)((this.value >>> 8 ) & 0xFF), 
      (byte)((this.value >>> 16) & 0xFF),
    };
  }        

  public double convertToDouble() {
    return (double)this.value / MAX_VALUE;
  }
}

class Sample32 extends Sample {
  public static int MAX_VALUE = (int)Math.pow(2, 31);  
  public int value;

  public Sample32(int value) {
    this.value = value;
  }

  // Sample members
  public Sample addInPlace(Sample other) {
    this.value += ((Sample16)other).value;
    return this;
  }

  public Sample buildFromBytes(byte[] valueAsBytes) {
    short valueAsShort = (short) (
    ((valueAsBytes[0] & 0xFF))
      | ((valueAsBytes[1] & 0xFF) << 8 )
      | ((valueAsBytes[2] & 0xFF) << 16)
      | ((valueAsBytes[3] & 0xFF) << 24));
    return new Sample32(valueAsShort);
  }

  public Sample buildFromDouble(double valueAsDouble) {
    return new Sample32((int)(valueAsDouble * MAX_VALUE));
  }

  public byte[] convertToBytes() {
    return new byte[] {
      (byte)((this.value) & 0xFF), 
      (byte)((this.value >>> 8 ) & 0xFF), 
      (byte)((this.value >>> 16) & 0xFF), 
      (byte)((this.value >>> 24) & 0xFF),
    };
  }

  public double convertToDouble() {
    return (double)this.value / MAX_VALUE;
  }
}

class SamplingInfo {
  public String name;
  public int chunkSize;
  public short formatCode;
  public short numberOfChannels;        
  public int samplesPerSecond;
  public short bitsPerSample;

  public SamplingInfo(
  String name, 
  int chunkSize, 
  short formatCode, 
  short numberOfChannels, 
  int samplesPerSecond, 
  short bitsPerSample) {
    this.name = name;
    this.chunkSize = chunkSize;
    this.formatCode = formatCode;
    this.numberOfChannels = numberOfChannels;
    this.samplesPerSecond = samplesPerSecond;
    this.bitsPerSample = bitsPerSample;
  }

  public static class Instances {
    public static SamplingInfo Default = new SamplingInfo(
    "Default", 
    16, // chunkSize
    (short)1, // formatCode
    (short)1, // numberOfChannels
    44100, // samplesPerSecond
    (short)16 // bitsPerSample
    );
  }

  public int bytesPerSecond() {
    return this.samplesPerSecond * this.numberOfChannels * this.bitsPerSample / 8;
  }

  public Sample samplePrototype() {
    Sample returnValue = null;
    if (this.bitsPerSample == 16) returnValue = new Sample16((short)0);
    else if (this.bitsPerSample == 24) returnValue = new Sample24(0);
    else if (this.bitsPerSample == 32) returnValue = new Sample32(0);
    return returnValue;
  }

  public String toString() {
    String returnValue =
      "<SamplingInfo "
      + "chunkSize='" + this.chunkSize + "' "
      + "formatCode='" + this.formatCode + "' "
      + "numberOfChannels='" + this.numberOfChannels + "' "
      + "samplesPerSecond='" + this.samplesPerSecond + "' "
      + "bitsPerSample='" + this.bitsPerSample + "' "
      + "/>";
    return returnValue;
  }
}

class WavFile {
  public String filePath;
  public SamplingInfo samplingInfo;
  public Sample[][] samplesForChannels;

  public WavFile (String filePath, double[][] samplesForChannels) {// stereo with default parameters, directly save
    this.samplingInfo = new SamplingInfo("wave", 16, (short)1, (short)2, 44100, (short)16);
    setSamplesFromDouble(samplesForChannels);
    writeToFilePath(filePath);
  }

  public WavFile (String filePath, SamplingInfo samplingInfo, Sample[][] samplesForChannels) {
    this.filePath = filePath;
    this.samplingInfo = samplingInfo;
    this.samplesForChannels = samplesForChannels;
  }

  public double[][] samplesAsDouble() {
    double[][] result = new double[samplesForChannels.length][];
    for (int i=0 ; i<samplesForChannels.length ; i++) {
      result[i] = new double[samplesForChannels[i].length];
      for (int j=0 ; j<samplesForChannels[i].length ; j++) {
        result[i][j] = samplesForChannels[i][j].convertToDouble();
      }
    }
    return result;
  }

  public void setSamplesFromDouble(double[][] input) {
   samplingInfo.numberOfChannels = (short)input.length;
   samplesForChannels = Sample.buildManyFromDoubles(samplingInfo, input);
  }

  public static WavFile readFromFilePath (String filePathToReadFrom) {
    WavFile returnValue = null;

    try {
      DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(filePathToReadFrom)));
      DataInputStreamLittleEndian reader;
      reader = new DataInputStreamLittleEndian(dataInputStream);

      byte[] riff = new byte[4];
      reader.read(riff);

      int subchunk2SizePlus36 = reader.readInt();
      byte[] wave = new byte[4];
      reader.read(wave);
      byte[] fmt = new byte[4];
      reader.read(fmt);
      int chunkSize = reader.readInt();
      Short formatCode = reader.readShort();

      // samplingInfo
      Short numberOfChannels = reader.readShort();
      int samplesPerSecond = reader.readInt();
      int bytesPerSecond = reader.readInt();
      Short bytesPerSampleMaybe = reader.readShort();
      Short bitsPerSample = reader.readShort();

      byte[] data = new byte[4];
      reader.read(data);
      int subchunk2Size = reader.readInt();

      byte[] samplesForChannelsMixedAsBytes = new byte[subchunk2Size];
      reader.read(samplesForChannelsMixedAsBytes );

      reader.close();

      SamplingInfo samplingInfo = new SamplingInfo(
      "[from file]", 
      chunkSize, 
      formatCode, 
      numberOfChannels, 
      samplesPerSecond, 
      bitsPerSample);

      Sample[][] samplesForChannels = Sample.buildManyFromBytes(samplingInfo, samplesForChannelsMixedAsBytes);

      returnValue = new WavFile(filePathToReadFrom, samplingInfo, samplesForChannels);
    }

    catch (IOException ex) {
      ex.printStackTrace();
    }            

    return returnValue;
  }

  public void writeToFilePath(String url)
  {
    this.filePath = url;
    writeToFilePath();
  }

  public void writeToFilePath()
  {
    try
    {
      int subchunk2Size = (int) (
      this.samplesForChannels[0].length
        * this.samplingInfo.numberOfChannels
        * this.samplingInfo.bitsPerSample / 8);

      DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(this.filePath)));

      DataOutputStreamLittleEndian writer;
      writer = new DataOutputStreamLittleEndian(dataOutputStream);

      writer.writeString("RIFF");
      writer.writeInt((int)(subchunk2Size + 36));
      writer.writeString("WAVE");

      writer.writeString("fmt ");
      writer.writeInt(this.samplingInfo.chunkSize);
      writer.writeShort(this.samplingInfo.formatCode);
      writer.writeShort((short)this.samplingInfo.numberOfChannels);
      writer.writeInt((int)this.samplingInfo.samplesPerSecond);
      writer.writeInt((int)this.samplingInfo.bytesPerSecond());
      writer.writeShort((short)(this.samplingInfo.numberOfChannels * this.samplingInfo.bitsPerSample / 8));
      writer.writeShort((short)this.samplingInfo.bitsPerSample);

      writer.writeString("data");
      writer.writeInt(subchunk2Size);

      byte[] samplesAsBytes = Sample.convertManyToBytes(this.samplesForChannels, this.samplingInfo);    
      writer.writeBytes(samplesAsBytes);
      writer.close();
    } 
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
