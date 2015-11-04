package test.org.blueskiron.goldilocks.membership;

import io.advantageous.boon.json.JsonFactory;
import io.advantageous.boon.json.ObjectMapper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.blueskiron.goldilocks.membership.messages.VoteRequestImpl;
import org.blueskiron.goldilocks.membership.net.sbe.EncodersAndDecoders;

import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class SimpleSerializationBenchmark {

  private static final ObjectMapper mapper = JsonFactory.create();
  private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4092);
  private final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
  private final int load = (int) Math.pow(10.0, 6.0);
  private final List<Object> objectPool = new ArrayList<>(load);
  private final EncodersAndDecoders helper = new EncodersAndDecoders();

  public void setup() {
    long counter = load;
    while (counter > 0) {
      objectPool.add(new VoteRequestImpl("test@localhost:0", 1, counter));
      counter--;
    }
  }
  
  public void order1_warmUp(){
    objectPool.subList(0, 10000).stream().forEach(obj -> System.out.println(obj));
    objectPool.subList(980000, 999999).stream().forEach(obj -> System.out.println(obj));
    System.out.println("Warm up complete");
  }
  
  public void order2_testSbeEncodingAndDecoding() {
    Object[] arr = objectPool.toArray();
    long start = System.currentTimeMillis();
    for (int i = 0; i < arr.length; i++){
      helper.encode(arr[i], directBuffer);
      helper.decode(directBuffer, 0);
    }
    long duration = System.currentTimeMillis() - start;
    System.out.println("=== SBE simple benchmark ===");
    System.out.println("Encoding/Decoding of " + objectPool.size() + " objects has finished in " + duration + " millis");
  }
  
  public void order3_testJsonEncodingAndDecoding(){
    Object[] arr = objectPool.toArray();
  
    long start = System.currentTimeMillis();
    for (int i = 0; i < arr.length; i++){
      String tmp = mapper.toJson(arr[i]);
      mapper.fromJson(tmp);
    }
    long duration = System.currentTimeMillis() - start;
    System.out.println("=== JSON simple benchmark ===");
    System.out.println("Encoding/Decoding of " + objectPool.size() + " objects has finished in " + duration + " millis");
  }
  
  public static void main(String[] args) {
    SimpleSerializationBenchmark instance = new SimpleSerializationBenchmark();
    instance.setup();
    instance.order1_warmUp();
    instance.order2_testSbeEncodingAndDecoding();
    instance.order3_testJsonEncodingAndDecoding();
  }
}
