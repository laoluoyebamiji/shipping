package main.java;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

import java.util.Arrays;
import java.util.HashSet;

/**
 * For the purposes of this project, this class is just boiler plate.
 * This is the class given to lambda for the alexa skill (full package - main.java.HelloWorldRequestStreamHandler)
 *
 * Look at SpeechletMain for logic.
 */
public class HelloWorldRequestStreamHandler extends SpeechletRequestStreamHandler {

    public HelloWorldRequestStreamHandler() {
        super(new SpeechletMain(),
                new HashSet<>(Arrays.asList("amzn1.ask.skill.7e25e36f-c1c4-428c-9e75-7146a8289967")));
    }
}
