package io.pivotal.faas.eip.hackday;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.function.Function;

@SpringBootApplication
public class HackdayApplication {

  private static final DefaultHttpClient HTTP_CLIENT = new DefaultHttpClient();


  @Bean
  public Function<Flux<String>, Flux<String>> uppercase() {
    return flux -> flux.map(value -> value.toUpperCase());
  }

  @Bean
  public Function<Flux<String>, Flux<String>> simpleruppercase() {
    return new Function<Flux<String>, Flux<String>>() {
      @Override
      public Flux<String> apply(Flux<String> stringFlux) {
        return stringFlux.map(new Function<String, String>() {

          @Override
          public String apply(String s) {
            return s.toUpperCase();
          }
        });
      }
    };
  }

  @Bean
  public Function<Flux<String>, Flux<String>> digitorletter()  {
    return new Function<Flux<String>, Flux<String>>() {
      @Override
      public Flux<String> apply(Flux<String> stringFlux) {
        return stringFlux.map(new Function<String, String>() {

          @Override
          public String apply(String s) {
            if(1!=s.length()){
              throw new RuntimeException("Expecting a value of length 1 only, but got ["+s+"]");
            }

            String destinationService;
            if(Character.isLetter(s.charAt(0))){
              destinationService = "uppercase";

            } else if(Character.isDigit(s.charAt(0))){
              destinationService = "uppercase";

            }else{
              throw new RuntimeException("Character ["+s+"] is neither a letter or a digit");
            }

            String returnVal;
            HttpPost postToLetterService = new HttpPost("http://localhost:8080/faas/"+destinationService);
            try {
              postToLetterService.setEntity(new StringEntity(s));
              HttpResponse response =  HTTP_CLIENT.execute(postToLetterService);

              OutputStream oStream = new ByteArrayOutputStream();
              response.getEntity().writeTo(oStream);
              returnVal = oStream.toString();
            } catch (UnsupportedEncodingException e) {
              throw new RuntimeException("Failed to init entity for payload ["+s+"]", e);
            } catch (IOException e) {
              throw new RuntimeException("Failed to call downstream service with payload ["+s+"]", e);
            }

            return returnVal;
          }
        });
      }
    };
  }


  public static void main(String[] args) {
    SpringApplication.run(HackdayApplication.class, args);
  }
}
