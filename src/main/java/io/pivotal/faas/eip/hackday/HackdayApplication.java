package io.pivotal.faas.eip.hackday;

import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.UserAgentType;
import net.sf.uadetector.service.UADetectorServiceFactory;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

@SpringBootApplication
public class HackdayApplication {

  private static final DefaultHttpClient HTTP_CLIENT = new DefaultHttpClient();

  private static final String MOBILE_TWEET_URL = "https://mobile.twitter.com/pivotalcf/status/875750653481635841";
  private static final String DESKTOP_TWEET_URL = "https://twitter.com/pivotalcf/status/875750653481635841";

  private static final UserAgentStringParser USER_AGENT_STRING_PARSER = UADetectorServiceFactory.getCachingAndUpdatingParser();


  @Bean
  public Function<Flux<String>, Flux<String>> mobileurlprocessor() {
    return new Function<Flux<String>, Flux<String>>() {
      @Override
      public Flux<String> apply(Flux<String> stringFlux) {
        return stringFlux.map(new Function<String, String>() {

          @Override
          public String apply(String s) {
            return MOBILE_TWEET_URL;
          }
        });
      }
    };
  }

  @Bean
  public Function<Flux<String>, Flux<String>> desktopurlprocessor() {
    return new Function<Flux<String>, Flux<String>>() {
      @Override
      public Flux<String> apply(Flux<String> stringFlux) {
        return stringFlux.map(new Function<String, String>() {

          @Override
          public String apply(String s) {
            return DESKTOP_TWEET_URL;
          }
        });
      }
    };
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
  public Function<Flux<String>, Flux<String>> tweeturlrouter()  {
    return new Function<Flux<String>, Flux<String>>() {
      @Override
      public Flux<String> apply(Flux<String> stringFlux) {
        return stringFlux.map(new Function<String, String>() {

          @Override
          public String apply(String s) {
            ReadableUserAgent userAgent = USER_AGENT_STRING_PARSER.parse(s);

            String destinationService = "desktopurlprocessor";
            if(UserAgentType.MOBILE_BROWSER.equals(userAgent.getType())){
              destinationService = "mobileurlprocessor";
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
